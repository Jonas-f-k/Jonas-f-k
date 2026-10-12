# Editable models and animation tools

The editor ZIP contains 39 self-contained `.bbmodel` files: 32 creatures and seven transformations. Open them in **Blockbench 5 or newer**. They are exported from the actual Minecraft cuboids and bone hierarchy, with embedded 64x128 skins and sampled idle, walk, windup, strike and flight tracks. Unsupported flight for a ground creature simply preserves its normal anatomy.

These are editable art copies. Changes in Blockbench do not automatically replace the mod's Java rigs. Native runtime animation remains in `CreatureMotion.java` and `BeastRig.java`. The sampled editor tracks are a two-second review segment; use the editor to refine timing and loop seams before importing changes into the runtime.

The included unchanged **Animation Sliders 0.5.0** plugin provides tween, ease, amplify, retime and normalize tools. In Blockbench, open **File → Plugins → Load Plugin from File** and select `animation_sliders/animation_sliders.js`. Its full GPLv3 license, usage notes and pinned source hashes are included. It is an editor extension, excluded from the playable mod JAR. It has not been installed on your computer automatically.

The optional **Reference Models** extension can be installed from Blockbench's Available plugin catalog to view a reference project beside the editable rig. No unlicensed reference models are bundled.

Rebuild the files after compiling the mod:

```sh
python tools/export_blockbench_models.py
```

Output: `build/reports/blockbench-models/`. The exporter checks JSON, identifiers, dimensions, UV bounds and embedded images. An interactive editor round-trip is a separate check; the native Minecraft captures validate the runtime models.
