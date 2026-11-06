package ru.potuzhnolit.client.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.potuzhnolit.client.FlightCamera;

@Mixin(MouseHandler.class)
public abstract class FlightMouseMixin {
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void potuzhnolit$mouseAim(LocalPlayer player, double dx, double dy) {
        if (!FlightCamera.turn(dx, dy)) player.turn(dx, dy);
    }
}
