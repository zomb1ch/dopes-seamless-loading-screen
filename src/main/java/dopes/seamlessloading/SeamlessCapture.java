package dopes.seamlessloading;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import dopes.seamlessloading.config.SeamlessConfig;
import dopes.seamlessloading.config.SeamlessConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.util.Util;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Takes the screenshot of the world you are leaving.
 *
 * <p>The screenshot has to be taken while the world is rendered, but the decision to leave the
 * world is made while no frame is being drawn. Therefore every exit path first <i>requests</i> a
 * capture and only performs the actual action once the capture is done:
 * <pre>
 * if (SeamlessCapture.request(() -&gt; realAction())) {
 *     return; // cancel this call, the continuation will run it again later
 * }
 * realAction();
 * </pre>
 */
public final class SeamlessCapture {

	private static volatile boolean pending;
	private static volatile boolean reentrant;
	private static volatile Runnable continuation;
	/** Path of the last successfully written screenshot, waiting to be picked up. */
	private static volatile Path writtenPath;

	private SeamlessCapture() {
	}

	/**
	 * Requests a screenshot. Returns {@code true} if the caller must abort and let the continuation
	 * perform the action later.
	 *
	 * @param source short description of the exit path, only used for logging
	 */
	public static boolean request(String source, Runnable after) {
		if (reentrant || !SeamlessConfigManager.get().modEnabled) {
			return false;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || !shouldCapture(minecraft)) {
			return false;
		}

		pending = true;
		continuation = after;
		writtenPath = null;
		DopesSeamlessLoadingScreen.LOGGER.info("[Seamless] Taking a screenshot before {}", source);
		return true;
	}

	/**
	 * Whether the current frame should be captured. The auxiliary exit screen always needs a fresh
	 * screenshot, even on servers with server screenshots turned off, so that option only applies
	 * when the auxiliary screens are disabled.
	 */
	private static boolean shouldCapture(Minecraft minecraft) {
		SeamlessConfig config = SeamlessConfigManager.get();
		if (config.transitionScreens) {
			return true;
		}

		return config.screenshotsOnServers || minecraft.getSingleplayerServer() != null;
	}

	/** Picks up the path of the screenshot written since the last call, or {@code null}. */
	public static Path takeWrittenPath() {
		Path path = writtenPath;
		writtenPath = null;
		return path;
	}

	public static void cancel() {
		pending = false;
		continuation = null;
	}

	/** Called right after the world has been rendered into the main render target. */
	public static void onFrameRendered() {
		if (!pending) {
			return;
		}

		pending = false;

		Path target = resolveTargetPath();
		if (target != null) {
			capture(target);
		}

		runContinuation();
	}

	private static void runContinuation() {
		Runnable action = continuation;
		continuation = null;
		if (action == null) {
			return;
		}

		reentrant = true;
		try {
			action.run();
		} catch (Throwable t) {
			DopesSeamlessLoadingScreen.LOGGER.error("[Seamless] Error while leaving the world", t);
		} finally {
			reentrant = false;
		}
	}

	/** The PNG the current world would be saved to, or {@code null} if it cannot be determined. */
	public static Path resolveTargetPath() {
		Minecraft minecraft = Minecraft.getInstance();

		MinecraftServer server = minecraft.getSingleplayerServer();
		if (server != null) {
			String worldId = SeamlessSession.singleplayerWorldId();
			if (worldId != null) {
				return SeamlessScreenshots.singleplayer(worldId);
			}

			Path worldPath = server.getWorldPath(LevelResource.ROOT);
			if (worldPath != null) {
				Path fileName = worldPath.normalize().getFileName();
				if (fileName != null) {
					return SeamlessScreenshots.singleplayer(fileName.toString());
				}
			}
		}

		ServerData serverData = minecraft.getCurrentServer();
		if (serverData != null && serverData.ip != null && !serverData.ip.isBlank() && !serverData.isLan()) {
			return SeamlessScreenshots.server(serverData.ip);
		}

		return null;
	}

	private static void capture(Path target) {
		Minecraft minecraft = Minecraft.getInstance();
		RenderTarget renderTarget = minecraft.getMainRenderTarget();
		if (renderTarget == null) {
			return;
		}

		Screenshot.takeScreenshot(renderTarget, image -> Util.ioPool().execute(() -> {
			try (NativeImage nativeImage = image) {
				Path parent = target.getParent();
				if (parent != null) {
					Files.createDirectories(parent);
				}

				nativeImage.writeToFile(target);
				writtenPath = target;
				DopesSeamlessLoadingScreen.LOGGER.info("[Seamless] Saved the screenshot of the world to {}", target);
			} catch (Exception e) {
				DopesSeamlessLoadingScreen.LOGGER.error("[Seamless] Unable to save the screenshot to {}", target, e);
			}
		}));
	}
}
