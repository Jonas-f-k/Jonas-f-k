# Creature catalogue — 0.6.0-rework.1

Dimensions are width × height in blocks and drive collision, rendering and targeting. All species are 25% wider and 30% taller than 0.4. See `LORE_RESEARCH.md` for sources and uncertainty; Matriarch classification is inferred, and Soul Devouring Tree uses the early Shore-era rank. Combat is an original game adaptation. Every species now has a complete articulated rig and its own native pixel material. Geometry validation distinguishes all 32 species; Minecraft baking/playtesting remains required.

| Species ID | Rank / class | Body | Dimensions | Affinity/behavior | Regions | Origin |
|---|---|---|---|---|---|---|
| carapace_scavenger | Awakened Beast | Carapace | 2.75 × 2.08 | armor | forgotten_shore | Canon reference |
| carapace_centurion | Awakened Monster | Carapace | 3.5 × 2.86 | charge | forgotten_shore | Canon reference |
| carapace_demon | Awakened Demon | Carapace | 4.25 × 3.64 | regenerate | forgotten_shore | Canon reference |
| iron_spider | Awakened Beast | Spider | 3.375 × 2.34 | web | forgotten_shore, scarlet_wilds | Canon reference |
| iron_spider_matriarch | Awakened Tyrant | Spider | 6 × 4.03 | brood | forgotten_shore | Canon reference |
| flesh_reaver | Awakened Beast | Insect | 2.375 × 2.73 | leap | forgotten_shore | Canon reference |
| rolling_stone | Awakened Monster | Faceted boulder | 2.875 × 2.99 | charge | forgotten_shore, hollow_mountains | Canon reference |
| stone_saint | Awakened Monster | Humanoid | 1.5 × 3.51 | armor | forgotten_shore | Canon reference |
| forsaken_knight | Fallen Devil | Humanoid | 1.875 × 4.16 | dark_regenerate | forgotten_shore, hollow_mountains | Canon reference |
| spire_messenger | Fallen Monster | Winged creature | 2.875 × 4.29 | screech | forgotten_shore | Canon reference |
| lord_of_dead | Fallen Tyrant | Colossus | 5.5 × 7.54 | brood | forgotten_shore, death_zone | Canon reference |
| soul_devouring_tree | Awakened Terror | Tree | 6 × 9.36 | lure | forgotten_shore | Canon reference |
| black_ravager | Fallen Beast | Wolf | 3 × 2.99 | leap | chained_isles | Canon reference |
| dread_wolf | Fallen Monster | Wolf | 3.875 × 3.25 | charge | chained_isles, frostglass_taiga | Canon reference |
| chain_worm | Fallen Demon | Winged serpent | 3.25 × 4.68 | leap | chained_isles | Canon reference |
| chalice_maiden | Fallen Demon | Wraith | 1.5 × 4.03 | drain | chained_isles, moonlit_grove | Canon reference |
| mordant_mimic | Fallen Devil | Mimic | 2.75 × 2.6 | ambush | chained_isles | Canon reference |
| sailor_doll | Fallen Beast | Humanoid | 1.25 × 3.64 | slash | chained_isles | Canon reference |
| blood_flower | Awakened Monster | Flower | 2.25 × 3.12 | poison | forgotten_shore, scarlet_wilds | Canon reference |
| rime_stalker | Awakened Monster | Wolf | 2.75 × 2.6 | frost | frostglass_taiga, aurora_fields | Original |
| aurora_moth | Awakened Beast | Winged creature | 3.125 × 2.21 | screech | aurora_fields, amethyst_heights | Original |
| glass_huntsman | Fallen Demon | Spider | 3.5 × 2.86 | web | amethyst_heights, nightmare_desert | Original |
| dune_revenant | Fallen Monster | Humanoid | 1.375 × 3.51 | ambush | nightmare_desert | Original |
| ivory_burrower | Fallen Demon | Winged serpent | 3.5 × 3.77 | poison | nightmare_desert | Original |
| hollow_watcher | Corrupted Monster | Wraith | 2.375 × 5.2 | screech | hollow_mountains, death_zone | Original |
| ashen_colossus | Corrupted Devil | Colossus | 4.5 × 7.15 | charge | death_zone, hollow_mountains | Original |
| moss_hart | Awakened Beast | Wolf | 2.5 × 3.12 | regenerate | mosslight_ravine, verdant_gardens, golden_grove | Original |
| azure_serpent | Awakened Monster | Winged serpent | 2.375 × 3.25 | frost | azure_woods | Original |
| moonveil_wraith | Awakened Demon | Wraith | 1.625 × 3.9 | drain | moonlit_grove | Original |
| gilded_mantis | Awakened Monster | Insect | 2.125 × 3.64 | slash | golden_grove, verdant_gardens | Original |
| briar_maw | Awakened Demon | Flower | 2.625 × 3.77 | poison | mosslight_ravine, scarlet_wilds | Original |
| cinder_hound | Awakened Monster | Wolf | 2.625 × 2.6 | fire | scarlet_wilds | Original |

Memory rank matches creature rank; Memory tier equals creature core count. Regional pools also filter item categories. `/spell bestiary` lists all entries. Special attacks are based on the body archetype with visible windups: carapace cleaves, spider webs, wolf/insect lunges, serpent/flyer beams, wraith drains, mimic bites, flower toxins, colossus stomps, tree area drains and humanoid strikes. Species fire/frost effects, regeneration and brood summoning supplement these. See `PROGRESSION.md`.
