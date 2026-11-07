package ru.potuzhno.cinema.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.potuzhno.cinema.CinemaClient;

@Mixin(Camera.class)
public abstract class CinemaCameraMixin {
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "setup", at = @At("TAIL"))
    private void potuzhno$cinema(Level level, Entity entity, boolean detached,
                                 boolean reversed, float partialTick, CallbackInfo ci) {
        CinemaClient.Pose pose = CinemaClient.camera(partialTick);
        if (pose != null) {
            setPosition(pose.position());
            setRotation(pose.yaw(), pose.pitch());
        }
    }
}
