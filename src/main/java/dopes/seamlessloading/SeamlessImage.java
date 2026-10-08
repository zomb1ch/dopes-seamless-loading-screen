package dopes.seamlessloading;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The pixels of one image (a screenshot, or one of the bundled placeholders), prepared for the
 * loading screen.
 *
 * <p>The image is stored at exactly the size it is drawn at, so the texture has one texel per screen
 * pixel and is sampled one to one. That makes the result independent of the texture filter, which
 * matters because the filter API differs inside the supported versions: up to 1.21.10 a texture is
 * configured with {@code setFilter} (and there is no {@code GpuSampler} class at all), from 1.21.11
 * on it is configured with a {@code GpuSampler} (and {@code setFilter} is gone). Vanilla's default
 * is nearest neighbour for minification, which would alias the picture.
 *
 * <p>If the user configured a blur, the image is also stored once per blur level. Level {@code n} is
 * the image averaged over blocks of {@code 2^n} pixels and interpolated back to the original size,
 * so the levels get blurrier step by step. Drawing blends two neighbouring levels, which is what
 * makes the blur radius grow and shrink smoothly instead of cross fading between a sharp and a mushy
 * image.
 *
 * <p>All resizing is plain pixel arithmetic on purpose: {@link NativeImage#resizeSubRectTo} is a
 * point sample, and the aliasing it leaves behind stays visible as stair steps in the blurred image.
 */
public final class SeamlessImage {

	private static final AtomicInteger COUNTER = new AtomicInteger();

	/** How many blur levels the strongest setting generates. */
	private static final int MAX_BLUR_LEVELS = 5;

	private final Identifier sharpId;
	/** Blur levels, weakest first. Empty when the blur is disabled. */
	private final Identifier[] blurIds;
	private final int width;
	private final int height;
	private final float ratio;
	private boolean closed;

	private SeamlessImage(Identifier sharpId, Identifier[] blurIds, int width, int height, float ratio) {
		this.sharpId = sharpId;
		this.blurIds = blurIds;
		this.width = width;
		this.height = height;
		this.ratio = ratio;
	}

	/**
	 * Loads the given file, or returns {@code null} if it is missing or unreadable.
	 *
	 * @param imageSize resolution the image is sampled at, in percent of its original size. The result
	 *                  is always stretched over the whole screen, this only trades quality (and time)
	 *                  for performance.
	 */
	public static SeamlessImage load(Path path, int blurStrength, int imageSize) {
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
	public static SeamlessImage loadResource(Identifier resource, int blurStrength, int imageSize) {
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

	private static SeamlessImage fromImage(NativeImage source, int blurStrength, int imageSize) {
		float ratio = (float) source.getWidth() / (float) source.getHeight();
		NativeImage sharp = fit(source, ratio, imageSize);

		String suffix = "_" + COUNTER.incrementAndGet();
		Identifier sharpId = Identifier.fromNamespaceAndPath(DopesSeamlessLoadingScreen.MOD_ID, "image" + suffix);
		register(sharpId, sharp);

		int count = blurLevels(blurStrength);
		Identifier[] blurIds = new Identifier[count];
		for (int i = 0; i < count; i++) {
			Identifier id = Identifier.fromNamespaceAndPath(DopesSeamlessLoadingScreen.MOD_ID,
					"image_blur" + (i + 1) + suffix);
			register(id, blur(sharp, i + 1));
			blurIds[i] = id;
		}

		return new SeamlessImage(sharpId, blurIds, sharp.getWidth(), sharp.getHeight(), ratio);
	}

	private static void register(Identifier id, NativeImage image) {
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "Seamless image", image));
	}

	/**
	 * Scales the image to the size it is drawn at: the screen is covered completely, so the result is
	 * as large as the window (minus whatever is cut off when the aspect ratios differ). It is never
	 * enlarged past its own resolution, a smaller picture would only get softer.
	 */
	private static NativeImage fit(NativeImage source, float ratio, int imageSize) {
		int originalWidth = source.getWidth();
		int originalHeight = source.getHeight();
		NativeImage working = reduce(source, imageSize);

		int[] rect = SeamlessBackground.coverRect(windowWidth(), windowHeight(), ratio);
		int targetWidth = Math.min(rect[2], originalWidth);
		int targetHeight = Math.min(rect[3], originalHeight);

		if (working.getWidth() == targetWidth && working.getHeight() == targetHeight) {
			return working;
		}

		// Cover: scale until both sides are at least as large as the target, then cut off the overhang
		// in the middle.
		float scale = Math.max((float) targetWidth / (float) working.getWidth(),
				(float) targetHeight / (float) working.getHeight());
		int scaledWidth = Math.max(targetWidth, Math.round(working.getWidth() * scale));
		int scaledHeight = Math.max(targetHeight, Math.round(working.getHeight() * scale));

		NativeImage scaled = resize(working, scaledWidth, scaledHeight);
		// The scaled copy is what we keep from here on; the working image has been read completely.
		working.close();

		NativeImage result = crop(scaled, (scaledWidth - targetWidth) / 2, (scaledHeight - targetHeight) / 2,
				targetWidth, targetHeight);
		scaled.close();
		return result;
	}

	/** Scales the image down to {@code percent} of its resolution. 100% keeps it untouched. */
	private static NativeImage reduce(NativeImage source, int percent) {
		if (percent >= 100) {
			return source;
		}

		int width = Math.max(1, source.getWidth() * Math.max(1, percent) / 100);
		int height = Math.max(1, source.getHeight() * Math.max(1, percent) / 100);
		NativeImage smaller = downscale(source, width, height);
		source.close();
		return smaller;
	}

	/** Shrinks with an area average and grows with an interpolation - both keep the image smooth. */
	private static NativeImage resize(NativeImage source, int width, int height) {
		long from = (long) source.getWidth() * source.getHeight();
		long to = (long) width * height;
		return to <= from ? downscale(source, width, height) : upscale(source, width, height);
	}

	private static NativeImage crop(NativeImage source, int x, int y, int width, int height) {
		NativeImage target = new NativeImage(width, height, false);
		for (int row = 0; row < height; row++) {
			for (int column = 0; column < width; column++) {
				target.setPixel(column, row, source.getPixel(x + column, y + row));
			}
		}

		return target;
	}

	/**
	 * Every target pixel is the average of all source pixels it covers. That is what removes the
	 * detail while leaving nothing behind to alias.
	 */
	private static NativeImage downscale(NativeImage source, int targetWidth, int targetHeight) {
		NativeImage target = new NativeImage(targetWidth, targetHeight, false);
		int sourceWidth = source.getWidth();
		int sourceHeight = source.getHeight();

		for (int y = 0; y < targetHeight; y++) {
			int firstY = (int) ((long) y * sourceHeight / targetHeight);
			int lastY = Math.max(firstY + 1, (int) ((long) (y + 1) * sourceHeight / targetHeight));

			for (int x = 0; x < targetWidth; x++) {
				int firstX = (int) ((long) x * sourceWidth / targetWidth);
				int lastX = Math.max(firstX + 1, (int) ((long) (x + 1) * sourceWidth / targetWidth));

				long alpha = 0L;
				long red = 0L;
				long green = 0L;
				long blue = 0L;

				for (int sourceY = firstY; sourceY < lastY; sourceY++) {
					for (int sourceX = firstX; sourceX < lastX; sourceX++) {
						int pixel = source.getPixel(sourceX, sourceY);
						alpha += (pixel >>> 24) & 0xFF;
						red += (pixel >>> 16) & 0xFF;
						green += (pixel >>> 8) & 0xFF;
						blue += pixel & 0xFF;
					}
				}

				int samples = (lastX - firstX) * (lastY - firstY);
				target.setPixel(x, y, (int) (alpha / samples) << 24 | (int) (red / samples) << 16
						| (int) (green / samples) << 8 | (int) (blue / samples));
			}
		}

		return target;
	}

	/** Bilinear interpolation: grows an image back without the blocks a point sample would show. */
	private static NativeImage upscale(NativeImage source, int targetWidth, int targetHeight) {
		NativeImage target = new NativeImage(targetWidth, targetHeight, false);
		int sourceWidth = source.getWidth();
		int sourceHeight = source.getHeight();

		for (int y = 0; y < targetHeight; y++) {
			float sourceY = targetHeight <= 1 ? 0.0F : y * (float) (sourceHeight - 1) / (float) (targetHeight - 1);
			int top = (int) sourceY;
			int bottom = Math.min(top + 1, sourceHeight - 1);
			float fractionY = sourceY - top;

			for (int x = 0; x < targetWidth; x++) {
				float sourceX = targetWidth <= 1 ? 0.0F : x * (float) (sourceWidth - 1) / (float) (targetWidth - 1);
				int left = (int) sourceX;
				int right = Math.min(left + 1, sourceWidth - 1);
				float fractionX = sourceX - left;

				int topLeft = source.getPixel(left, top);
				int topRight = source.getPixel(right, top);
				int bottomLeft = source.getPixel(left, bottom);
				int bottomRight = source.getPixel(right, bottom);

				int alpha = interpolate(topLeft >>> 24, topRight >>> 24, bottomLeft >>> 24, bottomRight >>> 24,
						fractionX, fractionY);
				int red = interpolate(topLeft >>> 16 & 0xFF, topRight >>> 16 & 0xFF, bottomLeft >>> 16 & 0xFF,
						bottomRight >>> 16 & 0xFF, fractionX, fractionY);
				int green = interpolate(topLeft >>> 8 & 0xFF, topRight >>> 8 & 0xFF, bottomLeft >>> 8 & 0xFF,
						bottomRight >>> 8 & 0xFF, fractionX, fractionY);
				int blue = interpolate(topLeft & 0xFF, topRight & 0xFF, bottomLeft & 0xFF, bottomRight & 0xFF,
						fractionX, fractionY);

				target.setPixel(x, y, alpha << 24 | red << 16 | green << 8 | blue);
			}
		}

		return target;
	}

	private static int interpolate(int topLeft, int topRight, int bottomLeft, int bottomRight, float x, float y) {
		float top = topLeft + (topRight - topLeft) * x;
		float bottom = bottomLeft + (bottomRight - bottomLeft) * x;
		return Math.round(top + (bottom - top) * y);
	}

	/**
	 * Level {@code level}: the image averaged over blocks of {@code 2^level} pixels and interpolated
	 * back to its own size. Averaging alone already removes the detail, the interpolation is what keeps
	 * the result smooth instead of blocky.
	 */
	private static NativeImage blur(NativeImage source, int level) {
		int width = Math.max(1, source.getWidth() >> level);
		int height = Math.max(1, source.getHeight() >> level);

		NativeImage smaller = downscale(source, width, height);
		NativeImage result = upscale(smaller, source.getWidth(), source.getHeight());
		smaller.close();
		return result;
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

	/** Size of the window in real pixels. The coordinates drawing uses are scaled by the GUI scale. */
	private static int windowWidth() {
		return Minecraft.getInstance().getWindow().getWidth();
	}

	private static int windowHeight() {
		return Minecraft.getInstance().getWindow().getHeight();
	}

	public Identifier sharpId() {
		return sharpId;
	}

	/** Blur levels, weakest first. Empty when the blur is disabled. */
	public Identifier[] blurIds() {
		return blurIds;
	}

	/** Width of the image in texels; it is drawn over the whole screen, so this is the screen size. */
	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	public float ratio() {
		return ratio;
	}

	public boolean hasBlur() {
		return blurIds.length > 0;
	}

	public boolean isClosed() {
		return closed;
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
		for (Identifier id : blurIds) {
			minecraft.getTextureManager().release(id);
		}
	}
}
