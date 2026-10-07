package dopes.seamlessloading.mixin;

import com.mojang.serialization.Dynamic;
import dopes.seamlessloading.SeamlessCurtain;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Starts the transition overlay as soon as an existing world is picked, so it covers the vanilla
 * "Reading world data" / "Loading resources" screens instead of letting them flash before the actual
 * loading screen shows up.
 */
@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenFlowsMixin {

	/** The world was picked from the world list / quick play. */
	@Inject(method = "openWorld", at = @At("HEAD"))
	private void dopes$beginEnter(String levelId, Runnable onCancel, CallbackInfo ci) {
		SeamlessCurtain.armEnter("entering the world", levelId);
	}

	/** Second entry point: after a backup / version confirmation, just before the resources load. */
	@Inject(method = "openWorldLoadLevelStem", at = @At("HEAD"))
	private void dopes$beginEnterAfterConfirm(LevelStorageSource.LevelStorageAccess levelStorageAccess,
			Dynamic<?> dynamic, boolean safeMode, Runnable onCancel, CallbackInfo ci) {
		SeamlessCurtain.armEnter("entering the world", levelStorageAccess.getLevelId());
	}
}
