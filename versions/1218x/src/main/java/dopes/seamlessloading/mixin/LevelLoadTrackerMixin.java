package dopes.seamlessloading.mixin;

import dopes.seamlessloading.SeamlessSession;
import net.minecraft.client.multiplayer.LevelLoadStatusManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21.5 - 1.21.8 equivalent of the LevelLoadTracker hook: the client keeps the loading screen open
 * with LevelLoadStatusManager here, so the same "hold it back" logic is applied to levelReady.
 */
@Mixin(LevelLoadStatusManager.class)
public abstract class LevelLoadTrackerMixin {

@Inject(method = "levelReady", at = @At("RETURN"), cancellable = true)
private void dopes$holdLoadingScreen(CallbackInfoReturnable<Boolean> cir) {
if (cir.getReturnValueZ() && SeamlessSession.shouldHoldLoadingScreen()) {
cir.setReturnValue(false);
}
}
}
