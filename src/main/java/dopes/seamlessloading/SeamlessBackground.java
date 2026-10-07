package dopes.seamlessloading;

import dopes.seamlessloading.config.SeamlessConfig;
import dopes.seamlessloading.config.SeamlessConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * Holds whatever image (or slideshow) should be drawn behind the loading screen and knows how to
 * draw it, including the blur &rarr; sharp animation.
 */
public final class SeamlessBackground {

	/** Number of bundled placeholder images used when the slideshow folder is empty. */
	private static final int PLACEHOLDER_COUNT = 10;

	private static SeamlessTexture current;
	private static SeamlessTexture incoming;
	private static long incomingStart;
	private static long lastSwitch;
	private static long firstShownMillis;
	private static boolean slideshow;
	private static List<Object> slides = List.of();
	private static int slideIndex;

	private SeamlessBackground() {
	}

	public static boolean isActive() {
		return current != null;
	}

	/** Shows a single screenshot, or a slideshow if that screenshot does not exist. */
	public static void show(Path screenshot) {
		SeamlessConfig config = SeamlessConfigManager.get();
		SeamlessTexture loaded = screenshot == null ? null
				: SeamlessTexture.load(screenshot, config.blurStrength, config.imageSize);

		if (loaded != null) {
			reset();
			current = loaded;
			lastSwitch = Util.getMillis();
			DopesSeamlessLoadingScreen.LOGGER.info("[Seamless] Showing the screenshot of {}", screenshot);
			return;
		}

		DopesSeamlessLoadingScreen.LOGGER.info("[Seamless] No screenshot to show ({})",
				screenshot == null ? "screenshots are disabled for this world" : screenshot);

		if (config.slideshowIfNoScreenshot) {
			showSlideshow();
		}
	}

	/** Shows the slideshow, using the images in {@code screenshots/seamless/slideshow}. */
	public static void showSlideshow() {
		reset();
		slideshow = true;

		List<Object> found = new ArrayList<>(collectSlideFiles());
		if (found.isEmpty()) {
			for (int i = 1; i <= PLACEHOLDER_COUNT; i++) {
				found.add(Identifier.fromNamespaceAndPath(DopesSeamlessLoadingScreen.MOD_ID, "placeholders/" + i + ".png"));
			}
		}

		slides = found;
		// Start on a random slide so the same picture is not shown every single time.
		slideIndex = slides.isEmpty() ? 0 : ThreadLocalRandom.current().nextInt(slides.size());
		current = loadSlide(slideIndex);
		lastSwitch = Util.getMillis();
	}

	private static List<Path> collectSlideFiles() {
		Path directory = SeamlessScreenshots.slideshowDirectory();
		if (!Files.isDirectory(directory)) {
			return List.of();
		}

		try (Stream<Path> stream = Files.list(directory)) {
			return stream.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
					.sorted(Comparator.comparing(path -> path.getFileName().toString()))
					.toList();
		} catch (Exception e) {
			DopesSeamlessLoadingScreen.LOGGER.warn("[Seamless] Unable to list the slideshow folder {}", directory, e);
			return List.of();
		}
	}

	private static SeamlessTexture loadSlide(int index) {
		if (slides.isEmpty()) {
			return null;
		}

		Object slide = slides.get(Math.floorMod(index, slides.size()));
		SeamlessConfig config = SeamlessConfigManager.get();
		if (slide instanceof Path path) {
			return SeamlessTexture.load(path, config.blurStrength, config.imageSize);
		}

		return SeamlessTexture.loadResource((Identifier) slide, config.blurStrength, config.imageSize);
	}

	public static void clear() {
		reset();
	}

	private static void reset() {
		if (current != null) {
			current.close();
			current = null;
		}

		if (incoming != null) {
			incoming.close();
			incoming = null;
		}

		slideshow = false;
		slides = List.of();
		slideIndex = 0;
		firstShownMillis = 0L;
	}

	/**
	 * Draws the background on the loading screen.
	 *
	 * <p>Like the original mod, the screenshot is shown fully blurred for as long as the loading
	 * screen is up; the blur only melts away during the closing fade (see {@link #renderFade}).
	 * The slideshow cross fade is a plain alpha blend between the two slides.
	 */
	public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
		if (current == null) {
			return;
		}

		long now = Util.getMillis();
		if (firstShownMillis == 0L) {
			// The slideshow has to start counting when it is actually visible, not when the world
			// started loading (a server login can take several seconds).
			firstShownMillis = now;
			lastSwitch = now;
		}

		SeamlessConfig config = SeamlessConfigManager.get();
		updateSlideshow(config, now);

		drawBackground(graphics, screenWidth, screenHeight, 1.0F, blurAmount(config, 1.0F),
				incoming == null ? 0.0F : crossfadeProgress(config, now));
		renderDim(graphics, screenWidth, screenHeight, 1.0F);
	}

	/**
	 * 1 = fully blurred, 0 = sharp. {@code alpha} is the opacity of the whole image: it is 1 while
	 * the loading screen is up (so the blur is shown at full strength) and drops to 0 during the
	 * closing fade, which is where the blur melts away.
	 *
	 * <p>{@code blurSpeedPercent} says how much of the fade the blur animation takes: 100% = the
	 * blur is gone exactly when the fade ends, 50% = twice as fast, 0% = no animation at all.
	 *
	 * <p>The slideshow never animates its blur: the slides change with their own cross fade, so the
	 * blur simply stays until the image has fully faded.
	 */
	private static float blurAmount(SeamlessConfig config, float alpha) {
		if (config.blurStrength <= 0 || !current.hasBlur()) {
			return 0.0F;
		}

		// The slideshow never animates its blur: the slides change with their own cross fade, so the
		// blur simply stays at full strength until the image has completely faded.
		if (slideshow) {
			return 1.0F;
		}

		float progress = 1.0F - Mth.clamp(alpha, 0.0F, 1.0F);
		int percent = Mth.clamp(config.blurSpeedPercent, 0, 100);
		if (percent <= 0) {
			// No animation: the blur does not melt at all, it stays until the end of the fade.
			return 1.0F;
		}

		return Mth.clamp(1.0F - progress * (100.0F / percent), 0.0F, 1.0F);
	}

	/** Draws the current slide and, while the slideshow cross fades, the incoming one on top. */
	private static void drawBackground(GuiGraphics graphics, int screenWidth, int screenHeight, float alpha, float blur,
			float incomingAlpha) {
		drawSlide(graphics, current, screenWidth, screenHeight, alpha, blur);
		drawSlide(graphics, incoming, screenWidth, screenHeight, incomingAlpha, blur);
	}

	private static void drawSlide(GuiGraphics graphics, SeamlessTexture texture, int screenWidth, int screenHeight,
			float alpha, float blur) {
		if (texture == null || alpha <= 0.001F) {
			return;
		}

		texture.draw(graphics, screenWidth, screenHeight, alpha, blur);
	}

	private static void updateSlideshow(SeamlessConfig config, long now) {
		// A single image means there is nothing to switch to, so the slideshow animation is off.
		if (!slideshow || slides.size() <= 1) {
			return;
		}

		if (incoming != null) {
			if (now - incomingStart >= Math.max(1, config.slideshowFadeSpeed)) {
				current.close();
				current = incoming;
				incoming = null;
				lastSwitch = now;
			}

			return;
		}

		if (config.slideshowSpeed <= 0 || now - lastSwitch < config.slideshowSpeed) {
			return;
		}

		slideIndex++;
		SeamlessTexture next = loadSlide(slideIndex);
		if (next == null) {
			lastSwitch = now;
			return;
		}

		incoming = next;
		incomingStart = now;
	}

	private static float crossfadeProgress(SeamlessConfig config, long now) {
		int duration = Math.max(1, config.slideshowFadeSpeed);
		return Mth.clamp((float) (now - incomingStart) / duration, 0.0F, 1.0F);
	}

	/** Draws the dim overlay. {@code factor} is 1 while the loading screen is up and fades out later. */
	private static void renderDim(GuiGraphics graphics, int screenWidth, int screenHeight, float factor) {
		int dim = SeamlessConfigManager.get().backgroundDim;
		if (dim <= 0) {
			return;
		}

		int dimAlpha = Math.round(255.0F * (dim / 100.0F) * factor);
		if (dimAlpha <= 0) {
			return;
		}

		graphics.fill(0, 0, screenWidth, screenHeight, ARGB.color(dimAlpha, 0));
	}

	/**
	 * Draws the background while fading it out (used after the loading screen closed). The blur
	 * melts away while the image fades into the world, {@code blurSpeedPercent} controls how much
	 * of the blur is gone by the time the image has fully faded.
	 */
	public static void renderFade(GuiGraphics graphics, int screenWidth, int screenHeight, float alpha) {
		if (current == null || alpha <= 0.001F) {
			return;
		}

		SeamlessConfig config = SeamlessConfigManager.get();
		long now = Util.getMillis();
		updateSlideshow(config, now);

		drawBackground(graphics, screenWidth, screenHeight, alpha, blurAmount(config, alpha),
				incoming == null ? 0.0F : crossfadeProgress(config, now) * alpha);
		renderDim(graphics, screenWidth, screenHeight, alpha);
	}

	/**
	 * Draws the image as a static overlay: fully blurred, with the very same dim as the loading
	 * screen. Used by the auxiliary transition screens, which never animate their blur.
	 */
	public static void renderOverlay(GuiGraphics graphics, int screenWidth, int screenHeight, float alpha) {
		if (current == null || alpha <= 0.001F) {
			return;
		}

		SeamlessConfig config = SeamlessConfigManager.get();
		long now = Util.getMillis();
		updateSlideshow(config, now);

		float blur = config.blurStrength > 0 && current.hasBlur() ? 1.0F : 0.0F;
		drawBackground(graphics, screenWidth, screenHeight, alpha, blur,
				incoming == null ? 0.0F : crossfadeProgress(config, now) * alpha);
		renderDim(graphics, screenWidth, screenHeight, alpha);
	}

	/** Rectangle (x, y, width, height) that covers the whole screen while keeping the aspect ratio. */
	public static int[] coverRect(int screenWidth, int screenHeight, float imageRatio) {
		float screenRatio = (float) screenWidth / (float) screenHeight;
		int width;
		int height;
		if (imageRatio > screenRatio) {
			height = screenHeight;
			width = Math.round(screenHeight * imageRatio);
		} else {
			width = screenWidth;
			height = Math.round(screenWidth / imageRatio);
		}

		return new int[]{(screenWidth - width) / 2, (screenHeight - height) / 2, width, height};
	}
}
