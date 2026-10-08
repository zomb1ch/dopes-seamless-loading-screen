package dopes.seamlessloading.mixin;

import dopes.seamlessloading.SeamlessBackground;
import dopes.seamlessloading.config.SeamlessConfig;
import dopes.seamlessloading.SeamlessHud;
import dopes.seamlessloading.SeamlessSession;
import dopes.seamlessloading.config.SeamlessConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.5 - 1.21.8 version of the loading screen hooks. There the screen has no reason, no loadTracker
 * and no tick / onClose of its own: it is closed with Minecraft#setScreen(null) once the level is
 * ready (see MinecraftClientMixin), and the bar is driven from the chunk count only.
 */
@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin {

@Inject(method = "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"))
private void dopes$renderScreenshot(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
if (!SeamlessSession.isActive()) {
return;
}

LevelLoadingScreen screen = (LevelLoadingScreen) (Object) this;
SeamlessBackground.render(guiGraphics, screen.width, screen.height);
}

@Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"), cancellable = true)
private void dopes$renderHud(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
if (!SeamlessSession.isRunning() || !SeamlessHud.isEnabled()) {
return;
}

LevelLoadingScreen screen = (LevelLoadingScreen) (Object) this;
SeamlessHud.render(guiGraphics, screen.width, screen.height, SeamlessHud.Style.LOADING, 1.0F);
renderChunkCounter(guiGraphics, screen);
ci.cancel();
}

@Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"))
private void dopes$renderCounterOverVanilla(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick,
CallbackInfo ci) {
if (!SeamlessSession.isRunning() || SeamlessHud.isEnabled()) {
return;
}

renderChunkCounter(guiGraphics, (LevelLoadingScreen) (Object) this);
}

private void renderChunkCounter(GuiGraphics guiGraphics, LevelLoadingScreen screen) {
SeamlessConfig config = SeamlessConfigManager.get();
if (!config.modEnabled || !config.chunkCounter) {
return;
}

int loaded = SeamlessSession.loadedChunks();
if (loaded < 0) {
return;
}

Component text = Component.translatable("dopes_seamless_loading_screen.counter", loaded, SeamlessSession.expectedChunks());
guiGraphics.drawCenteredString(screen.getFont(), text, screen.width / 2, 20, 0xFFFFFFFF);
}
}
