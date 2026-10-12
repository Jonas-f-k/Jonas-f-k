# Visual reference research

Research for the requested geometry, animation and built-in magic overhaul, 11 October 2026. The supplied development notes are requirements and historical context, not proof of this revision's quality or tests. Their seven Aspect-family forms are original game adaptations; they do not represent every novel character's canonical Transformation Ability.

## Source grounding

**Primary** below means the visible text of Guiltythree's actual book on Webnovel/Chereads was read. Other details use indexed, chapter-referenced community articles and are explicitly **secondary**. Many Webnovel search results are fanfiction; these were excluded. Public chapter previews stop before some descriptions, so this is a focused anatomy review, not a claim that every chapter was read. No novel prose, fan illustrations or third-party creature models are redistributed.

| Creature | Evidence | Design decision inferred from the evidence |
|---|---|---|
| Carapace Scavenger | **Primary:** [chapter 33](https://www.webnovel.com/book/22196546206090805/60347671537319606): eight walking legs, two pincer arms, mandibles, small eyes, bulky chitin and azure blood. | Broad crab abdomen, separate raised torso and independently closing pincers. Preserve eight leg contacts; expose softer joints below armored shell. |
| Carapace Centurion | **Primary:** [chapter 50](https://dynamic.webnovel.com/story/22196546206090805/60739200487586015): larger shell, spikes, black/crimson chitin and two bone scythes replacing pincers. | A sharp, forward-reaching silhouette. Scythes must dominate its attack pose and differ structurally from the Scavenger's jaws. |
| Carapace Demon | **Primary:** [chapter 63](https://en.webnovel.com/book/22196546206090805/61020039557478202): house-scale crab shell, eight segmented supports, raised torso and polished metallic armor. | Large plated knight above a crustacean chassis. Use clear shell facets, measured metal highlights and a horn crest; avoid merely enlarging dark chitin. |
| Stone Saint | **Primary:** [chapter 102](https://dynamic.webnovel.com/story/22196546206090805/61669536037928295): durable living statues, disciplined armed combat. **Secondary:** [Shadow Saint](https://shadowslave.fandom.com/wiki/Shadow_Saint) links stone armor and ruby dust to the early form. | Restrained sword/shield stance, overlapping fitted armor and clean joint gaps. Show weight through grounded steps and short precise strikes. |
| Forsaken Knight | **Secondary:** [article with chapter 99/266/268 references](https://shadowslave.fandom.com/wiki/Forsaken_Knight): matte anthracite gothic armor, curved horns, narrow red visor and darkness-assisted movement. | A tall, tapered armored knight with flared plate hems and curved horns. Engraving is texture work; do not cover the silhouette with repetitive cubes. |
| Lord of the Dead | **Primary:** [chapter 201](https://www.webnovel.com/th/book/shadow-slave_22196546206090805/lord-of-the-dead_62725807562358142): immense accumulation of human bones. | Low sprawling bone mass with readable rib arches/skulls; avoid treating its name as evidence for a humanoid skeleton king. |
| Mordant Mimic | **Primary:** [chapter 421](https://www.webnovel.com/book/22196546206090805/65152564479456660): rows of triangular fangs and a violently snapping lid. **Secondary:** [chapter-referenced article](https://shadowslave.fandom.com/wiki/Mordant_Mimic): flat long chest and retractable thin limbs. | A believable closed chest that reveals an unsettling leggy creature. Hinge the lid at the rear and let anatomy unfold before the bite. |
| Soul Devouring Tree | **Secondary:** [chapter-referenced article](https://shadowslave.fandom.com/wiki/Soul_Devouring_Tree): black bark, vast scarlet crown, fruit and deceptive beauty. | Branch hierarchy and broad leaf clusters with dangling fruit. Slow ornamental motion suits its lure; a cartoon face or walking stump is an original addition. |
| Iron Spider / Matriarch | **Secondary:** [Shore catalogue](https://shadowslave.fandom.com/wiki/Forgotten_Shore/Creature_Catalogue), chapters 220–221: spider anatomy, exceptionally strong webs; enormous armored mother. | Eight articulated legs, abdomen larger than the head, spread supports. Give the Matriarch a heavier belly/shell, not just a scaled spider. |
| Flesh Reaver | **Secondary:** same Shore catalogue, chapter 232: giant locust. | Long abdomen, strong folded rear legs, small forelegs and folded wing panels. A hopping/flying insect should not reuse a humanoid flesh rig. |
| Black Ravager | **Secondary:** [Isle catalogue](https://shadowslave.fandom.com/wiki/Chained_Isles/Creature_Catalogue), chapter 552: lean black-furred quadruped, broad fang maw, red eyes. | Longer feline-like torso with low shoulders and a predatory lunge distinguishes it from the bulkier Wolf. The feline proportion is an adaptation. |
| Dread Wolf | **Secondary:** same Isle catalogue, chapter 379: enormous wolf, wire-tough fur and elongated jaws. | Digitigrade hocks, sloping neck, narrowed hips and a long muzzle. Use a few flat fur tufts; keep its prowling stride low. |
| Chain Worm | **Secondary:** same Isle catalogue, chapters 385/402: serpentine body, humanoid torso, two hands, toothy muzzle, leathery wings; metal eater. | A serpent-demon with shoulder/hand anatomy and folded membranes, not a featureless worm. Wave motion propagates down the lower body. |
| Chalice Maiden | **Secondary:** same Isle catalogue, chapters 525–526: beautiful nocturnal specter masking violence. | Slender floating silhouette and trailing cloth/hair. Robe color and a carried chalice are design interpretations, not verified textual details. |
| Spire Messenger | **Secondary:** [Cursed Herald](https://shadowslave.fandom.com/wiki/Cursed_Herald), chapters 159/257: pale body, dark feathers, two hind legs, six chest arms, wings and tooth-lined beak. | Multi-armed raven/lion anatomy. Preserve the extra chest limbs and broad wing silhouette instead of a simple crow or Griffin recolor. |

Unlisted regional species marked `canon=false` in the source remain original Minecraft fantasy creatures. Keep their species identity and biome palettes while separating animal, insect, plant, serpent and spectral body plans. The rank/class damage rules and current physical sizes are game adaptations.

## Minecraft modeling and animation

[Blockbench's official format guide](https://www.blockbench.net/wiki/blockbench/formats/) distinguishes Java block/item JSON, Modded Entity Java rigs and GeckoLib/Bedrock formats. **Entity rigs may use rotated groups and cuboids without the Java block model's historical 22.5-degree restriction.** A generic mesh or `.glb` does not automatically become a native Minecraft entity model.

The following are original production recommendations: make the silhouette readable at normal camera distance; use a few large primary masses, then articulated secondary details. Taper snouts, limbs and horns with angled cuboids rather than dozens of stepped blocks. Keep intentional empty space under wings, between legs and through pincers. Use consistent texel density, larger material regions and small localized emissive masks. A larger atlas alone does not improve design.

[Blockbench's official animation tutorial](https://www.blockbench.net/wiki/guides/bedrock-modeling/) explains pivots, timeline keyframes and previews. Apply its workflow to the selected runtime format: idle breathing, distance-driven walking, attack anticipation, fast release, recovery, hurt and death should use different timing. Fold wings when grounded and extend them with flight state. Plant contact feet during part of the gait rather than rotating every limb on one sine wave.

Blockbench 5 includes native Java keyframe export. The official catalogue marks `animation_to_java` obsolete (maximum 4.12.9). `animation_utils` also gives way to `geckolib` on Blockbench 5. Do not recommend those older plugins as new requirements.

## Editor plugins actually obtained

The [official catalogue](https://github.com/JannisX11/blockbench-plugins/blob/91ac60fa067008937393e88c902f8871406f55ed/plugins.json) was inspected. **Animation Sliders 0.5.0** (`animation_sliders`, Blockbench ≥4.8.0) was downloaded unchanged with its complete GPLv3 license and source notes into `tools/blockbench-plugins/animation_sliders`. It supplies easing, amplification, retiming and pose normalization. [Installation and provenance](../tools/blockbench-plugins/animation_sliders/README.md) include a pinned upstream revision and SHA-256 records. The editor plugin is not inside the Minecraft JAR and has not been installed on the user's computer.

Optional catalogue tools: **Reference Models 1.1.0** (`reference_models`, desktop Blockbench ≥5.0.0) compares glTF/GLB or project tabs; **Bakery 1.1.1** (`bakery`) bakes animation to linear keyframes; **Animated Platforms 1.0.0** (`animated_platforms`) previews a moving ground plane. These are recommendations, not bundled or installed tools. No suitable ChatGPT Blockbench/Blender/3D-generation plugin was found in the connected catalogue search conducted for this request.

## Natural terrain and foliage

The [official Blockbench Minecraft style guide](https://www.blockbench.net/wiki/guides/minecraft-style-guide/) recommends deliberate pixel clusters, controlled palettes, preserved pixel proportions and checking block tiling before elaborate shading. It cautions against uninformative noise, banding and pillow shading. A 3 × 3 tiled preview reveals repeating patterns that a single sprite hides.

The following are original redesign decisions responding to the Dream Realm screenshot:

- Keep soil, grass, stone, bark and leaves readable as natural materials. Remove central gold glyphs from common ground; reserve carved symbols for ritual stones, ruins and constructed trims.
- Give each 16px tile a few asymmetric material clusters, two or three useful shades and seamless edges. Differentiate mineral facets from moss patches and earth clumps. Use a broken grassy cap over visible soil on side faces.
- Shape broadleaf trees with irregular crown lobes at unequal offsets and heights, visible branch trunks, and occasional crown openings. Give conifers staggered bough lengths and imperfect gaps rather than identical stacked disks. Seeded height, branch and crown variation should remain locally coherent.
- Keep cutout leaf clusters large enough to read; use deliberate holes instead of evenly distributed alpha noise. Alternate bloom/leaf colors within a restrained biome palette rather than making every tree a glowing decoration.
- Use rare, small, low-opacity drifting motes near the foliage that produces them. Ambient magic should reveal landscape depth; giant white squares and constant burst particles obscure it. Bright large effects belong to short combat moments.

Preview natural tile sheets and actual tree compositions beside vanilla grass, stone and oak before approving the art. Bedrock Vibrant Visuals/PBR documentation is not a Java 1.21.1 shader implementation guide and is not treated as one here.

## Runtime alternatives checked

| Library | Primary source status | Decision |
|---|---|---|
| GeckoLib 4 | Its [1.21.1 version catalogue](https://github.com/bernie-g/geckolib/blob/1.21.1/gradle/libs.versions.toml) currently pins **4.9.3**, Java 21, NeoForge minimum 21.1.150. [Project metadata](https://github.com/bernie-g/geckolib/blob/1.21.1/gradle.properties) declares MIT; [license](https://github.com/bernie-g/geckolib/blob/main/LICENSE) is MIT. | Viable alternate animation pipeline, but would require renderer/model/controller migration and runtime distribution. No library dependency added solely to change the appearance. |
| AzureLib | Its [1.21.1 properties](https://github.com/AzureDoom/AzureLib/blob/1.21.1/gradle.properties) pin **3.1.19**, Java 21, NeoForge 21.1.224; [license](https://github.com/AzureDoom/AzureLib/blob/1.21.1/LICENSE) is MIT, with additional notices for incorporated components. [Export guide](https://github.com/AzureDoom/AzureLib/blob/1.21.1/docs/azurelib/blockbench/exporting_assets.mdx) uses Azure `.geo`/animation assets. | Separate alternative, not interchangeable with native Java rigs or a drop-in visual upgrade. |

Those version numbers are verified upstream **source-branch pins**, not a claim that those exact released binaries were downloaded or tested here. Modrinth's version API was blocked in this environment. Native hierarchical geometry and animation remain capable of the requested design work without either runtime library.

## Built-in shader lifecycle

The target-version [NeoForge `RegisterShadersEvent` source](https://github.com/neoforged/NeoForge/blob/1.21.1/src/main/java/net/neoforged/neoforge/client/event/RegisterShadersEvent.java) specifies a **client mod-bus** event after vanilla shader registration. Construct `ShaderInstance` with `event.getResourceProvider()` and register a callback; store the **loaded callback instance** so reloads replace the reference. Target [1.21.1 networking documentation](https://docs.neoforged.net/docs/1.21.1/networking/) supplies the supported server-to-client payload path for combat emissions.

Ship the GLSL/JSON resources in the mod and select them through its own render types. This can implement animated magical materials, luminous edges, dissolves, pulse masks and fog-correct effects with no external shader pack. Keep the outer glow restrained and preserve a readable bright core, depth testing and transparent order; clean up on world changes and resource reload. These implementation/design choices need a graphical client check in Fast/Fancy/Fabulous and with shader stacks, plus performance profiling. CPU vertex/UV checks cannot prove shader compilation or visual quality.

Global shadow maps, screen-space reflections and full-scene bloom are different rendering systems and must not be claimed simply because a material uses a custom shader. No Iris/Oculus binary or external shader pack is automatically installed by this research. [Botania](https://github.com/VazkiiMods/Botania), [The Aether](https://github.com/The-Aether-Team/The-Aether) and [Twilight Forest](https://github.com/TeamTwilight/twilightforest) remain visual references; source availability alone is not permission to copy their art.
