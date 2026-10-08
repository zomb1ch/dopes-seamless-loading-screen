package dopes.seamlessloading.mixin;

import dopes.seamlessloading.SeamlessCurtain;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the auxiliary transition screen on top of whatever screen is currently shown. Doing it here,
 * instead of using a screen of our own, is what lets the overlay survive screen changes and hide the
 * vanilla screens that appear in between.
 *
 * <p>The overlay is advanced from {@code Minecraft#tick} and {@code LevelLoadingScreen#tick} instead
 * of from here: running the deferred world load from inside a screen tick confuses the screen
 * ticking bookkeeping of the Fabric screen API.
 */
@Mixin(Screen.class)
public abstract class ScreenMixin {

	/**
	 * {@code renderWithTooltipAndSubtitles} is {@code final} and runs after the screen has drawn
	 * itself completely (unlike {@code Screen#render}, whose tail fires in the middle of the drawing
	 * of subclasses such as the loading screen), so this is where the overlay can safely cover
	 * everything.
	 */
	@Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
	private void dopes$curtainRender(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick,
			CallbackInfo ci) {
		Screen screen = (Screen) (Object) this;
		SeamlessCurtain.render(guiGraphics, screen.width, screen.height);
	}
}
