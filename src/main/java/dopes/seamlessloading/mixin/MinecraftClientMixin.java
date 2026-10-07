package dopes.seamlessloading.mixin;

import dopes.seamlessloading.SeamlessCapture;
import dopes.seamlessloading.SeamlessCurtain;
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

	/**
	 * Remembers which singleplayer world is being loaded and shows the auxiliary transition screen.
	 * The world is only really loaded once that screen has fully faded in.
	 */
	@Inject(method = "doWorldLoad", at = @At("HEAD"), cancellable = true)
	private void dopes$beginSingleplayerSession(LevelStorageSource.LevelStorageAccess levelStorageAccess,
			PackRepository packRepository, WorldStem worldStem, boolean bl, CallbackInfo ci) {
		// The deferred action re-enters this method, and the session is already set up by then.
		if (!SeamlessCurtain.isReplaying()) {
			String worldId = levelStorageAccess.getLevelId();
			SeamlessSession.setSingleplayerWorldId(worldId);
			SeamlessSession.begin(SeamlessScreenshots.singleplayer(worldId));
		}

		if (SeamlessCurtain.beginEnter("entering the world",
				() -> Minecraft.getInstance().doWorldLoad(levelStorageAccess, packRepository, worldStem, bl))) {
			ci.cancel();
		}
	}

	/** "Save and Quit to Title" in the pause menu. */
	@Inject(method = "disconnectFromWorld", at = @At("HEAD"), cancellable = true)
	private void dopes$captureOnQuitToTitle(Component component, CallbackInfo ci) {
		if (SeamlessCurtain.beginLeave("quitting to the title screen",
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

	/**
	 * Advances the auxiliary transition screen. This is deliberately not done from {@code Screen#tick}:
	 * the deferred action may load a world, and the world load loop runs the loading screen tick in
	 * between the before/after screen tick events of the Fabric screen API, which then ends up firing
	 * its after-tick event with a null screen.
	 */
	@Inject(method = "tick", at = @At("HEAD"))
	private void dopes$curtainTick(CallbackInfo ci) {
		SeamlessCurtain.tick();
	}
}
