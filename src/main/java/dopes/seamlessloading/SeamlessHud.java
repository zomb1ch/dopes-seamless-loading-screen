package dopes.seamlessloading;

import com.mojang.blaze3d.platform.NativeImage;
import dopes.seamlessloading.config.SeamlessConfig;
import dopes.seamlessloading.config.SeamlessConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.io.InputStream;
import java.util.Arrays;
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
	private static final int SCALE = 5;
	/** Equal margin from the left, right and bottom screen edges. */
	private static final int MARGIN = 20;
	/** Gap between the progress bar and the text / icon above it. */
	private static final int TEXT_GAP = 10;
	/** Gap between the chunk counter and the loading icon; a bit wider than {@link #TEXT_GAP}. */
	private static final int COUNTER_GAP = TEXT_GAP + 6;
	/**
	 * Optical centre of the icon's visible art, in px below the top of the frame. The frame is
	 * 5x7 source px (25x35 GUI px) but only rows 0-4 have pixels, so the visible block is 25 px
	 * tall and its centre is ~12 px below iconY, not the 17.5 px of the whole frame.
	 */
	private static final int ICON_VISIBLE_CENTER = 12;
	/** Optical nudge: the digits look centred slightly lower than the sprite's geometric centre. */
	private static final int COUNTER_NUDGE_DOWN = 2;
	/** Thickness of the progress bar. */
	private static final int BAR_HEIGHT = 8;
	/** How long one animation frame is shown. */
	private static final long FRAME_MILLIS = 100L;
	/** How much of the remaining distance the bar covers per second. */
	private static final float BAR_SMOOTH_PER_SECOND = 2.0F;
	/** Longest step the bar may take at once, so a lag spike cannot make it jump. */
	private static final float BAR_MAX_STEP_SECONDS = 0.2F;
	/** Opacity of the bar background (semi transparent black). */
	private static final float BAR_BACKGROUND_ALPHA = 0.5F;
	/** How long the bar may take to reach the end once the world is ready. */
	private static final float FINISH_SECONDS = 0.4F;

	private static boolean loaded;
	private static Sprite loadingText;
	private static Sprite loadingIcon;
	private static Sprite savingText;
	private static Sprite connectingText;

	private static float shownProgress;
	/** Where the bar is heading, remembered so it can also be advanced while rendering. */
	private static float targetProgress;
	/** When the HUD started fading in, or 0 while it is not shown. */
	private static long fadeStart;
	/** When the bar was advanced the last time, used to move it with the clock. */
	private static long lastAdvance;
	/** {@code true} once the world is ready and the bar only has to catch up. */
	private static boolean finishing;
	/** Seconds spent catching up since {@link #finishing} was set. */
	private static float finishElapsed;

	private SeamlessHud() {
	}

	/** Restarts the bar and the fade, called when a new transition starts. */
	public static void reset() {
		shownProgress = 0.0F;
		targetProgress = 0.0F;
		fadeStart = 0L;
		lastAdvance = 0L;
		finishing = false;
		finishElapsed = 0.0F;
		SeamlessTips.reset();
	}

	/**
	 * The world is ready, so the bar only has to catch up now. Called right before the loading screen
	 * would close; it is held open until {@link #isFull()} is {@code true}.
	 */
	public static void startFinishing() {
		if (!finishing) {
			finishing = true;
			finishElapsed = 0.0F;
		}
	}

	/** Remembers where the bar should go. It is moved by {@link #advance()}, not by this call. */
	public static void tick(float target) {
		targetProgress = target;
		advance();
	}

	/**
	 * Moves the bar towards its target. This is driven by the clock instead of by ticks or frames, so
	 * a low tick rate or a heavy frame cannot make the bar stall, and calling it from both the tick
	 * and the render is harmless.
	 */
	private static void advance() {
		long now = Util.getMillis();
		float delta = lastAdvance == 0L ? 0.0F : Math.min(BAR_MAX_STEP_SECONDS, (now - lastAdvance) / 1000.0F);
		lastAdvance = now;

		if (finishing) {
			// The world is ready: reach the end within FINISH_SECONDS, but without jumping there in a
			// single step. The floor is what guarantees that the loading screen always closes.
			finishElapsed += delta;
			float floor = Mth.clamp(finishElapsed / FINISH_SECONDS, 0.0F, 1.0F);
			shownProgress = Math.max(floor, towards(shownProgress, 1.0F, delta));
			if (shownProgress > 0.999F) {
				shownProgress = 1.0F;
			}

			return;
		}

		if (targetProgress > shownProgress) {
			shownProgress = towards(shownProgress, targetProgress, delta);
			if (targetProgress - shownProgress < 0.0005F) {
				shownProgress = targetProgress;
			}
		}
	}

	/** Moves {@code from} towards {@code to} by the fraction that {@code delta} seconds allow. */
	private static float towards(float from, float to, float delta) {
		return from + (to - from) * Math.min(1.0F, BAR_SMOOTH_PER_SECOND * delta);
	}

	/** Whether the bar has reached the end; the loading screen waits for this before closing. */
	public static boolean isFull() {
		advance();
		return shownProgress >= 0.999F;
	}

	/**
	 * Whether the mod's own HUD replaces the vanilla loading screen furniture. When this is off the
	 * callers must leave the vanilla screen alone.
	 */
	public static boolean isEnabled() {
		SeamlessConfig config = SeamlessConfigManager.get();
		return config.modEnabled && config.customLoadingScreen;
	}

	/**
	 * Draws the HUD. {@code alpha} is the opacity coming from the screen it is drawn on; the HUD
	 * additionally fades in over the same duration as the transition screen.
	 */
	public static void render(GuiGraphics graphics, int width, int height, Style style, float alpha) {
		if (!isEnabled()) {
			return;
		}

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
		int textY = barTop - TEXT_GAP - text.height();

		drawFrame(graphics, text, MARGIN, textY, fade);
		if (style != Style.SAVING) {
			SeamlessTips.render(graphics, width, fade);
		}

		if (style != Style.LOADING) {
			return;
		}

		Sprite icon = icon();
		int iconX = width - MARGIN - icon.width();
		int iconY = textY + (text.height() - icon.height()) / 2;
		if (icon != null) {
			drawFrame(graphics, icon, iconX, iconY, fade);
		}

		// Chunk counter sits to the left of the loading icon, same font as the tips,
		// same gap (TEXT_GAP) as between the icon and the progress bar.
		if (SeamlessConfigManager.get().chunkCounter) {
			drawChunkCounter(graphics, iconX, iconY, fade);
		}

		advance();
		drawBar(graphics, width, barTop, barBottom, fade);
	}

	/** Fades the HUD in over the same duration as the transition screen. */
	private static float ownFade() {
		int duration = Math.max(1, SeamlessConfigManager.get().transitionFadeDuration);
		return Mth.clamp((float) (Util.getMillis() - fadeStart) / duration, 0.0F, 1.0F);
	}

	private static void drawFrame(GuiGraphics graphics, Sprite sprite, int x, int y, float alpha) {
		int frame = sprite.sheetFrame((int) ((Util.getMillis() / FRAME_MILLIS) % sprite.frames()));
		// Frames are laid out left to right first, then row by row.
		int column = frame % sprite.columns();
		int row = frame / sprite.columns();

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

	/**
	 * Draws the optional chunk counter to the left of the loading icon, using the uni tip font,
	 * digits only (no "чанки" word), scaled up to match the icon size.
	 */
	private static void drawChunkCounter(GuiGraphics graphics, int iconX, int iconY, float alpha) {
		int loaded = SeamlessSession.loadedChunks();
		int expected = SeamlessSession.expectedChunks();
		if (loaded < 0 || expected <= 0) {
			return;
		}

		String text = loaded + " / " + expected;
		int width = SeamlessTips.chunkCounterWidth(text);
		int x = iconX - COUNTER_GAP - width;
		// Centre the digits on the icon's visible block, not on the whole 7-row frame.
		int y = iconY + ICON_VISIBLE_CENTER + COUNTER_NUDGE_DOWN
				- SeamlessTips.COUNTER_DIGITS_HEIGHT / 2;
		SeamlessTips.drawChunkCounterText(graphics, text, x, y, alpha);
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

			int[] order = framesWithPixels(image, frameWidth, frameHeight);
			if (order.length == 0) {
				DopesSeamlessLoadingScreen.LOGGER.warn("[Seamless] The sprite {} has no visible frames", path);
				return null;
			}

			minecraft.getTextureManager().register(id, new DynamicTexture(() -> "Seamless sprite", image));
			return new Sprite(id, image.getWidth(), image.getHeight(), frameWidth, frameHeight, order);
		} catch (Exception e) {
			DopesSeamlessLoadingScreen.LOGGER.error("[Seamless] Unable to read the sprite {}", path, e);
			return null;
		}
	}

	/**
	 * The frames of a sheet that contain at least one visible pixel, in sheet order. Sheets often have
	 * unused cells (usually at the end, but sometimes in the middle) and animating over those would
	 * show empty gaps, so they are left out here.
	 */
	private static int[] framesWithPixels(NativeImage image, int frameWidth, int frameHeight) {
		int columns = Math.max(1, image.getWidth() / frameWidth);
		int rows = Math.max(1, image.getHeight() / frameHeight);
		int[] order = new int[columns * rows];
		int count = 0;

		for (int index = 0; index < order.length; index++) {
			int column = index % columns;
			int row = index / columns;
			if (hasPixels(image, column * frameWidth, row * frameHeight, frameWidth, frameHeight)) {
				order[count++] = index;
			}
		}

		return Arrays.copyOf(order, count);
	}

	/** {@code true} if at least one pixel of the frame is not fully transparent. */
	private static boolean hasPixels(NativeImage image, int startX, int startY, int width, int height) {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (ARGB.alpha(image.getPixel(startX + x, startY + y)) != 0) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * One sprite sheet: frames are laid out left to right, then row by row. Empty cells of the sheet
	 * are not part of the animation.
	 */
	private record Sprite(Identifier id, int sheetWidth, int sheetHeight, int frameWidth, int frameHeight, int[] order) {

		int columns() {
			return Math.max(1, this.sheetWidth / this.frameWidth);
		}

		/** How many frames the animation has. */
		int frames() {
			return this.order.length;
		}

		/** Sheet index of the animation frame with the given number. */
		int sheetFrame(int frame) {
			return this.order[frame];
		}

		int width() {
			return this.frameWidth * SCALE;
		}

		int height() {
			return this.frameHeight * SCALE;
		}
	}
}
