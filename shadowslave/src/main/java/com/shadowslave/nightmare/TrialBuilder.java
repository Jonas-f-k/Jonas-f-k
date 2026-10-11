package com.shadowslave.nightmare;

import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.content.Content;
import com.shadowslave.world.RealmBlocks;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Original block-built templates. Fixed geometry per kind; only the boss arena varies. */
public final class TrialBuilder {
  public record Placement(BlockPos pos, BlockState state) {}

  private static final Map<UUID, List<Placement>> CACHE = new HashMap<>();

  public static void clear() {
    CACHE.clear();
  }

  public static boolean buildStep(ServerLevel level, TrialInstance t) {
    var list = CACHE.computeIfAbsent(t.id, id -> blueprint(t));
    int end = Math.min(list.size(), t.buildCursor + 1800);
    for (; t.buildCursor < end; t.buildCursor++) {
      var p = list.get(t.buildCursor);
      level.setBlock(p.pos, p.state, 2);
    }
    if (t.buildCursor == list.size()) {
      CACHE.remove(t.id);
      return true;
    }
    return false;
  }

  private static Block block(String id) {
    return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
  }

  public static List<Placement> legacyBlueprint(TrialInstance t) {
    var list = new ArrayList<Placement>();
    var o = t.origin();
    var def = Content.trial(t.family);
    Block stone = block(def.primary()), accent = block(def.accent()), light = block(def.light());
    // Enclosed adventure islands, with low walls and an unobstructed network of paths.
    for (int x = -64; x <= 64; x++)
      for (int z = -64; z <= 64; z++) {
        put(
            list,
            o,
            x,
            0,
            z,
            Math.abs(x) <= 2
                ? RealmBlocks.ASH_TILES.get()
                : Math.floorMod(x * 31 + z * 17, 23) == 0 ? accent : stone);
        if (Math.abs(x) == 64 || Math.abs(z) == 64)
          for (int y = 1; y <= 8; y++) put(list, o, x, y, z, Blocks.BARRIER);
        if ((Math.abs(x) == 62 || Math.abs(z) == 62) && (x + z) % 4 == 0) {
          put(list, o, x, 1, z, accent);
          put(list, o, x, 2, z, light);
        }
      }
    // Central nave, spine of the garden/library/citadel, with broken ribs and open doors.
    for (int z = -36; z <= 36; z += 12)
      for (int x : new int[] {-15, 15}) {
        for (int y = 1; y <= 9; y++) put(list, o, x, y, z, accent);
        for (int a = -15; a <= 15; a++)
          if (Math.abs(a) > 9 || z % 24 == 0)
            put(list, o, a, 10 + (15 - Math.abs(a)) / 3, z, stone);
        put(list, o, x, 10, z, light);
        put(list, o, x, 1, z, RealmBlocks.BRONZE.get());
        if (z % 24 == 0) put(list, o, 0, 15, z, RealmBlocks.SILVER_RUNE.get());
      }
    for (int i = 0; i < 3; i++) {
      var a = t.altar(i);
      platform(list, a.below(), 6, stone, accent);
      for (int dx : new int[] {-6, 6})
        for (int dz : new int[] {-6, 6}) {
          for (int y = 1; y <= 5; y++) put(list, a.below(), dx, y, dz, accent);
          put(list, a.below(), dx, 6, dz, light);
        }
      list.add(new Placement(a, ShadowSlaveMod.ALTAR.get().defaultBlockState()));
    }
    // Every kind contains all five authored arenas. This guarantees reachable boss sites.
    for (int i = 0; i < 5; i++) {
      var a = t.arena(i).below();
      platform(list, a, 8, stone, accent);
      for (int dx = -8; dx <= 8; dx++)
        for (int dz = -8; dz <= 8; dz++)
          if ((Math.abs(dx) == 8 || Math.abs(dz) == 8) && Math.abs(dx) > 2 && Math.abs(dz) > 2) {
            for (int y = 1; y <= ((dx + dz) % 4 == 0 ? 5 : 2); y++) put(list, a, dx, y, dz, accent);
          }
      put(list, a, 0, 0, 0, RealmBlocks.SILVER_RUNE.get());
      for (int dx = -5; dx <= 5; dx++)
        for (int dz = -5; dz <= 5; dz++)
          if (dx * dx + dz * dz >= 22 && dx * dx + dz * dz <= 29)
            put(list, a, dx, 0, dz, RealmBlocks.BRONZE.get());
    }
    // Deterministic environmental silhouettes distinguish the seven maps.
    for (int x = -52; x <= 52; x += 13)
      for (int z = 0; z <= 24; z += 12) {
        if (Math.abs(x) < 20) continue;
        switch (t.family) {
          case 1, 4 -> { // dead shadow trees / thorn trees
            for (int y = 1; y <= 7; y++) put(list, o, x, y, z, RealmBlocks.BARK.get());
            for (int dx = -2; dx <= 2; dx++)
              for (int dz = -2; dz <= 2; dz++)
                if (Math.abs(dx) + Math.abs(dz) < 3)
                  put(
                      list,
                      o,
                      x + dx,
                      7 + Math.abs(dx),
                      z + dz,
                      t.family == 1
                          ? RealmBlocks.FRACTURED_STONE.get()
                          : RealmBlocks.ASH_GRASS.get());
          }
          case 2 -> {
            for (int dx = -2; dx <= 2; dx++)
              for (int y = 1; y <= 4; y++)
                put(
                    list,
                    o,
                    x + dx,
                    y,
                    z,
                    Math.abs(dx) == 2 ? RealmBlocks.DROWNED.get() : Blocks.BOOKSHELF);
            put(list, o, x, 5, z, RealmBlocks.LANTERN.get());
          }
          case 5 -> {
            for (int dx = -3; dx <= 3; dx++)
              for (int y = 1; y <= 6; y++)
                put(
                    list,
                    o,
                    x + dx,
                    y,
                    z,
                    Math.abs(dx) == 3 || y == 6 ? accent : Blocks.GRAY_STAINED_GLASS);
          }
          case 6 -> {
            for (int y = 1; y < 9; y++)
              for (int dx = -1; dx <= 1; dx++)
                put(
                    list,
                    o,
                    x + dx,
                    y,
                    z,
                    y == 8 ? RealmBlocks.SILVER_RUNE.get() : RealmBlocks.FRACTURED_STONE.get());
          }
          default -> {
            for (int dx = -3; dx <= 3; dx++)
              for (int y = 1; y <= 4; y++) if (dx != 0) put(list, o, x + dx, y, z, stone);
            put(list, o, x, 5, z, light);
          }
        }
      }
    list.add(new Placement(t.exit(), ShadowSlaveMod.EXIT.get().defaultBlockState()));
    return list;
  }

  public static List<Placement> blueprint(TrialInstance t) {
    if (t.layoutVersion == 0) return legacyBlueprint(t);
    var list = new ArrayList<Placement>();
    var o = t.origin();
    var def = Content.trial(t.family);
    Block stone = block(def.primary()), accent = block(def.accent()), light = block(def.light());
    // Three elevations, a broken aqueduct, three crossing routes, outer districts and an upper
    // sanctuary.
    for (int x = -128; x <= 128; x++)
      for (int z = -128; z <= 128; z++) {
        int y = t.elevation(x, z);
        boolean ravine = z >= 8 && z <= 18;
        boolean bridge = Math.abs(x) <= 5 || Math.abs(Math.abs(x) - 76) <= 5;
        if (!ravine || bridge)
          put(
              list,
              o,
              x,
              y,
              z,
              (Math.abs(x) <= 4
                      || Math.abs(Math.abs(x) - 76) <= 4
                      || Math.abs(z - 46) <= 3
                      || Math.abs(z + 78) <= 3)
                  ? RealmBlocks.ASH_TILES.get()
                  : stone);
        if (ravine && bridge && Math.abs(x) % 76 == 5) {
          put(list, o, x, y + 1, z, accent);
          put(list, o, x, y + 2, z, Blocks.IRON_BARS);
        }
        if (Math.abs(x) == 128 || Math.abs(z) == 128)
          for (int wall = 1; wall <= 16; wall++) put(list, o, x, y + wall, z, Blocks.BARRIER);
        if ((Math.abs(x) == 125 || Math.abs(z) == 125) && (x + z) % 8 == 0) {
          for (int h = 1; h <= 5; h++) put(list, o, x, y + h, z, accent);
          put(list, o, x, y + 6, z, light);
        }
      }
    for (int i = 0; i < t.seals(); i++) {
      var a = t.altar(i);
      platform(list, a.below(), 12, stone, accent);
      for (int dx : new int[] {-11, 11})
        for (int dz : new int[] {-11, 11}) {
          for (int y = 0; y < 12; y++) put(list, a, dx, y, dz, accent);
          put(list, a, dx, 12, dz, light);
        }
      for (int dx = -11; dx <= 11; dx++) {
        put(list, a, dx, 12, -11, accent);
        put(list, a, dx, 12, 11, accent);
      }
      list.add(new Placement(a, ShadowSlaveMod.ALTAR.get().defaultBlockState()));
    }
    for (int i = 0; i < 5; i++) {
      var a = t.arena(i).below();
      platform(list, a, 11, stone, accent);
      for (int dx = -11; dx <= 11; dx++)
        for (int dz = -11; dz <= 11; dz++)
          if ((Math.abs(dx) == 11 || Math.abs(dz) == 11) && Math.abs(dx) > 4 && Math.abs(dz) > 4)
            for (int y = 1; y <= 6; y++) put(list, a, dx, y, dz, accent);
      for (int dx = -7; dx <= 7; dx++)
        for (int dz = -7; dz <= 7; dz++)
          if (dx * dx + dz * dz > 34 && dx * dx + dz * dz < 50)
            put(list, a, dx, 0, dz, RealmBlocks.BRONZE.get());
    }
    // Open-front multi-storey districts. Their footprints never overlap seals or boss arenas.
    for (int x : new int[] {-40, 40})
      for (int z : new int[] {76, 42, -8, -42}) {
        var b = t.surface(x, z);
        platform(list, b.below(), 10, stone, accent);
        for (int dx = -10; dx <= 10; dx++)
          for (int dz = -9; dz <= 9; dz++)
            if (Math.abs(dx) == 10 || Math.abs(dz) == 9)
              for (int y = 0; y < 11; y++) {
                if (Math.abs(dx) <= 3 && Math.abs(dz) == 9 && y < 5) continue;
                if (y >= 3 && y <= 6 && Math.abs(dx) % 5 < 2 && Math.abs(dz) == 9) continue;
                put(list, b, dx, y, dz, y % 5 == 0 ? accent : stone);
              }
        for (int dx = -10; dx <= 10; dx++)
          for (int dz = -9; dz <= 9; dz++)
            if (Math.abs(dx) > 5 || Math.abs(dz) > 4) put(list, b, dx, 11, dz, accent);
        // Accessible interior staircase to a viewing gallery.
        for (int step = 0; step < 11; step++)
          for (int w = 0; w < 3; w++) put(list, b, -8 + w, step, -6 + step, stone);
        for (int sign : new int[] {-1, 1}) put(list, b, sign * 8, 7, 0, light);
        switch (t.family) {
          case 0 -> {
            for (int h = 0; h < 7; h++) put(list, b, 0, h, -5, RealmBlocks.BRONZE.get());
            put(list, b, 0, 7, -5, Blocks.CAMPFIRE);
          }
          case 1, 4 -> {
            for (int h = 0; h < 12; h++) put(list, b, 0, h, -4, RealmBlocks.BARK.get());
            for (int dx = -4; dx <= 4; dx++)
              for (int dz = -3; dz <= 3; dz++)
                if (Math.abs(dx) + Math.abs(dz) < 6)
                  put(
                      list,
                      b,
                      dx,
                      12,
                      -4 + dz,
                      t.family == 1 ? RealmBlocks.FRACTURED_STONE.get() : RealmBlocks.MOSS.get());
          }
          case 2 -> {
            for (int dx : new int[] {-7, 7})
              for (int dz = -5; dz <= 5; dz++)
                for (int h = 0; h < 5; h++) put(list, b, dx, h, dz, Blocks.BOOKSHELF);
          }
          case 3 -> {
            for (int dx : new int[] {-6, 6}) {
              put(list, b, dx, 0, -4, Blocks.ANVIL);
              for (int h = 0; h < 8; h++) put(list, b, dx, h, 5, Blocks.IRON_BARS);
            }
          }
          case 5 -> {
            for (int dx = -4; dx <= 4; dx++)
              for (int h = 1; h < 9; h++) put(list, b, dx, h, -6, Blocks.LIGHT_BLUE_STAINED_GLASS);
          }
          case 6 -> {
            for (int dx : new int[] {-6, 6})
              for (int h = 0; h < 20; h++)
                put(list, b, dx, h, -5, h % 5 == 0 ? RealmBlocks.SILVER_RUNE.get() : stone);
          }
        }
      }
    // Long monumental nave guides the player north without closing the alternate routes.
    for (int z = -102; z <= 90; z += 16)
      for (int x : new int[] {-13, 13}) {
        var b = t.surface(x, z);
        for (int h = 0; h < 15; h++) put(list, b, 0, h, 0, accent);
        put(list, b, 0, 15, 0, light);
        if (z % 32 == -6 || z % 32 == 10)
          for (int dx = -13; dx <= 13; dx++)
            put(list, t.surface(0, z), dx, 16 + (13 - Math.abs(dx)) / 3, 0, stone);
      }
    list.add(new Placement(t.exit(), ShadowSlaveMod.EXIT.get().defaultBlockState()));
    return list;
  }

  private static void platform(List<Placement> l, BlockPos o, int r, Block s, Block a) {
    for (int x = -r; x <= r; x++)
      for (int z = -r; z <= r; z++)
        put(l, o, x, 0, z, Math.abs(x) == r || Math.abs(z) == r ? a : s);
  }

  private static void put(List<Placement> l, BlockPos o, int x, int y, int z, Block block) {
    l.add(new Placement(o.offset(x, y, z), block.defaultBlockState()));
  }
}
