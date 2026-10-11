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

`profile` arguments are **family, Soul rank, Aspect rank**. Compare **O → Growth**, ability damage, Essence and these attributes:

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

The second grant must add no extra health. Open **O → Traits**, then die/relog and confirm ownership remains. Natural milestones include 25/100/250 kills, 10 Fallen-or-higher kills, 3 trial rulers, visiting 3/8 Dream biomes and enduring 100 damage after initiation. Affinity traits are earned once; switching test profiles does not collect all seven. Autocomplete lists all 23 IDs. Traits change the actual player attributes and ability calculations.

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

## Rework.1 visual review

This is a source revision; first compile using the current README. Previous test results in supplied documents do not apply to it.

Open **O** and inspect all six tabs, especially Bestiary and Soul Core. Set several GUI scales and resize the window. In Memories, search while moving the caret and check filtered paging, equipped rank labels, weapon manifestation and token actions. On the HUD, check all four cooldown slots, core fill and equipped icons.

Spawn every species using `/spell spawn` autocomplete. Review pincers, spider knees, matriarch abdomen, beast paws, mimic lid/tongue, plant petals, serpent segments, tree limbs, undead equipment and flying feathers. In Survival, verify warning areas appear before impact and the windup body pose is readable. Inspect each form from F5 and in first person, including the wyvern's two hind legs and wing forelimbs.

Compare minimum and saturated fragments using the existing growth commands. Watch growth-scaled geometric magic for all seven families. Repeated casts and dimension travel must expire and clear old effects. Run the base Fast/Fancy/Fabulous checks before an optional shader setup; use `docs/SHADER_COMPATIBILITY.md`.

Generate a fresh Dream world to inspect direct atlas biomes, explicit lagoon/ocean water, empty space under the island belt, irregular clusters and walkable chain approaches. Existing chunks do not change, and an existing save can keep its previous generator settings.
