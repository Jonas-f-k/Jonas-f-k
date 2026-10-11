# Progression and beast forms — 0.5.0-alpha

These are the implemented Minecraft balance rules. They are adaptations of Shadow Slave's separate Soul-rank, Aspect-rank and core/class systems; the exact combat formulas and milestone traits are original game rules.

## Rank and core growth

Soul rank 1–4 means Sleeper, Awakened, Master, Saint. Aspect rank 0–6 means Dormant through Divine. Higher Soul ranks come from Gateway return, Second Nightmare and Third Nightmare. Aspect rank stays separate.

| Divine cores | Cumulative fragments when formed | Current core capacity | Cumulative fragments when full |
|---|---:|---:|---:|
| 1 | 0 | 1,000 | 1,000 |
| 2 | 1,000 | 2,000 | 3,000 |
| 3 | 3,000 | 3,000 | 6,000 |
| 4 | 6,000 | 4,000 | 10,000 |
| 5 | 10,000 | 5,000 | 15,000 |
| 6 | 15,000 | 6,000 | 21,000 |
| 7 | 21,000 | 7,000 | 28,000 |

Only Divine Aspects form additional cores. Other Aspects have one core whose saturation caps at 1,000. Formed Divine cores persist after fragment loss; lost fragments can reduce saturation. Traits are also permanent. Operator `/spell cores N` explicitly overrides formed cores for testing.

Let `r = max(0, SoulRank − 1)`, `c = formed cores`, and `f = cumulative fragments`:

- Core floor: `c(c−1) × 500`; capacity: `c × 1000`.
- Saturation: `s = clamp((f − floor) / capacity, 0, 1)`.
- Growth: `g = c − 1 + s`. Filling core one to 100% and forming core two both give `g = 1`, so strength does not drop on core birth.
- Direct ability strength: `1.65^r × (1 + 0.20g) × (1 + 0.07 × AspectRank) × (1 + trait power)`.
- Essence capacity: rounded `100 × 1.65^r × (1 + 0.45g)`, then the trait capacity bonus.
- Maximum health: `20 + 8r + 8g + trait health`.
- Cooldowns: `base × max(0.35, (1 − trait reduction) / (1 + 0.15r + 0.10g))`, rounded to ticks with a 20-tick minimum.
- Recovery: `(1 + 0.65r + 0.30g) × (1 + trait recovery)` Essence per second outside combat, if natural regeneration is enabled; essence-enhancing Memories add their existing bonus.
- Duration: `base × (1 + 0.12r + 0.06g)`; reach increases with both rank and growth, up to 2.2 times base.

Melee damage and movement speed also gain rank/core bonuses. Status amplifiers improve in steps; healing uses a gentler square-root power curve. Gaining maximum health creates capacity without repeatedly healing the player for free.

| Trait-free Divine example | Growth | Ability multiplier | Maximum health | Essence capacity |
|---|---:|---:|---:|---:|
| Sleeper, empty first core | 0 | 1.42 | 20 | 100 |
| Awakened, empty first core | 0 | 2.34 | 28 | 165 |
| Master, empty first core | 0 | 3.87 | 36 | 272 |
| Saint, empty first core | 0 | 6.38 | 44 | 449 |
| Saint, seventh core just formed | 6 | 14.03 | 92 | 1,662 |
| Saint, seventh core saturated | 7 | 15.31 | 100 | 1,864 |

Actual characters earn traits and can wear Memories, so their displayed numbers will differ.

## Beast bodies

J unlocks at Saint and activates the original form assigned to your Aspect family. The player remains the controlled and damageable entity, with the same inventory. A dimensions mixin changes real collision and eye height; original articulated models replace the rendered human body. Vanilla Scale modifiers still apply once.

| Family | Form | Width × height, blocks | Special attack on R | Mobility |
|---|---|---|---|---|
| 0 Flame | Cinder Dragon | 3.4 × 3.8 | Repeated directional Cinder Breath and burning | Flight; fire resistance |
| 1 Shadow | Dread Wolf | 2.8 × 2.4 | Collision-checked pounce and close strike | Fast stride; night vision |
| 2 Tide | Tide Leviathan | 3.2 × 2.6 | Pulling Tidal Maw, stronger in water | Water breathing; fast swimming |
| 3 Steel | Iron Colossus | 3.0 × 5.6 | Seismic Stomp with knockup and weakness | Heavy, durable, slower |
| 4 Thorn | Worldroot Stag | 3.0 × 3.9 | Root/poison area and nearby healing | Regeneration; quick stride |
| 5 Mirror | Mirror Griffin | 3.8 × 3.4 | Wingbeat with knockback, blindness and absorption | Flight; Slow Falling |
| 6 Gravity | Astral Wyvern | 3.4 × 4.2 | Gravity pull, damage and levitation | Flight; Slow Falling |

Activation costs 60 Essence, beast attacks 20, and upkeep 2 per second. Base duration is 25 seconds and grows with progression. Beast attack base cooldown is 8 seconds, reduced by growth. Transformation recovery uses a 20-second base after the scheduled duration, reduced by growth; early dismissal does not cancel that lockout. Normal G/H abilities remain usable.

Form attributes add maximum health, natural armor, claws and knockback resistance. The Colossus has the strongest armor/claws. The body must fit at activation; crouching cannot shrink it through a small gap. Dismissal, death, depletion, expiry and dimension travel remove form modifiers and restore dimensions/flight. Flight uses double Space; form exit adds four seconds of Slow Falling. Creative flight is retained.

These are original Minecraft forms. Canon Saint Transformations include other possibilities, including forms that are not animals and character-specific systems.

## Permanent traits

One affinity is normally earned per character. Sixteen additional milestone definitions reward hunts, exploration, saturation, rank, endurance and Echo ownership. Each ID can be granted once; rechecking, replaying, dying and reloading do not multiply it. Operator profile changes retain owned traits. O → Traits and `/spell traits` show the bonuses. `Traits.java` contains all exact conditions.

The small bonuses supplement the main rank/core curve. Examples: Nightmare Survivor adds two maximum health, Hunter adds 4% melee damage, Ancient Slayer shortens cooldowns by 3%, Wayfarer adds 2% speed, and Unyielding adds two health after enduring 100 damage.

## Ancient creature combat

The 32 regional species are larger and stronger, with rank/class health scaling. Their 12 base body archetypes have individual geometry additions and dedicated material panels. Special attacks use 26- or 36-tick windups, locked aim and at least 60 ticks between releases. Warning rings show the aimed location. The player can dodge a released attack; walls and Sanctuary checks protect targets.

Wolves/insects lunge, carapaces cleave, colossi stomp, spiders/flowers apply aimed webs/toxins, serpents/flyers/wraiths/trees release aimed supernatural attacks, mimics bite and humanoids slash. Species-specific fire/frost effects, passive regeneration and brood summoning remain. Melee is paused during special windup; ordinary melee can resume between specials. These behaviors are game interpretations, not a claim that every species uses that exact attack in the novel.

