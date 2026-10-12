# Bundled magical rendering

The mod ships its own GLSL 150 vertex and fragment shaders under `assets/shadowslave/shaders/core/arcane.*`. NeoForge's client mod-bus `RegisterShadersEvent` automatically loads them. Installing the mod is sufficient; no external shader pack, Iris, Oculus, resource-pack toggle or video-settings step is required for these effects.

These are **local spell shaders**, not a full-scene lighting replacement. They add luminous runes, highlights and moving masks to magic. They do not implement global shadows, global bloom, water reflections or dynamic block light, and do not promise compatibility with every external shader pack.

## Art direction

Geometry stays angular and deliberate. Sparks use a 16-step pixel mask, crystals have discrete faces, thorn strips end in pointed leaves, and rune marks remain sparse enough to read around the player. A small feathered edge softens the aliasing without turning each cast into a white cloud. Local additive emission retains each Aspect's palette.

Shadow wisps use a separate standard-alpha render type: a matte dark center with a subdued colored rim. This preserves the visual distinction between shadow and a bright purple light. Both render types depth-test against the world, draw to the normal target and write only color; Minecraft's composite state restores the normal render settings.

Each cast follows anticipation, expansion and release envelopes. Impact packets instead start immediately and decay. Seven motifs have independent trajectories:

- Flame: tapered tongues spiral upward with falling-intensity stepped embers.
- Shadow: swept three-segment curling ribbons and inward-moving wisps.
- Tide: three rippling foam bands and arcing droplets.
- Steel: angular suspended shards and radial engraved bars.
- Thorn: bending root stems, secondary branches and pointed leaf masks.
- Mirror: floating three-face prisms and intermittent gleams.
- Gravity: three inclined orbits, moving orbital points and inward debris.

Ascended casts add upright sigil strokes. Transformations rise through eight narrow rings around the body instead of covering the model in an opaque sphere. Creature warnings have a fixed outer danger boundary, an inward timing ring and clockwise filling marks. Warnings remain informational: damage and attack timing still come from server combat code.

## Bounds and lifecycle

The existing network packet determines dimension, position, family, kind, radius and lifetime. Its validation and server-only dispatch remain unchanged. The renderer accepts up to 32 live effects and at most 12,000 effect vertices in one frame. It rejects effects farther than 96 blocks and fades the shader from 72 to 96 blocks. Disconnecting or changing worlds clears queued and active visuals. No per-entity framebuffer or unbounded particle queue is introduced.

`SpellGeometry.emit` remains a pure function. The old functional `Sink.quad` API still works. `Sink.styledQuad` has a default adapter for existing validators and adds nine procedural material IDs. UV coordinates encode U=`material*2+[0,1]`, V=`[0,1]` across the four vertices.

Shader references are updated only in NeoForge's load callback. A failed resource reload clears the old reference and uses vanilla lightning rendering, logging the first failure once. On fallback the shader-masked ground disk is omitted so it cannot appear as a solid square; geometric boundaries and glyphs remain visible.

## Gallery integration

`MagicShaders.isLoaded()` reports actual shader registration, rather than the existence of a JSON resource. `renderTypeForMaterial(style)` selects the correct glow or matte-shadow type; `renderType()` selects the glow/fallback type. `time(seconds)` controls gentle shimmer. The in-world renderer sets `viewRange(96)`; a screen-space gallery must set `viewRange(100000)` because its view coordinates use GUI units. Flush each used type with `BufferSource.endBatch(type)`.

## API references

- NeoForge 1.21.1 lifecycle: https://github.com/neoforged/NeoForge/blob/1.21.1/src/main/java/net/neoforged/neoforge/client/event/RegisterShadersEvent.java
- NeoForge namespaced shader constructor patch: https://github.com/neoforged/NeoForge/blob/1.21.1/patches/net/minecraft/client/renderer/ShaderInstance.java.patch
- Minecraft 1.21.1 shader resource format: https://github.com/misode/mcmeta/blob/1.21.1-assets/assets/minecraft/shaders/core/position_color.json

Offline geometry checks cover all 49 family/kind combinations, minimum and maximum radii, lifetime endpoints and invalid values. Java compilation and actual OpenGL loading are separate checks; passing the geometry check alone does not prove GLSL compilation or visual quality.
