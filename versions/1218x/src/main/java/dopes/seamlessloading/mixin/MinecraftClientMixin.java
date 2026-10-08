package dopes.seamlessloading.mixin;

import dopes.seamlessloading.SeamlessCapture;
import dopes.seamlessloading.SeamlessCurtain;
import dopes.seamlessloading.SeamlessFadeScreen;
import dopes.seamlessloading.SeamlessHud;
import dopes.seamlessloading.SeamlessScreenshots;
import dopes.seamlessloading.SeamlessSession;
import dopes.seamlessloading.config.SeamlessConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {

@Inject(method = "doWorldLoad", at = @At("HEAD"), cancellable = true)
private void dopes$beginSingleplayerSession(LevelStorageSource.LevelStorageAccess levelStorageAccess,
PackRepository packRepository, WorldStem worldStem, boolean bl, CallbackInfo ci) {
if (SeamlessCurtain.isReplaying()) {
return;
}

if (!SeamlessCurtain.isPrepared()) {
String worldId = levelStorageAccess.getLevelId();
SeamlessSession.setSingleplayerWorldId(worldId);
SeamlessSession.begin(SeamlessScreenshots.singleplayer(worldId));
}

if (SeamlessCurtain.beginEnter("entering the world",
() -> Minecraft.getInstance().doWorldLoad(levelStorageAccess, packRepository, worldStem, bl))) {
ci.cancel();
}
}

/**
 * In 1.21.5 - 1.21.8 the loading screen is closed with setScreen(null) (it has no onClose of its
 * own), so this is where the fade starts and where the screen is held back until the bar caught up.
 */
@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
private void dopes$hideIntermediateScreens(Screen screen, CallbackInfo ci) {
Minecraft minecraft = Minecraft.getInstance();
if (screen == null && minecraft.screen instanceof LevelLoadingScreen && SeamlessSession.isActive()) {
if (SeamlessHud.isEnabled() && !SeamlessHud.isFull()) {
SeamlessHud.startFinishing();
ci.cancel();
return;
}

if (SeamlessConfigManager.get().fadeDuration > 0) {
minecraft.setScreen(new SeamlessFadeScreen());
ci.cancel();
return;
}

SeamlessSession.end();
return;
}

if (SeamlessCurtain.shouldHideScreen(screen)) {
ci.cancel();
}
}

@Inject(method = "disconnectWithSavingScreen", at = @At("HEAD"), cancellable = true)
private void dopes$captureOnQuitToTitle(CallbackInfo ci) {
if (SeamlessCurtain.beginLeave("quitting to the title screen",
() -> Minecraft.getInstance().disconnectWithSavingScreen())) {
ci.cancel();
}
}

@Inject(method = "stop", at = @At("HEAD"), cancellable = true)
private void dopes$captureOnWindowClose(CallbackInfo ci) {
if (SeamlessCapture.request("closing the game", () -> Minecraft.getInstance().stop())) {
ci.cancel();
}
}

/** Advances the overlay and the bar (the loading screen has no tick of its own here). */
@Inject(method = "tick", at = @At("HEAD"))
private void dopes$curtainTick(CallbackInfo ci) {
SeamlessCurtain.tick();

Minecraft minecraft = Minecraft.getInstance();
if (minecraft.screen instanceof LevelLoadingScreen && SeamlessSession.isRunning() && SeamlessHud.isEnabled()) {
int loaded = SeamlessSession.loadedChunks();
int expected = SeamlessSession.expectedChunks();
float chunks = loaded < 0 || expected <= 0 ? 0.0F : Mth.clamp((float) loaded / expected, 0.0F, 1.0F);
SeamlessHud.tick(chunks);
}
}
}
