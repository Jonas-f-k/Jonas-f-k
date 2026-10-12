#!/usr/bin/env python3
"""Strict offline validation of the Dream Realm's world-generation data.

Minecraft only reports a malformed world-generation file when a world is created, and a single
mistake there makes the whole dimension fail to load. This script re-implements the structural
rules of every JSON type the mod uses (required keys, allowed keys, value ranges, cross-references)
so mistakes are found before the game starts. It also runs the Java invariant checks of the map.

    python3 tools/validate_worldgen.py

Exit status is non-zero when anything is wrong.
"""
import json
import re
import subprocess
import sys
import tempfile
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src/main/resources/data/shadowslave"
ASSETS = ROOT / "src/main/resources/assets/shadowslave"
JAVA = ROOT / "src/main/java/com/shadowslave"
errors = []


def err(where, msg):
    errors.append(f"{where}: {msg}")


def load(path):
    try:
        return json.loads(path.read_text())
    except Exception as e:  # noqa: BLE001
        err(path.name, f"invalid JSON ({e})")
        return None


# --------------------------------------------------------------------------- registries
MOD_BLOCKS = {p.stem for p in (ASSETS / "blockstates").glob("*.json")}
src = (JAVA / "world/RealmBlocks.java").read_text()
declared = set(re.findall(r'(?:material|register)\("([a-z_]+)"', src)) | set(
    re.findall(r'"([a-z_]+_bloom)"', src)) | {"trial_altar", "trial_exit", "gateway"}
for missing in sorted(declared - MOD_BLOCKS - {"trial_altar", "trial_exit", "gateway"}):
    err("blocks", f"{missing} is registered in Java but has no blockstate")
MOD_BLOCKS |= declared
VANILLA_BLOCKS = {"minecraft:air", "minecraft:bedrock", "minecraft:water"}
VANILLA_NOISES = {"minecraft:cave_cheese", "minecraft:surface", "minecraft:gravel", "minecraft:calcite"}
VANILLA_PARTICLES = {"cherry_leaves", "end_rod", "crimson_spore", "ash", "white_ash"}
VANILLA_SOUNDS = {"ambient.cave", "ambient.warped_forest.additions", "ambient.crimson_forest.additions",
                  "ambient.soul_sand_valley.additions", "ambient.basalt_deltas.loop"}
MOD_NOISES = {f"shadowslave:{p.stem}" for p in (DATA / "worldgen/noise").glob("*.json")}
CHANNELS = {"height", "region", "rough", "cave", "isle"}


def check_block(where, name):
    if name.startswith("shadowslave:"):
        if name.split(":")[1] not in MOD_BLOCKS:
            err(where, f"unknown block {name}")
    elif name not in VANILLA_BLOCKS:
        err(where, f"unreviewed vanilla block {name}")


def check_state(where, st):
    if not isinstance(st, dict) or "Name" not in st or set(st) - {"Name", "Properties"}:
        err(where, f"bad block state {st}")
        return
    check_block(where, st["Name"])
    if "Properties" in st and not all(isinstance(v, str) for v in st["Properties"].values()):
        err(where, "block state properties must be strings")


# --------------------------------------------------------------------------- generic helpers
def keys(where, obj, required, optional=()):
    if not isinstance(obj, dict):
        err(where, f"expected object, got {type(obj).__name__}")
        return False
    missing = set(required) - set(obj)
    extra = set(obj) - set(required) - set(optional) - {"type"}
    if missing:
        err(where, f"missing keys {sorted(missing)}")
    if extra:
        err(where, f"unknown keys {sorted(extra)}")
    return not missing


def num(where, v, lo=None, hi=None):
    if isinstance(v, bool) or not isinstance(v, (int, float)):
        err(where, f"expected number, got {v!r}")
    elif (lo is not None and v < lo) or (hi is not None and v > hi):
        err(where, f"{v} outside [{lo}, {hi}]")


def int_provider(where, v, lo=0, hi=None):
    if isinstance(v, int) and not isinstance(v, bool):
        num(where, v, lo, hi)
    elif isinstance(v, dict) and v.get("type") == "minecraft:uniform":
        keys(where, v, ["min_inclusive", "max_inclusive"])
        if v["min_inclusive"] > v["max_inclusive"]:
            err(where, "uniform min > max")
        num(where, v["min_inclusive"], lo, hi)
        num(where, v["max_inclusive"], lo, hi)
    else:
        err(where, f"unsupported int provider {v}")


def state_provider(where, p):
    t = p.get("type") if isinstance(p, dict) else None
    if t == "minecraft:simple_state_provider":
        keys(where, p, ["state"])
        check_state(where, p["state"])
    elif t == "minecraft:weighted_state_provider":
        keys(where, p, ["entries"])
        for e in p["entries"]:
            keys(where, e, ["data", "weight"])
            check_state(where, e["data"])
            num(where, e["weight"], 1)
    else:
        err(where, f"unsupported state provider {t}")


def block_predicate(where, p):
    t = p.get("type")
    if t == "minecraft:matching_blocks":
        keys(where, p, ["blocks"], ["offset"])
        blocks = p["blocks"] if isinstance(p["blocks"], list) else [p["blocks"]]
        for b in blocks:
            check_block(where, b)
        if "offset" in p and (len(p["offset"]) != 3 or not all(isinstance(i, int) for i in p["offset"])):
            err(where, "offset must be three ints")
    elif t == "minecraft:all_of":
        keys(where, p, ["predicates"])
        for q in p["predicates"]:
            block_predicate(where, q)
    else:
        err(where, f"unsupported block predicate {t}")


def placement(where, mods):
    for m in mods:
        t = m.get("type")
        if t == "minecraft:count":
            keys(where, m, ["count"])
            int_provider(where, m["count"], 0, 256)
        elif t == "minecraft:rarity_filter":
            keys(where, m, ["chance"])
            num(where, m["chance"], 1)
        elif t == "minecraft:in_square" or t == "minecraft:biome":
            keys(where, m, [])
        elif t == "minecraft:heightmap":
            keys(where, m, ["heightmap"])
            if m["heightmap"] not in ("WORLD_SURFACE_WG", "WORLD_SURFACE", "OCEAN_FLOOR_WG", "MOTION_BLOCKING"):
                err(where, f"heightmap {m['heightmap']}")
        elif t == "minecraft:block_predicate_filter":
            keys(where, m, ["predicate"])
            block_predicate(where, m["predicate"])
        elif t == "minecraft:noise_threshold_count":
            keys(where, m, ["noise_level", "below_noise", "above_noise"])
        else:
            err(where, f"unsupported placement modifier {t}")


def placed_inline(where, v):
    """A placed feature given as a reference string or an inline object."""
    if isinstance(v, str):
        if v.startswith("shadowslave:") and not (DATA / "worldgen/placed_feature" / (v.split(":")[1] + ".json")).exists():
            err(where, f"unknown placed feature {v}")
        return
    keys(where, v, ["feature", "placement"])
    configured(where, v["feature"])
    placement(where, v["placement"])


def foliage_placer(where, f):
    t = f.get("type")
    radius_off = ["radius", "offset"]
    spec = {
        "minecraft:blob_foliage_placer": radius_off + ["height"],
        "minecraft:fancy_foliage_placer": radius_off + ["height"],
        "minecraft:spruce_foliage_placer": radius_off + ["trunk_height"],
        "minecraft:pine_foliage_placer": radius_off + ["height"],
        "minecraft:dark_oak_foliage_placer": radius_off,
    }
    if t not in spec:
        err(where, f"unsupported foliage placer {t}")
        return
    keys(where, f, spec[t])
    for k in spec[t]:
        int_provider(f"{where}.{k}", f[k], 0, 24)


def trunk_placer(where, t):
    ok = {"minecraft:straight_trunk_placer", "minecraft:fancy_trunk_placer", "minecraft:dark_oak_trunk_placer"}
    if t.get("type") not in ok:
        err(where, f"unsupported trunk placer {t.get('type')}")
        return
    keys(where, t, ["base_height", "height_rand_a", "height_rand_b"])
    num(where, t["base_height"], 0, 32)
    num(where, t["height_rand_a"], 0, 24)
    num(where, t["height_rand_b"], 0, 24)


def configured(where, f):
    if isinstance(f, str):
        if not (DATA / "worldgen/configured_feature" / (f.split(":")[1] + ".json")).exists():
            err(where, f"unknown configured feature {f}")
        return
    t = f.get("type")
    cfg = f.get("config")
    keys(where, f, ["config"])
    if t == "minecraft:tree":
        keys(where + ".tree", cfg, ["trunk_provider", "trunk_placer", "foliage_provider", "foliage_placer",
                                    "minimum_size", "dirt_provider", "decorators", "ignore_vines"])
        state_provider(where, cfg["trunk_provider"])
        state_provider(where, cfg["foliage_provider"])
        state_provider(where, cfg["dirt_provider"])
        trunk_placer(where, cfg["trunk_placer"])
        foliage_placer(where, cfg["foliage_placer"])
        ms = cfg["minimum_size"]
        if ms["type"] == "minecraft:two_layers_feature_size":
            keys(where, ms, ["limit", "lower_size", "upper_size"])
        elif ms["type"] == "minecraft:three_layers_feature_size":
            keys(where, ms, ["limit", "upper_limit", "lower_size", "middle_size", "upper_size"])
        else:
            err(where, "unsupported feature size")
        if cfg["decorators"] != []:
            err(where, "decorators unsupported")
        # leaf blocks must be real leaves so that the 'persistent' property exists
        name = cfg["foliage_provider"]["state"]
        if not name["Name"].endswith("_leaves") or name.get("Properties", {}).get("persistent") != "true":
            err(where, "foliage must be a *_leaves block with persistent=true (otherwise it decays)")
    elif t == "minecraft:random_selector":
        keys(where, cfg, ["features", "default"])
        for e in cfg["features"]:
            keys(where, e, ["feature", "chance"])
            num(where, e["chance"], 0, 1)
            placed_inline(where, e["feature"])
        placed_inline(where, cfg["default"])
    elif t == "minecraft:random_patch":
        keys(where, cfg, ["feature", "tries", "xz_spread", "y_spread"])
        num(where, cfg["tries"], 1, 256)
        num(where, cfg["xz_spread"], 0, 16)
        num(where, cfg["y_spread"], 0, 16)
        placed_inline(where, cfg["feature"])
    elif t == "minecraft:simple_block":
        keys(where, cfg, ["to_place"])
        state_provider(where, cfg["to_place"])
    elif t == "minecraft:block_column":
        keys(where, cfg, ["direction", "allowed_placement", "prioritize_tip", "layers"])
        if cfg["direction"] not in ("up", "down"):
            err(where, "direction")
        block_predicate(where, cfg["allowed_placement"])
        for layer in cfg["layers"]:
            keys(where, layer, ["height", "provider"])
            int_provider(where, layer["height"], 0, 128)
            state_provider(where, layer["provider"])
    elif t == "shadowslave:dream_ruin" or t in ("shadowslave:island_chains", "shadowslave:realm_waters", "shadowslave:realm_decoration"):
        pass
    else:
        err(where, f"unsupported feature type {t}")


# --------------------------------------------------------------------------- density functions
def density(where, d, depth=0):
    if isinstance(d, (int, float)) and not isinstance(d, bool):
        return
    if depth > 40:
        err(where, "density function nested too deeply")
        return
    t = d.get("type") if isinstance(d, dict) else None
    sub = lambda k, v=None: density(f"{where}>{t.split(':')[1]}.{k}", d[k], depth + 1)
    if t in ("minecraft:add", "minecraft:mul", "minecraft:min", "minecraft:max"):
        keys(where, d, ["argument1", "argument2"])
        sub("argument1")
        sub("argument2")
    elif t == "minecraft:clamp":
        keys(where, d, ["input", "min", "max"])
        sub("input")
        if d["min"] >= d["max"]:
            err(where, "clamp min >= max")
    elif t in ("minecraft:squeeze", "minecraft:interpolated", "minecraft:blend_density",
               "minecraft:cache_2d", "minecraft:flat_cache"):
        keys(where, d, ["argument"])
        sub("argument")
    elif t == "minecraft:y_clamped_gradient":
        keys(where, d, ["from_y", "to_y", "from_value", "to_value"])
        if d["from_y"] >= d["to_y"]:
            err(where, "gradient from_y >= to_y")
    elif t == "minecraft:noise":
        keys(where, d, ["noise", "xz_scale", "y_scale"])
        if d["noise"] not in VANILLA_NOISES | MOD_NOISES:
            err(where, f"unknown noise {d['noise']}")
    elif t == "minecraft:range_choice":
        keys(where, d, ["input", "min_inclusive", "max_exclusive", "when_in_range", "when_out_of_range"])
        for k in ("input", "when_in_range", "when_out_of_range"):
            sub(k)
    elif t == "shadowslave:realm_map":
        keys(where, d, ["channel"])
        if d["channel"] not in CHANNELS:
            err(where, f"unknown realm_map channel {d['channel']}")
    elif t == "shadowslave:islands":
        keys(where, d, [])
    else:
        err(where, f"unsupported density function {t}")


# --------------------------------------------------------------------------- surface rules
def surface_condition(where, c):
    t = c.get("type")
    if t == "minecraft:biome":
        keys(where, c, ["biome_is"])
        for b in c["biome_is"]:
            if not (DATA / "worldgen/biome" / (b.split(":")[1] + ".json")).exists():
                err(where, f"unknown biome {b}")
    elif t == "minecraft:steep":
        keys(where, c, [])
    elif t == "minecraft:stone_depth":
        keys(where, c, ["add_surface_depth", "offset", "secondary_depth_range", "surface_type"])
    elif t == "minecraft:noise_threshold":
        keys(where, c, ["noise", "min_threshold", "max_threshold"])
        if c["noise"] not in VANILLA_NOISES | MOD_NOISES:
            err(where, f"unknown noise {c['noise']}")
        if c["min_threshold"] >= c["max_threshold"]:
            err(where, "empty noise threshold")
    elif t == "minecraft:water":
        keys(where, c, ["offset", "surface_depth_multiplier", "add_stone_depth"])
    elif t == "minecraft:y_above":
        keys(where, c, ["anchor", "surface_depth_multiplier", "add_stone_depth"])
    elif t == "minecraft:not":
        keys(where, c, ["invert"])
        surface_condition(where, c["invert"])
    elif t == "minecraft:vertical_gradient":
        keys(where, c, ["random_name", "true_at_and_below", "false_at_and_above"])
    else:
        err(where, f"unsupported surface condition {t}")


def surface_rule(where, r):
    t = r.get("type")
    if t == "minecraft:sequence":
        keys(where, r, ["sequence"])
        for x in r["sequence"]:
            surface_rule(where, x)
    elif t == "minecraft:condition":
        keys(where, r, ["if_true", "then_run"])
        surface_condition(where, r["if_true"])
        surface_rule(where, r["then_run"])
    elif t == "minecraft:block":
        keys(where, r, ["result_state"])
        check_state(where, r["result_state"])
    else:
        err(where, f"unsupported surface rule {t}")


# --------------------------------------------------------------------------- files
def main():
    for p in DATA.rglob("*.json"):
        load(p)

    # --- noise settings
    ns = load(DATA / "worldgen/noise_settings/dream.json")
    router_keys = {"barrier", "fluid_level_floodedness", "fluid_level_spread", "lava", "temperature", "vegetation",
                   "continents", "erosion", "depth", "ridges", "initial_density_without_jaggedness", "final_density",
                   "vein_toggle", "vein_ridged", "vein_gap"}
    keys("noise_settings", ns, ["sea_level", "disable_mob_generation", "aquifers_enabled", "ore_veins_enabled",
                                "legacy_random_source", "default_block", "default_fluid", "noise", "noise_router",
                                "spawn_target", "surface_rule"])
    if set(ns["noise_router"]) != router_keys:
        err("noise_router", f"keys differ: {sorted(set(ns['noise_router']) ^ router_keys)}")
    for k, v in ns["noise_router"].items():
        density(f"router.{k}", v)
    check_state("default_block", ns["default_block"])
    surface_rule("surface_rule", ns["surface_rule"])
    n = ns["noise"]
    if n["min_y"] % 16 or n["height"] % 16 or n["size_horizontal"] not in (1, 2, 3, 4) or n["size_vertical"] not in (1, 2, 3, 4):
        err("noise", f"invalid noise cell setup {n}")

    # --- region table and dimension
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-d", tmp, str(JAVA / "world/RealmMap.java"), str(JAVA / "world/Archipelago.java"), str(ROOT / "tools/java/RealmMapTool.java"),
                        str(ROOT / "tools/java/RealmMapCheck.java")], check=True, capture_output=True)
        table = json.loads(subprocess.run(["java", "-cp", tmp, "RealmMapTool", "regions"], check=True,
                                          capture_output=True, text=True).stdout)
        check = subprocess.run(["java", "-cp", tmp, "RealmMapCheck", "60"], capture_output=True, text=True)
        if check.returncode != 0:
            err("RealmMapCheck", "\n" + "\n".join(l for l in check.stdout.splitlines() if "JAVA_TOOL" not in l)[-1500:])
        else:
            print("RealmMapCheck:", [l for l in check.stdout.splitlines() if l.startswith("seeds=")][0])
    dim = load(DATA / "dimension/dream_realm.json")
    source = dim["generator"]["biome_source"]
    entries = source["biomes"]
    expected = [f"shadowslave:{b['biome']}" for b in table["biomes"]]
    if source["type"] != "shadowslave:dream_map" or entries != expected:
        err("dimension", "cartographic biome source must exactly follow RealmMap.BIOMES")
    for b in table["biomes"]:
        if not (DATA / "worldgen/biome" / f"{b['biome']}.json").exists():
            err("dimension", f"biome file for {b['biome']} missing")
        if f"biome.shadowslave.{b['biome']}" not in json.loads((ASSETS / "lang/en_us.json").read_text()):
            err("lang", f"no name for biome {b['biome']}")
    if ns['aquifers_enabled'] or ns['ore_veins_enabled'] or ns['sea_level']>=ns['noise']['min_y']:
        err("geology", "aquifers, ore veins or implicit fluid plane active")
    if 'minecraft:cave_' in json.dumps(ns) or 'minecraft:overworld/' in json.dumps(ns):
        err("geology", "vanilla cave/Overworld density reference present")

    # --- biomes + features
    placed_dir = DATA / "worldgen/placed_feature"
    graph = defaultdict(set)   # (step, feature) edges for the cycle check
    nodes = defaultdict(set)
    for p in sorted((DATA / "worldgen/biome").glob("*.json")):
        b = load(p)
        if p.stem == "nightmare_void":
            continue
        w = f"biome.{p.stem}"
        keys(w, b, ["has_precipitation", "temperature", "downfall", "effects", "carvers", "features", "spawners",
                    "spawn_costs"])
        if len(b["features"]) != 11:
            err(w, "needs exactly 11 feature steps")
        fx = b["effects"]
        keys(w + ".effects", fx, ["sky_color", "fog_color", "water_color", "water_fog_color"],
             ["grass_color", "foliage_color", "particle", "ambient_sound", "mood_sound", "additions_sound"])
        if "particle" in fx:
            if fx["particle"]["options"]["type"].split(":")[1] not in VANILLA_PARTICLES:
                err(w, f"unreviewed particle {fx['particle']}")
            num(w, fx["particle"]["probability"], 0, 1)
        for k in ("ambient_sound",):
            if k in fx and fx[k].split(":")[1] not in VANILLA_SOUNDS:
                err(w, f"unreviewed sound {fx[k]}")
        for k in ("mood_sound", "additions_sound"):
            if k in fx and fx[k]["sound"].split(":")[1] not in VANILLA_SOUNDS:
                err(w, f"unreviewed sound {fx[k]}")
        for step, lst in enumerate(b["features"]):
            for i, f in enumerate(lst):
                if not (placed_dir / (f.split(":")[1] + ".json")).exists():
                    err(w, f"unknown feature {f}")
                nodes[step].add(f)
                if i:
                    graph[(step, lst[i - 1])].add(f)
    # Minecraft refuses to start a dimension whose biomes order the same features in contradictory ways
    for step in nodes:
        indeg = {f: 0 for f in nodes[step]}
        for (s, a), outs in graph.items():
            if s == step:
                for o in outs:
                    indeg[o] += 1
        ready = [f for f, d in indeg.items() if d == 0]
        seen = 0
        while ready:
            f = ready.pop()
            seen += 1
            for o in graph.get((step, f), ()):
                indeg[o] -= 1
                if indeg[o] == 0:
                    ready.append(o)
        if seen != len(indeg):
            err("features", f"feature order cycle in decoration step {step}")

    for p in sorted(placed_dir.glob("*.json")):
        pf = load(p)
        keys(f"placed.{p.stem}", pf, ["feature", "placement"])
        placement(f"placed.{p.stem}", pf["placement"])
        c = pf["feature"]
        if not (DATA / "worldgen/configured_feature" / (c.split(":")[1] + ".json")).exists():
            err(f"placed.{p.stem}", f"unknown configured feature {c}")
    for p in sorted((DATA / "worldgen/configured_feature").glob("*.json")):
        configured(f"configured.{p.stem}", load(p))

    # --- constants shared with other Java classes
    cit = (JAVA / "world/GrandCitadels.java").read_text()
    xs = list(map(int, re.search(r"new int\[\]\s*\{\s*([-\d,\s]+)\s*\}\[index\]", cit).group(1).split(",")))
    zs = list(map(int, re.findall(r"new int\[\]\s*\{\s*([-\d,\s]+)\s*\}\[index\]", cit)[1].split(",")))
    cits = [l for l in table["landmarks"] if l["citadel"]]
    if sorted(zip(xs, zs)) != sorted((l["x"], l["z"]) for l in cits):
        err("landmarks", f"GrandCitadels centres {list(zip(xs, zs))} differ from RealmMap landmarks")
    if "SPACING=Archipelago.SPACING" not in (JAVA/"world/IslandDensity.java").read_text() or "ISLE_SPACING = Archipelago.SPACING" not in (JAVA/"world/RealmMap.java").read_text():
        err("islands", "island density and map must share Archipelago.SPACING")

    if errors:
        print(f"\n{len(errors)} PROBLEM(S):")
        for e in errors:
            print(" -", e)
        sys.exit(1)
    print(f"world-generation data OK: {len(list(placed_dir.glob('*.json')))} features, "
          f"{len(entries)} biomes, no feature-order cycles")


if __name__ == "__main__":
    main()
