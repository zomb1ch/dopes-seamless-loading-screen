package dopes.seamlessloading;

import dopes.seamlessloading.config.SeamlessConfig;
import dopes.seamlessloading.config.SeamlessConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The little tip in the top left corner of the loading screen, drawn in the bundled uni 05_53 pixel
 * font (a TTF font provider, see {@code assets/.../font/tips.json}).
 *
 * <p>The font lives inside the game's font system: the tip is a styled component whose style points
 * at it, so measuring and wrapping can be done with the game's own splitter
 * ({@link Font#getSplitter()}), which is style aware and therefore measures the text with the very
 * font it is drawn in.
 */
public final class SeamlessTips {

	/** The bundled font, defined by assets/.../font/tips.json. */
	private static final Identifier FONT_ID = Identifier.fromNamespaceAndPath(DopesSeamlessLoadingScreen.MOD_ID, "tips");
	/** Everything the tip is drawn with, including the font. */
	private static final Style TIP_STYLE = Style.EMPTY.withFont(new FontDescription.Resource(FONT_ID));
	/** The corner hint is drawn in the same uni font at half the size (assets/.../font/hint.json). */
	private static final Identifier HINT_FONT_ID = Identifier.fromNamespaceAndPath(DopesSeamlessLoadingScreen.MOD_ID, "hint");
	private static final Style HINT_STYLE = Style.EMPTY.withFont(new FontDescription.Resource(HINT_FONT_ID));
	/** The corner hint is drawn at this opacity, relative to the HUD fade. */
	private static final float HINT_ALPHA = 0.45F;

	/** Same margin from the screen edges as the rest of the HUD. */
	private static final int MARGIN = 20;
	/** Width of the little vertical dash on the left of the text. */
	private static final int DASH_WIDTH = 3;
	/** Gap between the dash and the text. */
	private static final int DASH_GAP = 8;
	/** One source line of the font is 8 px tall; two more make the lines readable. */
	private static final int LINE_HEIGHT = 14;
	/** The tip takes at most this many lines; longer tips are cut short. */
	private static final int MAX_LINES = 3;
	/** How many tips there are, see the {@code tip.<n>} language keys. */
	private static final int TIP_COUNT = 50;
	/** How long one tip is shown before auto-advancing, in milliseconds. */
	private static final long TIP_INTERVAL_MS = 5000L;

	/** Which tip is shown; picked per transition, cycled by hand in the preview. */
	private static int tipIndex;
	/** When the current tip was last advanced (auto or by hand). */
	private static long lastTipChange;

	private SeamlessTips() {
	}

	/** Picks a random tip, called when a new transition starts. */
	public static void reset() {
		tipIndex = TIP_COUNT == 0 ? 0 : ThreadLocalRandom.current().nextInt(TIP_COUNT);
		lastTipChange = Util.getMillis();
	}

	/** Advances to the next tip (cycling), used by the LMB handler. */
	public static void nextTip() {
		if (TIP_COUNT == 0) {
			return;
		}
		tipIndex = (tipIndex + 1) % TIP_COUNT;
		lastTipChange = Util.getMillis();
	}

	/**
	 * Advances the tip automatically if the interval has elapsed. Called from {@link #render} each
	 * frame so the tip changes every {@link #TIP_INTERVAL_MS} milliseconds.
	 */
	public static void update(long now) {
		if (TIP_COUNT == 0) {
			return;
		}
		if (now - lastTipChange >= TIP_INTERVAL_MS) {
			nextTip();
		}
	}

	/**
	 * Measures the width of a chunk-counter string in the uni tip font at 2x scale, so callers can
	 * position it relative to the loading icon.
	 */
	public static int chunkCounterWidth(String text) {
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		if (font == null) {
			return 0;
		}
		return font.width(Component.literal(text).setStyle(TIP_STYLE)) * 2;
	}

	/**
	 * On-screen height of the counter digits, in GUI pixels. Uni 05_53 is a pixel font: at
	 * size 16 its digits are ~7 px tall and start right at the drawString y, so with the 2x
	 * scale of {@link #drawChunkCounterText} the drawn block is 14 px. This is the REAL drawn
	 * height, not the font's line box (the box is taller and using it would push the digits
	 * up), so {@code y = centre - height / 2} genuinely centres the digits on the icon.
	 */
	public static final int COUNTER_DIGITS_HEIGHT = 14;

	/**
	 * Draws a chunk-counter string (digits only, e.g. {@code "42 / 100"}) in the uni tip font,
	 * scaled up to match the loading icon size. Used by the HUD and by the fallback counter drawn
	 * over the vanilla screen when the custom screen is off.
	 */
	public static void drawChunkCounterText(GuiGraphics graphics, String text, int x, int y, float alpha) {
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		if (font == null) {
			return;
		}

		int color = ARGB.color(Math.round(255.0F * alpha), 0xFFFFFF);

		graphics.pose().pushMatrix();
		graphics.pose().scale(2.0f, 2.0f);
		graphics.drawString(font, Component.literal(text).setStyle(TIP_STYLE),
				Math.round(x / 2.0f), Math.round(y / 2.0f), color, false);
		graphics.pose().popMatrix();
	}

	/** The small semi transparent "switch tips with the left mouse button" hint in the top right. */
	private static void drawHint(GuiGraphics graphics, int screenWidth, float alpha) {
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		if (font == null) {
			return;
		}

		String hint = Component.translatable(DopesSeamlessLoadingScreen.MOD_ID + ".tipsHint").getString();
		if (hint.isBlank()) {
			return;
		}

		int width = font.width(Component.literal(hint).setStyle(HINT_STYLE));
		int x = screenWidth - MARGIN - width;
		if (x <= MARGIN) {
			return;
		}

		int color = ARGB.color(Math.round(255.0F * HINT_ALPHA * alpha), 0xFFFFFF);
		graphics.drawString(font, Component.literal(hint).setStyle(HINT_STYLE), x, MARGIN, color, false);
	}

	/**
	 * Draws the tip into the top left corner. {@code alpha} is the opacity coming from the screen it
	 * is drawn on, so the tip fades in together with the rest of the HUD.
	 */
	public static void render(GuiGraphics graphics, int screenWidth, float alpha) {
		SeamlessConfig config = SeamlessConfigManager.get();
		if (!config.modEnabled || !config.loadingTips) {
			return;
		}

		if (alpha <= 0.01F || screenWidth <= 0) {
			return;
		}

		update(Util.getMillis());
		drawHint(graphics, screenWidth, alpha);

		int maxWidth = screenWidth / 2 - MARGIN - DASH_WIDTH - DASH_GAP;
		if (maxWidth < 60) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		if (font == null) {
			return;
		}

		String text = Component
				.translatable(DopesSeamlessLoadingScreen.MOD_ID + ".tip." + (tipIndex + 1))
				.getString();
		if (text.isBlank()) {
			return;
		}

		List<FormattedText> lines = font.getSplitter().splitLines(text, maxWidth, TIP_STYLE);
		if (lines.isEmpty()) {
			return;
		}

		boolean cut = lines.size() > MAX_LINES;
		if (cut) {
			lines = lines.subList(0, MAX_LINES);
		}

		int lineCount = lines.size();
		int top = MARGIN;
		int textColor = ARGB.color(Math.round(255.0F * alpha), 0xFFFFFF);

		// The dash on the left, spanning the block of text and reaching ~5 px above it.
		graphics.fill(MARGIN, top - 5, MARGIN + DASH_WIDTH, top + lineCount * LINE_HEIGHT - 4, textColor);

		int x = MARGIN + DASH_WIDTH + DASH_GAP;
		for (int i = 0; i < lineCount; i++) {
			String line = lines.get(i).getString();
			if (cut && i == lineCount - 1) {
				// The tip was longer than the screen can show: mark the cut with an ellipsis.
				String dots = "...";
				float dotsWidth = font.getSplitter().stringWidth(Component.literal(dots).setStyle(TIP_STYLE));
				String head = font.getSplitter().plainHeadByWidth(line,
						maxWidth - Math.round(dotsWidth) - 1, TIP_STYLE);
				line = head + dots;
			}

			graphics.drawString(font, Component.literal(line).setStyle(TIP_STYLE),
					x, top + i * LINE_HEIGHT, textColor, true);
		}
	}
}
