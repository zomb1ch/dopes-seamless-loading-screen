package dopes.seamlessloading.mixin;

import dopes.seamlessloading.SeamlessCapture;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Right after the world has been rendered (and before the HUD is drawn) the main render target
 * contains exactly the picture we want to save.
 *
 * <p>26.3 dropped the {@code DeltaTracker} parameter from {@code GameRenderer#renderLevel} (and from
 * {@code render}), so unlike the older versions this handler takes no parameters.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

	@Inject(method = "renderLevel", at = @At("RETURN"))
	private void dopes$captureScreenshot(CallbackInfo ci) {
		SeamlessCapture.onFrameRendered();
	}
}
