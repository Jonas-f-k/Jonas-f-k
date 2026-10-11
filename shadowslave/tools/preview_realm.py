"""Render the fixed Dream Realm map to a PNG (hill-shaded, biome coloured).

Usage: python3 tools/preview_realm.py OUT.png [seed [x0 z0 width height step]]

Compiles the pure-Java RealmMap with the local JDK; no Minecraft needed.
"""
import json, struct, subprocess, sys, tempfile
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
COLORS = {
    "dark_sea": (14, 40, 58), "ember_haven": (232, 164, 70), "sunpetal_meadows": (170, 200, 70),
    "verdant_gardens": (80, 170, 80), "blossom_vale": (238, 150, 180), "golden_grove": (222, 176, 40),
    "moonlit_grove": (60, 150, 160), "azure_woods": (60, 120, 210), "mosslight_ravine": (50, 130, 100),
    "aurora_fields": (160, 130, 220), "frostglass_taiga": (170, 205, 225), "amethyst_heights": (150, 70, 190),
    "hollow_mountains": (110, 112, 125), "death_zone": (90, 40, 40), "nightmare_desert": (230, 215, 160),
    "scarlet_wilds": (190, 40, 50), "forgotten_shore": (125, 120, 110), "chained_isles": (120, 200, 235),
}

def build(tmp):
    subprocess.run(["javac", "-d", tmp, str(ROOT / "src/main/java/com/shadowslave/world/RealmMap.java"), str(ROOT / "src/main/java/com/shadowslave/world/Archipelago.java"),
                    str(ROOT / "tools/java/RealmMapTool.java")], check=True, capture_output=True)

def run(tmp, *args):
    return subprocess.run(["java", "-cp", tmp, "RealmMapTool", *map(str, args)], check=True, capture_output=True, text=True).stdout

def main():
    out = sys.argv[1]
    seed = int(sys.argv[2]) if len(sys.argv) >= 3 else 0
    x0, z0, w, h, step = (int(v) for v in sys.argv[3:8]) if len(sys.argv) >= 8 else (-2900, -2900, 1000, 1000, 6)
    with tempfile.TemporaryDirectory() as tmp:
        build(tmp)
        info = json.loads(run(tmp, "regions"))
        raw = Path(tmp) / "grid.bin"
        run(tmp, "preview", seed, x0, z0, w, h, step, raw)
        layout = json.loads(run(tmp, "layout", seed))
        data = raw.read_bytes()
    gw, gh = struct.unpack(">ii", data[:8])
    rec = np.dtype([("h", ">f4"), ("r", "u1"), ("i", "u1")])
    grid = np.frombuffer(data[8:], dtype=rec).reshape(gh, gw)
    height = grid["h"].astype(np.float64); region = grid["r"]
    sea = info["sea_level"]
    names = [b["biome"] for b in info["biomes"]]
    palette = np.array([COLORS[n] for n in names], dtype=np.float64)
    color = palette[region]
    gy, gx = np.gradient(height)
    shade = np.clip(0.75 + (-gx - gy) * 0.22 * 6 / max(step, 1) * 2.0, 0.45, 1.35)
    rgb = color * shade[..., None]
    deep = np.clip((sea - height) / 50.0, 0, 1)
    water = height < sea
    sea_col = np.array([22, 78, 120]) * (1 - 0.75 * deep[..., None]) + np.array([4, 18, 34]) * (0.75 * deep[..., None])
    rgb[water] = rgb[water] * 0.25 + sea_col[water] * 0.75
    isle = grid["i"] == 1
    rgb[isle] = rgb[isle] * 0.6 + np.array([140, 210, 245]) * 0.4
    img = Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8)).convert("RGB")
    d = ImageDraw.Draw(img)
    f = ImageFont.load_default()
    for lm in info["landmarks"]:
        px, py = (lm["x"] - x0) / step, (lm["z"] - z0) / step
        d.ellipse([px - 5, py - 5, px + 5, py + 5], outline=(255, 255, 255), width=2)
        d.text((px + 8, py - 5), lm["name"], fill=(255, 255, 255), font=f)
    seen = set()
    for s in layout["sites"]:
        name = names[s["region"]]
        if name in ("dark_sea", "chained_isles"):
            continue
        px, py = (s["x"] - x0) / step, (s["z"] - z0) / step
        if 0 <= px < w and 0 <= py < h:
            d.text((px - 30, py), name, fill=(255, 255, 255), font=f)
    d.text((8, 8), f"seed {seed}", fill=(255, 255, 255), font=f)
    img.save(out)
    print("saved", out, img.size)

if __name__ == "__main__":
    main()
