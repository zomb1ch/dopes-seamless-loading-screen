package dopes.seamlessloading.mixin;

import dopes.seamlessloading.SeamlessSession;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelLoadTracker.class)
public abstract class LevelLoadTrackerMixin {

	/**
	 * Keeps the loading screen (and the "player loaded" notification) back while the mod still
	 * wants to show the screenshot / wait for the chunks.
	 */
	@Inject(method = "isLevelReady", at = @At("RETURN"), cancellable = true)
	private void dopes$holdLoadingScreen(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && SeamlessSession.shouldHoldLoadingScreen()) {
			cir.setReturnValue(false);
		}
	}
}
