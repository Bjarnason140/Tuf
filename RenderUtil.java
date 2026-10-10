package dev.tuff;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Through-walls line renderer for MC 26.2 (Blaze3D / extraction+draw model).
 * Based on the Fabric docs "Rendering in the World" example.
 *
 * Modules call line()/box() during EXTRACTION (world coordinates). Drawing happens later,
 * so everything is stored in immutable records.
 */
public final class RenderUtil {
    private record Line(double x1, double y1, double z1, double x2, double y2, double z2,
                        int argb, boolean fromCrosshair) {}

    private static final RenderPipeline LINES_THROUGH_WALLS = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("tuffclient", "pipeline/lines_through_walls"))
                    .withPrimitiveTopology(PrimitiveTopology.DEBUG_LINES) // always 1px wide
                    .withDepthStencilState(Optional.empty())              // no depth test = see through walls
                    .build());

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final StagedVertexBuffer STAGED =
            new StagedVertexBuffer(() -> "TuffClient Lines", RenderType.SMALL_BUFFER_SIZE);

    private static List<Line> building = new ArrayList<>();
    private static volatile List<Line> ready = List.of();

    private RenderUtil() {}

    /** Touch the class so the pipeline registers during mod init. */
    public static void init() {}

    // ---- extraction phase API ----
    public static void beginExtract() { building = new ArrayList<>(); }
    public static void endExtract() { ready = building; }

    public static void line(double x1, double y1, double z1, double x2, double y2, double z2, int argb) {
        building.add(new Line(x1, y1, z1, x2, y2, z2, argb, false));
    }

    /** Line from the crosshair (just in front of the camera) to a world point. */
    public static void lineFromCrosshair(double x, double y, double z, int argb) {
        building.add(new Line(0, 0, 0, x, y, z, argb, true));
    }

    public static void box(AABB b, int argb) {
        double x1 = b.minX, y1 = b.minY, z1 = b.minZ, x2 = b.maxX, y2 = b.maxY, z2 = b.maxZ;
        line(x1, y1, z1, x2, y1, z1, argb); line(x2, y1, z1, x2, y1, z2, argb);
        line(x2, y1, z2, x1, y1, z2, argb); line(x1, y1, z2, x1, y1, z1, argb);
        line(x1, y2, z1, x2, y2, z1, argb); line(x2, y2, z1, x2, y2, z2, argb);
        line(x2, y2, z2, x1, y2, z2, argb); line(x1, y2, z2, x1, y2, z1, argb);
        line(x1, y1, z1, x1, y2, z1, argb); line(x2, y1, z1, x2, y2, z1, argb);
        line(x2, y1, z2, x2, y2, z2, argb); line(x1, y1, z2, x1, y2, z2, argb);
    }

    // ---- drawing phase ----
    public static void draw(LevelRenderContext context) {
        List<Line> lines = ready;

        RenderPipeline pipeline = LINES_THROUGH_WALLS;
        VertexFormat format = pipeline.getVertexFormatBinding(0);
        if (format == null) return;
        PrimitiveTopology topology = pipeline.getPrimitiveTopology();

        StagedVertexBuffer.Draw draw = STAGED.appendDraw(format, topology, null);

        var camState = context.levelState().cameraRenderState;
        Vec3 cam = camState.pos;
        Vector3f look = new Vector3f(0, 0, -1).rotate(camState.orientation);

        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);
        Matrix4fc pose = poseStack.last().pose();
        VertexConsumer vb = STAGED.getVertexBuilder(draw);

        for (Line l : lines) {
            float a = ((l.argb >>> 24) & 0xFF) / 255f;
            float r = ((l.argb >>> 16) & 0xFF) / 255f;
            float g = ((l.argb >>> 8) & 0xFF) / 255f;
            float b = (l.argb & 0xFF) / 255f;
            double sx = l.x1, sy = l.y1, sz = l.z1;
            if (l.fromCrosshair) {
                sx = cam.x + look.x * 0.5; sy = cam.y + look.y * 0.5; sz = cam.z + look.z * 0.5;
            }
            vb.addVertex(pose, (float) sx, (float) sy, (float) sz).setColor(r, g, b, a);
            vb.addVertex(pose, (float) l.x2, (float) l.y2, (float) l.z2).setColor(r, g, b, a);
        }
        poseStack.popPose();

        STAGED.upload();
        StagedVertexBuffer.ExecuteInfo info = STAGED.getExecuteInfo(draw);
        if (info != null) execute(Minecraft.getInstance(), info, pipeline);
        STAGED.endFrame();
    }

    private static void execute(Minecraft client, StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        RenderTarget mainTarget = client.gameRenderer.mainRenderTarget();
        GpuTextureView colorTexture = mainTarget.getColorTextureView();
        if (colorTexture == null) return;

        try (RenderPass pass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "tuffclient lines", colorTexture, Optional.empty(),
                        mainTarget.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransforms);
            pass.setVertexBuffer(0, info.vertexBuffer().slice());
            pass.setIndexBuffer(info.indexBuffer(), info.indexType());
            pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }

    /** Called from GameRendererMixin when the renderer shuts down. */
    public static void close() { STAGED.close(); }
}
