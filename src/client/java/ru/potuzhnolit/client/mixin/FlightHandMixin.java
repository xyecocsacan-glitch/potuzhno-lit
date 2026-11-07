package ru.potuzhnolit.client.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.potuzhnolit.client.PotuzhnoLitClient;

@Mixin(GameRenderer.class)
public abstract class FlightHandMixin {
    @Inject(method = "renderItemInHand(FZLorg/joml/Matrix4f;)V", at = @At("HEAD"), cancellable = true)
    private void potuzhnolit$hideFpvHand(CallbackInfo ci) {
        // Only the FPV feed has no hands. The player's inventory and ordinary view are unchanged.
        if (PotuzhnoLitClient.remote()) ci.cancel();
    }
}
