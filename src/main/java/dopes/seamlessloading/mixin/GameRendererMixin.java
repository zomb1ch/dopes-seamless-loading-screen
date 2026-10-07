package dopes.seamlessloading.mixin;

import dopes.seamlessloading.SeamlessCapture;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

	/**
	 * Right after the world has been rendered (and before the HUD is drawn) the main render target
	 * contains exactly the picture we want to save.
	 */
	@Inject(
			method = "render",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER)
	)
	private void dopes$captureScreenshot(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
		SeamlessCapture.onFrameRendered();
	}
}
