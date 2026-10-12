# Validation — 0.6.0-rework.3

Verified on **2026-10-12** against Minecraft 1.21.1, NeoForge 21.1.256 and Java 21 in [GitHub Actions](https://github.com/Jonas-f-k/Jonas-f-k/actions/runs/38256745252). Built source commit: `a9e07f1167111c59790fcb959c39de213cc3cd20`.

## Runtime and assets

- Compilation, client preparation and **all 33 required server GameTests passed**. Four presentation tests cover all-species ecology reachability, elevated/wide footing, synchronized melee/special attack phases and tracked ability poses for all seven transformations.
- Actual Minecraft baking and animation passed for **32 species, seven legacy rigs, seven forms and 30 armor layers**: **404,418 finite part poses**, **111,384 UV-bounded vertices**, and painted-face sampling for **46 creature skins**.
- Native resources passed: **321 models, 286 native sprites, 128 different transparent Memory icons**, transparent native flower/leaf silhouettes, matching turf edges and **62 unchanged credited asset hashes**.
- World data passed across **60 seeds**, **18 biomes** and **50 features**, with no feature-order cycles. Shipped tree feature resources now route to the registered native branching feature.
- Archipelago checks passed for **7,017 islands and 5,737 links**, all five island shapes and void below the floating belt.
- Effect checks passed for **all 49 family/type combinations**, **121,050 finite bounded quads**, valid alpha, lifetime bounds and rejected invalid radii.
- The independent geometry harness passed **39 rigs, 1,705 cuboids, 61,632 animated joint poses** and 32 distinct species geometry signatures.

## Actual client rendering

The opt-in test launched the real Minecraft/NeoForge client under Xvfb with software OpenGL. The native resource reload **loaded the bundled arcane ShaderInstance**. The client drew and captured five distinct **1280×900** frames:

1. All 32 species.
2. Seven transformation bodies.
3. Seven magic families using the shipped GLSL.
4. Transformation strike poses.
5. Transformation flight poses.

The check required shader loading rather than accepting fallback, checked OpenGL errors, rejected reported missing mod models/textures, verified five distinct PNGs and exited after capture. [Download original captures](https://github.com/Jonas-f-k/Jonas-f-k/actions/runs/38256745252/artifacts/11750119045). Resized copies in `docs/previews` were inspected.

This validates client loading and the actual renderer. It is a gallery check, not a full in-world combat, realm traversal, external shader-pack, graphics-mode or performance certification. Natural spawn balance and visual composition in a real saved world still benefit from local playtesting. The native resource checker did not separately resolve vanilla references against an original unmodified client resources JAR; the real client resource load is the stronger runtime check performed here.

## Editable exports

**39 self-contained Blockbench 5 models** exported from actual baked Minecraft geometry. Checks verified unique cube/bone UUIDs, positive cuboids, bounded native UV offsets, embedded skins, five sampled animation tracks per model and the included licensed Animation Sliders plugin. [Download editor package](https://github.com/Jonas-f-k/Jonas-f-k/actions/runs/38256745252/artifacts/11749954155).

The exported tracks are sampled review/editing segments. No interactive Blockbench import/export round-trip was performed, and changing an editor copy does not automatically alter Java runtime rigs.

## Compiled distribution

[Playable mod ZIP](https://github.com/Jonas-f-k/Jonas-f-k/actions/runs/38256745252/artifacts/11750373710) contains `shadowslave-0.6.0-rework.3.jar`. GitHub reports artifact ZIP digest `sha256:2fa223ff4bdeaf198bbe75218e313387d01abb301de271f250e826709fd8942c`. The editor plugin and developer tools are outside runtime resources and are not compiled into the JAR.

Install on Minecraft 1.21.1 / NeoForge 21.1.256, replacing earlier Shadow Slave JARs. Textures and models update in existing worlds. New tree shapes require new chunks; a fresh test world also makes terrain and spawning review clearer.

Earlier material-only revisions and user-supplied documents contain different test totals. They describe those historical revisions, not this one. Nonfatal deprecated API, test-server properties and audio/auth service messages do not constitute gameplay or shader failures; required tests and the bounded client check passed.
