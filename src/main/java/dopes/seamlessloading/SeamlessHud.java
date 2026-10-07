package dopes.seamlessloading;

import com.mojang.blaze3d.platform.NativeImage;
import dopes.seamlessloading.config.SeamlessConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.io.InputStream;
import java.util.Optional;

/**
 * The mod's own loading screen furniture: the animated text (and, while a world loads, the animated
 * icon and the progress bar). Replaces the vanilla "Downloading terrain" text, the chunk map and the
 * vanilla progress bar.
 */
public final class SeamlessHud {

	/** Which text to show. */
	public enum Style {
		/** Loading a world: text on the left, icon on the right, progress bar at the bottom. */
		LOADING,
		/** Saving the world while leaving it: text only. */
		SAVING,
		/** Connecting to a server: text only. */
		CONNECTING
	}

	/** One source pixel is drawn as this many GUI pixels. */
	private static final int SCALE = 3;
	/** Equal margin from the left, right and bottom screen edges. */
	private static final int MARGIN = 20;
	/** Thickness of the progress bar. */
	private static final int BAR_HEIGHT = 8;
	/** How long one animation frame is shown. */
	private static final long FRAME_MILLIS = 200L;
	/** How much of the remaining distance the bar covers per tick, which keeps it smooth. */
	private static final float BAR_SMOOTHING = 0.06F;
	/** Opacity of the bar background (semi transparent black). */
	private static final float BAR_BACKGROUND_ALPHA = 0.5F;

	private static boolean loaded;
	private static Sprite loadingText;
	private static Sprite loadingIcon;
	private static Sprite savingText;
	private static Sprite connectingText;

	private static float shownProgress;
	/** When the HUD started fading in, or 0 while it is not shown. */
	private static long fadeStart;

	private SeamlessHud() {
	}

	/** Restarts the bar and the fade, called when a new transition starts. */
	public static void reset() {
		shownProgress = 0.0F;
		fadeStart = 0L;
	}

	/** Moves the shown value towards the target, never jumping and never going backwards. */
	public static void tick(float target) {
		if (target < shownProgress) {
			return;
		}

		shownProgress += (target - shownProgress) * BAR_SMOOTHING;
		if (target - shownProgress < 0.0005F) {
			shownProgress = target;
		}
	}

	/** Whether the bar has reached the end; the loading screen waits for this before closing. */
	public static boolean isFull() {
		return shownProgress >= 0.999F;
	}

	/**
	 * Draws the HUD. {@code alpha} is the opacity coming from the screen it is drawn on; the HUD
	 * additionally fades in over the same duration as the transition screen.
	 */
	public static void render(GuiGraphics graphics, int width, int height, Style style, float alpha) {
		Sprite text = text(style);
		if (text == null) {
			return;
		}

		if (fadeStart == 0L) {
			fadeStart = Util.getMillis();
		}

		float fade = Math.min(alpha, ownFade());
		if (fade <= 0.01F) {
			return;
		}

		int barBottom = height - MARGIN;
		int barTop = barBottom - BAR_HEIGHT;
		int textY = barTop - MARGIN - text.height();

		drawFrame(graphics, text, MARGIN, textY, fade);

		if (style != Style.LOADING) {
			return;
		}

		Sprite icon = icon();
		if (icon != null) {
			int iconY = textY + (text.height() - icon.height()) / 2;
			drawFrame(graphics, icon, width - MARGIN - icon.width(), iconY, fade);
		}

		drawBar(graphics, width, barTop, barBottom, fade);
	}

	/** Fades the HUD in over the same duration as the transition screen. */
	private static float ownFade() {
		int duration = Math.max(1, SeamlessConfigManager.get().transitionFadeDuration);
		return Mth.clamp((float) (Util.getMillis() - fadeStart) / duration, 0.0F, 1.0F);
	}

	private static void drawFrame(GuiGraphics graphics, Sprite sprite, int x, int y, float alpha) {
		int frame = (int) ((Util.getMillis() / FRAME_MILLIS) % sprite.frames());
		// Frames are laid out top to bottom first, then column by column.
		int column = frame / sprite.rows();
		int row = frame % sprite.rows();

		graphics.blit(RenderPipelines.GUI_TEXTURED, sprite.id(), x, y,
				(float) (column * sprite.frameWidth()), (float) (row * sprite.frameHeight()),
				sprite.width(), sprite.height(),
				sprite.frameWidth(), sprite.frameHeight(),
				sprite.sheetWidth(), sprite.sheetHeight(),
				ARGB.white(alpha));
	}

	private static void drawBar(GuiGraphics graphics, int width, int barTop, int barBottom, float alpha) {
		int left = MARGIN;
		int right = width - MARGIN;
		if (right <= left) {
			return;
		}

		graphics.fill(left, barTop, right, barBottom,
				ARGB.color(Math.round(255.0F * BAR_BACKGROUND_ALPHA * alpha), 0x000000));

		int filled = Math.round((right - left) * shownProgress);
		if (filled > 0) {
			graphics.fill(left, barTop, left + filled, barBottom, ARGB.white(alpha));
		}
	}

	private static Sprite text(Style style) {
		load();
		return switch (style) {
			case LOADING -> loadingText;
			case SAVING -> savingText;
			case CONNECTING -> connectingText;
		};
	}

	private static Sprite icon() {
		load();
		return loadingIcon;
	}

	private static void load() {
		if (loaded) {
			return;
		}

		loaded = true;
		loadingText = read("gui/loading_text.png", 37, 7);
		loadingIcon = read("gui/loading_icon.png", 5, 7);
		savingText = read("gui/saving_text.png", 33, 7);
		connectingText = read("gui/connecting_text.png", 52, 7);
	}

	private static Sprite read(String path, int frameWidth, int frameHeight) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.getResourceManager() == null || minecraft.getTextureManager() == null) {
			return null;
		}

		Identifier id = Identifier.fromNamespaceAndPath(DopesSeamlessLoadingScreen.MOD_ID, path);
		try {
			Optional<Resource> optional = minecraft.getResourceManager().getResource(id);
			if (optional.isEmpty()) {
				DopesSeamlessLoadingScreen.LOGGER.warn("[Seamless] Missing the sprite {}", path);
				return null;
			}

			NativeImage image;
			try (InputStream in = optional.get().open()) {
				image = NativeImage.read(in);
			}

			minecraft.getTextureManager().register(id, new DynamicTexture(() -> "Seamless sprite", image));
			return new Sprite(id, image.getWidth(), image.getHeight(), frameWidth, frameHeight);
		} catch (Exception e) {
			DopesSeamlessLoadingScreen.LOGGER.error("[Seamless] Unable to read the sprite {}", path, e);
			return null;
		}
	}

	/** One sprite sheet: frames are laid out top to bottom, then left to right. */
	private record Sprite(Identifier id, int sheetWidth, int sheetHeight, int frameWidth, int frameHeight) {

		int columns() {
			return Math.max(1, this.sheetWidth / this.frameWidth);
		}

		int rows() {
			return Math.max(1, this.sheetHeight / this.frameHeight);
		}

		int frames() {
			return columns() * rows();
		}

		int width() {
			return this.frameWidth * SCALE;
		}

		int height() {
			return this.frameHeight * SCALE;
		}
	}
}
