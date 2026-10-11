# Validation — 0.6.0-rework.1

## 0.6.0-rework.2 material update

Offline checks pass for the new 64x128 creature skins, four painted material regions per skin, unchanged asset credits and the articulated rigs. The replacement GitHub build and baked-face pixel sampling check are pending. The results below describe the previous release only.

Verified on 2026-10-12 with Minecraft 1.21.1, NeoForge 21.1.256, Java 21 and Gradle 9.2.1 on GitHub Actions.

## Build and runtime checks

[Successful build](https://github.com/Jonas-f-k/Jonas-f-k/actions/runs/38183545392) of source commit `ab83a87b68793021b6154612f2b6fb394f7f59f0`:

- `clean build prepareClientRun runGameTestServer` passed. **All 29 required server GameTests passed.** They cover the foundation's trials, progression, Memories, ownership, transformations and generated features. New checks round-trip all 49 visual packets, reject invalid visual positions and verify actual Dream chunks use the cartographic biome source and native geology. The updated island test checks two actual island cores and their generated chain.
- The real Minecraft client model checker baked all **32 species, seven legacy trial families, seven transformations and 30 armor layers**. **427,008 part poses were finite** and **70,608 cube texture vertices were inside sprite UV bounds**.
- Native resources passed: **321 models, 280 native sprites, 128 different transparent Memory icons**, full-cube grass, matching natural edges, transparent cutout leaves and **62 credited original asset hashes**.
- World data passed across **60 seeds**: **18 atlas biomes**, **50 features**, direct biome ordering, native geology settings and no feature-order cycles.
- Archipelago math passed for **7,017 islands and 5,737 connected links**, 4–7 islands per cluster, five shape families, deterministic seeds and an empty density field below the floating belt.
- All 49 visual geometry combinations passed across three radii and six animation times: **86,292 finite, bounded quads**; invalid radii rejected.
- The independent rig harness also passed: **39 rigs, 1,798 cuboids, 64,764 finite animated joint poses** and 32 different species geometry signatures.

[Compiled mod artifact](https://github.com/Jonas-f-k/Jonas-f-k/actions/runs/38183545392/artifacts/11718721513) contains `shadowslave-0.6.0-rework.1.jar`. The uploaded artifact ZIP digest reported by GitHub is `1b5f5162c241dfc477d6205577e77c436ea37d4b73feca31ef39f9272ec51e4f`.

## Limits

These checks are server/runtime/model/asset checks, not an interactive client playtest. GUI appearance, camera framing, actual magic appearance in Fast/Fancy/Fabulous, optional shader stacks, sustained performance, balance and multiplayer/modpack behavior still require the routes in `TESTING_COMMANDS.md` and `docs/SHADER_COMPATIBILITY.md`.

The native resource checker did not resolve vanilla resource names against an original client resources JAR. Custom resource references and their assets resolve. The model checker uses actual Minecraft geometry but does not render a graphical frame.

The source preview is an offline geometry render, not an in-game screenshot. The supplied input documents' historical claims remain reference material; they are not used as test evidence.

Nonfatal deprecated API warnings and initial test-server property/heightmap messages occurred. They did not fail required tests. Earlier local build logs under `docs/checks` document the old network restriction; this GitHub result supersedes the former 'unbuilt source' status.
