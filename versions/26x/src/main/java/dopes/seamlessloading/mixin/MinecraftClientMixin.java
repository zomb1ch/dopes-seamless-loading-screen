package dopes.seamlessloading.mixin;

import dopes.seamlessloading.SeamlessCapture;
import dopes.seamlessloading.SeamlessCurtain;
import dopes.seamlessloading.SeamlessScreenshots;
import dopes.seamlessloading.SeamlessSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {

	/**
	 * Remembers which singleplayer world is being loaded and shows the auxiliary transition screen.
	 * The world is only really loaded once that screen has fully faded in.
	 *
	 * <p>When the world was picked from the world list the overlay has already been started (see
	 * {@code WorldOpenFlowsMixin}), and the load simply continues.
	 */
	@Inject(method = "doWorldLoad", at = @At("HEAD"), cancellable = true)
	private void dopes$beginSingleplayerSession(LevelStorageSource.LevelStorageAccess levelStorageAccess,
			PackRepository packRepository, WorldStem worldStem, Optional<GameRules> gameRules, boolean bl,
			CallbackInfo ci) {
		// The deferred action re-enters this method, and the overlay is already running by then.
		if (SeamlessCurtain.isReplaying()) {
			return;
		}

		// The overlay was prepared when the world was picked or created (see WorldOpenFlowsMixin).
		// Paths that do not go through it (e.g. quick play) set the session up here instead.
		if (!SeamlessCurtain.isPrepared()) {
			String worldId = levelStorageAccess.getLevelId();
			SeamlessSession.setSingleplayerWorldId(worldId);
			SeamlessSession.begin(SeamlessScreenshots.singleplayer(worldId));
		}

		if (SeamlessCurtain.beginEnter("entering the world",
				() -> Minecraft.getInstance().doWorldLoad(levelStorageAccess, packRepository, worldStem, gameRules, bl))) {
			ci.cancel();
		}
	}

	/**
	 * Skips the vanilla "Reading world data" / "Loading resources" / "Saving world" screens while the
	 * transition overlay is up, so only the screenshot is visible and nothing flashes underneath it.
	 */
	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	private void dopes$hideIntermediateScreens(Screen screen, CallbackInfo ci) {
		if (SeamlessCurtain.shouldHideScreen(screen)) {
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
