# Native anatomy and animation redesign

This revision replaces the generic upright shapes with connected Minecraft cuboid anatomies. Source evidence and uncertainty are recorded in `VISUAL_REFERENCE_RESEARCH.md`; the seven Aspect transformations remain original mod adaptations.

- Scavengers have eight jointed walking legs and separate pincers. Centurions have bone scythes and dark crimson chitin. Demons have an elevated chest and metallic shell.
- Spiders have low connected thoraxes, rear abdomens and eight radial legs. Matriarchs retain a larger abdomen. Flesh Reavers use a locust body, folded wings and jumping hind legs.
- Wolves and ravagers use horizontal bodies, muzzles, jaws and articulated ground gaits. The Chain Worm has a serpentine lower body, torso, arms and wings.
- The Lord of the Dead is a sprawling bone mound. Knights use armor, weapon and shield silhouettes. The Mimic has a rear-hinged lid and teeth inside the chest.
- The Soul Devouring Tree has branching foliage and fruit, with small bark seams instead of a cartoon face. Original regional creatures keep explicitly distinct designs.

Rest angles are preserved by delta animation. Breathing moves the whole skeleton instead of disconnecting its torso from its neck. Walk cycles depend on travelled distance. Synced windup, strike and recovery phases drive pincers, mandibles, lids, wings and attack poses.

The Bestiary's model viewer renders the same baked models and materials as the world renderer. It supports rotation, idle, walk, windup, strike and flight previews. Numeric model checks and a graphical capture are validation aids; they do not replace artistic review or gameplay testing.
