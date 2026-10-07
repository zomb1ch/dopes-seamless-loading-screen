package dopes.seamlessloading.mixin;

import dopes.seamlessloading.SeamlessCapture;
import dopes.seamlessloading.SeamlessScreenshots;
import dopes.seamlessloading.SeamlessSession;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {

	/** Remembers which singleplayer world is being loaded, so the right screenshot can be shown. */
	@Inject(method = "doWorldLoad", at = @At("HEAD"))
	private void dopes$beginSingleplayerSession(LevelStorageSource.LevelStorageAccess levelStorageAccess,
			PackRepository packRepository, WorldStem worldStem, boolean bl, CallbackInfo ci) {
		String worldId = levelStorageAccess.getLevelId();
		SeamlessSession.setSingleplayerWorldId(worldId);
		SeamlessSession.begin(SeamlessScreenshots.singleplayer(worldId));
	}

	/** "Save and Quit to Title" in the pause menu. */
	@Inject(method = "disconnectFromWorld", at = @At("HEAD"), cancellable = true)
	private void dopes$captureOnQuitToTitle(Component component, CallbackInfo ci) {
		if (SeamlessCapture.request("quitting to the title screen",
				() -> Minecraft.getInstance().disconnectFromWorld(component))) {
			ci.cancel();
		}
	}

	/** Closing the game window. */
	@Inject(method = "stop", at = @At("HEAD"), cancellable = true)
	private void dopes$captureOnWindowClose(CallbackInfo ci) {
		if (SeamlessCapture.request("closing the game", () -> Minecraft.getInstance().stop())) {
			ci.cancel();
		}
	}
}
