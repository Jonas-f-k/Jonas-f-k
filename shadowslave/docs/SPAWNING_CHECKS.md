# Nightmare Creature spawning and animation checks

Nightmare Creatures naturally inhabit the **Dream Realm**, in Survival, after initiating a Soul profile. They do not populate the Overworld. Creative and Spectator players do not trigger natural encounters. Peaceful difficulty and `/gamerule doMobSpawning false` suppress natural encounters.

The arrival Sanctuary, gateway refuge, protected Citadel courts, Ember Haven, Blossom Vale, and Sunpetal Meadows are intentionally safe. Move into a wilderness biome to find creatures. Neither a model redesign nor installing shaders changes these rules.

## What changed

- Natural encounters try up to six positions every ten seconds instead of abandoning a whole interval after one obstructed position. At most one creature appears per interval, with a ceiling of eight natural hostile creatures within 64 blocks. Echoes and trial entities do not consume that ceiling.
- Candidate positions lie 24–40 blocks away. Encounters use only loaded chunks, avoid appearing within 24 blocks of another player, and keep their species' fixed rank, class, biome, and day/night conditions.
- Natural spawning uses the actual ground height. The old 66–220 clamp belonged to arrival placement and incorrectly rejected elevated floating-island surfaces.
- Body collision, fluid, headroom, and supporting corner checks prevent broad creatures from appearing in walls or balancing on a single block over the void. No species is assigned aquatic movement: Azure Serpents inhabit Azure Woods; Chain Worms, flying creatures, and wraiths retain their floating navigation.
- Melee attacks now swing. Special attacks synchronize windup, strike, and recovery timing with tracking clients. Previously the renderer read vanilla `attackTime`, but creature melee damage never called `swing`, so attack motion could remain zero.

## Manual test

Use a separate test world with cheats enabled. Run:

```mcfunction
/difficulty normal
/gamerule doMobSpawning true
/spell profile 0 4 6
/spell dream
/gamemode creative
/locate biome shadowslave:forgotten_shore
```

Teleport to the reported biome coordinates while in Creative; the terrain height depends on the world seed. Land on open ground, away from the arrival Sanctuary and Citadel courts. Then run:

```mcfunction
/gamemode survival
/spell refill
```

Wait 10–30 seconds while exploring. Creatures can appear during the day; Chalice Maidens, Dune Revenants, and Moonveil Wraiths are nocturnal. Spawn rates remain bounded, and a dense canopy can still legitimately obstruct a large species.

For a controlled model and animation test, `/spell spawn carapace_scavenger`, `/spell spawn iron_spider`, and `/spell spawn forsaken_knight` work in Creative or Survival, including the Overworld, with cheats. Leave Sanctuary protection and provide clear solid ground first. Press F3+B to compare each model with its hitbox; switch to Survival to see windup, strike, and recovery poses. This command does not rely on natural encounter timing.

## Automated checks

`CreaturePresentationGameTests` checks that all 32 species are reachable through their declared biome ecology, that nocturnal species are excluded during daytime, that elevated supported terrain remains spawnable, that broad creatures reject missing corner support, and that synchronized attacks transition through strike and recovery back to idle.

An interactive client retest is still necessary to judge how attractive each model and animation looks in a real scene. Automated ecology and animation state checks establish behavior, not artistic quality.
