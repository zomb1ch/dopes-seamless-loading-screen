package dopes.seamlessloading.mixin;

import dopes.seamlessloading.SeamlessBackground;
import dopes.seamlessloading.SeamlessFadeScreen;
import dopes.seamlessloading.SeamlessSession;
import dopes.seamlessloading.config.SeamlessConfig;
import dopes.seamlessloading.config.SeamlessConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin {

	@Shadow
	private LevelLoadingScreen.Reason reason;

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

	/** Optional "loaded / total chunks" counter at the top of the screen. */
	@Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"))
	private void dopes$renderChunkCounter(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		SeamlessConfig config = SeamlessConfigManager.get();
		// isRunning (not isActive): the counter is useful even when there is no image to show.
		if (!config.modEnabled || !config.chunkCounter || !SeamlessSession.isRunning()) {
			return;
		}

		int loaded = SeamlessSession.loadedChunks();
		if (loaded < 0) {
			return;
		}

		LevelLoadingScreen screen = (LevelLoadingScreen) (Object) this;
		Component text = Component.translatable("dopes_seamless_loading_screen.counter", loaded, SeamlessSession.expectedChunks());
		guiGraphics.drawCenteredString(screen.getFont(), text, screen.width / 2, 20, 0xFFFFFFFF);
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
