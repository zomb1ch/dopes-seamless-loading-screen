package dopes.seamlessloading;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;

/**
 * The main render target: the framebuffer the world is rendered into, and the one the screenshot is
 * taken from.
 *
 * <p>Minecraft 26.3 keeps the 26.2 layout: {@code Minecraft#getMainRenderTarget()} is gone and the
 * render target lives in {@code GameRenderer#mainRenderTarget()}.
 */
public final class SeamlessRenderTargets {

	private SeamlessRenderTargets() {
	}

	/** The main render target, or {@code null} when the client has not created it yet. */
	public static RenderTarget main() {
		return Minecraft.getInstance().gameRenderer.mainRenderTarget();
	}
}
