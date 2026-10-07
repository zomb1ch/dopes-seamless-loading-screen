package dopes.seamlessloading.config;

public class SeamlessConfig {
	public boolean modEnabled = true;
	public boolean slideshowIfNoScreenshot = true;
	public boolean waitForAllChunks = true;
	public boolean chunkCounter = false;

	/** Whether screenshots are taken and shown for multiplayer servers. */
	public boolean screenshotsOnServers = false;

	/** Show an auxiliary screenshot screen over the transitions into and out of a world. */
	public boolean transitionScreens = true;

	/** Fade duration of those screens, in milliseconds. The same for entering and leaving. */
	public int transitionFadeDuration = 1500;

	public int blurStrength = 16;
	public int backgroundDim = 35;
	public int fadeDuration = 800;

	/** How much of the fade duration the blur animation takes, in percent (0 = no animation). */
	public int blurSpeedPercent = 50;

	/** Resolution the screenshot is rendered at, in percent. The image always covers the screen. */
	public int imageSize = 100;

	public int minShowTime = 2000;
	public int maxShowTime = 30000;

	public int slideshowSpeed = 5000;
	public int slideshowFadeSpeed = 1500;
}
