package dev.tuff.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side spectator. A throwaway (never-spawned) entity becomes the camera entity and we fly it
 * around ourselves. Your real player stays put: KeyboardInputMixin zeroes its input and
 * EntityMixin routes the mouse to this camera.
 */
public class Freecam extends Module {
    public double speed = 0.8;

    private Entity cam;
    private double x, y, z;
    private float yaw, pitch;

    public Freecam() { super("Freecam"); }

    @SuppressWarnings("unchecked")
    private Entity makeCamera() {
        // Registry lookup instead of a constant, so this survives EntityType/EntityTypes shuffling.
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("armor_stand"));
        return new ArmorStand((EntityType<? extends ArmorStand>) type, mc.level);
    }

    @Override
    protected void onEnable() {
        if (mc.level == null || mc.player == null) { forceDisable(); return; }
        Vec3 eye = mc.player.getEyePosition();
        x = eye.x; y = eye.y; z = eye.z;
        yaw = mc.player.getYRot();
        pitch = mc.player.getXRot();
        cam = makeCamera();
        snapOld();
        applyToEntity();
        mc.setCameraEntity(cam);
    }

    @Override
    protected void onDisable() {
        if (mc.player != null) mc.setCameraEntity(mc.player);
        cam = null;
    }

    /** Mouse look, called from EntityMixin. */
    public void look(double dyaw, double dpitch) {
        yaw += (float) dyaw * 0.15f;
        pitch = Mth.clamp(pitch + (float) dpitch * 0.15f, -90f, 90f);
        if (cam != null) { cam.setYRot(yaw); cam.setXRot(pitch); }
    }

    private void applyToEntity() {
        cam.setPos(x, y - cam.getEyeHeight(), z);
        cam.setYRot(yaw);
        cam.setXRot(pitch);
    }

    private void snapOld() {
        cam.xo = cam.getX(); cam.yo = cam.getY(); cam.zo = cam.getZ();
        cam.yRotO = cam.getYRot(); cam.xRotO = cam.getXRot();
    }

    @Override
    public void onTick(Minecraft mc) {
        if (cam == null || cam.level() != mc.level) { // dimension change / respawn
            cam = makeCamera();
            applyToEntity();
            snapOld();
        }
        if (mc.getCameraEntity() != cam) mc.setCameraEntity(cam);

        snapOld(); // previous-tick values for smooth interpolation

        Options o = mc.options;
        Vec3 fwd = Vec3.directionFromRotation(pitch, yaw);
        Vec3 right = Vec3.directionFromRotation(0, yaw + 90);
        Vec3 move = Vec3.ZERO;
        if (o.keyUp.isDown())    move = move.add(fwd);
        if (o.keyDown.isDown())  move = move.subtract(fwd);
        if (o.keyRight.isDown()) move = move.add(right);
        if (o.keyLeft.isDown())  move = move.subtract(right);
        if (o.keyJump.isDown())  move = move.add(0, 1, 0);
        if (o.keyShift.isDown()) move = move.subtract(0, 1, 0);
        if (move.lengthSqr() > 0) {
            double s = speed * (o.keySprint.isDown() ? 2.5 : 1.0);
            move = move.normalize().scale(s);
            x += move.x; y += move.y; z += move.z;
        }
        applyToEntity();
    }
}
