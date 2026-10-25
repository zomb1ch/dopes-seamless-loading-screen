package dopes.seamlessloading.config;

import dopes.seamlessloading.DopesSeamlessLoadingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Освобождает видеопамять, занятую анимированными превью настроек, когда экран настроек
 * закрывается.
 *
 * <p>YACL загружает превью лениво — при первом упоминании картинки, то есть при открытии
 * экрана, — но готовый атлас остаётся в статическом кэше до конца сессии. Для наших превью
 * это несколько сотен мегабайт: 600-пиксельные GIF-ы с большим числом кадров складываются
 * в атлас на каждый кадр. Поэтому при закрытии экрана мы выкидываем только свои картинки
 * из кэша и закрываем их рендереры; при следующем открытии настроек YACL соберёт атласы
 * заново.
 *
 * <p>Кэш читается рефлексией намеренно: поле {@code IMAGE_CACHE} одинаково называется и
 * устроено в YACL 3.8.x (1.21.x) и 3.9.x (26.x), а рефлексия позволяет не тащить в этот
 * класс зависимости от YACL — переименование поля не сломает сборку, просто выгрузка
 * перестанет работать.
 */
public final class PreviewAtlasCache {

	private static final String MANAGER_CLASS = "dev.isxander.yacl3.gui.image.ImageRendererManager";
	private static final String CACHE_FIELD = "IMAGE_CACHE";

	/** Идентификаторы превью, которые нужно выгружать вместе с экраном настроек. */
	private static final Set<Identifier> PREVIEWS = new LinkedHashSet<>();

	/** Открытый сейчас экран настроек; null, когда он закрыт. */
	private static Screen openScreen;

	private PreviewAtlasCache() {
	}

	/** Регистрирует превью и возвращает его идентификатор для {@code gifImage(...)}. */
	public static Identifier preview(String path) {
		Identifier id = Identifier.fromNamespaceAndPath(DopesSeamlessLoadingScreen.MOD_ID, path);
		PREVIEWS.add(id);
		return id;
	}

	/** Запоминает открытый экран, чтобы отличить его закрытие от закрытия любого другого экрана. */
	public static void attach(Screen screen) {
		openScreen = screen;
	}

	/**
	 * Вызывается из миксина на {@code Screen#removed}. Атласы выгружаются только когда уходит
	 * именно наш экран настроек: подэкран, открытый поверх него, не должен ничего освобождать.
	 */
	public static void removed(Screen screen) {
		if (screen == null || screen != openScreen) {
			return;
		}
		openScreen = null;
		release();
	}

	private static void release() {
		Map<?, ?> cache = imageCache();
		if (cache == null || PREVIEWS.isEmpty()) {
			return;
		}
		int released = 0;
		for (Identifier id : PREVIEWS) {
			Object entry = cache.remove(id);
			if (entry == null) {
				continue;
			}
			released++;
			if (entry instanceof CompletableFuture<?> future) {
				// Атлас может ещё собираться в фоновом потоке YACL. Закрывать рендерер можно
				// только после его создания и только на потоке рендера: close() освобождает
				// GL-текстуру через TextureManager.
				future.thenAccept(renderer -> runOnClientThread(() -> close(renderer)));
			} else {
				runOnClientThread(() -> close(entry));
			}
		}
		if (released > 0) {
			DopesSeamlessLoadingScreen.LOGGER.debug("[Seamless] Released {} preview atlases", released);
		}
	}

	private static void runOnClientThread(Runnable action) {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			action.run();
		} else {
			client.execute(action);
		}
	}

	private static Map<?, ?> imageCache() {
		try {
			Class<?> manager = Class.forName(MANAGER_CLASS);
			Field field = manager.getDeclaredField(CACHE_FIELD);
			field.setAccessible(true);
			return field.get(null) instanceof Map<?, ?> cache ? cache : null;
		} catch (ReflectiveOperationException | RuntimeException e) {
			DopesSeamlessLoadingScreen.LOGGER.debug("[Seamless] Preview atlas cache is not available", e);
			return null;
		}
	}

	private static void close(Object renderer) {
		try {
			renderer.getClass().getMethod("close").invoke(renderer);
		} catch (ReflectiveOperationException | RuntimeException e) {
			DopesSeamlessLoadingScreen.LOGGER.debug("[Seamless] Unable to close a preview atlas", e);
		}
	}
}
