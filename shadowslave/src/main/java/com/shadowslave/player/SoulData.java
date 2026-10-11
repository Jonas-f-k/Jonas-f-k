package com.shadowslave.player;

import com.shadowslave.core.Rules;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class SoulData implements INBTSerializable<CompoundTag> {
  public String stage = "aspirant";
  public long elapsed, lastDayTime = -1;
  public int family = -1, aspectRank, soulRank, fragments, essence = 100;
  public long cooldownUntil, lastCombatTick, lastActionTick = -100;
  public long awakenedCooldownUntil, ascendedCooldownUntil, transformUntil, transformCooldownUntil;
  public int formedCores = 1, totalKills, eliteKills, bossKills;
  public double damageEndured, regenRemainder;
  public boolean formGrantedFlight;
  public int activeBeast = -1;
  public long beastCooldownUntil, beastAttackUntil;
  public final Set<String> traits = new LinkedHashSet<>(), visitedBiomes = new LinkedHashSet<>();
  public final Map<String, Integer> memoryRanks = new HashMap<>(), memoryTiers = new HashMap<>();
  public UUID instance;
  public boolean activeMemories = true, welcomed;
  public final List<String> echoes = new ArrayList<>();
  public final List<String> owned = new ArrayList<>();
  public final List<String> equipped = new ArrayList<>();
  public CompoundTag returnPoint = new CompoundTag();
  public ListTag inventoryBackup = new ListTag();
  public int xpLevel, xpTotal, selectedSlot;
  public float xpProgress;
  public String previousMode = "survival";
  public boolean inventoryStored;
  public int dreamAnchorX, dreamAnchorY = 80, dreamAnchorZ;
  public boolean hasDreamAnchor;

  public int maxEssence() {
    return (int)
        Math.round(
            Rules.maximumEssence(soulRank, fragments, cores())
                * (1 + com.shadowslave.player.Traits.stats(this).essence()));
  }

  public int cores() {
    if (aspectRank != 6) return 1;
    formedCores = Math.max(formedCores, Rules.cores(aspectRank, fragments));
    return Math.clamp(formedCores, 1, 7);
  }

  public double growth() {
    return Rules.growth(cores(), fragments);
  }

  public boolean initiated() {
    return soulRank > 0;
  }

  public void refill(int amount) {
    essence = Math.clamp(essence + amount, 0, maxEssence());
  }

  public boolean spend(int cost) {
    if (essence < cost) return false;
    essence -= cost;
    return true;
  }

  @Override
  public CompoundTag serializeNBT(HolderLookup.Provider provider) {
    var n = new CompoundTag();
    n.putInt("schema", 3);
    n.putInt("formedCores", cores());
    n.putInt("totalKills", totalKills);
    n.putInt("eliteKills", eliteKills);
    n.putInt("bossKills", bossKills);
    n.putLong("beastCooldown", beastCooldownUntil);
    n.putDouble("damageEndured", damageEndured);
    n.putDouble("regenRemainder", regenRemainder);
    n.putBoolean("formGrantedFlight", formGrantedFlight);
    n.put("traits", strings(new ArrayList<>(traits)));
    n.put("visitedBiomes", strings(new ArrayList<>(visitedBiomes)));
    n.putLong("ascendedCooldown", ascendedCooldownUntil);
    n.putLong("transform", transformUntil);
    n.putLong("transformCooldown", transformCooldownUntil);
    n.putLong("awakenedCooldown", awakenedCooldownUntil);
    var rolls = new CompoundTag();
    memoryRanks.forEach(
        (id, rank) -> {
          var roll = new CompoundTag();
          roll.putInt("rank", rank);
          roll.putInt("tier", memoryTiers.getOrDefault(id, 1));
          rolls.put(id, roll);
        });
    n.put("memoryRolls", rolls);
    n.putString("stage", stage);
    n.putLong("elapsed", elapsed);
    n.putInt("family", family);
    n.putInt("aspectRank", aspectRank);
    n.putInt("soulRank", soulRank);
    n.putInt("fragments", fragments);
    n.putInt("essence", essence);
    n.putLong("cooldown", cooldownUntil);
    n.putLong("combat", lastCombatTick);
    if (instance != null) n.putUUID("instance", instance);
    n.putBoolean("active", activeMemories);
    n.putBoolean("welcomed", welcomed);
    n.put("echoes", strings(echoes));
    n.put("owned", strings(owned));
    n.put("equipped", strings(equipped));
    n.put("return", returnPoint.copy());
    n.put("inventory", inventoryBackup.copy());
    n.putBoolean("inventoryStored", inventoryStored);
    n.putInt("xpLevel", xpLevel);
    n.putInt("xpTotal", xpTotal);
    n.putFloat("xpProgress", xpProgress);
    n.putInt("selected", selectedSlot);
    n.putString("mode", previousMode);
    n.putBoolean("anchor", hasDreamAnchor);
    n.putInt("anchorX", dreamAnchorX);
    n.putInt("anchorY", dreamAnchorY);
    n.putInt("anchorZ", dreamAnchorZ);
    return n;
  }

  @Override
  public void deserializeNBT(HolderLookup.Provider provider, CompoundTag n) {
    formedCores = Math.clamp(n.contains("formedCores") ? n.getInt("formedCores") : 1, 1, 7);
    beastCooldownUntil = n.getLong("beastCooldown");
    beastAttackUntil = 0;
    activeBeast = -1;
    totalKills = Math.max(0, n.getInt("totalKills"));
    eliteKills = Math.max(0, n.getInt("eliteKills"));
    bossKills = Math.max(0, n.getInt("bossKills"));
    damageEndured = Math.max(0, n.getDouble("damageEndured"));
    regenRemainder = Math.clamp(n.getDouble("regenRemainder"), 0, 1);
    formGrantedFlight = n.getBoolean("formGrantedFlight");
    traits.clear();
    var traitList = new ArrayList<String>();
    readStrings(n.getList("traits", Tag.TAG_STRING), traitList, 64);
    traits.addAll(traitList);
    traits.removeIf(id -> com.shadowslave.player.Traits.get(id) == null);
    visitedBiomes.clear();
    var regions = new ArrayList<String>();
    readStrings(n.getList("visitedBiomes", Tag.TAG_STRING), regions, 64);
    visitedBiomes.addAll(regions);
    ascendedCooldownUntil = n.getLong("ascendedCooldown");
    transformUntil = n.getLong("transform");
    transformCooldownUntil = n.getLong("transformCooldown");
    awakenedCooldownUntil = n.getLong("awakenedCooldown");
    memoryRanks.clear();
    memoryTiers.clear();
    var rolls = n.getCompound("memoryRolls");
    for (String id : rolls.getAllKeys()) {
      var roll = rolls.getCompound(id);
      memoryRanks.put(id, Math.clamp(roll.getInt("rank"), 0, 6));
      memoryTiers.put(id, Math.clamp(roll.getInt("tier"), 1, 7));
    }
    stage = n.contains("stage") ? n.getString("stage") : "aspirant";
    elapsed = Math.max(0, n.getLong("elapsed"));
    family = n.contains("family") ? Math.clamp(n.getInt("family"), -1, 6) : -1;
    aspectRank = Math.clamp(n.getInt("aspectRank"), 0, 6);
    soulRank = Math.clamp(n.getInt("soulRank"), 0, 7);
    fragments = Math.max(0, n.getInt("fragments"));
    essence = Math.clamp(n.getInt("essence"), 0, maxEssence());
    cooldownUntil = n.getLong("cooldown");
    lastCombatTick = n.getLong("combat");
    instance = n.hasUUID("instance") ? n.getUUID("instance") : null;
    activeMemories = !n.contains("active") || n.getBoolean("active");
    welcomed = n.getBoolean("welcomed");
    echoes.clear();
    readStrings(n.getList("echoes", Tag.TAG_STRING), echoes, 64);
    owned.clear();
    readStrings(n.getList("owned", Tag.TAG_STRING), owned, 256);
    equipped.clear();
    readStrings(n.getList("equipped", Tag.TAG_STRING), equipped, 7);
    equipped.removeIf(id -> !owned.contains(id));
    returnPoint = n.getCompound("return").copy();
    inventoryBackup = n.getList("inventory", Tag.TAG_COMPOUND).copy();
    inventoryStored = n.getBoolean("inventoryStored");
    xpLevel = n.getInt("xpLevel");
    xpTotal = n.getInt("xpTotal");
    xpProgress = n.getFloat("xpProgress");
    selectedSlot = Math.clamp(n.getInt("selected"), 0, 8);
    previousMode = n.getString("mode");
    hasDreamAnchor = n.getBoolean("anchor");
    dreamAnchorX = n.getInt("anchorX");
    dreamAnchorY = n.getInt("anchorY");
    dreamAnchorZ = n.getInt("anchorZ");
    lastDayTime = -1;
  }

  private static ListTag strings(List<String> strings) {
    var l = new ListTag();
    strings.forEach(s -> l.add(StringTag.valueOf(s)));
    return l;
  }

  private static void readStrings(ListTag n, List<String> into, int limit) {
    for (int i = 0; i < Math.min(n.size(), limit); i++)
      if (!into.contains(n.getString(i))) into.add(n.getString(i));
  }
}
