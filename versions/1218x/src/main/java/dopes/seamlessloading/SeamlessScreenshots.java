package dopes.seamlessloading;

import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Resolves the PNG file used for a specific world / server.
 *
 * <pre>
 * screenshots/seamless/singleplayer/&lt;level id&gt;.png
 * screenshots/seamless/servers/&lt;address&gt;.png
 * screenshots/seamless/slideshow/&lt;any png you drop in here&gt;
 * </pre>
 *
 * Only the latest screenshot per world is kept.
 */
public final class SeamlessScreenshots {

	public static final String SINGLEPLAYER_DIR = "singleplayer";
	public static final String SERVERS_DIR = "servers";
	public static final String SLIDESHOW_DIR = "slideshow";

	/** Characters that are not allowed in file names on the common operating systems. */
	private static final char[] ILLEGAL_CHARACTERS = { '<', '>', ':', '"', '/', '\\', '|', '?', '*', '\0' };
	/** Windows refuses to open files that use these names. */
	private static final Pattern RESERVED_NAMES = Pattern.compile(
			".*\\.|(?:COM|CLOCK\\$|CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?",
			Pattern.CASE_INSENSITIVE);

	private SeamlessScreenshots() {
	}

	public static Path singleplayer(String levelId) {
		return DopesSeamlessLoadingScreen.getScreenshotsDirectory()
				.resolve(SINGLEPLAYER_DIR)
				.resolve(clean(levelId) + ".png");
	}

	public static Path server(String address) {
		return DopesSeamlessLoadingScreen.getScreenshotsDirectory()
				.resolve(SERVERS_DIR)
				.resolve(clean(address) + ".png");
	}

	public static Path slideshowDirectory() {
		return DopesSeamlessLoadingScreen.getScreenshotsDirectory().resolve(SLIDESHOW_DIR);
	}

	/** Turns arbitrary text (world name, server address, ...) into a safe file name. */
	public static String clean(String name) {
		if (name == null || name.isBlank()) {
			return "unknown";
		}

		String cleaned = name;
		for (char c : ILLEGAL_CHARACTERS) {
			cleaned = cleaned.replace(c, '_');
		}

		if (RESERVED_NAMES.matcher(cleaned).matches()) {
			cleaned = "_" + cleaned + "_";
		}

		if (cleaned.length() > 200) {
			cleaned = cleaned.substring(0, 200);
		}

		return cleaned;
	}
}
