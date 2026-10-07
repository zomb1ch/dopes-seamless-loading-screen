package dopes.seamlessloading.mixin;

import dopes.seamlessloading.SeamlessHud;
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
	 * wants to show the screenshot / wait for the chunks / let the progress bar catch up.
	 */
	@Inject(method = "isLevelReady", at = @At("RETURN"), cancellable = true)
	private void dopes$holdLoadingScreen(CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ()) {
			return;
		}

		if (SeamlessSession.shouldHoldLoadingScreen()) {
			cir.setReturnValue(false);
		} else if (SeamlessSession.isRunning() && !SeamlessHud.isFull()) {
			// The world is ready, but the bar has not caught up yet: let it finish smoothly.
			cir.setReturnValue(false);
		}
	}
}
