package dev.tuff.module;

import dev.tuff.RenderUtil;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;

/** Line from your crosshair to every other nearby player. */
public class Tracers extends Module {
    public static double maxDistance = 256;

    public Tracers() { super("Player Tracers"); }

    @Override
    public void onExtract() {
        if (mc.level == null || mc.player == null) return;
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 me = mc.player.position();

        for (AbstractClientPlayer p : mc.level.players()) {
            if (p == mc.player) continue;
            Vec3 pos = p.getPosition(pt);
            double dist = me.distanceTo(pos);
            if (dist > maxDistance) continue;
            int color = dist < 32 ? 0xFFFF3030 : dist < 96 ? 0xFFFFFF00 : 0xFFFFFFFF;
            RenderUtil.lineFromCrosshair(pos.x, pos.y + p.getBbHeight() / 2.0, pos.z, color);
            RenderUtil.box(p.getBoundingBox().move(pos.subtract(p.position())), color);
        }
    }
}
