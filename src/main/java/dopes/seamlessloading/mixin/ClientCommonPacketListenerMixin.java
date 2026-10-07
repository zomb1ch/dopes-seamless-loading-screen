package dopes.seamlessloading.mixin;

import dopes.seamlessloading.SeamlessCapture;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.DisconnectionDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerMixin {

	/** The server (or the connection) closed the world: save a screenshot before leaving it. */
	@Inject(method = "onDisconnect", at = @At("HEAD"), cancellable = true)
	private void dopes$captureOnServerDisconnect(DisconnectionDetails details, CallbackInfo ci) {
		if (SeamlessCapture.request("disconnecting from the server",
				() -> ((ClientCommonPacketListenerImpl) (Object) this).onDisconnect(details))) {
			ci.cancel();
		}
	}
}
