# Seven transformation anatomies

The Aspect-family transformations are original mod adaptations. Their names and abilities are retained; they are not advertised as reproductions of seven canonical character transformations from the novel. The redesign concentrates on readable anatomy, weight-bearing joints, connected surfaces and distinct movement. Magical materials and VFX complement the silhouettes rather than supplying their identity.

`BeastRig` is independent from creature-family construction. All seven bodies use native Minecraft cuboids and articulated pivots; no runtime animation-library dependency or external shader installation is required for their geometry or motion. Collision dimensions, ability mechanics, progression and flight permissions are unchanged by this rig.

| Form | Silhouette and construction | Motion |
| --- | --- | --- |
| Cinder Dragon | Four weight-bearing legs, a horizontal ribcage, short swept horns, low muzzle, five-section tail and a separate pair of bat wings. Broad membrane panels overlap the finger bones. | Diagonal walking gait; wrist-folded wings at rest; shoulder and delayed wrist flap in flight; lowered neck, opened jaw and forward muzzle during breath. |
| Dread Wolf | Broad shoulder ruff, narrower abdomen, long muzzle, large hinged jaw, paired ears, digitigrade hind legs and a thick shadow tail. The wire-furred, long-jawed Dread Wolf description informs the proportions. | Fast low gait, opposing diagonal legs, independent knee flex, ears pulled back during a strike, body compression during pounce and delayed tail motion. |
| Tide Leviathan | A horizontal aquatic serpent with a wide maw, gill fans, broad paired front flippers, five tapered body sections, dorsal fins and a split tail fluke. It has no humanoid torso or standing legs. | Floating respiration, wave motion through successive body sections, slow flipper sweeps, expanding gills and a forward maw attack. |
| Iron Colossus | A compact squared head, continuous stone chest and hips, recessed glowing core, broad shoulders and hands, visible elbows and knees, and flat weight-bearing feet. No horns or antlers. | Slow, short strides; controlled body roll; arms counterbalance the walk; a raised-arm anticipation resolves into a heavy compressed impact pose. |
| Worldroot Stag | A slim horizontal deer torso, long connected neck, narrow split hooves, lateral ears and a branching crown of antlers with emissive local tines. | Smaller controlled knee bends, a light diagonal gait, quiet ear movement and an antler-lowering cast pose. |
| Mirror Griffin | Eagle head, hooked beak, connected feathered breast, avian forelegs with talons, lion hindquarters, tufted lion tail and broad overlapping feather wings. No generic dragon horns. | Grounded diagonal gait; bird wings folded at their wrists; quicker flight strokes than the dragon; secondary feather motion and trailing lion tail. |
| Astral Wyvern | Exactly two hind legs, a pitched torso and long neck, angular jaw fins, swept horns, long balancing tail with a blade tip, and wings whose wrists carry hooked claws. Its limb count differs from the dragon's four legs plus two wings. | Bipedal counterbalanced steps, crouched legs in flight, broad bat-wing strokes, balancing tail and a raised-neck roar pose. |

## Geometry and materials

- Front is negative Z; model feet sit near Y=24. Native rendering keeps the existing form scale and entity collision box.
- Every form has root `body` and `head` parts, and root first-person limb candidates using `arm_-1`/`arm_1`, `leg_0`/`leg_1`, or `wing_-1`/`wing_1`.
- Wings articulate at the shoulder and wrist. Legs articulate through knee and foot; tails form a chain rather than independent floating boxes.
- The existing 64×128 skin has four explicit 32-pixel-high material bands: 0 skin/fur/stone, 1 flexible joints and membranes, 2 plates/feathers, and 3 claws/horns/beaks/hooves. Cube UV spans are checked against the atlas bounds.
- Eyes and local runes are hidden during the base pass. The emissive pass must retain every ancestor transform while rendering named `eyes` and `runes` parts. In particular, antler runes and the Colossus core follow their animated parents.
- There are 329 cuboids across the seven forms. The maximum is 66 for the dragon; individual budgets avoid excessive thin decorative pieces.

## Pose states

Animation starts from the saved bind pose on every call. Locomotion, idle respiration, head tracking, attack anticipation, impact and recovery layer onto that pose. Grounded wings use folded shoulder/wrist poses; flight uses an open shoulder and delayed wrist stroke. A sustained charging flag opens the mouth and changes the relevant neck or gill pose.

Inputs are sanitized for nonfinite values. Stride and attack progress are clamped to 0–1; head tracking is bounded; periodic phases are reduced before trigonometric evaluation. This prevents stale rotations or invalid transforms when a player changes forms or when preview tools supply unusual values.

## Validation and review

The standalone geometry harness baked all seven rigs, checked positive cube extents and 64×128 UV limits, sampled 11,844 finite joint poses across charge, flight, attack and time combinations, and confirmed seven different geometry signatures. A generated isometric geometry sheet was inspected to check the four-legged versus two-legged forms, connected necks and branching stag crown. This is a geometry review, not an in-game screenshot or a substitute for Minecraft client baking.

Minecraft client model validation and the automated client gallery capture are integrated by the main build workflow. In-game review should compare idle side silhouettes, ground gait, wings folded/open, attack anticipation, bright daylight and night-time emissives. Test the dragon and wyvern together to verify their limb counts remain unmistakable, and view the Leviathan side-on to verify its body remains horizontal.
