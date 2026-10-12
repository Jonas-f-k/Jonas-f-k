# Rendering and shader checks

This source targets Minecraft 1.21.1 / NeoForge 21.1.256. A shader mod is optional. This revision compiled successfully on GitHub Actions and passed server GameTests and real client model checks. Interactive graphics and optional shader stacks remain untested.

Native pixel albedos, entity normals and Minecraft's lightmap provide ordinary shading. Eyes and equipped trims have emissive layers. Local spell geometry uses the mod's automatically loaded GLSL shader with depth-tested glow and matte-shadow passes after weather. A resource-load failure uses bounded vanilla rendering as fallback. The bundled shader covers spell effects; a full-world lighting shader pack is a separate optional capability. Effects and warnings remain server-triggered.

The linked Iris, Oculus, Sodium and Embeddium projects are compatibility references. Select releases that explicitly support Minecraft 1.21.1 and NeoForge and satisfy that release's dependencies. This package does not contain them or claim that they can all be installed together. Complementary and Bliss are optional shader-pack references, not tested bundled dependencies.

Before release, test:

| Setup | Checks |
|---|---|
| Base NeoForge, Fast / Fancy / Fabulous | All 32 species, seven transformations, armor, transparent foliage, warning rings, spell expiration, dimension changes and GUI resizing |
| One compatible shader-mod stack, shaders disabled | Same checks; no missing atlas sprites or unusual effect layering |
| Same stack, one chosen shader pack enabled | Eye/trim brightness, effect bloom, water, fog, depth occlusion and warning contrast |
| Multiplayer, both clients using this mod revision | Nearby cast and warning packets, different graphics settings, transformed hitboxes and repeated realm travel |

Test minimum and saturated cores with `/spell cores`, `/spell fragments` and `/spell growth`. Compare cast size, actual damage and cooldowns. Use `/spell profile <family> 4 6` and `/spell transform` for each form. Spawn each species from `/spell spawn` autocomplete and examine F3+B collision bounds.

No performance measurements or compatibility certification are claimed by the offline geometry checks. The renderer currently limits 32 active effects, a 64-packet pending queue and 12,000 vertices per frame.
