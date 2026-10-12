# Working on Shadow Slave locally

Minecraft **1.21.1** · NeoForge **21.1.256** · **JDK 21** · mod **0.6.0-rework.3**.

## Build your own JAR

Extract the complete ZIP. Open this `shadowslave` folder in IntelliJ IDEA or another Java/Gradle editor; import `build.gradle` as a Gradle project and use JDK 21 as the project SDK and Gradle JVM.

Open a terminal in this folder, where `build.gradle` and `gradlew.bat` are located. Check Java first:

```text
java -version
```

Windows PowerShell:

```powershell
.\gradlew.bat build
```

Windows Command Prompt:

```bat
gradlew.bat build
```

Linux/macOS:

```sh
bash gradlew build
```

When Gradle reports `BUILD SUCCESSFUL`, your JAR is `build/libs/shadowslave-0.6.0-rework.3.jar`. Install it in a Minecraft 1.21.1 / NeoForge 21.1.256 instance, replacing your older Shadow Slave JAR. `BUILD_WINDOWS.bat` also runs the build on Windows.

You do not need to install Gradle separately. The included wrapper downloads the project's pinned Gradle version. The first build also downloads NeoForge/Minecraft dependencies, so an internet connection is required. If it fails, keep the first error and the lines around it; `--stacktrace` can provide more detail.

After changing Java, shaders, textures, JSON or metadata, run the build command again to produce your new JAR. If you change the version in `gradle.properties`, the output JAR filename changes accordingly.

## Run the mod from your editor

Windows PowerShell:

```powershell
.\gradlew.bat runClient
```

Linux/macOS:

```sh
bash gradlew runClient
```

This starts a development Minecraft client with the mod loaded. Assets may need to download before it opens. Use a fresh test world to inspect new terrain. Press **O → Bestiary → View animated models** to inspect creatures, forms and magic. Natural Nightmare spawns are in Dream Realm Survival wilderness outside Sanctuaries.

For server GameTests, use `gradlew.bat runGameTestServer` on Windows or `bash gradlew runGameTestServer` on Linux/macOS. See `TESTING_COMMANDS.md` for in-game commands. The successfully built source passed 33 required GameTests; packaging does not constitute another compilation or gameplay test. Details are in `VALIDATION.md`.

## Where to edit

All Java paths below start inside `src/main/java/com/shadowslave/`:

| What you want to change | Where to start |
| --- | --- |
| Creature body shapes and joints | `client/CreatureRig.java` |
| Creature animation poses and timing | `client/CreatureMotion.java` |
| Transformation body shapes and movement | `client/BeastRig.java` |
| Mob AI, combat and synchronized attack states | `entity/NightmareCreature.java`, `entity/CreatureEcology.java` |
| Spell visuals and effect geometry | `client/SpellEffects.java`, `client/MagicShaders.java` |
| Dream Realm and native tree/structure generation | `world/` |
| Player UI and model gallery | `client/` |
| Memory items and progression rules | `memory/`, `core/` |

Resources:

- `src/main/resources/assets/shadowslave/`: textures, models, language files and bundled shaders.
- `src/main/resources/data/shadowslave/`: biomes, worldgen, loot and other gameplay data.
- `src/main/templates/META-INF/neoforge.mods.toml`: mod metadata template.
- `art_source/native/`: editable SVG art sources.
- `art_source/upstream/` and `ASSET_CREDITS.md`: credited original third-party assets.
- `tools/`: art and worldgen builders, verification tools and native Blockbench exporter.

## Blockbench workflow

The runtime model shapes and animations are written in Java. All their source is included. To create the 39 editable Blockbench copies from your local compiled project:

1. Install Python 3 and make sure Java 21 is on PATH.
2. Run `gradlew.bat build prepareClientRun` on Windows, or `bash gradlew build prepareClientRun` on Linux/macOS.
3. Run `python tools/export_blockbench_models.py`.
4. Open the resulting `.bbmodel` files in `build/reports/blockbench-models/` using Blockbench 5 or newer.

The exporter also copies the licensed Animation Sliders plugin into that folder. Exported model tracks are sampled editing/review segments. Saving a `.bbmodel` does not automatically update the mod's Java rigs; integration into the runtime model/animation code and a new build are required. See `docs/EDITABLE_MODELS.md`.

Already exported copies are available separately in the [editor ZIP](https://github.com/Jonas-f-k/Jonas-f-k/actions/runs/38256745252/artifacts/11749954155).

Optional resource builders use Python packages `pillow` and `cairosvg`:

```sh
python -m pip install pillow cairosvg
python tools/build_native_art.py
python tools/build_worldgen.py
```

The generated resources are already included, so you can compile the existing project without running these builders. A Cairo system library may be required for CairoSVG on your operating system.

## Version control and CI

The ZIP preserves the original layout: `.github/` is beside `shadowslave/`. If you put this in your own Git repository, retain that layout for the supplied workflow. It currently runs on the `shadowslave/rework-0.6.0` branch and can also be started manually with `workflow_dispatch`; change its branch list for your own repository. It builds the JAR, runs GameTests and source/resource checks, exports models and captures the native client.

The archive excludes `.git`, dependency caches, generated builds, private machine settings and saved run worlds. Commit your own code changes regularly. Original mod code is MIT licensed. `LICENSE` and `LICENSES.md` explain the code license and preserved third-party and artwork terms. The full existing attribution and license files are included.
