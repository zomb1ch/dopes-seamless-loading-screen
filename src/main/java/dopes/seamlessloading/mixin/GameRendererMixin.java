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
	 *
	 * <p>Hooked at the end of {@code renderLevel} instead of after the call inside {@code render}:
	 * from 26.3 the level is rendered from {@code Minecraft#renderFrame}, so the old call site is gone.
	 */
	@Inject(method = "renderLevel", at = @At("RETURN"))
	private void dopes$captureScreenshot(DeltaTracker deltaTracker, CallbackInfo ci) {
		SeamlessCapture.onFrameRendered();
	}
}
