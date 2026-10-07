package dopes.seamlessloading;

import com.mojang.logging.LogUtils;
import dopes.seamlessloading.config.SeamlessConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Main entrypoint of the mod.
 *
 * <p>Everything this mod does happens on the client, therefore we only implement
 * {@link ClientModInitializer}. The mod:
 * <ul>
 *     <li>takes a screenshot of the world right before you leave it,</li>
 *     <li>shows that screenshot (or a slideshow) on the loading screen when you rejoin,</li>
 *     <li>optionally waits for all chunks and shows a chunk counter,</li>
 *     <li>fades the screenshot away once the world is ready.</li>
 * </ul>
 */
public class DopesSeamlessLoadingScreen implements ClientModInitializer {

	public static final String MOD_ID = "dopes_seamless_loading_screen";
	public static final Logger LOGGER = LogUtils.getLogger();

	@Override
	public void onInitializeClient() {
		SeamlessConfigManager.load();

		try {
			Files.createDirectories(getScreenshotsDirectory().resolve(SeamlessScreenshots.SINGLEPLAYER_DIR));
			Files.createDirectories(getScreenshotsDirectory().resolve(SeamlessScreenshots.SERVERS_DIR));
			Files.createDirectories(getScreenshotsDirectory().resolve(SeamlessScreenshots.SLIDESHOW_DIR));
		} catch (IOException e) {
			LOGGER.error("[Seamless] Unable to create the screenshot directories, capturing/displaying may not work!", e);
		}
	}

	/** Root folder of this mod: {@code <gameDir>/screenshots/seamless}. */
	public static Path getScreenshotsDirectory() {
		return FabricLoader.getInstance().getGameDir().resolve("screenshots").resolve("seamless");
	}
}
