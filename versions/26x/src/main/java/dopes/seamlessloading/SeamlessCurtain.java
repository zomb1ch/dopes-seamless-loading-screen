package dopes.seamlessloading;

import dopes.seamlessloading.config.SeamlessConfig;
import dopes.seamlessloading.config.SeamlessConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.nio.file.Path;

/**
 * The auxiliary screen drawn over the transitions into and out of a world.
 *
 * <p>It is not a {@link Screen} of its own: it is an overlay drawn on top of whatever screen is
 * currently shown (see {@code ScreenMixin}), which is what allows it to hide the intermediate
 * vanilla screens ("Saving world", "Connecting to the server", ...) and to hand over to the loading
 * screen without a single visible cut.
 *
 * <p>Entering a world:
 * <ol>
 *   <li>the screenshot fades in over the screen you came from,</li>
 *   <li>once it is fully visible the world is actually loaded,</li>
 *   <li>the overlay stays on top until the loading screen is there, hiding everything in between,</li>
 *   <li>then it fades out, revealing the loading screen with the very same picture behind it.</li>
 * </ol>
 *
 * <p>Leaving a world works the same way, except that the screenshot is captured first and that there
 * is no loading screen to wait for on the way out.
 *
 * <p>The overlay always shows a static, fully blurred image with the same dim as the loading screen,
 * so the hand over between the two is invisible.
 */
public final class SeamlessCurtain {

	private enum Phase {
		/** Nothing to do. */
		IDLE,
		/** Leaving: waiting for the fresh screenshot to be written to disk. */
		WAIT_FOR_CAPTURE,
		/** The image fades in over the current screen. */
		FADE_IN,
		/** Entering: the load is running, waiting for the loading screen to show up. */
		WAIT_FOR_LOADING_SCREEN,
		/** The image fades out, revealing the screen underneath. */
		FADE_OUT
	}

	/** Give up waiting for the loading screen after this long. */
	private static final long WAIT_FOR_SCREEN_TIMEOUT_MILLIS = 20000L;
	/** Give up waiting for the captured screenshot after this long. */
	private static final long CAPTURE_TIMEOUT_MILLIS = 5000L;

	private static Phase phase = Phase.IDLE;
	private static long phaseStartMillis;
	private static Runnable action;
	private static boolean waitForLoadingScreen;
	private static boolean endSessionAfterFade;
	private static boolean running;
	/** A world is being opened/created and the vanilla loading screens must stay hidden. */
	private static boolean prepared;
	private static String label = "";
	/** HUD style drawn on the overlay, or {@code null} when the loading screen handles it. */
	private static SeamlessHud.Style hudStyle;
	/** Screen that was current when the enter overlay started; used to tell loading apart from errors. */
	private static Screen originScreen;

	private SeamlessCurtain() {
	}

	/** {@code true} while the overlay is on screen. */
	public static boolean isActive() {
		return phase != Phase.IDLE;
	}

	/**
	 * {@code true} while the deferred action is running. The mixins check this so that re-entering
	 * {@code doWorldLoad} / {@code connect} does not set the session up a second time.
	 */
	public static boolean isReplaying() {
		return running;
	}

	/**
	 * Starts the overlay for entering a world. The background has to be prepared already (see
	 * {@link SeamlessSession#begin(Path)}).
	 *
	 * @return {@code true} if the caller must abort; the overlay runs {@code action} once it is fully
	 *         faded in
	 */
	public static boolean beginEnter(String source, Runnable action) {
		if (running || !enabled() || !SeamlessBackground.isActive()) {
			return false;
		}

		originScreen = Minecraft.getInstance().screen;
		waitForLoadingScreen = true;
		endSessionAfterFade = false;
		start(Phase.FADE_IN, source, action);
		return true;
	}

	/**
	 * Prepares the overlay as soon as a world is picked or created, <i>before</i> it actually loads.
	 *
	 * <p>Nothing is drawn yet: the fade is started later (see {@link #beginEnter(String, Runnable)},
	 * called from the world load), so that it plays while the game is rendering. Starting it here
	 * instead would make it stutter, because loading the resources blocks the render thread. All this
	 * does is show the background and remember that the vanilla "Reading world data" /
	 * "Loading resources" screens have to be hidden from now on (see {@link #shouldHideScreen(Screen)}).
	 */
	public static void prepare(Path screenshot) {
		if (running || !enabled() || prepared) {
			return;
		}

		SeamlessSession.begin(screenshot);
		SeamlessHud.reset();
		prepared = SeamlessBackground.isActive();
	}

	/** {@code true} while a world is being prepared but the overlay has not faded in yet. */
	public static boolean isPrepared() {
		return prepared;
	}

	/** Prepares the overlay for a singleplayer world that is about to be opened or created. */
	public static void prepareSingleplayer(String worldId) {
		SeamlessSession.setSingleplayerWorldId(worldId);
		prepare(SeamlessScreenshots.singleplayer(worldId));
	}

	/** Prepares the overlay for a server that is about to be joined. */
	public static void prepareServer(ServerData serverData) {
		Path screenshot = SeamlessScreenshots.server(serverData.ip);

		if (!SeamlessConfigManager.get().serverPositionCheck) {
			// No position check: show the screenshot right away.
			prepare(screenshot);
			return;
		}

		// Start with the slideshow: whether the screenshot fits is only known once the server tells
		// us where the player actually is (see SeamlessSession#onServerSpotKnown).
		prepare(null);
		SeamlessSession.setServer(serverData.ip, screenshot);
	}

	/**
	 * Starts the overlay for leaving a world. A fresh screenshot is captured first.
	 *
	 * @return {@code true} if the caller must abort; the overlay runs {@code action} once the fresh
	 *         screenshot has faded in
	 */
	public static boolean beginLeave(String source, Runnable action) {
		if (running || !enabled()) {
			return false;
		}

		// Capture only, the continuation is run by the overlay once it is on screen.
		if (!SeamlessCapture.request(source, null)) {
			return false;
		}

		originScreen = Minecraft.getInstance().screen;
		waitForLoadingScreen = false;
		endSessionAfterFade = true;
		start(Phase.WAIT_FOR_CAPTURE, source, action);
		return true;
	}

	private static void start(Phase next, String source, Runnable nextAction) {
		phase = next;
		phaseStartMillis = Util.getMillis();
		action = nextAction;
		label = source;
		hudStyle = styleFor(source);
		DopesSeamlessLoadingScreen.LOGGER.info("[Seamless] Transition screen: {} ({})", next, source);
	}

	/**
	 * The overlay draws the "saving" / "connecting" text itself. While a world is loading there is
	 * nothing to draw here: the loading screen shows its own HUD (with the progress bar).
	 */
	private static SeamlessHud.Style styleFor(String source) {
		if (source.contains("connecting")) {
			return SeamlessHud.Style.CONNECTING;
		}
		if (source.contains("quitting") || source.contains("disconnecting") || source.contains("closing")) {
			return SeamlessHud.Style.SAVING;
		}

		return null;
	}

	/**
	 * Advances the overlay. Called once per client tick, which is also where the deferred actions are
	 * run: they may load a world or disconnect from one, and doing that from inside
	 * {@code Screen#tick} breaks the screen ticking bookkeeping of the Fabric screen API (it ends up
	 * firing its after-tick event with a null screen).
	 */
	public static void tick() {
		advance(true);
	}

	/**
	 * Advances the overlay while the loading screen ticks itself inside the world load loop. That
	 * loop does not run the client tick at all, so without this the fade out over the loading screen
	 * would never happen. Deferred actions are never run from here: the world load loop must not be
	 * re-entered.
	 */
	public static void tickLoadingScreen() {
		advance(false);
	}

	private static void advance(boolean mayRunActions) {
		if (phase == Phase.IDLE) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		long now = Util.getMillis();

		switch (phase) {
			case WAIT_FOR_CAPTURE -> {
				if (mayRunActions) {
					tickWaitForCapture(now);
				}
			}
			case FADE_IN -> {
				if (!isLoadingScreen(minecraft.screen)) {
					// Something unexpected appeared (an error, a confirmation, ...): get out of the way.
					endSessionAfterFade = true;
					start(Phase.FADE_OUT, label, null);
				} else if (now - phaseStartMillis >= fadeDuration()) {
					if (action == null) {
						// Nothing was deferred (the world loads on its own), so the phase can advance
						// even from the loading screen tick, where deferred actions must not run.
						start(waitForLoadingScreen ? Phase.WAIT_FOR_LOADING_SCREEN : Phase.FADE_OUT, label, null);
					} else if (mayRunActions) {
						runAction();
						// Running the action may already have advanced the phase: while it loads the
						// world, the loading screen ticks itself and moves us on to the next phase (or
						// all the way to the end). Only move on here if we are still fading in.
						if (phase == Phase.FADE_IN) {
							start(waitForLoadingScreen ? Phase.WAIT_FOR_LOADING_SCREEN : Phase.FADE_OUT, label, null);
						}
					}
				}
			}
			case WAIT_FOR_LOADING_SCREEN -> {
				Screen screen = minecraft.screen;
				if (screen instanceof LevelLoadingScreen) {
					start(Phase.FADE_OUT, label, null);
				} else if (!isLoadingScreen(screen)
						|| now - phaseStartMillis >= WAIT_FOR_SCREEN_TIMEOUT_MILLIS) {
					// The world never showed up (failed connection, cancelled, error, ...): step aside.
					endSessionAfterFade = true;
					start(Phase.FADE_OUT, label, null);
				}
			}
			case FADE_OUT -> {
				if (now - phaseStartMillis >= fadeDuration()) {
					finish();
				}
			}
			default -> {
			}
		}
	}

	private static void tickWaitForCapture(long now) {
		Path captured = SeamlessCapture.takeWrittenPath();
		if (captured != null) {
			SeamlessBackground.show(captured);
			if (SeamlessBackground.isActive()) {
				start(Phase.FADE_IN, label, action);
			} else {
				// Nothing to draw: do not keep the player waiting inside the world.
				runAction();
				finish();
			}
		} else if (now - phaseStartMillis >= CAPTURE_TIMEOUT_MILLIS) {
			runAction();
			finish();
		}
	}

	/** Draws the overlay on top of the current screen. */
	public static void render(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight) {
		if (phase == Phase.IDLE || phase == Phase.WAIT_FOR_CAPTURE) {
			return;
		}

		float alpha = switch (phase) {
			case FADE_IN -> progress();
			case FADE_OUT -> 1.0F - progress();
			default -> 1.0F;
		};

		SeamlessBackground.renderOverlay(graphics, screenWidth, screenHeight, alpha);

		if (hudStyle != null) {
			SeamlessHud.render(graphics, screenWidth, screenHeight, hudStyle, alpha);
		}
	}

	private static float progress() {
		float duration = fadeDuration();
		return duration <= 0.0F ? 1.0F
				: Mth.clamp((float) (Util.getMillis() - phaseStartMillis) / duration, 0.0F, 1.0F);
	}

	private static float fadeDuration() {
		return Math.max(0, SeamlessConfigManager.get().transitionFadeDuration);
	}

	private static boolean enabled() {
		SeamlessConfig config = SeamlessConfigManager.get();
		return config.modEnabled && config.transitionScreens;
	}

	/**
	 * {@code true} while a vanilla "loading" message screen must not be shown: the overlay covers it
	 * instead, so only the screenshot stays visible during the whole transition.
	 */
	public static boolean shouldHideScreen(Screen screen) {
		if (screen == null) {
			return false;
		}

		boolean transitioning = prepared || (phase != Phase.IDLE && phase != Phase.WAIT_FOR_CAPTURE);
		if (!transitioning) {
			return false;
		}

		return screen instanceof GenericMessageScreen
				|| screen instanceof ProgressScreen
				|| screen instanceof ConnectScreen;
	}

	/** Screens that legitimately appear while the world is still being loaded. */
	private static boolean isLoadingScreen(Screen screen) {
		return screen == null
				|| screen == originScreen
				|| screen instanceof LevelLoadingScreen
				|| screen instanceof GenericMessageScreen
				|| screen instanceof ProgressScreen
				|| screen instanceof ConnectScreen;
	}

	/** Runs the action the caller gave up on, guarding against the mixins intercepting it again. */
	private static void runAction() {
		Runnable run = action;
		action = null;
		if (run == null) {
			return;
		}

		DopesSeamlessLoadingScreen.LOGGER.info("[Seamless] Transition screen: running the deferred action ({})", label);
		running = true;
		try {
			run.run();
		} catch (Throwable t) {
			DopesSeamlessLoadingScreen.LOGGER.error("[Seamless] Error while running the deferred action", t);
		} finally {
			running = false;
		}
	}

	private static void finish() {
		phase = Phase.IDLE;
		action = null;
		prepared = false;
		originScreen = null;

		if (endSessionAfterFade) {
			SeamlessSession.end();
		}

		endSessionAfterFade = false;
		waitForLoadingScreen = false;
	}
}
