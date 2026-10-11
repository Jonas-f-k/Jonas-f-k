package com.shadowslave.world;

import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.nightmare.SpellSavedData;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.*;

/** Deterministic, resumable fortress construction around three conquered Gateways. */
public final class GrandCitadels {
  public record Placement(BlockPos pos, Block block) {}

  private static final Map<Integer, List<Placement>> CACHE = new HashMap<>();

  public static void clear() {
    CACHE.clear();
  }

  public static BlockPos center(int index, int y) {
    return new BlockPos(new int[] {480, -1024, 1024}[index], y, new int[] {0, 640, -768}[index]);
  }

  public static boolean protectedCourt(BlockPos p) {
    for (int i = 0; i < 3; i++) {
      var c = center(i, 0);
      if (Math.abs(p.getX() - c.getX()) <= 39 && Math.abs(p.getZ() - c.getZ()) <= 39) return true;
    }
    return false;
  }

  public static void tick(MinecraftServer server) {
    var level = server.getLevel(DreamRealm.KEY);
    if (level == null) return;
    var saved = SpellSavedData.get(server);
    for (int i = 0; i < 3; i++) {
      final int city = i;
      var origin = center(i, 0);
      if (saved.citadelComplete[i]) continue;
      if (level.players().stream()
          .noneMatch(p -> p.distanceToSqr(origin.getX(), p.getY(), origin.getZ()) < 160 * 160))
        continue;
      if (saved.citadelY[i] == 0)
        saved.citadelY[i] = DreamRealm.safeSurface(level, origin.getX(), origin.getZ()).getY();
      var list =
          CACHE.computeIfAbsent(i, key -> blueprint(center(city, saved.citadelY[city]), city));
      int end = Math.min(list.size(), saved.citadelCursor[i] + 1600);
      while (saved.citadelCursor[i] < end) {
        var a = list.get(saved.citadelCursor[i]++);
        level.setBlock(a.pos(), a.block().defaultBlockState(), 2);
      }
      if (end == list.size()) {
        saved.citadelComplete[i] = true;
        CACHE.remove(i);
      }
      saved.setDirty();
      break;
    }
  }

  public static List<Placement> blueprint(BlockPos o, int style) {
    var list = new ArrayList<Placement>();
    Block stone =
        style == 0
            ? RealmBlocks.LIMESTONE.get()
            : style == 1 ? RealmBlocks.ASH_BRICKS.get() : Blocks.QUARTZ_BRICKS;
    Block trim = style == 1 ? RealmBlocks.SILVER_RUNE.get() : RealmBlocks.BRONZE.get();
    Block floor = RealmBlocks.ASH_TILES.get();
    // 81-square curtain wall, four 24-block watchtowers, four broad gatehouses.
    for (int x = -40; x <= 40; x++)
      for (int z = -40; z <= 40; z++) {
        put(list, o, x, -2, z, stone);
        put(
            list,
            o,
            x,
            -1,
            z,
            (Math.abs(x) < 5 || Math.abs(z) < 5) ? floor : RealmBlocks.MOSS.get());
        for (int y = 0; y < 26; y++) put(list, o, x, y, z, Blocks.AIR);
        if (Math.abs(x) == 40 || Math.abs(z) == 40)
          for (int y = 0; y < 13; y++)
            if (y >= 7 || Math.abs(x) > 4 && Math.abs(z) > 4)
              put(list, o, x, y, z, y == 11 ? trim : stone);
        if ((Math.abs(x) == 40 || Math.abs(z) == 40) && (x + z) % 3 == 0)
          put(list, o, x, 13, z, stone);
      }
    for (int x : new int[] {-35, 35})
      for (int z : new int[] {-35, 35}) {
        for (int dx = -4; dx <= 4; dx++)
          for (int dz = -4; dz <= 4; dz++)
            for (int y = 0; y < 24; y++)
              if (Math.abs(dx) == 4 || Math.abs(dz) == 4 || y == 22)
                put(list, o, x + dx, y, z + dz, y % 7 == 0 ? trim : stone);
        put(list, o, x, 24, z, RealmBlocks.LANTERN.get());
      }
    // Raised cathedral nave with open aisles, stained glass and a vaulted silhouette.
    for (int x = -14; x <= 14; x++)
      for (int z = -22; z <= 4; z++) {
        put(list, o, x, 0, z, floor);
        if (Math.abs(x) == 14 || z == -22)
          for (int y = 1; y < 18; y++) {
            boolean window = Math.abs(x) == 14 && z % 7 >= 2 && z % 7 <= 4 && y > 5 && y < 13;
            put(list, o, x, y, z, window ? Blocks.LIGHT_BLUE_STAINED_GLASS : stone);
          }
        put(list, o, x, 18 + (14 - Math.abs(x)) / 2, z, stone);
      }
    for (int z = -18; z <= 0; z += 6)
      for (int x : new int[] {-9, 9})
        for (int y = 1; y < 18; y++) put(list, o, x, y, z, y == 1 || y == 16 ? trim : stone);
    for (int x = -4; x <= 4; x++) for (int z = -14; z <= -6; z++) put(list, o, x, 1, z, trim);
    put(list, o, 0, 2, -10, ShadowSlaveMod.GATEWAY.get());
    // Accessible dais; spawn anchor is beside the Gateway at floor height.
    for (int x = -4; x <= 4; x++) put(list, o, x, 0, -5, floor);
    for (int sign : new int[] {-1, 1}) {
      for (int x = 20; x <= 30; x++)
        for (int z = -15; z <= 15; z++) {
          put(list, o, sign * x, 0, z, stone);
          if (x == 20 || x == 30 || Math.abs(z) == 15)
            for (int y = 1; y <= 6; y++)
              if (!(x == 20 && Math.abs(z) < 3 && y < 4)) put(list, o, sign * x, y, z, stone);
          put(list, o, sign * x, 7, z, stone);
        }
      for (int z = -12; z <= 12; z += 6) {
        put(list, o, sign * 25, 1, z, Blocks.BARREL);
        put(list, o, sign * 25, 2, z, RealmBlocks.LANTERN.get());
      }
      for (int z = 14; z <= 30; z += 4) {
        put(list, o, sign * 10, 0, z, Blocks.FLOWERING_AZALEA);
        put(list, o, sign * 16, 0, z, Blocks.PINK_TULIP);
      }
    }
    for (int x = -3; x <= 3; x++)
      for (int z = 15; z <= 21; z++) {
        put(list, o, x, 0, z, stone);
        if (Math.abs(x) < 3 && z > 15 && z < 21) put(list, o, x, 0, z, Blocks.WATER);
      }
    // A broad open southern entrance and stairs up both curtain walls.
    for (int sign : new int[] {-1, 1})
      for (int step = 0; step < 12; step++)
        for (int w = 0; w < 3; w++) put(list, o, sign * (27 + step), step, 30 + w, stone);
    return list;
  }

  private static void put(List<Placement> l, BlockPos o, int x, int y, int z, Block b) {
    l.add(new Placement(o.offset(x, y, z), b));
  }
}
