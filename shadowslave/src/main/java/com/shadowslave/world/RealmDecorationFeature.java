package com.shadowslave.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Broad canopies, sparse meadow trees, flower drifts, crystals and contrasting stones. */
public final class RealmDecorationFeature extends Feature<NoneFeatureConfiguration> {
  public RealmDecorationFeature() {
    super(NoneFeatureConfiguration.CODEC);
  }

  @Override
  public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
    var l = c.level();
    var o = c.origin();
    String biome = l.getBiome(o).unwrapKey().map(k -> k.location().getPath()).orElse("");
    var p = RealmPalette.BIOMES.get(biome);
    if (p == null
        || !l.isEmptyBlock(o)
        || !l.getBlockState(o.below()).is(RealmBlocks.MATERIALS.get(p.ground()).get()))
      return false;
    for (int i = 0; i < 7; i++) {
      int dx = c.random().nextInt(9) - 4, dz = c.random().nextInt(9) - 4;
      var f = l.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, o.offset(dx, 0, dz));
      if (Math.abs(f.getY() - o.getY()) < 5
          && l.isEmptyBlock(f)
          && l.getBlockState(f.below()).is(RealmBlocks.MATERIALS.get(p.ground()).get()))
        put(l, f, p.flower());
    }
    boolean meadow =
        biome.contains("meadow") || biome.contains("fields") || biome.contains("isles");
    if (p.leaves().isEmpty()) {
      if (c.random().nextInt(3) != 0) return true;
      int h = 2 + c.random().nextInt(4);
      String stone =
          biome.contains("shore")
              ? "fossil_bone"
              : biome.contains("desert")
                  ? "red_sandstone"
                  : biome.contains("death") ? "petrified_bark" : "fractured_stone";
      for (int y = 0; y < h; y++) put(l, o.above(y), stone);
      if (biome.contains("death")) {
        put(l, o.offset(1, h - 1, 0), stone);
        put(l, o.offset(-1, h - 2, 0), stone);
      }
      return true;
    }
    if (meadow && c.random().nextInt(5) != 0) return true;
    int h = 5 + c.random().nextInt(4);
    for (int y = 1; y <= h + 2; y++) if (!l.isEmptyBlock(o.above(y))) return true;
    boolean conifer = biome.contains("taiga"),
        spreading =
            biome.contains("blossom") || biome.contains("ravine") || biome.contains("wilds");
    for (int y = 0; y < h; y++) put(l, o.above(y), p.trunk());
    if (conifer) {
      for (int y = 2; y <= h + 1; y++) {
        int radius = (h + 1 - y) / 2;
        for (int x = -radius; x <= radius; x++)
          for (int z = -radius; z <= radius; z++)
            if (Math.abs(x) + Math.abs(z) < radius * 2 + 1) leaf(l, o.offset(x, y, z), p.leaves());
      }
    } else {
      for (int y = h - 2; y <= h + 1; y++) {
        int radius = y == h + 1 ? 1 : spreading ? 3 : 2;
        for (int x = -radius; x <= radius; x++)
          for (int z = -radius; z <= radius; z++)
            if (x * x + z * z <= radius * radius + 1) leaf(l, o.offset(x, y, z), p.leaves());
      }
      if (spreading) {
        int sign = c.random().nextBoolean() ? 1 : -1;
        put(l, o.offset(sign, h - 3, 0), p.trunk());
        put(l, o.offset(sign * 2, h - 2, 0), p.trunk());
        for (int x = -2; x <= 2; x++)
          for (int z = -2; z <= 2; z++)
            if (x * x + z * z <= 5) leaf(l, o.offset(sign * 2 + x, h - 1, z), p.leaves());
      }
      if (biome.equals("scarlet_wilds") || biome.equals("moonlit_grove"))
        for (int y = h - 3; y >= h - 5; y--) leaf(l, o.offset(2, y, 1), p.leaves());
    }
    if (biome.equals("amethyst_heights"))
      for (int y = 0; y < 3; y++)
        if (l.isEmptyBlock(o.offset(2, y, 0))) put(l, o.offset(2, y, 0), "amethyst_crystal");
    if (biome.equals("ember_haven") || biome.equals("verdant_gardens"))
      l.setBlock(o.offset(1, 1, 0), RealmBlocks.LANTERN.get().defaultBlockState(), 2);
    return true;
  }

  private static void leaf(WorldGenLevel l, BlockPos p, String name) {
    if (l.isEmptyBlock(p)) put(l, p, name);
  }

  private static void put(WorldGenLevel l, BlockPos p, String name) {
    l.setBlock(p, RealmBlocks.state(name), 2);
  }
}
