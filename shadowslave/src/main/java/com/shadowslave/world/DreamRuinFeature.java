package com.shadowslave.world;

import com.shadowslave.ShadowSlaveMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A compact ruin feature that fits within its generation region. */
public final class DreamRuinFeature extends Feature<NoneFeatureConfiguration> {
  public DreamRuinFeature() {
    super(NoneFeatureConfiguration.CODEC);
  }

  @Override
  public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
    var level = context.level();
    var origin = context.origin();
    if (origin.getY() < 62 || !level.getBlockState(origin.below()).isSolid()) return false;
    if (level.getBiome(origin).is(ShadowSlaveMod.id("ember_haven"))) {
      Sanctuary.build(level, origin, true);
      return true;
    }
    String biome =
        level
            .getBiome(origin)
            .unwrapKey()
            .map(k -> k.location().getPath())
            .orElse("forgotten_shore");
    if (java.util.List.of("forgotten_shore", "death_zone", "hollow_mountains", "nightmare_desert")
        .contains(biome)) build(level, origin, context.random().nextInt(3) == 0, context.random());
    else
      FantasyLandmarks.build(
          level, origin, biome, context.random().nextInt(3) == 0, context.random());
    return true;
  }

  public static void build(
      ServerLevelAccessor level, BlockPos o, boolean gateway, RandomSource random) {
    for (int x = -4; x <= 4; x++)
      for (int z = -4; z <= 4; z++) {
        level.setBlock(o.offset(x, -1, z), RealmBlocks.ASH_TILES.get().defaultBlockState(), 2);
        for (int y = 0; y <= 4; y++)
          level.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        if ((Math.abs(x) == 4 || Math.abs(z) == 4) && Math.abs(x) > 1 && Math.abs(z) > 1)
          for (int y = 0; y < 2 + random.nextInt(3); y++)
            level.setBlock(
                o.offset(x, y, z), RealmBlocks.FRACTURED_STONE.get().defaultBlockState(), 2);
      }
    for (int x : new int[] {-3, 3})
      for (int y = 0; y < 7; y++)
        level.setBlock(
            o.offset(x, y, -1),
            (y == 0 || y == 6 ? RealmBlocks.BRONZE : RealmBlocks.ASH_BRICKS)
                .get()
                .defaultBlockState(),
            2);
    for (int x = -3; x <= 3; x++)
      level.setBlock(o.offset(x, 7, -1), RealmBlocks.ASH_TILES.get().defaultBlockState(), 2);
    level.setBlock(o.offset(0, 7, -1), RealmBlocks.SILVER_RUNE.get().defaultBlockState(), 2);
    for (int x : new int[] {-2, 2})
      level.setBlock(o.offset(x, 0, 2), RealmBlocks.LANTERN.get().defaultBlockState(), 2);
    level.setBlock(
        o,
        gateway
            ? ShadowSlaveMod.GATEWAY.get().defaultBlockState()
            : RealmBlocks.LANTERN.get().defaultBlockState(),
        2);
    var chest = o.offset(0, 0, 3);
    level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
    if (level.getBlockEntity(chest) instanceof ChestBlockEntity c)
      c.setLootTable(
          ResourceKey.create(Registries.LOOT_TABLE, ShadowSlaveMod.id("chests/dream_ruin")),
          random.nextLong());
  }
}
