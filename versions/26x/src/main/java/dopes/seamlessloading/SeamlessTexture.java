package dopes.seamlessloading;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.nio.file.Path;

/**
 * One prepared image ({@link SeamlessImage}) plus the drawing: the image is stretched over the whole
 * screen and, while the blur amount grows or shrinks, two neighbouring blur levels are blended into
 * each other.
 *
 * <p>How the image is resized and blurred, and why that is not done with {@code NativeImage}, is
 * described in {@link SeamlessImage}. The texture has exactly the size it is drawn at, so the blit
 * below samples it one to one and the picture does not depend on the texture filter.
 */
public final class SeamlessTexture {

	private final SeamlessImage image;

	private SeamlessTexture(SeamlessImage image) {
		this.image = image;
	}

	/**
	 * Loads the given file, or returns {@code null} if it is missing or unreadable.
	 *
	 * @param imageSize resolution the image is sampled at, in percent of its original size. The image
	 *                  is always stretched over the whole screen when drawn, this only trades quality
	 *                  (and time) for performance.
	 */
	public static SeamlessTexture load(Path path, int blurStrength, int imageSize) {
		SeamlessImage loaded = SeamlessImage.load(path, blurStrength, imageSize);
		return loaded == null ? null : new SeamlessTexture(loaded);
	}

	/** Loads an image that ships inside the mod jar (the bundled slideshow placeholders). */
	public static SeamlessTexture loadResource(Identifier resource, int blurStrength, int imageSize) {
		SeamlessImage loaded = SeamlessImage.loadResource(resource, blurStrength, imageSize);
		return loaded == null ? null : new SeamlessTexture(loaded);
	}

	public boolean hasBlur() {
		return image.hasBlur();
	}

	public float ratio() {
		return image.ratio();
	}

	/**
	 * Draws the image. {@code blur} is the blur amount: 0 shows the sharp image, 1 the strongest
	 * level, everything in between blends the two neighbouring levels.
	 */
	public void draw(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight, float alpha, float blur) {
		if (image.isClosed() || alpha <= 0.001F) {
			return;
		}

		Identifier[] blurIds = image.blurIds();
		int count = blurIds.length;
		if (count == 0 || blur <= 0.001F) {
			drawLevel(graphics, image.sharpId(), screenWidth, screenHeight, alpha);
			return;
		}

		float position = Mth.clamp(blur, 0.0F, 1.0F) * count;
		int base = (int) position;

		if (base >= count) {
			drawLevel(graphics, blurIds[count - 1], screenWidth, screenHeight, alpha);
			return;
		}

		drawLevel(graphics, base == 0 ? image.sharpId() : blurIds[base - 1], screenWidth, screenHeight, alpha);

		float fraction = position - base;
		if (fraction > 0.001F) {
			drawLevel(graphics, blurIds[base], screenWidth, screenHeight, alpha * fraction);
		}
	}

	private void drawLevel(GuiGraphicsExtractor graphics, Identifier id, int screenWidth, int screenHeight, float alpha) {
		if (alpha <= 0.001F) {
			return;
		}

		int[] rect = SeamlessBackground.coverRect(screenWidth, screenHeight, image.ratio());
		graphics.blit(RenderPipelines.GUI_TEXTURED, id, rect[0], rect[1], 0.0F, 0.0F, rect[2], rect[3],
				image.width(), image.height(), image.width(), image.height(), ARGB.white(alpha));
	}

	public void close() {
		image.close();
	}
}
