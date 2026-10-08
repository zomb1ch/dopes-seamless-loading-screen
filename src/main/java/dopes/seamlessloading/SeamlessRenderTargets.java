package dopes.seamlessloading;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;

/**
 * The main render target: the framebuffer the world is rendered into, and the one the screenshot is
 * taken from.
 *
 * <p>Up to 26.1 this is {@code Minecraft#getMainRenderTarget()}. The lookup used to be reflective
 * (so that one source file could serve every supported version), but reflection by name only works
 * while the game is not obfuscated: on 1.21.x the method is renamed to its intermediary name at
 * runtime, so the lookup silently failed and no screenshot was ever written. A direct call is
 * remapped by Loom at build time and works everywhere.
 *
 * <p>From 26.2 the render target moved to {@code GameRenderer#mainRenderTarget()}, so the versions
 * where that happened carry their own copy of this class.
 */
public final class SeamlessRenderTargets {

	private SeamlessRenderTargets() {
	}

	/** The main render target, or {@code null} when the client has not created it yet. */
	public static RenderTarget main() {
		return Minecraft.getInstance().getMainRenderTarget();
	}
}
