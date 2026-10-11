package com.shadowslave.world;

import com.shadowslave.ShadowSlaveMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;

/** Quiet biome pockets and two guaranteed refuges; this rule is shared by AI and spawning. */
public final class Sanctuary {
  public static boolean isSafe(Level level, BlockPos pos) {
    if (!DreamRealm.isDream(level)) return false;
    long x = pos.getX(), z = pos.getZ();
    return GrandCitadels.protectedCourt(pos)
        || x * x + z * z <= 18 * 18
        || (x - 160) * (x - 160) + z * z <= 18 * 18
        || level.getBiome(pos).is(ShadowSlaveMod.id("ember_haven"))
        || level.getBiome(pos).is(ShadowSlaveMod.id("blossom_vale"))
        || level.getBiome(pos).is(ShadowSlaveMod.id("sunpetal_meadows"));
  }

  public static void build(ServerLevelAccessor level, BlockPos o, boolean gateway) {
    for (int x = -8; x <= 8; x++)
      for (int z = -8; z <= 8; z++) {
        if (x * x + z * z > 85) continue;
        boolean edge = x * x + z * z > 62;
        set(level, o.offset(x, -2, z), RealmBlocks.ASH_STONE.get());
        set(
            level,
            o.offset(x, -1, z),
            edge
                ? RealmBlocks.ASH_BRICKS.get()
                : (Math.abs(x) <= 2 || z == -2)
                    ? RealmBlocks.LIMESTONE.get()
                    : RealmBlocks.MOSS.get());
        for (int y = 0; y < 7; y++) set(level, o.offset(x, y, z), Blocks.AIR);
      }
    // A broad, roofed limestone arcade; the centre remains open to the sky.
    for (int x : new int[] {-6, 6})
      for (int z : new int[] {-6, -2}) {
        for (int y = 0; y < 5; y++)
          set(
              level,
              o.offset(x, y, z),
              y == 0 ? RealmBlocks.BRONZE.get() : RealmBlocks.LIMESTONE.get());
        set(level, o.offset(x, 4, z), RealmBlocks.AMBER_GLASS.get());
      }
    for (int x = -6; x <= 6; x++)
      for (int z = -6; z <= -2; z++)
        set(
            level,
            o.offset(x, 5, z),
            z == -6 || z == -2 ? RealmBlocks.LIMESTONE.get() : RealmBlocks.MOSS.get());
    for (int x = -2; x <= 2; x++)
      for (int z = -5; z <= -4; z++) set(level, o.offset(x, -1, z), Blocks.WATER);
    for (int x : new int[] {-4, 4}) {
      for (int z = 1; z <= 3; z++) set(level, o.offset(x, 0, z), RealmBlocks.SLAB.get());
      for (int z : new int[] {-1, 5}) set(level, o.offset(x, 0, z), RealmBlocks.LANTERN.get());
      for (int z : new int[] {0, 4})
        set(level, o.offset(x + Integer.signum(x), 0, z), Blocks.WHITE_TULIP);
    }
    if (gateway) set(level, o, ShadowSlaveMod.GATEWAY.get());
    else set(level, o.offset(0, 0, 6), RealmBlocks.LANTERN.get());
  }

  private static void set(ServerLevelAccessor level, BlockPos pos, Block block) {
    level.setBlock(pos, block.defaultBlockState(), 2);
  }
}
