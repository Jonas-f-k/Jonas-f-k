# Animation Sliders for Blockbench

This directory contains the **unchanged Animation Sliders 0.5.0** editor plugin by JannisX11, its original usage notes, full GNU GPLv3 license, and a source manifest. It is an editor tool; it is not a Minecraft dependency and is not packaged in the mod JAR. It has been downloaded into this source project, not installed on your computer.

## Install

1. Install/open [Blockbench](https://www.blockbench.net/) **4.8.0 or newer**. Blockbench 5 is recommended for native Java animation export.
2. Open **File → Plugins** and use **Load Plugin from File** (the local-file button).
3. Select `animation_sliders.js` from this directory.
4. Open an editable `.bbmodel`, switch to **Animate**, and select keyframes.
5. Use the three dots on an animation toolbar → **Customize** to add **Ease Keyframes**, **Amplify Keyframes**, **Retime Keyframes**, or **Normalize Keyframes**. The original [about.md](about.md) explains each control.

Alternatively, find **Animation Sliders** in File → Plugins → Available and install it from the official catalogue. These are editor controls; they do not automatically redesign geometry or add animation to Minecraft.

Use eased anticipation, fast release and slower recovery for attacks. Keep paws/feet planted during the contact half of a walk cycle, then lift them during recovery. Compare side and front views; check that the jaw, wing or claw makes a readable silhouette before adding secondary movement.

See the root art documentation for the rework's editable model exports. A saved editor model alone does not replace compiled Java geometry. Implement edited shapes/animation in the runtime rig and rebuild the mod.

## Source and license

- Upstream: [JannisX11/blockbench-plugins](https://github.com/JannisX11/blockbench-plugins/tree/91ac60fa067008937393e88c902f8871406f55ed/plugins/animation_sliders)
- Revision: `91ac60fa067008937393e88c902f8871406f55ed`
- Original files: `animation_sliders.js`, `about.md`, `LICENSE.MD`
- License: [GNU GPL version 3](LICENSE.MD). Preserve it and the source if redistributing this editor plugin.
- Exact source URLs, byte counts and SHA-256 hashes are recorded in [SOURCE.json](SOURCE.json).

This separate editor plugin is aggregated with the mod source. Its GPL license applies to the plugin, not to independently written mod code. No plugin code is compiled into the mod or used as a runtime library.

[Reference Models](https://github.com/JannisX11/blockbench-plugins/tree/master/plugins/reference_models) is an optional desktop Blockbench plugin for comparing `.glb`/`.gltf` anatomy or another project tab beside your cuboids. Install it through the official catalogue if useful. Its files are not redistributed here.
