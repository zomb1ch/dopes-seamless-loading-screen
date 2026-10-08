package dopes.seamlessloading;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One image loaded from disk and uploaded to the GPU.
 *
 * <p>An image is stored several times: once sharp and, if the user configured a blur, once per blur
 * level. Level {@code n} halves the resolution {@code n} times, so the levels get blurrier step by
 * step. Drawing blends between two neighbouring levels, which is what makes the blur radius grow and
 * shrink smoothly instead of cross fading between a sharp and a mushy image.
 *
 * <p>The blur is produced by resizing (1.21.11 no longer supports the old core shader API the
 * original mod used).
 */
public final class SeamlessTexture {

	private static final AtomicInteger COUNTER = new AtomicInteger();

	/** How many blur levels the strongest setting generates. */
	private static final int MAX_BLUR_LEVELS = 5;

	private final ResourceLocation sharpId;
	/** Blur levels, weakest first. Empty when the blur is disabled. */
	private final ResourceLocation[] blurIds;
	private final float ratio;
	private boolean closed;

	private SeamlessTexture(ResourceLocation sharpId, ResourceLocation[] blurIds, float ratio) {
		this.sharpId = sharpId;
		this.blurIds = blurIds;
		this.ratio = ratio;
	}

	/**
	 * Loads the given file, or returns {@code null} if it is missing or unreadable.
	 *
	 * @param imageSize resolution the image is kept at, in percent of its original size. The image
	 *                  is always stretched over the whole screen when drawn, this only trades
	 *                  quality (and memory) for performance.
	 */
	public static SeamlessTexture load(Path path, int blurStrength, int imageSize) {
		if (path == null || !Files.isRegularFile(path)) {
			return null;
		}

		try (InputStream in = Files.newInputStream(path)) {
			return fromImage(NativeImage.read(in), blurStrength, imageSize);
		} catch (Exception e) {
			DopesSeamlessLoadingScreen.LOGGER.error("[Seamless] Unable to read the image {}", path, e);
			return null;
		}
	}

	/** Loads an image that ships inside the mod jar (the bundled slideshow placeholders). */
	public static SeamlessTexture loadResource(ResourceLocation resource, int blurStrength, int imageSize) {
		try {
			Optional<Resource> optional = Minecraft.getInstance().getResourceManager().getResource(resource);
			if (optional.isEmpty()) {
				return null;
			}

			try (InputStream in = optional.get().open()) {
				return fromImage(NativeImage.read(in), blurStrength, imageSize);
			}
		} catch (Exception e) {
			DopesSeamlessLoadingScreen.LOGGER.error("[Seamless] Unable to read the bundled image {}", resource, e);
			return null;
		}
	}

	private static SeamlessTexture fromImage(NativeImage image, int blurStrength, int imageSize) {
		NativeImage sharp = resize(image, imageSize);
		String suffix = "_" + COUNTER.incrementAndGet();
		ResourceLocation sharpId = ResourceLocation.fromNamespaceAndPath(DopesSeamlessLoadingScreen.MOD_ID, "image" + suffix);
		Minecraft.getInstance().getTextureManager().register(sharpId, new LinearDynamicTexture(sharp));

		int count = blurLevels(blurStrength);
		ResourceLocation[] blurIds = new ResourceLocation[count];
		if (count > 0) {
			NativeImage[] levels = buildBlurLevels(sharp, count);
			for (int i = 0; i < count; i++) {
				ResourceLocation id = ResourceLocation.fromNamespaceAndPath(DopesSeamlessLoadingScreen.MOD_ID,
						"image_blur" + (i + 1) + suffix);
				Minecraft.getInstance().getTextureManager().register(id, new LinearDynamicTexture(levels[i]));
				blurIds[i] = id;
			}
		}

		return new SeamlessTexture(sharpId, blurIds, (float) sharp.getWidth() / (float) sharp.getHeight());
	}

	/** Scales the image down to {@code percent} of its resolution. 100% keeps it untouched. */
	private static NativeImage resize(NativeImage source, int percent) {
		if (percent >= 100) {
			return source;
		}

		int width = Math.max(1, source.getWidth() * Math.max(1, percent) / 100);
		int height = Math.max(1, source.getHeight() * Math.max(1, percent) / 100);
		NativeImage target = new NativeImage(width, height, false);
		source.resizeSubRectTo(0, 0, source.getWidth(), source.getHeight(), target);
		source.close();
		return target;
	}

	/**
	 * How many halving steps the configured strength asks for. 1 = a mild blur (half resolution),
	 * every extra level doubles the radius. 0 disables the blur.
	 */
	private static int blurLevels(int strength) {
		if (strength <= 0) {
			return 0;
		}

		int levels = 31 - Integer.numberOfLeadingZeros(Math.min(strength, 64)) - 1;
		return Math.max(1, Math.min(MAX_BLUR_LEVELS, levels));
	}

	/**
	 * Builds the blur pyramid: every level halves the resolution one more time and is then scaled
	 * back up to the original size. Halving in several small steps instead of one big jump is what
	 * keeps the result smooth, a single big jump looks like a coarse, low quality image.
	 */
	private static NativeImage[] buildBlurLevels(NativeImage source, int count) {
		NativeImage[] levels = new NativeImage[count];
		NativeImage current = source;

		for (int i = 0; i < count; i++) {
			NativeImage smaller = new NativeImage(Math.max(1, current.getWidth() / 2),
					Math.max(1, current.getHeight() / 2), false);
			current.resizeSubRectTo(0, 0, current.getWidth(), current.getHeight(), smaller);
			if (current != source) {
				current.close();
			}

			current = smaller;

			NativeImage level = new NativeImage(source.getWidth(), source.getHeight(), false);
			smaller.resizeSubRectTo(0, 0, smaller.getWidth(), smaller.getHeight(), level);
			levels[i] = level;
		}

		if (current != source) {
			current.close();
		}

		return levels;
	}

	public boolean hasBlur() {
		return blurIds.length > 0;
	}

	public float ratio() {
		return ratio;
	}

	/**
	 * Draws the image. {@code blur} is the blur amount: 0 shows the sharp image, 1 the strongest
	 * level, everything in between blends the two neighbouring levels.
	 */
	public void draw(GuiGraphics graphics, int screenWidth, int screenHeight, float alpha, float blur) {
		if (closed || alpha <= 0.001F) {
			return;
		}

		int count = blurIds.length;
		if (count == 0 || blur <= 0.001F) {
			drawLevel(graphics, sharpId, screenWidth, screenHeight, alpha);
			return;
		}

		float position = Mth.clamp(blur, 0.0F, 1.0F) * count;
		int base = (int) position;

		if (base >= count) {
			drawLevel(graphics, blurIds[count - 1], screenWidth, screenHeight, alpha);
			return;
		}

		drawLevel(graphics, base == 0 ? sharpId : blurIds[base - 1], screenWidth, screenHeight, alpha);

		float fraction = position - base;
		if (fraction > 0.001F) {
			drawLevel(graphics, blurIds[base], screenWidth, screenHeight, alpha * fraction);
		}
	}

	private void drawLevel(GuiGraphics graphics, ResourceLocation id, int screenWidth, int screenHeight, float alpha) {
		if (alpha <= 0.001F) {
			return;
		}

		int[] rect = SeamlessBackground.coverRect(screenWidth, screenHeight, ratio);
		int x = rect[0];
		int y = rect[1];
		int width = rect[2];
		int height = rect[3];

		graphics.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0.0F, 0.0F, width, height, width, height, width, height,
				ARGB.white(alpha));
	}

	public void close() {
		if (closed) {
			return;
		}

		closed = true;
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.getTextureManager() == null) {
			return;
		}

		minecraft.getTextureManager().release(sharpId);
		for (ResourceLocation id : blurIds) {
			minecraft.getTextureManager().release(id);
		}
	}

	/**
	 * Vanilla's {@link DynamicTexture} uses nearest neighbour filtering, which looks blocky when the
	 * screenshot is scaled to a different resolution than it was taken at.
	 *
	 * <p>How the filtering is set changed over the versions the mod supports: 1.21.11 and newer have a
	 * sampler cache, older ones a plain {@code setFilter} flag. Both are looked up reflectively, so a
	 * jar built against the newest version still runs on the older ones. If neither exists the texture
	 * keeps vanilla's nearest filtering: it still renders, it is just a bit blockier.
	 */
	private static class LinearDynamicTexture extends DynamicTexture {

		LinearDynamicTexture(NativeImage image) {
			super(() -> "Seamless image", image);
			applyLinearFilter(this);
		}
	}

	private static void applyLinearFilter(AbstractTexture texture) {
		if (!useSamplerCache(texture) && !useSetFilter(texture)) {
			DopesSeamlessLoadingScreen.LOGGER.warn(
					"[Seamless] This Minecraft version has no linear texture filtering, the image may look blocky");
		}
	}

	/** 1.21.11 and newer: {@code RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)}. */
	private static boolean useSamplerCache(AbstractTexture texture) {
		try {
			Object cache = RenderSystem.class.getMethod("getSamplerCache").invoke(null);
			Object sampler = cache.getClass().getMethod("getClampToEdge", FilterMode.class).invoke(cache, FilterMode.LINEAR);
			Field field = AbstractTexture.class.getDeclaredField("sampler");
			field.setAccessible(true);
			field.set(texture, sampler);
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	/** Older versions: the plain {@code setFilter(blur, mipmap)} flag. */
	private static boolean useSetFilter(AbstractTexture texture) {
		try {
			AbstractTexture.class.getMethod("setFilter", boolean.class, boolean.class).invoke(texture, true, false);
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}
}
