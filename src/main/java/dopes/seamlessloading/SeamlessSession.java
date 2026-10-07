package dopes.seamlessloading;

import dopes.seamlessloading.config.SeamlessConfig;
import dopes.seamlessloading.config.SeamlessConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

import java.nio.file.Path;

/**
 * State of the currently running "join a world" sequence: which image is displayed, when it
 * started and whether the loading screen should stay open for a bit longer.
 */
public final class SeamlessSession {

	private static boolean active;
	private static long startMillis;
	private static String singleplayerWorldId;

	/** Chunk count seen on the previous check and when it last changed, used to detect "loading done". */
	private static int lastChunkCount = -2;
	private static long lastChunkCountChange;
	/** Highest chunk count seen so far; stays 0 while the server has not sent a single chunk. */
	private static int maxChunkCount;

	/** How long the chunk count has to stay unchanged before the world is considered fully loaded. */
	private static final long CHUNK_SETTLE_MILLIS = 1000L;

	/**
	 * How long the server gets to send the very first chunk. Some servers (void spawns, registration
	 * lobbies, ...) never send one, and waiting for chunks that will never arrive would keep the
	 * loading screen open until the max show time.
	 */
	private static final long FIRST_CHUNK_GRACE_MILLIS = 5000L;

	private SeamlessSession() {
	}

	/** Remembers which singleplayer world folder is currently being loaded. */
	public static void setSingleplayerWorldId(String worldId) {
		singleplayerWorldId = worldId;
	}

	/** The folder name of the singleplayer world that is currently open, or {@code null}. */
	public static String singleplayerWorldId() {
		return singleplayerWorldId;
	}

	/** Called when the player starts connecting to a world / server. */
	public static void begin(Path screenshot) {
		end();

		SeamlessConfig config = SeamlessConfigManager.get();
		if (!config.modEnabled) {
			return;
		}

		active = true;
		startMillis = Util.getMillis();
		lastChunkCount = -2;
		lastChunkCountChange = 0L;
		maxChunkCount = 0;
		SeamlessBackground.show(screenshot);
	}

	/** {@code true} while a join is in progress, whether or not there is an image to draw. */
	public static boolean isRunning() {
		return active && SeamlessConfigManager.get().modEnabled;
	}

	/** {@code true} while a join is in progress and there is actually a background to draw. */
	public static boolean isActive() {
		return isRunning() && SeamlessBackground.isActive();
	}

	public static float elapsedMs() {
		return (float) (Util.getMillis() - startMillis);
	}

	public static void end() {
		active = false;
		SeamlessBackground.clear();
	}

	/**
	 * Returns {@code true} while the loading screen must not close yet. This implements both the
	 * "min show time" and the "wait for all chunks" options.
	 */
	public static boolean shouldHoldLoadingScreen() {
		SeamlessConfig config = SeamlessConfigManager.get();
		if (!active || !config.modEnabled) {
			return false;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.levelRenderer == null) {
			return false;
		}

		float elapsed = elapsedMs();
		if (config.maxShowTime > 0 && elapsed >= config.maxShowTime) {
			return false;
		}

		if (config.minShowTime > 0 && elapsed < config.minShowTime) {
			return true;
		}

		if (!config.waitForAllChunks) {
			return false;
		}

		// The server has not sent a new chunk for a while. This works for any server view distance,
		// unlike comparing the count against a fixed expected number.
		long now = Util.getMillis();
		int chunks = loadedChunks();
		if (chunks != lastChunkCount) {
			lastChunkCount = chunks;
			lastChunkCountChange = now;
			maxChunkCount = Math.max(maxChunkCount, chunks);
		}

		// Every chunk the renderer knows about has been built, and the server is either clearly
		// sending chunks (so the settle timer above is meaningful) or has had enough time to send the
		// first one. The second part is what stops a void spawn or a registration lobby, where no
		// chunk ever arrives, from keeping the screen open until the max show time.
		boolean rendererSettled = minecraft.levelRenderer.hasRenderedAllSections()
				&& (maxChunkCount > 0 || now - startMillis >= FIRST_CHUNK_GRACE_MILLIS);

		return !(rendererSettled && now - lastChunkCountChange >= CHUNK_SETTLE_MILLIS);
	}

	/** Number of chunks the client currently has loaded, or {@code -1} if unknown. */
	public static int loadedChunks() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return -1;
		}

		try {
			return minecraft.level.getChunkSource().getLoadedChunksCount();
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * Maximum number of chunks the client keeps around: the storage radius is
	 * {@code max(2, renderDistance) + 3}, so it is a square of {@code 2 * radius + 1} chunks.
	 */
	public static int expectedChunks() {
		Minecraft minecraft = Minecraft.getInstance();
		int radius = Math.max(2, minecraft.options.getEffectiveRenderDistance()) + 3;
		int side = radius * 2 + 1;
		return side * side;
	}
}
