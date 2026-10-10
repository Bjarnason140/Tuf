package dev.tuff.mixin;

import dev.tuff.TuffClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    // Mouse movement arrives here for the player. While freecam is on, steer the camera instead.
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void tuff$turn(double yRot, double xRot, CallbackInfo ci) {
        if ((Object) this == Minecraft.getInstance().player && TuffClient.FREECAM.isEnabled()) {
            TuffClient.FREECAM.look(yRot, xRot);
            ci.cancel();
        }
    }
}
