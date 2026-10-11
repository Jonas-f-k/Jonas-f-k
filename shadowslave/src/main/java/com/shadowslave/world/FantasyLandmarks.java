package com.shadowslave.world;

import com.shadowslave.ShadowSlaveMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/**
 * Small authored landmarks: blossom observatories, cozy workshops, garden portals and mossy
 * bridges.
 */
public final class FantasyLandmarks {
  public static void build(
      ServerLevelAccessor l, BlockPos o, String biome, boolean gateway, RandomSource random) {
    boolean tower =
        biome.equals("amethyst_heights")
            || biome.equals("blossom_vale")
            || biome.equals("aurora_fields");
    boolean garden = biome.equals("verdant_gardens") || biome.equals("sunpetal_meadows");
    boolean bridge = biome.equals("mosslight_ravine") || biome.equals("moonlit_grove");
    for (int x = -4; x <= 4; x++)
      for (int z = -4; z <= 4; z++) {
        put(
            l,
            o,
            x,
            -1,
            z,
            bridge ? "mossy_ruin_stone" : garden ? "sanctuary_limestone" : "honey_planks");
        for (int y = 0; y < 6; y++)
          l.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
      }
    if (tower) {
      for (int y = 0; y < 14; y++)
        for (int x = -3; x <= 3; x++)
          for (int z = -3; z <= 3; z++)
            if (Math.abs(x) == 3 || Math.abs(z) == 3) {
              if ((x == 0 && z == 3 && y < 3) || (y % 5 == 2 && (x == 0 || z == 0))) continue;
              put(l, o, x, y, z, y % 5 == 0 ? "rose_bark" : "pale_marble");
            }
      for (int tier = 0; tier < 3; tier++) {
        int base = 5 + tier * 5;
        for (int r = 5; r >= 1; r--)
          for (int x = -r; x <= r; x++)
            for (int z = -r; z <= r; z++)
              if (Math.abs(x) == r || Math.abs(z) == r)
                put(l, o, x, base + 5 - r, z, "plum_planks");
      }
      for (int y = 14; y <= 21; y++) put(l, o, 0, y, 0, "amethyst_crystal");
      put(l, o, -2, 0, -2, "star_books");
      put(l, o, 2, 0, -2, "amethyst_cluster");
    } else if (garden) {
      for (int x : new int[] {-3, 3})
        for (int y = 0; y < 6; y++) put(l, o, x, y, -2, "runic_timber");
      for (int x = -3; x <= 3; x++) put(l, o, x, 6, -2, "runic_timber");
      put(l, o, 0, 6, -2, "emerald_crystal");
      put(l, o, 0, 0, 0, "runic_basin");
      for (int x : new int[] {-3, 3})
        for (int z : new int[] {-2, 2}) {
          put(l, o, x, 0, z, "lantern_pedestal");
          put(l, o, x, 1, z, "jade_bloom");
        }
      for (int x = -4; x <= 4; x += 2) put(l, o, x, 0, 4, "sun_bloom");
    } else if (bridge) {
      for (int x : new int[] {-4, 4})
        for (int z : new int[] {-3, 3})
          for (int y = 0; y < 6; y++) put(l, o, x, y, z, "mossy_ruin_stone");
      for (int x = -4; x <= 4; x++)
        for (int z = -3; z <= 3; z++)
          put(l, o, x, 5, z, (x + z) % 3 == 0 ? "sanctuary_moss" : "mossy_ruin_stone");
      for (int x = -3; x <= 3; x++)
        for (int z : new int[] {-3, 3})
          if (Math.abs(x) > 1) put(l, o, x, 3 + Math.abs(x) / 2, z, "mossy_ruin_stone");
      for (int x : new int[] {-4, 4})
        for (int z = -2; z <= 2; z++) if ((x + z) % 2 == 0) put(l, o, x, 6, z, "emerald_leaves");
      put(l, o, -2, 0, -1, "moon_lantern");
      put(l, o, 2, 0, -1, "bloom_pot");
    } else {
      for (int x : new int[] {-4, 4})
        for (int z : new int[] {-4, 4}) for (int y = 0; y < 5; y++) put(l, o, x, y, z, "dream_oak");
      for (int x = -4; x <= 4; x++)
        for (int y = 0; y < 4; y++) put(l, o, x, y, -4, y == 2 ? "potion_shelf" : "honey_planks");
      for (int x = -5; x <= 5; x++)
        for (int z = -5; z <= 5; z++)
          put(l, o, x, 5 + Math.max(0, 3 - Math.abs(x)), z, "plum_planks");
      for (int x : new int[] {-3, 3}) {
        put(l, o, x, 0, -2, "star_books");
        put(l, o, x, 1, -2, "bloom_pot");
      }
      for (int x = -3; x <= 3; x += 3) put(l, o, x, 4, 2, "moon_lantern");
      l.setBlock(o.offset(2, 0, 0), Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
      put(l, o, -2, 0, 0, "runic_basin");
    }
    if (gateway)
      l.setBlock(o.offset(0, 0, -2), ShadowSlaveMod.GATEWAY.get().defaultBlockState(), 2);
    put(l, o, -2, 0, 2, "moon_lantern");
    var chest = o.offset(2, 0, 2);
    l.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
    if (l.getBlockEntity(chest) instanceof ChestBlockEntity c)
      c.setLootTable(
          ResourceKey.create(Registries.LOOT_TABLE, ShadowSlaveMod.id("chests/" + biome)),
          random.nextLong());
  }

  private static void put(ServerLevelAccessor l, BlockPos p, int x, int y, int z, String block) {
    l.setBlock(p.offset(x, y, z), RealmBlocks.state(block), 2);
  }
}
