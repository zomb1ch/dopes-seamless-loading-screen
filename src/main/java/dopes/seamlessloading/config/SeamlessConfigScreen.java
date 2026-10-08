package dopes.seamlessloading.config;

import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.gui.YACLScreen;
import dopes.seamlessloading.DopesSeamlessLoadingScreen;
import dopes.seamlessloading.SeamlessScreenshots;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The YACL settings screen, opened from Mod Menu.
 */
public final class SeamlessConfigScreen {

	private static final String PREFIX = DopesSeamlessLoadingScreen.MOD_ID + ".config.";

	private SeamlessConfigScreen() {
	}

	public static Screen create(Screen parent) {
		SeamlessConfig config = SeamlessConfigManager.get();
		SeamlessConfig defaults = new SeamlessConfig();

		Option<Boolean> enabled = bool("general.enabled", defaults.modEnabled,
				() -> config.modEnabled, value -> config.modEnabled = value);

		Option<Boolean> slideshowIfNoScreenshot = bool("general.slideshow", defaults.slideshowIfNoScreenshot,
				() -> config.slideshowIfNoScreenshot, value -> config.slideshowIfNoScreenshot = value);

		Option<Boolean> waitForAllChunks = bool("general.waitChunks", defaults.waitForAllChunks,
				() -> config.waitForAllChunks, value -> config.waitForAllChunks = value);

		Option<Boolean> chunkCounter = bool("general.chunkCounter", defaults.chunkCounter,
				() -> config.chunkCounter, value -> config.chunkCounter = value);

		Option<Boolean> customLoadingScreen = bool("general.customScreen", defaults.customLoadingScreen,
				() -> config.customLoadingScreen, value -> config.customLoadingScreen = value);

		Option<Boolean> serverPositionCheck = bool("general.serverPositionCheck", defaults.serverPositionCheck,
				() -> config.serverPositionCheck, value -> config.serverPositionCheck = value);

		Option<Boolean> transitionScreens = bool("general.transition", defaults.transitionScreens,
				() -> config.transitionScreens, value -> config.transitionScreens = value);

		Option<Integer> transitionFadeDuration = integer("general.transitionFade", defaults.transitionFadeDuration,
				0, 10000, () -> config.transitionFadeDuration, value -> config.transitionFadeDuration = value);

		Option<Integer> blurStrength = slider("display.blurStrength", defaults.blurStrength, 0, 64,
				() -> config.blurStrength, value -> config.blurStrength = value);

		Option<Integer> backgroundDim = slider("display.dim", defaults.backgroundDim, 0, 100,
				() -> config.backgroundDim, value -> config.backgroundDim = value);

		Option<Integer> fadeDuration = integer("display.fadeDuration", defaults.fadeDuration, 0, 10000,
				() -> config.fadeDuration, value -> config.fadeDuration = value);

		Option<Integer> blurSpeed = slider("display.blurSpeed", defaults.blurSpeedPercent, 0, 100,
				() -> config.blurSpeedPercent, value -> config.blurSpeedPercent = value);

		Option<Integer> imageSize = slider("display.imageSize", defaults.imageSize, 10, 100,
				() -> config.imageSize, value -> config.imageSize = value);

		Option<Integer> minShowTime = integer("timing.minShowTime", defaults.minShowTime, 0, 600000,
				() -> config.minShowTime, value -> config.minShowTime = value);

		Option<Integer> maxShowTime = integer("timing.maxShowTime", defaults.maxShowTime, 0, 600000,
				() -> config.maxShowTime, value -> config.maxShowTime = value);

		Option<Integer> slideshowSpeed = integer("slideshow.speed", defaults.slideshowSpeed, 0, 60000,
				() -> config.slideshowSpeed, value -> config.slideshowSpeed = value);

		Option<Integer> slideshowFadeSpeed = integer("slideshow.fadeSpeed", defaults.slideshowFadeSpeed, 0, 10000,
				() -> config.slideshowFadeSpeed, value -> config.slideshowFadeSpeed = value);

		ButtonOption openFolder = ButtonOption.createBuilder()
				.name(name("slideshow.folder"))
				.text(name("slideshow.openFolder"))
				.description(OptionDescription.of(desc("slideshow.openFolder")))
				.action((screen, option) -> openSlideshowFolder())
				.build();

		return YetAnotherConfigLib.createBuilder()
				.title(name("title"))
				// Everything lives on one page, split into collapsible groups instead of one tab per
				// category. The now pointless tab buttons are hidden by hideTabs below.
				.category(ConfigCategory.createBuilder()
						.name(name("page"))
						.group(group("general", enabled, serverPositionCheck, slideshowIfNoScreenshot,
								waitForAllChunks, customLoadingScreen, chunkCounter))
						.group(group("display", transitionScreens, transitionFadeDuration, blurStrength, blurSpeed,
								backgroundDim, fadeDuration, imageSize))
						.group(group("timing", minShowTime, maxShowTime))
						.group(group("slideshow", slideshowSpeed, slideshowFadeSpeed, openFolder))
						.build())
				.save(SeamlessConfigManager::save)
				.screenInit(SeamlessConfigScreen::hideTabs)
				.build()
				.generateScreen(parent);
	}

	/**
	 * With a single category the tab bar has nothing to switch between, so its buttons are hidden to
	 * leave a clean single page. {@code AbstractWidget} skips rendering and mouse input when it is
	 * not visible, which is exactly what is needed here.
	 */
	private static void hideTabs(YACLScreen screen) {
		for (GuiEventListener child : screen.tabNavigationBar.children()) {
			if (child instanceof AbstractWidget widget) {
				widget.visible = false;
			}
		}
	}

	private static OptionGroup group(String key, Option<?>... options) {
		return OptionGroup.createBuilder()
				.name(name(key))
				.options(List.of(options))
				.build();
	}

	private static void openSlideshowFolder() {
		Path directory = SeamlessScreenshots.slideshowDirectory();
		try {
			Files.createDirectories(directory);
			// Открываем папку средствами Java: Util.getPlatform().openPath(...) убрали в 26.3.
			Desktop.getDesktop().open(directory.toFile());
		} catch (Exception e) {
			DopesSeamlessLoadingScreen.LOGGER.error("[Seamless] Unable to open {}", directory, e);
		}
	}

	private static Component name(String key) {
		return Component.translatable(PREFIX + key);
	}

	private static Component desc(String key) {
		return Component.translatable(PREFIX + key + ".description");
	}

	private static <T> Option<T> build(String key, T defaultValue, Supplier<T> getter, Consumer<T> setter,
			Function<Option<T>, ControllerBuilder<T>> controller) {
		return Option.<T>createBuilder()
				.name(name(key))
				.description(OptionDescription.of(desc(key)))
				.binding(defaultValue, getter, setter)
				.controller(controller)
				.build();
	}

	private static Option<Boolean> bool(String key, boolean defaultValue, Supplier<Boolean> getter,
			Consumer<Boolean> setter) {
		return build(key, defaultValue, getter, setter, option -> BooleanControllerBuilder.create(option).yesNoFormatter());
	}

	private static Option<Integer> slider(String key, int defaultValue, int min, int max, Supplier<Integer> getter,
			Consumer<Integer> setter) {
		return build(key, defaultValue, getter, setter,
				option -> IntegerSliderControllerBuilder.create(option).range(min, max).step(1));
	}

	private static Option<Integer> integer(String key, int defaultValue, int min, int max, Supplier<Integer> getter,
			Consumer<Integer> setter) {
		return build(key, defaultValue, getter, setter,
				option -> IntegerFieldControllerBuilder.create(option).range(min, max));
	}
}
