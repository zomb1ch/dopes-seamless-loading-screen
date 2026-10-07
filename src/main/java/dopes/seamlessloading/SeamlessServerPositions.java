package dopes.seamlessloading;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Remembers where you were the last time you left each server.
 *
 * <p>Many servers drop you into a lobby (or a different world) when you rejoin. Showing the
 * screenshot of where you actually were would then be misleading, so it is only shown when the
 * server puts you back at (roughly) the same spot. Stored in
 * {@code config/dopes_seamless_loading_screen_servers.json}.
 */
public final class SeamlessServerPositions {

	/** How far from the remembered spot still counts as "the same place", in blocks. */
	private static final double MATCH_RADIUS = 16.0;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final Type MAP_TYPE = new TypeToken<Map<String, Spot>>() {
	}.getType();

	private static final Path CONFIG_PATH = FabricLoader.getInstance()
			.getConfigDir()
			.resolve(DopesSeamlessLoadingScreen.MOD_ID + "_servers.json");

	private static Map<String, Spot> spots = new HashMap<>();

	private SeamlessServerPositions() {
	}

	/** Where the player was the last time they left a server. */
	public record Spot(String dimension, double x, double y, double z) {
	}

	public static void load() {
		if (!Files.isRegularFile(CONFIG_PATH)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
			Map<String, Spot> loaded = GSON.fromJson(reader, MAP_TYPE);
			if (loaded != null) {
				spots = loaded;
			}
		} catch (Exception e) {
			DopesSeamlessLoadingScreen.LOGGER.error("[Seamless] Unable to read the server positions", e);
		}
	}

	public static synchronized void remember(String address, String dimension, double x, double y, double z) {
		if (address == null || address.isBlank()) {
			return;
		}

		spots.put(address, new Spot(dimension, x, y, z));
		save();
	}

	/** Whether the given spot is (roughly) the same place the player was the last time. */
	public static synchronized boolean matches(String address, String dimension, double x, double y, double z) {
		Spot spot = address == null ? null : spots.get(address);
		if (spot == null || !spot.dimension().equals(dimension)) {
			return false;
		}

		double dx = spot.x() - x;
		double dy = spot.y() - y;
		double dz = spot.z() - z;
		return dx * dx + dy * dy + dz * dz <= MATCH_RADIUS * MATCH_RADIUS;
	}

	private static void save() {
		try {
			Path parent = CONFIG_PATH.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}

			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(spots, MAP_TYPE, writer);
			}
		} catch (Exception e) {
			DopesSeamlessLoadingScreen.LOGGER.error("[Seamless] Unable to save the server positions", e);
		}
	}
}
