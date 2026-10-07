package dopes.seamlessloading;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;
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
	private static final int MARGIN = 10;
	/** Thickness of the progress bar. */
	private static final int BAR_HEIGHT = 8;
	/** How long one animation frame is shown. */
	private static final long FRAME_MILLIS = 100L;
	/** How much of the remaining distance the bar covers per frame, which keeps it smooth. */
	private static final float BAR_SMOOTHING = 0.06F;

	private static final int BAR_BACKGROUND = 0x80000000;
	private static final int BAR_FILL = 0xFFFFFFFF;

	private static boolean loaded;
	private static Sprite loadingText;
	private static Sprite loadingIcon;
	private static Sprite savingText;
	private static Sprite connectingText;

	private static float shownProgress;

	private SeamlessHud() {
	}

	/** Restarts the bar, called when a new transition starts. */
	public static void reset() {
		shownProgress = 0.0F;
	}

	/**
	 * Draws the HUD. {@code progress} is the target fill of the bar (0..1) and is only used for
	 * {@link Style#LOADING}.
	 */
	public static void render(GuiGraphics graphics, int width, int height, Style style, float progress) {
		Sprite text = text(style);
		if (text == null) {
			return;
		}

		int barBottom = height - MARGIN;
		int barTop = barBottom - BAR_HEIGHT;
		int textY = barTop - MARGIN - text.height();

		drawFrame(graphics, text, MARGIN, textY);

		if (style != Style.LOADING) {
			return;
		}

		Sprite icon = icon();
		if (icon != null) {
			int iconY = textY + (text.height() - icon.height()) / 2;
			drawFrame(graphics, icon, width - MARGIN - icon.width(), iconY);
		}

		drawBar(graphics, width, barTop, barBottom, smooth(progress));
	}

	private static void drawFrame(GuiGraphics graphics, Sprite sprite, int x, int y) {
		int frame = (int) ((Util.getMillis() / FRAME_MILLIS) % sprite.frames());
		int column = frame % sprite.columns();
		int row = frame / sprite.columns();

		graphics.blit(RenderPipelines.GUI_TEXTURED, sprite.id(), x, y,
				(float) (column * sprite.frameWidth()), (float) (row * sprite.frameHeight()),
				sprite.width(), sprite.height(),
				sprite.frameWidth(), sprite.frameHeight(),
				sprite.sheetWidth(), sprite.sheetHeight(),
				ARGB.white(1.0F));
	}

	private static void drawBar(GuiGraphics graphics, int width, int barTop, int barBottom, float progress) {
		int left = MARGIN;
		int right = width - MARGIN;
		if (right <= left) {
			return;
		}

		graphics.fill(left, barTop, right, barBottom, BAR_BACKGROUND);

		int filled = Math.round((right - left) * progress);
		if (filled > 0) {
			graphics.fill(left, barTop, left + filled, barBottom, BAR_FILL);
		}
	}

	/** Moves the shown value towards the target, never jumping and never going backwards. */
	private static float smooth(float target) {
		if (target < shownProgress) {
			return shownProgress;
		}

		shownProgress += (target - shownProgress) * BAR_SMOOTHING;
		if (target - shownProgress < 0.0005F) {
			shownProgress = target;
		}

		return shownProgress;
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
