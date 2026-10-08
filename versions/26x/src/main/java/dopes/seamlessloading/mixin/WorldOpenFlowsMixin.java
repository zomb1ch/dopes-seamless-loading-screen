package dopes.seamlessloading.mixin;

import com.mojang.serialization.Dynamic;
import dopes.seamlessloading.SeamlessCurtain;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Function;

/**
 * Prepares the transition overlay as soon as an existing world is picked or a new one is created,
 * so it covers the vanilla "Reading world data" / "Loading resources" screens instead of letting
 * them flash before the actual loading screen shows up. The fade itself starts later, from the
 * world load (see {@code MinecraftClientMixin}), where the game is actually rendering.
 */
@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenFlowsMixin {

	/** An existing world was picked from the world list / quick play. */
	@Inject(method = "openWorld", at = @At("HEAD"))
	private void dopes$prepareOpenWorld(String levelId, Runnable onCancel, CallbackInfo ci) {
		SeamlessCurtain.prepareSingleplayer(levelId);
	}

	/** A new world is being created (this is where a normal "Create New World" ends up). */
	@Inject(method = "createLevelFromExistingSettings", at = @At("HEAD"))
	private void dopes$prepareCreatedWorld(LevelStorageSource.LevelStorageAccess levelStorageAccess,
			ReloadableServerResources serverResources, LayeredRegistryAccess<RegistryLayer> registries,
			LevelDataAndDimensions.WorldDataAndGenSettings worldData, Optional<?> extra, CallbackInfo ci) {
		SeamlessCurtain.prepareSingleplayer(levelStorageAccess.getLevelId());
	}

	/** Demo / debug world creation. */
	@Inject(method = "createFreshLevel", at = @At("HEAD"))
	private void dopes$prepareFreshWorld(String levelId, LevelSettings levelSettings, WorldOptions worldOptions,
			Function<HolderLookup.Provider, WorldDimensions> dimensions, Screen screen, CallbackInfo ci) {
		SeamlessCurtain.prepareSingleplayer(levelId);
	}

	/** Second entry point: after a backup / version confirmation, just before the resources load. */
	@Inject(method = "openWorldLoadLevelStem", at = @At("HEAD"))
	private void dopes$prepareAfterConfirm(LevelStorageSource.LevelStorageAccess levelStorageAccess,
			Dynamic<?> dynamic, boolean safeMode, Runnable onCancel, CallbackInfo ci) {
		SeamlessCurtain.prepareSingleplayer(levelStorageAccess.getLevelId());
	}
}
