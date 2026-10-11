# Validation — 0.6.0-rework.1

Checked on 2026-10-12. These results belong to this source revision, not the historical claims in the supplied notes.

## Passed here

- JavaParser parsed all **57 runtime Java source files** as Java 21. This is syntax validation, not Minecraft API compilation.
- `tools/verify_art_resources.py`: **321 models, 280 native sprites, 128 different transparent Memory icons**, full-cube grass, matching grass edges, cutout leaves and **62 credited upstream SHA-256 hashes**. References within the mod resolve. Vanilla resource names were not resolved against a client JAR because dependencies were unavailable.
- `tools/validate_worldgen.py`: **60 world seeds**, all **18 atlas biomes**, **50 placed/configured features**, cartographic biome ordering, native geology settings and no feature-order cycles. Map heights, hub, Citadel placement and biome coverage passed the existing mathematical invariants.
- `ArchipelagoCheck`: **7,017 seeded islands**, **5,737 connected MST links**, cluster sizes 4–7, five shape families, variable dimensions, deterministic seeds and empty density below the sky belt. These are mathematical density/graph checks, not actual Minecraft chunk tests.
- `VisualMathCheck`: all **49 family/effect combinations**, three radii and six animation times; **86,292 finite, bounded quads**; invalid radii rejected.
- `tools/verify_rigs_offline.py`: **39 articulated rigs**, **1,798 cuboids inside 64px UV limits**, **64,764 finite animated joint poses** and **32 different species geometry signatures**. The actual rig builder and animation code are compiled against a small geometry harness. The harness does not reproduce the Minecraft renderer or validate its APIs.
- Distribution archive CRC, entry uniqueness and content checks are performed during packaging.

## Blocked / still required

The Gradle build stops while resolving `net.neoforged:neoform-runtime:2.0.31`: the environment's HTTPS policy returns **403 Forbidden** from `maven.neoforged.net`. Gradle 9.2.1 and a full JDK 21 are available. Minecraft Java compilation, client model baking, GameTests and a playable JAR were not produced. No test success from the uploaded Markdown is inherited.

Once dependency access is available, run:

```sh
./gradlew clean build prepareClientRun runGameTestServer
python tools/verify_client_models.py
python tools/verify_art_resources.py <Minecraft-client-resources-JAR>
```

The real client checker now covers all species, legacy trial families, beast animations and armor layers, including cube UV vertices. The island GameTest now samples a link inside one cluster. Actual graphical checks must then cover the test matrix in `docs/SHADER_COMPATIBILITY.md`.

The preview sheet is an offline shaded geometry render, not an in-game capture. UI layout, all graphics modes, shader appearance, actual generated water/void/chain chunks, balance and multiplayer remain unverified.
