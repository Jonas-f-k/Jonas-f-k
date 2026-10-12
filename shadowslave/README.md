# Shadow Slave — 0.6.0-rework.3

Minecraft **1.21.1 · NeoForge 21.1.256 · Java 21**.

[Download the playable mod ZIP](https://github.com/Jonas-f-k/Jonas-f-k/actions/runs/38256745252/artifacts/11750373710). Extract `shadowslave-0.6.0-rework.3.jar`, replace the earlier Shadow Slave JAR in `mods`, and restart Minecraft. GitHub may require sign-in for artifact downloads.

[Actual Minecraft visual captures](https://github.com/Jonas-f-k/Jonas-f-k/actions/runs/38256745252/artifacts/11750119045) · [39 editable Blockbench models and animation tool](https://github.com/Jonas-f-k/Jonas-f-k/actions/runs/38256745252/artifacts/11749954155) · [Successful build](https://github.com/Jonas-f-k/Jonas-f-k/actions/runs/38256745252)

## The redesign

- **32 creature anatomies**: eight-legged carapaces with separate pincers/scythes, low spiders, locust Reavers, long-bodied predators, six-armed Messengers, a bone-mound Lord of the Dead, hinged Mimics and branching Soul Trees. Source evidence and uncertain adaptations are documented in [the visual research](docs/VISUAL_REFERENCE_RESEARCH.md).
- **Seven transformations**: connected quadruped dragon/wolf/stag/griffin skeletons, a horizontal finned Leviathan, a heavy Colossus and a two-legged Wyvern. Knees, jaws, wings, tail segments and expressive anticipation/strike/recovery poses animate. R publishes a tracked ability pose for every form.
- **Automatic bundled spell shaders**: nine procedural materials create stepped flames, luminous rune seals, matte shadow wisps, foam, thorn leaves, prism highlights and orbital motes. These are local magic shaders. Full-scene shader-pack lighting is a separate capability. No external shader mod is needed for the included effects.
- **Natural terrain art**: 286 native sprites include subtle stone/soil/grass clusters, ragged turf sides, bark and cut log ends, transparent foliage and transparent flowers. Source-based creature palettes use controlled contrast.
- **Varied trees and quieter atmosphere**: biome tree features now use the connected branching generator, uneven canopy lobes and staggered conifer boughs. Bright ambient end rods are replaced with sparse small motes. Existing placed trees keep their old geometry; explore new chunks to see the new crowns.
- **Animated Bestiary**: press **O → Bestiary → View animated models**. Inspect creatures, transformations and magic; drag to rotate, choose idle/walk/windup/strike/flight, and browse the overview.
- **Spawning and presentation fixes**: true terrain height, broad-body footing, bounded candidate searches, Peaceful/gamerule compliance and synchronized creature attacks. Natural Nightmare mobs appear in **Dream Realm Survival wilderness**, outside Sanctuaries.

The seven transformation families and the roster's explicitly original species are mod adaptations. The source research distinguishes primary novel evidence, secondary descriptions and artistic inference. Gameplay progression, Memory IDs, cores, traits, trials and ownership are preserved.

## Verified

This source compiled and passed **33 required server GameTests**, actual Minecraft model/UV checks, painted-face checks, asset/terrain/effect checks and **five actual OpenGL client captures**. The real client loaded the bundled shader and drew the mod's atlas-backed creature/form models. Details and limits are in [VALIDATION.md](VALIDATION.md).

[Creatures](docs/previews/species-overview.png) · [Transformations](docs/previews/forms-overview.png) · [Flight poses](docs/previews/forms-flight.png) · [Magic](docs/previews/magic-overview.png). Documentation previews are resized native client captures. The artifact above contains the original 1280×900 images.

## Build and use

```sh
bash gradlew clean build prepareClientRun runGameTestServer
python tools/verify_client_models.py
python tools/verify_offline.py
python tools/export_blockbench_models.py
```

The result is `build/libs/shadowslave-0.6.0-rework.3.jar`. On Windows use `gradlew.bat`. Put the same revision on clients and server. Build downloads need the usual NeoForge/Minecraft Maven and asset hosts; the current local cloud environment cannot fetch all dependencies, so GitHub Actions performs compilation and client rendering.

For the opt-in graphical developer check:

```sh
xvfb-run -a python tools/run_visual_smoke.py --timeout 240
```

This launches the real client in a virtual display, requires shader loading, captures models/effects, then exits. It is disabled in normal gameplay. [Testing commands](TESTING_COMMANDS.md) cover trials, forms, mobs, Memories and realm exploration.

## Editable art and plugins

[Editor instructions](docs/EDITABLE_MODELS.md) explain the exported Blockbench 5 models and included **Animation Sliders 0.5.0** plugin. The plugin is supplied with its GPLv3 license and pinned source hashes, separately from the mod JAR. Install it in Blockbench with **File → Plugins → Load Plugin from File**. It has not been installed automatically on your computer.

Original texture SVGs live in `art_source/native`. Rebuild sprites and biome data with:

```sh
python -m pip install cairosvg pillow
python tools/build_native_art.py
python tools/build_worldgen.py
```

62 unchanged Malcolm Riley sprites retain attribution and licenses in [ASSET_CREDITS.md](ASSET_CREDITS.md). Other reference projects informed design; their code/models are not bundled.

The uploaded source ZIP is the gameplay foundation. Uploaded documents are reference specifications; their historical release/test claims are not used as validation evidence for this revision.
