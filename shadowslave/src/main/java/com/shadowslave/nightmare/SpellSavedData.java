package com.shadowslave.nightmare;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class SpellSavedData extends SavedData {
  public final Map<UUID, TrialInstance> instances = new LinkedHashMap<>();
  public int nextSlot;
  public boolean dreamInitialized;
  public final int[] citadelY = new int[3], citadelCursor = new int[3];
  public final boolean[] citadelComplete = new boolean[3];

  public static SpellSavedData get(MinecraftServer server) {
    return server
        .overworld()
        .getDataStorage()
        .computeIfAbsent(
            new Factory<>(SpellSavedData::new, SpellSavedData::load), "shadowslave_instances");
  }

  private static SpellSavedData load(CompoundTag n, HolderLookup.Provider provider) {
    var d = new SpellSavedData();
    d.nextSlot = n.getInt("nextSlot");
    d.dreamInitialized = n.getBoolean("dreamInitialized");
    for (int i = 0; i < 3; i++) {
      d.citadelY[i] = n.getInt("cityY" + i);
      d.citadelCursor[i] = n.getInt("cityCursor" + i);
      d.citadelComplete[i] = n.getBoolean("cityDone" + i);
    }
    for (var value : n.getList("instances", Tag.TAG_COMPOUND)) {
      var t = TrialInstance.load((CompoundTag) value);
      d.instances.put(t.id, t);
    }
    return d;
  }

  @Override
  public CompoundTag save(CompoundTag n, HolderLookup.Provider provider) {
    n.putInt("schema", 1);
    n.putInt("nextSlot", nextSlot);
    n.putBoolean("dreamInitialized", dreamInitialized);
    var list = new ListTag();
    for (int i = 0; i < 3; i++) {
      n.putInt("cityY" + i, citadelY[i]);
      n.putInt("cityCursor" + i, citadelCursor[i]);
      n.putBoolean("cityDone" + i, citadelComplete[i]);
    }
    instances.values().forEach(t -> list.add(t.save()));
    n.put("instances", list);
    return n;
  }
}
