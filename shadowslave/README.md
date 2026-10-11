# Shadow Slave — 0.6.0-rework.1

Combined source rework for **Minecraft 1.21.1 · NeoForge 21.1.256 · Java 21**.

The uploaded `0.6.0-alpha-source` ZIP is the gameplay foundation. The uploaded Markdown and development-goal text are reference specifications. Their historical build/test claims are not evidence that this source revision passes those checks. See `VALIDATION.md` for this revision's actual results.

This deliverable is source. A playable JAR has not been produced in this environment: its network policy blocks the NeoForge dependency server. The offline checks passed; Minecraft compilation, GameTests and in-game/shader checks still need the commands below once dependencies are available.

## Main changes

- All 32 regional creatures have complete articulated species rigs, with unique geometry. Crabs have jointed pincers, spiders have eight articulated legs, mimics have hinged lids, plants open their petals, worms have segmented bodies, and undead, beasts and flyers have their own anatomy. Warning poses, jaws, knees, forearms, feathers, veils and tails animate.
- The seven transformations use dragon, quadruped wolf, finned leviathan, heavy colossus, antlered stag, four-legged winged griffin and two-legged wyvern bodies. Original collision dimensions, form abilities and cleanup stay in place.
- 280 native pixel sprites replace the illustrated-atlas runtime path. There are 128 distinct Memory icons, dedicated creature materials, full-cube grass with separate soil faces, transparent leaves, and brighter equipped armor materials. 62 unchanged Malcolm Riley sprites retain attribution and licenses.
- The Soul Sea has custom controls, visible core saturation, ranked Memory rows, compact responsive actions and a paginated bestiary. The HUD shows Essence, core fill, ability cooldowns and actual equipped Memory icons.
- Seven geometric magic families add growth-scaled casts, transformation effects, beast effects, weapon impacts and locked creature warning rings. Server packets are bounded; the client caps active effects at 32 and vertices at 12,000 per frame.
- The Dream Realm uses direct cartographic biome lookup and seeded terrain profiles. Clusters of 4–7 unequal islands have five shapes and connected sagging-chain graphs. Vanilla cave density is removed; explicit basin water replaces the global fluid plane. Existing Citadels, ruins and regional scenery remain.

Gameplay progression, Memory IDs, Divine core cap, traits, trials, Echo ownership and magical-damage protection logic are retained from the source foundation. This pass does not claim new lore verification or balance playtesting.

## Build and test

Use a complete JDK 21, not a JRE. The wrapper downloads Gradle 9.2.1.

```sh
./gradlew clean build prepareClientRun runGameTestServer
python tools/verify_client_models.py
python tools/verify_art_resources.py
python tools/validate_worldgen.py
```

On Windows, use `gradlew.bat`. The JAR will be `build/libs/shadowslave-0.6.0-rework.1.jar` after a successful build. Put the same JAR on server and clients; remove the older version. Use a fresh world to evaluate the new generation. Existing worlds keep already-generated chunks and can retain older saved generator settings.

Build downloads need HTTPS access to the Gradle/Maven repositories and Minecraft metadata/libraries, including `maven.neoforged.net`, `libraries.minecraft.net`, `piston-meta.mojang.com` and `piston-data.mojang.com`. A dependency download failure does not constitute a Java compilation result.

## Offline checks

```sh
python tools/verify_art_resources.py
python tools/validate_worldgen.py
python tools/verify_rigs_offline.py rigs.json
javac -d work/checks src/main/java/com/shadowslave/world/Archipelago.java src/main/java/com/shadowslave/client/SpellGeometry.java tools/java/ArchipelagoCheck.java tools/java/VisualMathCheck.java
java -cp work/checks ArchipelagoCheck
java -cp work/checks VisualMathCheck
```

The rig harness tests the real builder/animation code using small geometry stand-ins. It does not validate Minecraft APIs or replace real model baking. The preview sheet is generated from that geometry; it is not an in-game screenshot.

`TESTING_COMMANDS.md` supplies gameplay commands. `docs/SHADER_COMPATIBILITY.md` supplies the render test matrix. `ASSET_CREDITS.md` and `REWORK_REFERENCES.md` distinguish reused assets from inspiration.

## Editable art

Generated SVG sources are in `art_source/native`; the 62 selected upstream originals and license are in `art_source/upstream`. Rebuild current assets with Python plus CairoSVG:

```sh
python -m pip install cairosvg pillow
python tools/build_native_art.py
python tools/build_worldgen.py
```

The older art generators remain historical tools; they target the retired atlas pipeline. Use the current generators above for this revision.
