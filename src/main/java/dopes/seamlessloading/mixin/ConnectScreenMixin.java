package dopes.seamlessloading.mixin;

import dopes.seamlessloading.SeamlessScreenshots;
import dopes.seamlessloading.SeamlessSession;
import dopes.seamlessloading.config.SeamlessConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin {

	/**
	 * Called once the connection thread is started, which is after the old world has already been
	 * closed, so the screenshot of the server we are joining can be loaded now.
	 */
	@Inject(
			method = "connect(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/multiplayer/resolver/ServerAddress;Lnet/minecraft/client/multiplayer/ServerData;Lnet/minecraft/client/multiplayer/TransferState;)V",
			at = @At("HEAD")
	)
	private void dopes$beginServerSession(Minecraft minecraft, ServerAddress serverAddress, ServerData serverData,
			TransferState transferState, CallbackInfo ci) {
		// With server screenshots disabled the session still runs, so the slideshow and the chunk
		// counter keep working; only the screenshot is skipped (both reading and writing it).
		Path screenshot = SeamlessConfigManager.get().screenshotsOnServers
				? SeamlessScreenshots.server(serverData.ip)
				: null;
		SeamlessSession.begin(screenshot);
	}
}
