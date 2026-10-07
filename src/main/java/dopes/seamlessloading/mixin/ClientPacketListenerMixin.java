package dopes.seamlessloading.mixin;

import dopes.seamlessloading.SeamlessSession;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The server teleports the player to their real position right after the login. That is the moment
 * the transition screen learns whether we ended up at the same spot as last time (and can cross fade
 * to the screenshot) or somewhere else (and keeps the slideshow).
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

	@Inject(method = "handleMovePlayer", at = @At("HEAD"))
	private void dopes$onPlayerPosition(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
		SeamlessSession.onServerSpotKnown();
	}
}
