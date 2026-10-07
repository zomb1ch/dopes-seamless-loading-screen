package dopes.seamlessloading.mixin;

import dopes.seamlessloading.SeamlessBackground;
import dopes.seamlessloading.SeamlessCurtain;
import dopes.seamlessloading.SeamlessFadeScreen;
import dopes.seamlessloading.SeamlessHud;
import dopes.seamlessloading.SeamlessSession;
import dopes.seamlessloading.config.SeamlessConfig;
import dopes.seamlessloading.config.SeamlessConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin {

	@Shadow
	private LevelLoadingScreen.Reason reason;

	@Shadow
	private LevelLoadTracker loadTracker;

	/**
	 * Advances the transition overlay while the loading screen is up. The world load loop ticks the
	 * loading screen without running the client tick, so without this the overlay would never fade
	 * out over the loading screen. Deferred actions are never run from here.
	 */
	@Inject(method = "tick", at = @At("HEAD"))
	private void dopes$curtainTick(CallbackInfo ci) {
		SeamlessCurtain.tickLoadingScreen();
	}

	/**
	 * Draws the screenshot on top of the vanilla background (panorama + blur + dirt texture), so
	 * the image fades in from a blurred background.
	 */
	@Inject(method = "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"))
	private void dopes$renderScreenshot(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (this.reason != LevelLoadingScreen.Reason.OTHER || !SeamlessSession.isActive()) {
			return;
		}

		LevelLoadingScreen screen = (LevelLoadingScreen) (Object) this;
		SeamlessBackground.render(guiGraphics, screen.width, screen.height);
	}

	/**
	 * Replaces the whole vanilla loading screen content (the chunk map, the "Downloading terrain"
	 * text and the vanilla progress bar) with our own HUD. The optional chunk counter is kept.
	 */
	@Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"), cancellable = true)
	private void dopes$renderHud(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (this.reason != LevelLoadingScreen.Reason.OTHER || !SeamlessSession.isRunning()) {
			return;
		}

		LevelLoadingScreen screen = (LevelLoadingScreen) (Object) this;
		SeamlessHud.render(guiGraphics, screen.width, screen.height, SeamlessHud.Style.LOADING, progress());
		renderChunkCounter(guiGraphics, screen);
		ci.cancel();
	}

	/** Optional "loaded / total chunks" counter at the top of the screen. */
	private void renderChunkCounter(GuiGraphics guiGraphics, LevelLoadingScreen screen) {
		SeamlessConfig config = SeamlessConfigManager.get();
		if (!config.modEnabled || !config.chunkCounter) {
			return;
		}

		int loaded = SeamlessSession.loadedChunks();
		if (loaded < 0) {
			return;
		}

		Component text = Component.translatable("dopes_seamless_loading_screen.counter", loaded, SeamlessSession.expectedChunks());
		guiGraphics.drawCenteredString(screen.getFont(), text, screen.width / 2, 20, 0xFFFFFFFF);
	}

	/**
	 * Progress of the bar: the first half covers loading the world, the second half covers waiting
	 * for all chunks, so the bar only fills up completely once the world is really ready.
	 */
	private float progress() {
		float world = this.loadTracker != null && this.loadTracker.hasProgress() ? this.loadTracker.serverProgress() : 0.0F;
		if (world < 1.0F) {
			return world * 0.5F;
		}

		int loaded = SeamlessSession.loadedChunks();
		int expected = SeamlessSession.expectedChunks();
		float chunks = loaded < 0 || expected <= 0 ? 0.0F : Mth.clamp((float) loaded / expected, 0.0F, 1.0F);
		return 0.5F + chunks * 0.5F;
	}

	/** Replaces the vanilla "close and show the world" with our fade animation. */
	@Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
	private void dopes$fadeOut(CallbackInfo ci) {
		if (!SeamlessSession.isActive()) {
			return;
		}

		if (SeamlessConfigManager.get().fadeDuration <= 0) {
			SeamlessSession.end();
			return;
		}

		Minecraft.getInstance().setScreen(new SeamlessFadeScreen());
		ci.cancel();
	}
}
