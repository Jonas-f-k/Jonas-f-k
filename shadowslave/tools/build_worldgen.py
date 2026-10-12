#!/usr/bin/env python3
"""Generate the Dream Realm's world-generation data from the Java map definition.

    python3 tools/build_worldgen.py

Reads the region table from ``RealmMap.java`` (compiled with the local JDK, no Minecraft needed) and
writes, under ``src/main/resources/data/shadowslave``:

    dimension/dream_realm.json               biome source: one temperature code per region
    worldgen/noise/overhang.json             3D noise for cliffs and arches
    worldgen/noise_settings/dream.json       terrain router, surface rules
    worldgen/biome/<biome>.json              18 biomes (colours, sounds, particles, features)
    worldgen/configured_feature/*.json       trees, flower patches, rubble, spires, bones
    worldgen/placed_feature/*.json

Everything is generated, so tuning a biome means editing the tables below and re-running.
Run ``tools/validate_worldgen.py`` afterwards.
"""
import json
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src/main/resources/data/shadowslave"
NS = "shadowslave"


# --------------------------------------------------------------------------- region table from Java
def region_table():
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(
            ["javac", "-d", tmp, str(ROOT / "src/main/java/com/shadowslave/world/RealmMap.java"), str(ROOT / "src/main/java/com/shadowslave/world/Archipelago.java"),
             str(ROOT / "tools/java/RealmMapTool.java")],
            check=True, capture_output=True)
        out = subprocess.run(["java", "-cp", tmp, "RealmMapTool", "regions"],
                             check=True, capture_output=True, text=True).stdout
    return json.loads(out)


# --------------------------------------------------------------------------- small JSON builders
def blk(name, **props):
    ns = name if ":" in name else f"{NS}:{name}"
    state = {"Name": ns}
    if props:
        state["Properties"] = {k: str(v).lower() if isinstance(v, bool) else str(v) for k, v in props.items()}
    return state


def leaves(name):
    return blk(name, persistent=True)


def simple(state):
    return {"type": "minecraft:simple_state_provider", "state": state}


def weighted(entries):
    return {"type": "minecraft:weighted_state_provider",
            "entries": [{"weight": w, "data": s} for s, w in entries]}


def uniform(lo, hi):
    return {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}


def mc(name):
    return f"minecraft:{name}"


# density functions
def df(kind, **kw):
    return {"type": mc(kind), **kw}


def add(a, b):
    return df("add", argument1=a, argument2=b)


def mul(a, b):
    return df("mul", argument1=a, argument2=b)


def dmin(a, b):
    return df("min", argument1=a, argument2=b)


def dmax(a, b):
    return df("max", argument1=a, argument2=b)


def clamp(x, lo, hi):
    return df("clamp", input=x, min=lo, max=hi)


def cache2d(x):
    return df("cache_2d", argument=x)


def realm(channel):
    return {"type": f"{NS}:realm_map", "channel": channel}


def noise3d(name, y_scale=1.0):
    return df("noise", noise=name, xz_scale=1.0, y_scale=y_scale)


# --------------------------------------------------------------------------- biome design tables
#  top/sub/deep/steep: surface layers; shore: dry ground just above the sea; lake: under water.
#  patches: (noise, lo, hi, block) scattered over the top layer for variety.
#  trees: list of (style, weight); density: (sparse, dense) trees per chunk, switched by a noise.
GRASS = 8501580
FOLIAGE = 7252044

BIOME = {
    "dark_sea": dict(
        top="ashen_soil", sub="ashen_soil", deep="ash_stone", steep="ash_stone", shore="ashen_soil",
        lake="drowned_stone", patches=[("minecraft:gravel", 0.1, 0.55, "ashen_soil")],
        sky=6580082, fog=3825770, water=0x0A3340, water_fog=0x05202A, temp=0.5, trees=None, flowers=[]),
    "ember_haven": dict(
        top="meadow_grass", sub="dream_loam", deep="sanctuary_limestone", steep="sanctuary_limestone",
        shore="warm_sand", lake="dream_loam", patches=[("minecraft:surface", 0.3, 0.7, "blossom_grass")],
        sky=11261155, fog=15325114, water=0x6FC1D8, water_fog=0x3E8CA0,
        leaves="golden_leaves", trunk="dream_oak", trees=[("broadleaf", 3), ("fancy", 1)], density=(0, 2),
        flowers=["sun_bloom", "marigold_bloom", "cloud_bloom"], flower_count=3, rubble=None),
    "sunpetal_meadows": dict(
        top="meadow_grass", sub="dream_loam", deep="sanctuary_limestone", steep="sanctuary_limestone",
        shore="warm_sand", lake="dream_loam", patches=[("minecraft:surface", 0.35, 0.8, "blossom_grass")],
        sky=9227757, fog=15722176, water=0x63B8D6, water_fog=0x387F99,
        leaves="golden_leaves", trunk="dream_oak", trees=[("broadleaf", 3), ("fancy", 1)], density=(0, 1),
        flowers=["sun_bloom", "marigold_bloom", "hibiscus_bloom", "cloud_bloom"], flower_count=7, rubble=None),
    "verdant_gardens": dict(
        top="meadow_grass", sub="dream_loam", deep="sanctuary_limestone", steep="sanctuary_limestone",
        shore="warm_sand", lake="dream_loam", patches=[("minecraft:surface", 0.3, 0.7, "sanctuary_moss")],
        sky=9359342, fog=13953988, water=0x4FB7A7, water_fog=0x2C8176, particle=("cherry_leaves", 0.001),
        leaves="emerald_leaves", trunk="runic_timber", trees=[("broadleaf", 3), ("fancy", 2), ("grand", 1)],
        density=(2, 7), flowers=["foxglove_bloom", "jade_bloom", "lotus_bloom"], flower_count=5, rubble=None),
    "blossom_vale": dict(
        top="blossom_grass", sub="dream_loam", deep="sanctuary_limestone", steep="sanctuary_limestone",
        shore="warm_sand", lake="rose_clay", patches=[("minecraft:surface", 0.3, 0.7, "meadow_grass")],
        sky=10146289, fog=15259878, water=0xE59DBB, water_fog=0xA86A87, particle=("cherry_leaves", 0.0015),
        leaves="pink_leaves", trunk="rose_bark", trees=[("broadleaf", 3), ("fancy", 2), ("grand", 1)],
        density=(2, 8), flowers=["peony_bloom", "rose_bloom", "hibiscus_bloom"], flower_count=6, rubble=None),
    "golden_grove": dict(
        top="autumn_litter", sub="dream_loam", deep="sanctuary_limestone", steep="sanctuary_limestone",
        shore="warm_sand", lake="dream_loam", patches=[("minecraft:surface", 0.35, 0.8, "meadow_grass")],
        sky=11786726, fog=15849895, water=0xC6A650, water_fog=0x8A7432,
        leaves="golden_leaves", trunk="dream_birch", trees=[("broadleaf", 3), ("fancy", 2), ("grand", 1)],
        density=(2, 8), flowers=["marigold_bloom", "ember_bloom"], flower_count=3, rubble=None),
    "moonlit_grove": dict(
        top="turquoise_moss", sub="dream_loam", deep="sanctuary_limestone", steep="sanctuary_limestone",
        shore="warm_sand", lake="dream_loam", patches=[("minecraft:surface", 0.3, 0.7, "sanctuary_moss")],
        sky=7442090, fog=7969951, water=0x2B6F80, water_fog=0x153F4C, particle=("white_ash", 0.00035),
        additions=("ambient.warped_forest.additions", 0.003),
        leaves="teal_leaves", trunk="blue_bark", trees=[("broadleaf", 2), ("fancy", 2), ("grand", 2)],
        density=(3, 9), flowers=["lotus_bloom", "moon_bloom", "azure_bloom"], flower_count=4, rubble=None),
    "azure_woods": dict(
        top="turquoise_moss", sub="dream_loam", deep="blue_shale", steep="blue_shale",
        shore="warm_sand", lake="dream_loam", patches=[("minecraft:surface", 0.3, 0.7, "frost_grass")],
        sky=7914728, fog=11721452, water=0x2E6FB0, water_fog=0x1B4372, particle=("white_ash", 0.00035),
        leaves="cyan_leaves", trunk="blue_bark", trees=[("broadleaf", 3), ("fancy", 2), ("grand", 1)],
        density=(3, 9), flowers=["azure_bloom", "lotus_bloom", "cloud_bloom"], flower_count=4, rubble=None),
    "mosslight_ravine": dict(
        top="sanctuary_moss", sub="dream_loam", deep="sanctuary_limestone", steep="sanctuary_limestone",
        shore="dream_loam", lake="dream_loam", patches=[("minecraft:surface", 0.3, 0.7, "turquoise_moss")],
        sky=9551308, fog=11849141, water=0x3F9A7C, water_fog=0x226147,
        leaves="emerald_leaves", trunk="dream_oak", trees=[("broadleaf", 3), ("fancy", 3)], density=(3, 10),
        flowers=["jade_bloom", "foxglove_bloom"], flower_count=4, rubble=("mossy_ruin_stone", "sanctuary_limestone")),
    "aurora_fields": dict(
        top="lavender_grass", sub="dream_loam", deep="blue_shale", steep="blue_shale",
        shore="snow_soil", lake="dream_loam", patches=[("minecraft:surface", 0.35, 0.8, "frost_grass")],
        sky=9881046, fog=12639449, water=0x8C7BD1, water_fog=0x5A4C96, particle=("cherry_leaves", 0.001),
        leaves="lavender_leaves", trunk="dream_birch", trees=[("broadleaf", 3), ("fancy", 1)], density=(0, 2),
        flowers=["violet_bloom", "crocus_bloom", "frost_bloom"], flower_count=6, rubble=None),
    "frostglass_taiga": dict(
        top="frost_grass", sub="snow_soil", deep="blue_shale", steep="blue_shale",
        shore="snow_soil", lake="snow_soil", patches=[("minecraft:surface", 0.3, 0.7, "snow_soil")],
        sky=11327466, fog=13821675, water=0x8DB6D8, water_fog=0x4F7BA0, precipitation=True, temp=-0.5,
        leaves="frost_leaves", trunk="blue_bark", trees=[("conifer", 3), ("pine", 2)], density=(3, 11),
        flowers=["frost_bloom"], flower_count=2, rubble=("blue_shale", "ice_plate")),
    "amethyst_heights": dict(
        top="lavender_grass", sub="dream_loam", deep="crystal_rock", steep="crystal_rock",
        shore="dream_loam", lake="dream_loam", patches=[("minecraft:gravel", 0.1, 0.45, "crystal_rock")],
        sky=11777001, fog=13945828, water=0x8A6AB0, water_fog=0x553B7C, particle=("white_ash", 0.0004),
        leaves="lavender_leaves", trunk="rose_bark", trees=[("broadleaf", 3), ("fancy", 1)], density=(0, 2),
        flowers=["crocus_bloom", "violet_bloom"], flower_count=3,
        rubble=("crystal_rock", "amethyst_cluster"), spires=[("amethyst_crystal", 3), ("rose_crystal", 1)]),
    "hollow_mountains": dict(
        top="ash_stone", sub="dream_loam", deep="ash_stone", steep="ash_stone",
        shore="ashen_soil", lake="ashen_soil", patches=[("minecraft:gravel", 0.1, 0.5, "fractured_stone")],
        sky=8294317, fog=9871016, water=0x41607C, water_fog=0x27384A,
        mood=True, loop=None, dead="petrified_bark", dead_count=(0, 1),
        trees=None, flowers=["moon_bloom"], flower_count=1, rubble=("ash_stone", "fractured_stone")),
    "death_zone": dict(
        top="ashen_soil", sub="dream_loam", deep="ash_stone", steep="ash_stone",
        shore="ashen_soil", lake="ashen_soil", patches=[("minecraft:gravel", 0.1, 0.5, "fractured_stone"),
                                                         ("minecraft:calcite", -0.1, 0.1, "fossil_bone")],
        sky=6580082, fog=9277591, water=0x3B4A36, water_fog=0x232B20,
        loop="ambient.basalt_deltas.loop", mood=True, particle=("ash", 0.0015),
        dead="petrified_bark", dead_count=(2, 6), trees=None, flowers=["moon_bloom"], flower_count=1,
        rubble=("fractured_stone", "ash_stone"), bones=True),
    "nightmare_desert": dict(
        top="warm_sand", sub="warm_sand", deep="golden_sandstone", steep="golden_sandstone",
        shore="warm_sand", lake="warm_sand", patches=[("minecraft:surface", 0.2, 0.55, "golden_sandstone")],
        sky=12561809, fog=14269847, water=0x52A7A8, water_fog=0x2F6A6B,
        dead="redwood_bark", dead_count=(0, 1), trees=None, flowers=["crimson_bloom"], flower_count=1,
        rubble=("red_sandstone", "golden_sandstone"), spires=[("red_sandstone", 3), ("golden_sandstone", 2)], bones=True),
    "scarlet_wilds": dict(
        top="scarlet_litter", sub="dream_loam", deep="red_sandstone", steep="red_sandstone",
        shore="warm_sand", lake="rose_clay", patches=[("minecraft:surface", 0.3, 0.7, "autumn_litter")],
        sky=11895449, fog=13148063, water=0xA5424F, water_fog=0x6C2430, particle=("crimson_spore", 0.0008),
        additions=("ambient.crimson_forest.additions", 0.003),
        leaves="crimson_leaves", trunk="redwood_bark", trees=[("broadleaf", 3), ("fancy", 2), ("grand", 1)],
        density=(3, 9), flowers=["crimson_bloom", "hibiscus_bloom", "ember_bloom"], flower_count=4, rubble=None),
    "forgotten_shore": dict(
        top="ashen_grass", sub="dream_loam", deep="ash_stone", steep="ash_stone",
        shore="ashen_soil", lake="ashen_soil", patches=[("minecraft:gravel", 0.1, 0.5, "fractured_stone"),
                                                         ("minecraft:surface", 0.35, 0.7, "ashen_soil")],
        sky=10200502, fog=11974844, water=0x2E5A5C, water_fog=0x183436,
        additions=("ambient.soul_sand_valley.additions", 0.002), mood=True,
        dead="petrified_bark", dead_count=(0, 2), trees=None, flowers=["moon_bloom"], flower_count=1,
        rubble=("fractured_stone", "ash_bricks"), bones=True),
    "chained_isles": dict(
        top="meadow_grass", sub="dream_loam", deep="blue_shale", steep="blue_shale",
        shore="warm_sand", lake="dream_loam", patches=[("minecraft:surface", 0.3, 0.7, "frost_grass")],
        sky=10932204, fog=13558505, water=0x4F8FB5, water_fog=0x2A5A78,
        leaves="cyan_leaves", trunk="dream_oak", trees=[("broadleaf", 3), ("fancy", 1)], density=(0, 2),
        flowers=["cloud_bloom"], flower_count=3, rubble=("blue_shale", "ice_plate")),
}


# --------------------------------------------------------------------------- features
CONFIGURED = {}
PLACED = {}
# every placed feature has a fixed decoration step and rank; sorting by (rank) keeps one global
# order across biomes, which is what prevents Minecraft's "feature order cycle" crash.
STEP = {"rubble": 2, "spires": 2, "bones": 2, "dead": 9, "trees": 9, "flowers": 9, "ruin": 5, "chains": 5}
ORDER = ["chains", "ruin", "rubble", "spires", "bones", "dead", "trees", "flowers"]


def tree_config(style, trunk, leaf, ground):
    base = dict(
        trunk_provider=simple(blk(trunk)),
        foliage_provider=simple(leaves(leaf)),
        dirt_provider=simple(blk(ground)),
        decorators=[],
        ignore_vines=True,
    )
    two = lambda limit, up: {"type": mc("two_layers_feature_size"), "limit": limit, "lower_size": 0, "upper_size": up}
    if style == "broadleaf":
        cfg = dict(
            trunk_placer={"type": mc("straight_trunk_placer"), "base_height": 5, "height_rand_a": 2, "height_rand_b": 1},
            foliage_placer={"type": mc("blob_foliage_placer"), "radius": 2, "offset": 0, "height": 3},
            minimum_size=two(1, 1))
    elif style == "fancy":
        cfg = dict(
            trunk_placer={"type": mc("fancy_trunk_placer"), "base_height": 3, "height_rand_a": 11, "height_rand_b": 0},
            foliage_placer={"type": mc("fancy_foliage_placer"), "radius": 2, "offset": 4, "height": 4},
            minimum_size=two(0, 0))
    elif style == "grand":
        cfg = dict(
            trunk_placer={"type": mc("dark_oak_trunk_placer"), "base_height": 6, "height_rand_a": 2, "height_rand_b": 1},
            foliage_placer={"type": mc("dark_oak_foliage_placer"), "radius": 0, "offset": 0},
            minimum_size={"type": mc("three_layers_feature_size"), "limit": 1, "upper_limit": 1,
                          "lower_size": 0, "middle_size": 1, "upper_size": 2})
    elif style == "conifer":
        cfg = dict(
            trunk_placer={"type": mc("straight_trunk_placer"), "base_height": 5, "height_rand_a": 2, "height_rand_b": 1},
            foliage_placer={"type": mc("spruce_foliage_placer"), "radius": uniform(2, 3),
                            "offset": uniform(0, 2), "trunk_height": uniform(1, 2)},
            minimum_size=two(2, 2))
    elif style == "pine":
        cfg = dict(
            trunk_placer={"type": mc("straight_trunk_placer"), "base_height": 6, "height_rand_a": 4, "height_rand_b": 0},
            foliage_placer={"type": mc("pine_foliage_placer"), "radius": 1, "offset": 1, "height": uniform(3, 4)},
            minimum_size=two(2, 2))
    else:
        raise ValueError(style)
    cfg.update(base)
    return {"type": mc("tree"), "config": cfg}


def ground_filter(ground, offset=(0, -1, 0)):
    return {"type": mc("block_predicate_filter"),
            "predicate": {"type": mc("matching_blocks"), "offset": list(offset), "blocks": [f"{NS}:{g}" for g in ground]}}


def air_and_ground(ground):
    return {"type": mc("all_of"), "predicates": [
        {"type": mc("matching_blocks"), "blocks": mc("air")},
        {"type": mc("matching_blocks"), "offset": [0, -1, 0], "blocks": [f"{NS}:{g}" for g in ground]}]}


def surface_placement(first, ground):
    return first + [{"type": mc("in_square")},
                    {"type": mc("heightmap"), "heightmap": "WORLD_SURFACE_WG"},
                    {"type": mc("biome")},
                    ground_filter(ground)]


def register(kind, biome, name, configured, placement):
    cname = f"{kind}_{biome}"
    CONFIGURED[cname] = {"type": configured[0], "config": configured[1]} if isinstance(configured, tuple) else configured
    PLACED[cname] = {"feature": f"{NS}:{cname}", "placement": placement}
    return cname


def build_features(biome, spec):
    out = []
    ground = [spec["top"]] + [p[3] for p in spec["patches"] if p[3] in
                              ("meadow_grass", "blossom_grass", "sanctuary_moss", "turquoise_moss", "frost_grass",
                               "snow_soil", "ashen_soil", "autumn_litter")]
    ground = list(dict.fromkeys(ground))

    # --- living trees: random selection between styles, clustered into groves by a noise switch
    if spec.get("trees"):
        selector = {"type": f"{NS}:realm_decoration", "config": {}}
        sparse, dense = spec["density"]
        counter = {"type": mc("noise_threshold_count"), "noise_level": -0.25, "below_noise": sparse, "above_noise": dense}
        out.append(register("trees", biome, "trees", selector, surface_placement([counter], ground)))

    # --- dead trees: bare pillars of bark
    if spec.get("dead"):
        lo, hi = spec["dead_count"]
        col = {"type": mc("block_column"), "config": {
            "direction": "up", "allowed_placement": {"type": mc("matching_blocks"), "blocks": mc("air")},
            "prioritize_tip": False,
            "layers": [{"height": uniform(3, 8), "provider": simple(blk(spec["dead"]))}]}}
        count = {"type": mc("count"), "count": uniform(lo, hi)} if hi > lo else {"type": mc("count"), "count": hi}
        out.append(register("dead", biome, "dead", col, surface_placement([count], [spec["top"]])))

    # --- flowers
    if spec.get("flowers"):
        flowers = spec["flowers"]
        patch = {"type": mc("random_patch"), "config": {
            "tries": 28, "xz_spread": 7, "y_spread": 2,
            "feature": {"feature": {"type": mc("simple_block"), "config": {
                "to_place": weighted([(blk(f), 3 if i == 0 else 2) for i, f in enumerate(flowers)])}},
                "placement": [{"type": mc("block_predicate_filter"), "predicate": air_and_ground(ground)}]}}}
        placement = [{"type": mc("count"), "count": max(1, spec.get("flower_count", 3) // 2)},
                     {"type": mc("rarity_filter"), "chance": 1}, {"type": mc("in_square")},
                     {"type": mc("heightmap"), "heightmap": "WORLD_SURFACE_WG"}, {"type": mc("biome")}]
        out.append(register("flowers", biome, "flowers", patch, placement))

    # --- rubble heaps: clumps of short pillars (raised boulders without a custom Java feature)
    if spec.get("rubble"):
        a, b = spec["rubble"]
        patch = {"type": mc("random_patch"), "config": {
            "tries": 16, "xz_spread": 3, "y_spread": 1,
            "feature": {"feature": {"type": mc("block_column"), "config": {
                "direction": "up", "allowed_placement": {"type": mc("matching_blocks"), "blocks": mc("air")},
                "prioritize_tip": False,
                "layers": [{"height": uniform(1, 3), "provider": weighted([(blk(a), 3), (blk(b), 1)])}]}},
                "placement": [{"type": mc("block_predicate_filter"), "predicate": air_and_ground(ground)}]}}}
        placement = [{"type": mc("rarity_filter"), "chance": 4}, {"type": mc("in_square")},
                     {"type": mc("heightmap"), "heightmap": "WORLD_SURFACE_WG"}, {"type": mc("biome")}]
        out.append(register("rubble", biome, "rubble", patch, placement))

    # --- crystal / sandstone spires
    if spec.get("spires"):
        col = {"type": mc("block_column"), "config": {
            "direction": "up", "allowed_placement": {"type": mc("matching_blocks"), "blocks": mc("air")},
            "prioritize_tip": True,
            "layers": [{"height": uniform(4, 12), "provider": weighted([(blk(n), w) for n, w in spec["spires"]])}]}}
        placement = [{"type": mc("rarity_filter"), "chance": 5}, {"type": mc("in_square")},
                     {"type": mc("heightmap"), "heightmap": "WORLD_SURFACE_WG"}, {"type": mc("biome")},
                     ground_filter(ground)]
        out.append(register("spires", biome, "spires", col, placement))

    # --- bone fields: ribs of long-dead Nightmare Creatures
    if spec.get("bones"):
        patch = {"type": mc("random_patch"), "config": {
            "tries": 8, "xz_spread": 5, "y_spread": 1,
            "feature": {"feature": {"type": mc("block_column"), "config": {
                "direction": "up", "allowed_placement": {"type": mc("matching_blocks"), "blocks": mc("air")},
                "prioritize_tip": False,
                "layers": [{"height": uniform(1, 4), "provider": simple(blk("fossil_bone"))}]}},
                "placement": [{"type": mc("block_predicate_filter"), "predicate": air_and_ground(ground)}]}}}
        placement = [{"type": mc("rarity_filter"), "chance": 6}, {"type": mc("in_square")},
                     {"type": mc("heightmap"), "heightmap": "WORLD_SURFACE_WG"}, {"type": mc("biome")}]
        out.append(register("bones", biome, "bones", patch, placement))
    return out


def feature_lists(biome, own):
    """Eleven decoration steps; features sorted by the global ORDER."""
    steps = [[] for _ in range(11)]
    entries = [(f.split("_")[0], f"{NS}:{f}") for f in own]
    if biome == "chained_isles":
        entries.append(("chains", f"{NS}:island_chains"))
    if biome not in ("dark_sea",):
        entries.append(("ruin", f"{NS}:dream_ruin"))
    for kind, ref in sorted(entries, key=lambda e: ORDER.index(e[0])):
        steps[STEP[kind]].append(ref)
    return steps


# --------------------------------------------------------------------------- biomes
def biome_json(biome, spec):
    effects = {
        "sky_color": spec["sky"], "fog_color": spec["fog"],
        "water_color": spec["water"], "water_fog_color": spec["water_fog"],
        "grass_color": spec.get("grass", GRASS), "foliage_color": spec.get("foliage", FOLIAGE),
    }
    if spec.get("particle"):
        name, prob = spec["particle"]
        effects["particle"] = {"probability": prob, "options": {"type": mc(name)}}
    if spec.get("loop"):
        effects["ambient_sound"] = mc(spec["loop"])
    if spec.get("mood"):
        effects["mood_sound"] = {"sound": mc("ambient.cave"), "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0}
    if spec.get("additions"):
        effects["additions_sound"] = {"sound": mc(spec["additions"][0]), "tick_chance": spec["additions"][1]}
    own = build_features(biome, spec)
    return {
        "has_precipitation": bool(spec.get("precipitation")),
        "temperature": spec.get("temp", 0.8),
        "downfall": 0.5 if spec.get("precipitation") else 0.0,
        "effects": effects,
        "carvers": {},
        "features": feature_lists(biome, own),
        "spawners": {},
        "spawn_costs": {},
    }


# --------------------------------------------------------------------------- surface rules
def rule_block(name):
    return {"type": mc("block"), "result_state": blk(name)}


def cond(if_true, then_run):
    return {"type": mc("condition"), "if_true": if_true, "then_run": then_run}


def seq(*rules):
    return {"type": mc("sequence"), "sequence": list(rules)}


def stone_depth(offset):
    return {"type": mc("stone_depth"), "add_surface_depth": False, "offset": offset,
            "secondary_depth_range": 0, "surface_type": "floor"}


WATER_BELOW = {"type": mc("water"), "offset": -1, "surface_depth_multiplier": 0, "add_stone_depth": False}
ABOVE_SHORE = {"type": mc("y_above"), "anchor": {"absolute": 66}, "surface_depth_multiplier": 0, "add_stone_depth": False}


def biome_surface(biome, s):
    patches = [cond({"type": mc("noise_threshold"), "noise": n, "min_threshold": lo, "max_threshold": hi}, rule_block(b))
               for n, lo, hi, b in s["patches"]]
    top_layer = seq(
        cond({"type": mc("not"), "invert": WATER_BELOW}, rule_block(s["lake"])),          # under water
        cond({"type": mc("not"), "invert": ABOVE_SHORE}, rule_block(s["shore"])),          # beach
        cond(stone_depth(0), seq(*patches, rule_block(s["top"]))),
        rule_block(s["sub"]))
    return cond({"type": mc("biome"), "biome_is": [f"{NS}:{biome}"]}, seq(
        cond({"type": mc("steep")}, rule_block(s["steep"])),
        cond(stone_depth(3), top_layer),
        cond(stone_depth(9), rule_block(s["deep"]))))


# --------------------------------------------------------------------------- noise settings
def noise_settings(table):
    H = cache2d(realm("height"))
    Y = df("y_clamped_gradient", from_y=-64, to_y=320, from_value=-64.0, to_value=320.0)
    below = add(H, mul(-1.0, Y))                                  # blocks under the ground surface
    slope = mul(0.05, below)                                      # +1 twenty blocks deep, linear through 0
    rough = cache2d(realm("rough"))
    overhang = mul(mul(rough, 0.45), noise3d(f"{NS}:overhang", 0.7))
    terrain = add(slope, overhang)

    final_land = df("squeeze", argument=df("interpolated", argument=df("blend_density", argument=terrain)))
    final = df("range_choice", input=cache2d(realm("isle")), min_inclusive=0.5, max_exclusive=2.0,
               when_in_range={"type": f"{NS}:islands"}, when_out_of_range=final_land)

    region_code = df("flat_cache", argument=realm("region"))
    router = {
        "barrier": 0.0, "fluid_level_floodedness": 0.0, "fluid_level_spread": 0.0, "lava": 0.0,
        "temperature": region_code, "vegetation": 0.0, "continents": 0.0, "erosion": 0.0, "depth": 0.0, "ridges": 0.0,
        "initial_density_without_jaggedness": slope,
        "final_density": final,
        "vein_toggle": 0.0, "vein_ridged": 0.0, "vein_gap": 0.0,
    }
    rules = [cond({"type": mc("vertical_gradient"), "random_name": mc("bedrock_floor"),
                   "true_at_and_below": {"above_bottom": 0}, "false_at_and_above": {"above_bottom": 5}},
                  rule_block("minecraft:bedrock"))]
    rules += [biome_surface(b["biome"], BIOME[b["biome"]]) for b in table["biomes"]]
    return {
        "sea_level": -96,
        "disable_mob_generation": False,
        "aquifers_enabled": False,
        "ore_veins_enabled": False,
        "legacy_random_source": False,
        "default_block": blk("ash_stone"),
        "default_fluid": {"Name": "minecraft:water", "Properties": {"level": "0"}},
        "noise": {"min_y": -64, "height": 384, "size_horizontal": 1, "size_vertical": 2},
        "noise_router": router,
        "spawn_target": [{"temperature": [-1.0, 1.0], "humidity": [-1.0, 1.0], "continentalness": [-0.11, 1.0],
                          "erosion": [-1.0, 1.0], "weirdness": [-1.0, 1.0], "depth": 0.0, "offset": 0.0}],
        "surface_rule": seq(*rules),
    }


def dimension(table):
    return {"type": f"{NS}:dream_realm", "generator": {"type": mc("noise"), "settings": f"{NS}:dream",
        "biome_source": {"type": f"{NS}:dream_map", "biomes": [f"{NS}:"+b["biome"] for b in table["biomes"]]}}}


# --------------------------------------------------------------------------- write
def dump(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def main():
    table = region_table()
    names = [b["biome"] for b in table["biomes"]]
    assert set(names) == set(BIOME), (set(names) ^ set(BIOME))

    # drop stale generated files from earlier versions before writing
    for sub in ("configured_feature", "placed_feature"):
        for f in (DATA / "worldgen" / sub).glob("*.json"):
            if f.stem not in ("dream_ruin", "island_chains"):
                f.unlink()

    for name in names:
        dump(DATA / "worldgen/biome" / f"{name}.json", biome_json(name, BIOME[name]))
    for name, cfg in CONFIGURED.items():
        dump(DATA / "worldgen/configured_feature" / f"{name}.json", cfg)
    for name, plc in PLACED.items():
        dump(DATA / "worldgen/placed_feature" / f"{name}.json", plc)

    dump(DATA / "worldgen/noise/overhang.json", {"firstOctave": -6, "amplitudes": [1.0, 1.0, 0.5, 0.25]})
    dump(DATA / "worldgen/noise_settings/dream.json", noise_settings(table))
    dump(DATA / "dimension/dream_realm.json", dimension(table))
    print(f"{len(names)} biomes, {len(CONFIGURED)} configured features, {len(PLACED)} placed features")


if __name__ == "__main__":
    main()

# Explicit surface water must run in every Dream biome, including the ocean.
dump(DATA / "worldgen/configured_feature/realm_waters.json", {"type": f"{NS}:realm_waters", "config": {}})
dump(DATA / "worldgen/placed_feature/realm_waters.json", {"feature": f"{NS}:realm_waters", "placement": []})
for file in (DATA / "worldgen/biome").glob("*.json"):
    if file.stem == "nightmare_void": continue
    biome = json.loads(file.read_text()); biome["features"][1].insert(0, f"{NS}:realm_waters"); dump(file, biome)
