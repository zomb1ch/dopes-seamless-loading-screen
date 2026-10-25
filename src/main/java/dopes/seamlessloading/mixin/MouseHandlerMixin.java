package dopes.seamlessloading.mixin;

import dopes.seamlessloading.SeamlessHud;
import dopes.seamlessloading.SeamlessSession;
import dopes.seamlessloading.SeamlessTips;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Left click cycles the loading tip while the custom loading screen is up.
 *
 * <p>{@code Screen} does not declare {@code mouseClicked} itself (it is a default method of
 * {@code ContainerEventHandler}), so injecting into the screen classes never fires. Instead the
 * raw GLFW button callback is used: it runs for every click while the loading screen is up.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

	@Inject(method = "onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V", at = @At("HEAD"))
	private void dopes$tipOnLeftClick(long window, MouseButtonInfo buttonInfo, int action,
			CallbackInfo ci) {
		if (action == 1 && buttonInfo.button() == 0
				&& Minecraft.getInstance().screen instanceof LevelLoadingScreen
				&& SeamlessSession.isRunning() && SeamlessHud.isEnabled()) {
			SeamlessTips.nextTip();
		}
	}
}