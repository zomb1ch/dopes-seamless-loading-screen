package dopes.seamlessloading.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dopes.seamlessloading.DopesSeamlessLoadingScreen;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads and stores {@link SeamlessConfig} as JSON in {@code config/dopes_seamless_loading_screen.json}.
 */
public final class SeamlessConfigManager {

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();

	private static final Path CONFIG_PATH = FabricLoader.getInstance()
			.getConfigDir()
			.resolve(DopesSeamlessLoadingScreen.MOD_ID + ".json");

	private static SeamlessConfig instance = new SeamlessConfig();

	private SeamlessConfigManager() {
	}

	public static SeamlessConfig get() {
		return instance;
	}

	public static void load() {
		if (!Files.isRegularFile(CONFIG_PATH)) {
			save();
			return;
		}

		try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
			SeamlessConfig loaded = GSON.fromJson(reader, SeamlessConfig.class);
			if (loaded != null) {
				instance = loaded;
			}
		} catch (Exception e) {
			DopesSeamlessLoadingScreen.LOGGER.error("[Seamless] Unable to read the config, using the defaults", e);
			instance = new SeamlessConfig();
		}

		sanitize();
	}

	public static void save() {
		sanitize();

		try {
			Path parent = CONFIG_PATH.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}

			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(instance, writer);
			}
		} catch (Exception e) {
			DopesSeamlessLoadingScreen.LOGGER.error("[Seamless] Unable to save the config", e);
		}
	}

	public static void reset() {
		instance = new SeamlessConfig();
		save();
	}

	private static void sanitize() {
		SeamlessConfig config = instance;
		config.blurStrength = clamp(config.blurStrength, 0, 64);
		config.backgroundDim = clamp(config.backgroundDim, 0, 100);
		config.fadeDuration = clamp(config.fadeDuration, 0, 10000);
		config.imageSize = clamp(config.imageSize, 10, 100);
		config.minShowTime = clamp(config.minShowTime, 0, 600000);
		config.maxShowTime = clamp(config.maxShowTime, 0, 600000);
		config.slideshowSpeed = clamp(config.slideshowSpeed, 0, 600000);
		config.slideshowFadeSpeed = clamp(config.slideshowFadeSpeed, 0, 60000);
		config.blurSpeedPercent = clamp(config.blurSpeedPercent, 0, 100);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
