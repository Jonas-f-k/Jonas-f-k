> User-supplied reference document. Historical release/test claims below are not validation of 0.6.0-rework.1. See the current root VALIDATION.md.

# Dream Realm and visual rework — 0.6.0-alpha

The previous build still used Overworld climate parameters, caves, aquifers, ores and stone for most of the Dream Realm. Grass models were stacked layers. Item, ground, foliage and creature materials were extracted from illustrated atlases. Those paths are replaced in this release.

## World

- A custom Dream biome source selects regions from an irregular cartographic atlas; temperature and humidity no longer choose biomes.
- World-seeded noise drives the relief. Sixteen terrestrial profiles supply different shapes: glacial ridges, cathedral peaks with open vaults, snowfield swells, shattered channels, forest valleys, lagoons, a drowned shore, blossom valleys, deep ravines, crystal terraces, autumn uplands, garden shelves, dunes, meadow downs, red escarpments and refuge hills.
- Vanilla cave carvers, cave-density references, ore features, ore veins and aquifers are absent. The natural base is realm ash stone. Explicit waters fill surface coast/lagoon basins; the generator's fluid level is below the build range, also removing Minecraft's implicit deep lava from the sky belt.
- The Chained Isles use irregular clusters of 4–7 medium islands, five shape archetypes, unequal ellipses, different thicknesses and elevations, and connected sagging chain graphs. There is no regular 104-block grid of identical islands.
- Biome scenery adds fossil ribs, crystal fans, giant fungi, arches, garden rings, fallen timber and obelisks alongside biome-shaped trees, flower drifts, ruins and citadels.

## Art and combat

- Retired all eight illustrated atlas PNGs. Each of the 128 Memory icons has distinct native pixel artwork; selected upstream sprites are unchanged, including animation metadata. See `ASSET_CREDITS.md`.
- Grass is a full cube with independent top, side and soil underside. Natural ground tiles have matching edges. Leaves are cutout models with explicit transparent pixels and remain persistent when generated.
- Equipped armor has stronger colors, simple broad material panels and emissive trims on its existing sculpted geometry. Creature and beast materials use coherent small palettes rather than muddy illustrated panels.
- Snow, ash, spores, motes and biome fog supply atmosphere. No shader mod is required.
- Aspect casts have animated family-specific geometry: flame spirals, lances, tidal shells, iron sigils, roots, prism facets and gravity orbits. Transformations, beast attacks and Memory weapon swings have separate effects. Creatures display their locked warning area before impact.
- Effects use one bounded visual packet per emission, a 32-effect client cap, a vertex budget, depth testing, expiration and level-change cleanup. Existing particle bursts are reduced. Damage, cooldowns, healing and progression stay authoritative on the server.

## Validation and use

A clean build and all 36 required GameTests passed. These include real Dream chunks with realm-only geology, a clear island void, seed-dependent relief, different landforms, connected archipelago graphs, and all 49 visual packet family/type combinations. CPU checks exercised 177,312 effect quads plus every creature, beast and armor model, including 31,008 texture vertices inside their assigned atlas sprites. Resource checks verified 321 models, 280 sprites, all 128 unique icons, soil faces, terrain edge matching, leaf alpha, animation metadata and credited asset integrity.

A full interactive graphical playtest remains a separate check; automated model and geometry checks do not prove camera framing, every graphics mode or performance on all hardware. See `VALIDATION.md` for the exact checks and graphical launch result.

Install on Minecraft 1.21.1 with NeoForge 21.1.256 and Java 21, including through a matching CurseForge profile. Replace the previous mod JAR on both client and server. New terrain appears in new chunks; use a fresh test world for the complete atlas. Existing characters and Memory IDs remain compatible. `TESTING_COMMANDS.md` includes biome visits, all Aspect families, beast forms, armor and weapon checks.


---

# Validation — 0.6.0-alpha

Verified on 2026-10-11 with Minecraft Java 1.21.1, NeoForge 21.1.256, JDK 21 and Gradle 9.2.1. The distribution uses classes and resources built from the current reviewed source.

## Build and gameplay

`clean build prepareClientRun runGameTestServer` completed successfully. **All 36 required NeoForge GameTests passed.** The final first-person trim light adjustment was compiled into the release afterward.

The original 32 checks cover trial inventory/XP restoration, six-seal maps and bosses, Gateway Awakening, Memory quality and real armor attributes, rank/saturation growth and seven Divine cores, persistent traits and attunement, creature sizes/rewards/Echo ownership, beast hitboxes and flight cleanup, real grove generation, tree headroom and walkable ruin approaches.

Four new checks exercise actual Dream chunk generation without vanilla ores or stone, a clear void below floating islands, seeded relief, different regional terrain profiles, connected clusters of 4–7 islands with all five shape families, and round-trips for all 49 effect family/type packet combinations. Invalid visual positions are rejected. The existing chain test now verifies multiple unequal islands and a walkable link within a cluster.

Nonfatal public-key network errors and heightmap warnings occurred in isolated server tests. Heightmap warnings arise when features are manually invoked on already completed test chunks. Deprecated Minecraft/Gradle APIs remain; they did not fail a required test.

## Assets and client models

`python tools/verify_art_resources.py <Minecraft client resources JAR>` passed: **321 models, 280 native sprites and 128 distinct pixel Memory icons**. Checks include JSON and vanilla references, PNG dimensions, animation metadata, transparent silhouettes, proper grass top/side/bottom faces, matching natural terrain edges, cutout leaf alpha, and SHA-256 equality for all 80 credited original assets. The same check rejects vanilla carvers, ore features, aquifers and an Overworld noise router in Dream Realm resources.

`python tools/verify_client_models.py` passed against the built NeoForge client classpath. It baked and animated all 32 regional species, legacy creatures, all seven beast forms and 30 armor shell/trim layers. **10,227 part poses were finite, and all 31,008 model texture vertices remained inside their assigned atlas sprites.** Glowing eyes and species details follow their animated body.

The checker also exercised **177,312 finite spell quads** across seven families, seven visual types, three radii and six animation times, checking size bounds, alpha and expiration. Soul Sea controls were constructed at nine screen sizes; they remained on screen without overlapping, retained search caret position and exposed the first selected weapon's action. These are CPU checks rather than screenshots of every model or effect.

## Graphical launch

A live NeoForge client successfully initialized software OpenGL on a local virtual display, loaded the mod and resource atlases, and displayed Minecraft's initial accessibility screen. The frame was captured and inspected. No missing Shadow Slave model/texture errors were reported during that resource load. This is a client startup/resource smoke check, not an in-world combat or realm traversal test: the onboarding screen prevented the quick-play world from opening before the bounded test ended.

The environment has no audio device and could not reach Mojang profile/public-key services. These produced audio/authentication messages without preventing the inspected screen from rendering. The earlier display setup attempts failed before mod loading; the final virtual display launch succeeded.

Actual Dream Realm camera composition, effect appearance in Fast/Fancy/Fabulous, saved-encounter rendering, extended balance, performance and real multiplayer/modpack behavior still require local gameplay. `TESTING_COMMANDS.md` supplies the routes and commands.

## Distribution and compatibility

The release JAR and source ZIP pass CRC and duplicate-entry checks. The mod metadata declares **0.6.0-alpha**. The JAR contains no retired atlas PNGs, unused illustrated hide or GameTest-only world preset. Credited originals and their license are included; the source archive also contains SVG sources, upstream source sprites and the native builders. Compiled outputs, dependency caches, run worlds and local machine helpers are excluded from the source archive.

Install the same JAR on each client and server using a Minecraft 1.21.1 / NeoForge profile, including in CurseForge. Remove the older Shadow Slave JAR. Existing Memory IDs, ownership, attunement and saved trials retain their state. New terrain appears only in newly generated chunks; use a fresh test world to inspect the complete new Dream Realm geography.


---

# Testing commands — Shadow Slave 0.6.0-alpha

Enable cheats or use operator permission level 2. Replace `@s` with your player name when executing from the server console. Commands that act on the executing player, such as `profile`, `spawn` and `trialwarp`, must be run in the player's chat.

## First Nightmare

Use a fresh character for the first-trial sequence:

```mcfunction
/difficulty normal
/spell start @s 0
```

Wait for the map to finish building. Families are 0 Flame, 1 Shadow, 2 Tide, 3 Steel, 4 Thorn, 5 Mirror and 6 Gravity.

```mcfunction
/spell status
/spell trialwarp 0
/spell trialwarp 1
/spell trialwarp 2
/spell trialwarp 3
/spell trialwarp 4
/spell trialwarp 5
```

Each command visits the corresponding seal. Interact, defeat its wardens and interact again. The two tracks are **0 → 2 → 4** and **1 → 3 → 5**. Warp sites **6–10** visit the five possible boss arenas; site **11** visits the exit. These commands do not mark objectives completed.

For the reported rendering crash, start family **0** and interact with a seal so its Flame Nightmares appear. Turn toward the wardens and boss. These trial creatures intentionally have no regional species ID; that case now uses a valid family model instead of calling `List.of().indexOf(null)`. Check an existing saved trial too.

For a quick successful return after examining the map:

```mcfunction
/spell trialfinish
```

This operator command marks seals complete and invokes the normal finish path. In a higher Nightmare, the ruler must still have been defeated. It does not grant a fake boss victory. A real player-attributed test kill of the spawned ruler can use:

```mcfunction
/damage @e[type=shadowslave:nightmare_creature,nbt={SpellBoss:1b},limit=1,sort=nearest] 10000 minecraft:player_attack by @s
```

`/spell abandon` restores the stored inventory. `/spell retry` starts a fresh attempt with the same family and boss-site assignment after failure.

## Abilities and armor without replaying the opening

```mcfunction
/spell profile 1 1 6
/spell cast
/spell awakened
```

The arguments are **family, Soul rank, Aspect rank**. Soul ranks 1–4 are Sleeper, Awakened, Master and Saint. Aspect ranks 0–6 are Dormant through Divine. The profile command keeps your inventory and existing Memories, and refuses to run during an active/preparing trial. In the example, the second ability must report that a Gateway return is required.

```mcfunction
/spell profile 1 2 6
/spell dream
/spell awakened
/spell refill
/spell profile 3 3 6
/spell ascended
/spell profile 4 4 6
/spell transform
```

Use **R/G/H/J** for the same ability actions. A clear space is required for the form; use **F3+B** to inspect the expanded player hitbox. `/spell refill` ends an active form, restores Essence and clears all ability cooldowns. Dream entry requires ten seconds outside combat.

## Continuous power progression

`profile` arguments are **family, Soul rank, Aspect rank**. Compare **O → Soul Core**, ability damage, Essence and these attributes:

```mcfunction
/spell profile 1 2 6
/spell cores 1
/spell fragments 0
/spell growth
/attribute @s minecraft:generic.max_health get
/attribute @s minecraft:generic.attack_damage get
/spell fragments 500
/spell growth
/spell fragments 999
/spell growth
/spell fragments 1000
/spell growth
/spell fragments 3000
/spell growth
/spell fragments 6000
/spell fragments 10000
/spell fragments 15000
/spell fragments 21000
/spell growth
/spell fragments 28000
/spell growth
/spell profile 1 3 6
/spell growth
/spell profile 1 4 6
/spell growth
```

`fragments` **sets** the cumulative amount. Core birth must preserve strength; each rank must increase it. At 21,000 you have seven cores with the seventh empty; at 28,000 it is saturated. Extra fragments cannot form an eighth core. `/spell fragments 0` does not erase formed cores or traits. For a repeatable low-core test, explicitly use `/spell cores 1`, which resets cores and fragments for operator testing. Previously earned traits remain; use a fresh character for an untouched baseline.

```mcfunction
/spell profile 1 2 3
/spell fragments 28000
/spell growth
```

A non-Divine Aspect must still have only one core, fully saturated. Restore a Divine profile before testing `cores`.

## Seven actual beast forms

Use a flat outdoor space with at least **6 blocks of headroom** and **5 blocks of width**. Use F5 and F3+B. Choose one profile, then run the activation and attack commands:

| Command | Form | Width × height | Attack on R |
|---|---|---|---|
| `/spell profile 0 4 6` | Cinder Dragon | 3.4 × 3.8 | Cinder Breath |
| `/spell profile 1 4 6` | Dread Wolf | 2.8 × 2.4 | Predator's Pounce |
| `/spell profile 2 4 6` | Tide Leviathan | 3.2 × 2.6 | Tidal Maw |
| `/spell profile 3 4 6` | Iron Colossus | 3.0 × 5.6 | Seismic Stomp |
| `/spell profile 4 4 6` | Worldroot Stag | 3.0 × 3.9 | Rootbound Sanctuary |
| `/spell profile 5 4 6` | Mirror Griffin | 3.8 × 3.4 | Prismatic Wingbeat |
| `/spell profile 6 4 6` | Astral Wyvern | 3.4 × 4.2 | Gravity Roar |

```mcfunction
/spell refill
/spell transform
/spell beast
```

J toggles the body; R uses its attack. Dragon, Griffin and Wyvern fly with double Space. The Leviathan gains water breathing/swimming power. Test the Wolf's pounce, the Colossus's area stomp, the Stag's healing/roots and the Wyvern's gravity pull. Check the first-person claws, full body, eye height, real collision, animation and material.

Press J again or wait for expiry/Essence depletion. The human hitbox must return and Survival flight must end. Try transforming beneath a low ceiling: it should refuse without spending Essence. Crouching must preserve the large beast dimensions. `/spell refill` provides a quick cleanup between forms; switching profile also ends a current form. Check death and dimension travel restore the normal body.

## Persistent traits

```mcfunction
/spell traits
/spell trait unyielding
/attribute @s minecraft:generic.max_health get
/spell trait unyielding
/attribute @s minecraft:generic.max_health get
```

The second grant must add no extra health. Open **O → Attributes**, then die/relog and confirm ownership remains. Natural milestones include 25/100/250 kills, 10 Fallen-or-higher kills, 3 trial rulers, visiting 3/8 Dream biomes and enduring 100 damage after initiation. Affinity traits are earned once; switching test profiles does not collect all seven. Autocomplete lists all 23 IDs. Traits change the actual player attributes and ability calculations.

## Equipped armor and Memories

```mcfunction
/spell reward ashen_shroud 2 4
/spell reward soul_edge 1 3
/attribute @s minecraft:generic.armor get
/attribute @s minecraft:generic.armor_toughness get
```

Open **O → Memories**, equip the armor, and compare the attribute values. Press **B** and verify the points disappear. The reward arguments are **Memory ID, rank 0–6, tier 1–7**. Autocomplete lists all IDs. A duplicate reward becomes a ranked trade token. `memory <player> <id>` grants the catalogue's default rank/Tier I:

```mcfunction
/spell memory @s tide_crown
```

## Creatures and Echoes

Move outside a Sanctuary before spawning hostile creatures. Choose clear, solid ground and enough space:

```mcfunction
/spell bestiary
/spell spawn carapace_scavenger
/spell spawn carapace_demon
/spell spawn iron_spider
/spell spawn dread_wolf
/spell spawn chain_worm
/spell spawn mordant_mimic
/spell spawn forsaken_knight
/spell spawn soul_devouring_tree
/spell echoadd carapace_scavenger
/spell echo carapace_scavenger
```

Autocomplete lists all 32 species. **F3+B** displays their physical hitboxes. The Soul Devouring Tree is now 6 × 9.36 blocks, so provide enough space. Look for distinct crests/plates/horns along with each material. Each species retains a fixed rank and class; killing one can drop its matching Memory quality. In Survival, watch for the warning ring and dodge before a lunge, stomp, web, beam or bite releases. Special attacks have recovery windows. The `echoadd` command grants test ownership, while `echo` uses the normal summon cost and space checks.

## Dream Realm, Citadels and higher Nightmares

```mcfunction
/spell profile 0 2 6
/spell dream
/execute in shadowslave:dream_realm run tp @s 480 220 0
/effect give @s minecraft:slow_falling 60 0 true
```

The fortress constructs as you approach. Its floor height follows local terrain, so the high teleport plus Slow Falling is a viewing shortcut. Other centers are **-1024,640** and **1024,-768**. Wait for construction before evaluating rooms and towers.

```mcfunction
/give @s shadowslave:nightmare_seed
```

Right-click the Seed while standing in the Dream Realm as an Awakened. Complete the Second Nightmare to become Master. Obtain/use another Seed as Master to attempt the Third and become Saint. Verify that your Aspect rank, equipped Memories, original inventory and waking return location survive both success and failure.

Biome search examples:

```mcfunction
/locate biome shadowslave:chained_isles
/locate biome shadowslave:forgotten_shore
/locate biome shadowslave:frostglass_taiga
/locate biome shadowslave:blossom_vale
/time set night
```

Run biome searches in the Dream Realm. Existing generated terrain does not change; use a new world or unexplored chunks for the new flora and white desert surface.


## Native terrain, foliage and combat effects (0.6)

Use a new test world so existing Dream Realm chunks do not retain old terrain. In creative mode:

```mcfunction
/spell profile 0 4 6
/spell dream
/locate biome shadowslave:chained_isles
/locate biome shadowslave:amethyst_heights
/locate biome shadowslave:mosslight_ravine
/locate biome shadowslave:moonlit_grove
```

Teleport to the returned coordinates while in the Dream Realm. Inspect island gaps from above and below; iron walkways connect groups of medium islands, and the void has no lava or ocean floor. Shore and lagoon waters are explicit features. Check grass from above, beside it and underneath, and view leaves against the sky. Browse all 128 icons in the creative tab and compare the equipped armor in third person.

```mcfunction
/spell profile 0 4 6
/spell cast
/spell awakened
/spell ascended
/spell refill
/spell profile 4 4 6
/spell ascended
/spell refill
/spell profile 6 4 6
/spell ascended
```

Repeat with families 1, 2, 3 and 5. Reset with `/spell refill` between casts. R/G/H show family-specific animated effects; J shows the transformation and its enlarged body. Summon a Memory weapon with O → Memories → Summon, then hit a creature to check slash trails. Creature specials display a warning circle before striking. Test with Fast, Fancy and Fabulous graphics and with particles set to Minimal; geometry still renders when particle density is reduced.
