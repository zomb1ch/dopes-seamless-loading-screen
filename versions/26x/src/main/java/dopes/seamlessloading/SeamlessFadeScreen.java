package dopes.seamlessloading;

import dopes.seamlessloading.config.SeamlessConfigManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * Shown for a short moment after the loading screen closes: the screenshot fades out and reveals
 * the world behind it.
 */
public class SeamlessFadeScreen extends Screen {

	private final float durationMs;
	private final long startMillis;
	private boolean finished;

	public SeamlessFadeScreen() {
		super(Component.empty());
		this.durationMs = Math.max(1, SeamlessConfigManager.get().fadeDuration);
		this.startMillis = Util.getMillis();
		DopesSeamlessLoadingScreen.LOGGER.info("[Seamless] Loading screen finished after {} ms, fading the screenshot out over {} ms",
				Math.round(SeamlessSession.elapsedMs()), Math.round(this.durationMs));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
		float progress = Mth.clamp((float) (Util.getMillis() - startMillis) / durationMs, 0.0F, 1.0F);
		SeamlessBackground.renderFade(guiGraphics, this.width, this.height, 1.0F - progress);
	}

	@Override
	public void tick() {
		if (!this.finished && Util.getMillis() - startMillis >= durationMs) {
			this.finished = true;
			if (this.minecraft != null && this.minecraft.screen == this) {
				this.minecraft.setScreen(null);
			}
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
		// The world is already rendered behind this screen, we only fade our image out.
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void removed() {
		super.removed();
		SeamlessSession.end();
	}
}
