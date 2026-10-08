# Supported Minecraft versions

One jar covers **Minecraft 1.21.9, 1.21.10 and 1.21.11**. The mod is compiled against the newest of
them; Fabric's intermediary names are stable across those versions, so the same file loads on all
three.

## The pool

| Versions | Status | Why |
|---|---|---|
| **1.21.9 – 1.21.11** | **Supported by this jar** | The whole API the mod uses exists in all three. 1.21.11 was mostly a renaming release, and renames do not change intermediary names. |
| 1.21 – 1.21.8 | Needs separate code | `LevelLoadTracker`, `ARGB`, `RenderPipelines` and `Minecraft#disconnectFromWorld(Component)` do not exist yet, and `LevelLoadingScreen.Reason` has a different shape. |
| 26.1 – 26.3 | Needs a **separate build** | Minecraft 26.x ships **unobfuscated**: Fabric reports no intermediary mappings for it (`0.0.0`), while 1.21.x does. A jar remapped to intermediary cannot load on 26.x, and vice versa. The API also changes between 26.x releases (Fabric documents a 26.1 → 26.2 migration). |

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
