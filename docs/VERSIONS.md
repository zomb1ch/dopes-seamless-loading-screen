# Supported Minecraft versions

One jar covers **Minecraft 1.21.9, 1.21.10 and 1.21.11**. The mod is compiled against the newest of
them; Fabric's intermediary names are stable across those versions, so the same file loads on all
three.

## The pool

| Versions | Status | Why |
|---|---|---|
| **1.21.9 – 1.21.11** | **Supported by the root jar** | The whole API the mod uses exists in all three. 1.21.11 was mostly a renaming release, and renames do not change intermediary names. |
| 1.21 – 1.21.8 | **Not supported** | Out of scope: the mod needs `LevelLoadTracker` (1.21.9+), `Identifier`, `ARGB` and `RenderPipelines`. |
| **26.1 – 26.1.2** | **Supported by the `versions/26x` jar** | Minecraft 26.x ships **unobfuscated**, so it needs the no-remapping Loom and its own sources — see below. |
| **26.2** | **Supported by the `versions/262x` jar** | Unobfuscated; `Gui` owns the screen and `GameRenderer` the main render target — see below. |
| **26.3** | **Supported by the `versions/263x` jar** | The same, plus `RenderPipeline` moved to another package in 26.3, so the descriptor of `RenderPipelines.GUI_TEXTURED` differs — 26.2 and 26.3 cannot share one jar. |

## The 26.x build

`versions/26x` is a second Gradle project that builds the same mod for the 26.x line:

```
gradlew -p versions/26x build      ->  versions/26x/build/libs/dopes-seamless-loading-screen-2.0+26.1-26.1.2.jar
gradlew -p versions/262x build     ->  versions/262x/build/libs/dopes-seamless-loading-screen-2.0+26.2.jar
gradlew -p versions/263x build     ->  versions/263x/build/libs/dopes-seamless-loading-screen-2.0+26.3.jar
gradlew build                      ->  build/libs/dopes-seamless-loading-screen-2.0+1.21.9-1.21.10-1.21.11.jar
```

Differences from the root project:

* it uses **`net.fabricmc.fabric-loom`**, the Loom variant **without remapping**, because 26.x is not
  obfuscated: there is no `mappings` dependency at all, and that Loom has no `mod*` configurations,
  so `implementation` / `compileOnly` are used for Fabric Loader, YACL and Mod Menu;
* shared sources are taken from `../../src/main/java`; a `syncSharedSources` task copies them into
  `build/generated` and **excludes the ten files that really differ**, which live in
  `versions/26x/src/main/java`. Edit shared code in one place, and only touch the fork when the
  26.x API actually differs.

What changed for 26.x (the GUI rendering model was reworked):

| 1.21.x | 26.x |
|---|---|
| `net.minecraft.client.gui.GuiGraphics` | `net.minecraft.client.gui.GuiGraphicsExtractor` |
| `Screen#render` | `Screen#extractRenderState` |
| `Screen#renderBackground` | `Screen#extractBackground` |
| `Screen#renderWithTooltipAndSubtitles` | `Screen#extractRenderStateWithTooltipAndSubtitles` |
| `GuiGraphics#drawCenteredString` | `GuiGraphicsExtractor#centeredText` |
| `Minecraft#doWorldLoad(..., boolean)` | `Minecraft#doWorldLoad(..., Optional<GameRules>, boolean)` |
| `WorldOpenFlows#createLevelFromExistingSettings(..., WorldData, ...)` | `..., LevelDataAndDimensions.WorldDataAndGenSettings, Optional, ...` |
| `CreateWorldScreen#createNewWorld(..., WorldData, ...)` | `..., LevelDataAndDimensions.WorldDataAndGenSettings, Optional, ...` |

The rest of the mixin targets (`Screen`, `LevelLoadingScreen`, `LevelLoadTracker`, `ConnectScreen`,
`CreateWorldScreen`, `ClientPacketListener`, `ClientCommonPacketListenerImpl`, `GameRenderer`) were
checked against the 26.1 classes and are unchanged.

## How this was measured

Compiling the current sources against every candidate version (`gradlew compileJava`) and counting
the missing symbols:

| Build target | Errors | Missing symbols |
|---|---|---|
| 1.21.11 | 0 | — |
| 1.21.10 | 108 | `Util`, `Identifier`, `identifier()`, `getSamplerCache()` |
| 1.21.9 | 108 | same as 1.21.10 |
| 1.21.8 | 130 | + `LevelLoadingScreen.Reason`, `LevelLoadTracker`, `Minecraft#disconnectFromWorld(Component)` |
| 1.21.1 | 170 | + `ARGB`, `RenderPipelines` |

Two different kinds of difference show up:

* **Renames** — `net.minecraft.Util` → `net.minecraft.util.Util`, `ResourceLocation` → `Identifier`,
  `Level#dimension().location()` → `identifier()`. These only break *compiling against* an older
  version; the bytecode of a remapped jar refers to intermediary names, which do not change when a
  class is renamed. That is why the 1.21.11 build also runs on 1.21.10 and 1.21.9.
* **New API** — `RenderSystem#getSamplerCache()` appeared in 1.21.11. This is a real runtime
  blocker, so the texture filter is now looked up reflectively (`SeamlessTexture#useSamplerCache`,
  falling back to the classic `setFilter` and finally to vanilla's nearest filtering).

**1.21.8 is the hard lower bound**: from there down the mod would need genuinely different code
paths, not just shims.

## Adding a version

1. **Same cluster (another 1.21.9–1.21.11 style release).** Add it to `minecraft_versions` in
   `gradle.properties` (comma separated). The jar name and the `minecraft` dependency range in
   `fabric.mod.json` are generated from that list. Verify with
   `./gradlew compileJava -Pminecraft_version=<version>` — a compile without missing symbols means
   the API surface is there.
2. **A different cluster (for example 26.x).** Build it separately, because the mapping scheme
   differs:
   * switch the Loom plugin to the no-remapping variant (`net.fabricmc.fabric-loom`) and target the
     version directly — 26.x is unobfuscated, so no intermediary step is involved;
   * update the code for that version's API (start from the Fabric porting guide for the version);
   * keep the shared sources in `src/main` and put the version-specific files in a separate source
     set, or keep a separate branch, so the two clusters can still be developed together.

## Porting to 26.2 / 26.3

**Done** — `versions/263x` builds a jar for the 26.2/26.3 line, and the same sources compile against
both versions (checked with `compileJava` against 26.2 and 26.3).

What this line changed, and how it is handled:

| 26.1 | 26.2 / 26.3 | How it is handled |
|---|---|---|
| `Minecraft#screen` | moved into `Gui` | `minecraft.gui.screen()` |
| `Minecraft#setScreen` | `Minecraft#setScreenAndShow`, `Gui#setScreen` | `minecraft.gui.setScreen(...)` |
| `Minecraft#getMainRenderTarget` | `GameRenderer#mainRenderTarget` | looked up reflectively in `SeamlessCapture`, so the same file works on 1.21.x, 26.1 and 26.2/26.3 |
| `Util.getPlatform().openPath` | removed | `java.awt.Desktop` in `SeamlessConfigScreen` (works on every version) |
| `com.mojang.blaze3d.textures.FilterMode` | moved to `com.mojang.renderpearl.api.textures.FilterMode` in 26.3 | looked up by name in `SeamlessTexture` |
| `RenderPipelines.GUI_TEXTURED` (field type `com.mojang.blaze3d.pipeline.RenderPipeline`) | `RenderPipeline` moved to another package in 26.3 | nothing can be shimmed: the bytecode looks the field up by name **and descriptor**, so 26.2 and 26.3 each get their own jar (`versions/262x`, `versions/263x`) |

Every mixin target is unchanged from 26.1, so no injection needed touching.

## Below 1.21.9: not supported

1.21.1 – 1.21.8 is out of scope. The mod needs `LevelLoadTracker` (added in 1.21.9), and the older
lines build the loading screen differently. The tables below are kept as a reference for a future
attempt — the `versions/1218x` project from that attempt was removed.

**Historical note (1.21.6 – 1.21.8):** it was ported once — `LevelLoadStatusManager` instead of
`LevelLoadTracker`, the HUD ticked from `Minecraft#tick` (the screen has no `tick` there), the screen
closed through `setScreen(null)` and `disconnectWithSavingScreen` for the quit hook. 1.21.1 – 1.21.5
additionally lack the pipeline based `blit` and `ARGB`/`RenderPipelines`.

| Piece | 1.21.9+ | 1.21.5 – 1.21.8 | 1.21.1 – 1.21.4 |
|---|---|---|---|
| identifier type | `Identifier` | `ResourceLocation` | `ResourceLocation` |
| `Util` | `net.minecraft.util.Util` | `net.minecraft.Util` | `net.minecraft.Util` |
| text drawing | `GuiGraphicsExtractor#centeredText` | `GuiGraphics#drawCenteredString` | `GuiGraphics#drawCenteredString` |
| progress source | `LevelLoadTracker` (`hasProgress`, `serverProgress`) | `LevelLoadStatusManager` (`levelReady` only, no progress) | same |
| loading screen | `reason`, `loadTracker`, `tick`, `onClose` | no `reason`/`loadTracker`/`tick`/`onClose` — only `render`/`removed` | same |
| quit hook | `Minecraft#disconnectFromWorld(Component)` | `Minecraft#disconnect(Screen, boolean)` / `disconnectWithSavingScreen()` | same |
| `ARGB`, `RenderPipelines` | present | present | **missing** (pre-1.21.5 rendering) |

Measured with `compileJava`:

| Target | Errors | Cause |
|---|---|---|
| 1.21.5 | 124 | the renames + the loading screen integration |
| 1.21.6 | 124 | the same |
| 1.21.8 | 26 | after the renames (see `versions/1218x`) only the integration is left: `LevelLoadingScreen.Reason` (12×), `LevelLoadTracker` (8×), `ResourceLocation()` (4×), `disconnectFromWorld` (2×) |
| 1.21.4 | 144 | the above plus `ARGB`/`RenderPipelines` |
| 1.21.1 | 166 | the same as 1.21.4 |

The `versions/1218x` project from that attempt was **removed** — it never compiled: the loading screen
would have to be driven from `LevelLoadStatusManager` and the HUD ticked from `Minecraft#tick` (the
screen has no `tick` there). If a leftover
`dopes-seamless-loading-screen-2.0+1.21.6-1.21.7-1.21.8.jar` is still sitting in a 1.21.6 – 1.21.8
instance's `mods` folder, delete it — that build is unsupported and predates the working code.

## Verification

Two independent checks were run for the 1.21.9 – 1.21.11 range:

1. **Compilation.** The sources compile against all three versions apart from the renames listed
   above (plus `RenderSystem#getSamplerCache`, which is looked up reflectively). Renames do not
   change the bytecode of a remapped jar.
2. **Mixin targets.** Loom remaps the mixin annotations into intermediary names at build time. All 37
   intermediary names the mixins inject into or shadow (`method_25393` tick, `method_25394` render,
   `method_25419` onClose, `method_1507` setScreen, `method_29610` doWorldLoad, the `LevelLoadTracker`
   methods, the shadowed fields, ...) exist in 1.21.9, 1.21.10 and 1.21.11, so every injection
   resolves on all three.

The mod is developed and run on **1.21.11**. If something misbehaves on 1.21.9 or 1.21.10, please
open an issue with your `latest.log`.

## "Failed to load registries" is not this mod

A crash during *Registry Loading* comes from a **datapack** (usually a worldgen mod), for example:

```
Failed to parse minecraft:dimension_type/overworld.json from pack mr_lithosphere
```

This mod ships no `data/` folder at all — only `assets/` — so it cannot influence registries. Look at
the mod named in the error message, and check that it is built for the Minecraft version you run
(worldgen packs often declare no `minecraft` dependency at all, so they load everywhere and then fail
to parse).
