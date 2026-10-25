package dopes.seamlessloading.mixin;

import dopes.seamlessloading.config.PreviewAtlasCache;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Освобождает атласы анимированных превью, когда экран настроек закрывается.
 *
 * <p>YACL грузит картинку при открытии экрана, но хранит готовый атлас в статическом кэше до
 * конца сессии. Наши превью занимают суммарно несколько сотен мегабайт видеопамяти, поэтому
 * {@link PreviewAtlasCache} выкидывает их из этого кэша, как только экран уходит. Срабатывает
 * только на нашем экране: {@code removed()} вызывается и при открытии подэкрана поверх.
 */
@Mixin(Screen.class)
public abstract class ConfigScreenMixin {

	@Inject(method = "removed", at = @At("HEAD"))
	private void seamless$releasePreviewAtlases(CallbackInfo ci) {
		PreviewAtlasCache.removed((Screen) (Object) this);
	}
}
