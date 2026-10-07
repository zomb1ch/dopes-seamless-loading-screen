package dopes.seamlessloading.mixin;

import dopes.seamlessloading.SeamlessCurtain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin {

	/**
	 * Prepares the overlay before the vanilla "Connecting to the server" screen is shown, so that
	 * screen stays hidden and only the screenshot is visible while the connection is being made.
	 */
	@Inject(method = "startConnecting", at = @At("HEAD"))
	private static void dopes$prepareServer(Screen parent, Minecraft minecraft, ServerAddress serverAddress,
			ServerData serverData, boolean quickPlay, TransferState transferState, CallbackInfo ci) {
		SeamlessCurtain.prepareServer(serverData);
	}

	/**
	 * Starts the overlay once the connection thread is about to be started, which is after the old
	 * world has already been closed, so the screenshot of the server we are joining can be loaded now.
	 */
	@Inject(
			method = "connect(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/multiplayer/resolver/ServerAddress;Lnet/minecraft/client/multiplayer/ServerData;Lnet/minecraft/client/multiplayer/TransferState;)V",
			at = @At("HEAD")
	)
	private void dopes$beginServerSession(Minecraft minecraft, ServerAddress serverAddress, ServerData serverData,
			TransferState transferState, CallbackInfo ci) {
		if (SeamlessCurtain.beginEnter("connecting to the server",
				() -> ((ConnectScreenAccessor) (Object) this)
						.seamless$connect(minecraft, serverAddress, serverData, transferState))) {
			ci.cancel();
		}
	}
}
