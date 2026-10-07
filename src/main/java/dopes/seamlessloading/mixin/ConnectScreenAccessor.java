package dopes.seamlessloading.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * {@code ConnectScreen.connect} is private, but the transition screen has to start the connection
 * later (after it has faded in), so it needs a way back in.
 */
@Mixin(ConnectScreen.class)
public interface ConnectScreenAccessor {

	@Invoker("connect")
	void seamless$connect(Minecraft minecraft, ServerAddress serverAddress, ServerData serverData,
			TransferState transferState);
}
