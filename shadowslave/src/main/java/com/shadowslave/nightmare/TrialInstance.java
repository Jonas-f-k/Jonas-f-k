package com.shadowslave.nightmare;

import com.shadowslave.core.Rules;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;

public final class TrialInstance {
  public UUID id, owner, bossId;
  public int targetRank = 1;
  public CompoundTag wakingReturn = new CompoundTag();
  public int layoutVersion = 1;
  public int family, slot, buildCursor, points, attempt;
  public long seed;
  public int bossSite;
  public String state = "building";
  public boolean bossDefeated;
  public int altars;
  public final Set<Integer> spawned = new HashSet<>(), killed = new HashSet<>();
  public final Map<Integer, UUID> liveEntities = new HashMap<>();
  public final Set<Integer> activatedWaves = new HashSet<>();

  public TrialInstance(UUID owner, int family, int slot, long seed, int attempt) {
    this.id = UUID.randomUUID();
    this.owner = owner;
    this.family = family;
    this.slot = slot;
    this.seed = seed;
    this.attempt = attempt;
    bossSite = Rules.bossSite(seed, 5);
  }

  public BlockPos origin() {
    return new BlockPos((slot % 4096) * 512, 80, (slot / 4096) * 512);
  }

  public int seals() {
    return layoutVersion == 0 ? 3 : 6;
  }

  public int elevation(int x, int z) {
    return layoutVersion == 0 ? 0 : Math.clamp((-24 - z) / 4, 0, 8);
  }

  public BlockPos surface(int x, int z) {
    return origin().offset(x, elevation(x, z) + 1, z);
  }

  public BlockPos start() {
    return surface(0, layoutVersion == 0 ? 50 : 108);
  }

  public BlockPos exit() {
    return surface(0, layoutVersion == 0 ? 56 : 114);
  }

  public BlockPos altar(int index) {
    if (layoutVersion == 0) return origin().offset(new int[] {-38, 0, 38}[index], 1, -12);
    int[][] a = {{-76, 46}, {76, 46}, {-76, -20}, {76, -20}, {-38, -78}, {38, -78}};
    return surface(a[index][0], a[index][1]);
  }

  public BlockPos arena(int index) {
    int[][] positions =
        layoutVersion == 0
            ? new int[][] {{-44, -44}, {0, -46}, {44, -44}, {-46, 30}, {46, 30}}
            : new int[][] {{-94, -94}, {0, -102}, {94, -94}, {-100, 88}, {100, 88}};
    return surface(positions[index][0], positions[index][1]);
  }

  public BlockPos bossPosition() {
    return arena(bossSite);
  }

  public boolean contains(BlockPos pos) {
    return Math.abs(pos.getX() - origin().getX()) <= (layoutVersion == 0 ? 63 : 127)
        && Math.abs(pos.getZ() - origin().getZ()) <= (layoutVersion == 0 ? 63 : 127)
        && pos.getY() > 75
        && pos.getY() < 160;
  }

  public boolean complete() {
    return (altars & ((1 << seals()) - 1)) == (1 << seals()) - 1;
  }

  public CompoundTag save() {
    var n = new CompoundTag();
    n.putUUID("id", id);
    n.putUUID("owner", owner);
    n.putInt("targetRank", targetRank);
    n.put("wakingReturn", wakingReturn.copy());
    n.putInt("layout", layoutVersion);
    n.putInt("family", family);
    n.putInt("slot", slot);
    n.putLong("seed", seed);
    n.putInt("site", bossSite);
    n.putInt("build", buildCursor);
    n.putString("state", state);
    n.putInt("points", points);
    n.putInt("attempt", attempt);
    n.putBoolean("bossDefeated", bossDefeated);
    n.putInt("altars", altars);
    if (bossId != null) n.putUUID("boss", bossId);
    n.putIntArray("spawned", spawned.stream().mapToInt(Integer::intValue).toArray());
    n.putIntArray("killed", killed.stream().mapToInt(Integer::intValue).toArray());
    n.putIntArray("waves", activatedWaves.stream().mapToInt(Integer::intValue).toArray());
    var entities = new ListTag();
    liveEntities.forEach(
        (key, value) -> {
          var t = new CompoundTag();
          t.putInt("index", key);
          t.putUUID("uuid", value);
          entities.add(t);
        });
    n.put("entities", entities);
    return n;
  }

  public static TrialInstance load(CompoundTag n) {
    var t =
        new TrialInstance(
            n.getUUID("owner"),
            n.getInt("family"),
            n.getInt("slot"),
            n.getLong("seed"),
            n.getInt("attempt"));
    t.targetRank = n.contains("targetRank") ? Math.clamp(n.getInt("targetRank"), 1, 4) : 1;
    t.wakingReturn = n.getCompound("wakingReturn").copy();
    t.layoutVersion = n.getInt("layout");
    t.id = n.getUUID("id");
    t.bossSite = Math.clamp(n.getInt("site"), 0, 4);
    t.buildCursor = n.getInt("build");
    t.state = n.getString("state");
    t.points = n.getInt("points");
    t.altars = n.getInt("altars");
    t.bossDefeated = n.getBoolean("bossDefeated");
    if (n.hasUUID("boss")) t.bossId = n.getUUID("boss");
    for (int i : n.getIntArray("spawned")) t.spawned.add(i);
    for (int i : n.getIntArray("killed")) t.killed.add(i);
    for (int i : n.getIntArray("waves")) t.activatedWaves.add(i);
    for (var v : n.getList("entities", Tag.TAG_COMPOUND)) {
      var e = (CompoundTag) v;
      t.liveEntities.put(e.getInt("index"), e.getUUID("uuid"));
    }
    return t;
  }
}
