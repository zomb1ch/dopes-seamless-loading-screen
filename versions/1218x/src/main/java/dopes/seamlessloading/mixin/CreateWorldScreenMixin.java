package dopes.seamlessloading.mixin;

import dopes.seamlessloading.SeamlessCurtain;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prepares the transition overlay when a new world is being created, before the vanilla
 * "Preparing world data" screen would be shown.
 */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {

	@Shadow
	WorldCreationUiState uiState;

	@Inject(method = "createNewWorld", at = @At("HEAD"))
	private void dopes$prepareNewWorld(LayeredRegistryAccess<RegistryLayer> registries, WorldData worldData,
			CallbackInfoReturnable<Boolean> cir) {
		SeamlessCurtain.prepareSingleplayer(this.uiState.getTargetFolder());
	}
}
