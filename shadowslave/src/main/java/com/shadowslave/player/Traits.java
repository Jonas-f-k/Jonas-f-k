package com.shadowslave.player;

import com.shadowslave.ShadowSlaveMod;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Permanent, modest earned bonuses. Each milestone is granted once; no random reroll. */
public final class Traits {
  public record Stats(
      double health,
      double armor,
      double power,
      double melee,
      double cooldown,
      double essence,
      double speed,
      double regen,
      double fire,
      double fall) {
    public Stats plus(Stats b) {
      return new Stats(
          health + b.health,
          armor + b.armor,
          power + b.power,
          melee + b.melee,
          cooldown + b.cooldown,
          essence + b.essence,
          speed + b.speed,
          regen + b.regen,
          fire + b.fire,
          fall + b.fall);
    }
  }

  public record Trait(
      String id, String name, String description, Stats bonus, Predicate<SoulData> earned) {}

  private static Stats b(
      double hp,
      double armor,
      double power,
      double melee,
      double cd,
      double essence,
      double speed,
      double regen,
      double fire,
      double fall) {
    return new Stats(hp, armor, power, melee, cd, essence, speed, regen, fire, fall);
  }

  private static Trait t(
      String id, String name, String desc, Stats bonus, Predicate<SoulData> earned) {
    return new Trait(id, name, desc, bonus, earned);
  }

  public static final List<Trait> ALL =
      List.of(
          t(
              "ember_blood",
              "Ember Blood",
              "Flame affinity · 10% less fire damage",
              b(0, 0, 0, 0, 0, 0, 0, 0, .10, 0),
              d -> d.family == 0),
          t(
              "shadow_sense",
              "Shadow Sense",
              "Shadow affinity · +4% ability strength",
              b(0, 0, .04, 0, 0, 0, 0, 0, 0, 0),
              d -> d.family == 1),
          t(
              "tideborn",
              "Tideborn",
              "Tide affinity · +5% Essence capacity",
              b(0, 0, 0, 0, 0, .05, 0, 0, 0, 0),
              d -> d.family == 2),
          t(
              "steelbound",
              "Steelbound",
              "Steel affinity · +1 armor point",
              b(0, 1, 0, 0, 0, 0, 0, 0, 0, 0),
              d -> d.family == 3),
          t(
              "thorn_blessed",
              "Thorn Blessed",
              "Thorn affinity · +2 maximum health",
              b(2, 0, 0, 0, 0, 0, 0, 0, 0, 0),
              d -> d.family == 4),
          t(
              "mirror_mind",
              "Mirror Mind",
              "Mirror affinity · 4% shorter cooldowns",
              b(0, 0, 0, 0, .04, 0, 0, 0, 0, 0),
              d -> d.family == 5),
          t(
              "starkin",
              "Starkin",
              "Gravity affinity · 15% less fall damage",
              b(0, 0, 0, 0, 0, 0, 0, 0, 0, .15),
              d -> d.family == 6),
          t(
              "survivor",
              "Nightmare Survivor",
              "Complete initiation · +2 maximum health",
              b(2, 0, 0, 0, 0, 0, 0, 0, 0, 0),
              SoulData::initiated),
          t(
              "hunter",
              "Hunter",
              "25 creature kills · +4% melee damage",
              b(0, 0, 0, .04, 0, 0, 0, 0, 0, 0),
              d -> d.totalKills >= 25),
          t(
              "predator",
              "Predator",
              "100 creature kills · +4% ability strength",
              b(0, 0, .04, 0, 0, 0, 0, 0, 0, 0),
              d -> d.totalKills >= 100),
          t(
              "veteran",
              "Realm Veteran",
              "250 creature kills · +4% melee damage",
              b(0, 0, 0, .04, 0, 0, 0, 0, 0, 0),
              d -> d.totalKills >= 250),
          t(
              "ancient_slayer",
              "Ancient Slayer",
              "10 Fallen-or-higher kills · 3% shorter cooldowns",
              b(0, 0, 0, 0, .03, 0, 0, 0, 0, 0),
              d -> d.eliteKills >= 10),
          t(
              "ruler_breaker",
              "Ruler Breaker",
              "3 trial rulers defeated · +1 armor point",
              b(0, 1, 0, 0, 0, 0, 0, 0, 0, 0),
              d -> d.bossKills >= 3),
          t(
              "soul_attuned",
              "Soul Attuned",
              "250 fragments · +5% Essence capacity",
              b(0, 0, 0, 0, 0, .05, 0, 0, 0, 0),
              d -> d.fragments >= 250),
          t(
              "saturated",
              "Saturated Soul",
              "First core filled · +3% ability strength",
              b(0, 0, .03, 0, 0, 0, 0, 0, 0, 0),
              d -> d.fragments >= 1000),
          t(
              "core_forged",
              "Core Forged",
              "Form a second core · +2 maximum health",
              b(2, 0, 0, 0, 0, 0, 0, 0, 0, 0),
              d -> d.cores() >= 2),
          t(
              "wayfarer",
              "Wayfarer",
              "Visit 3 Dream biomes · +2% movement speed",
              b(0, 0, 0, 0, 0, 0, .02, 0, 0, 0),
              d -> d.visitedBiomes.size() >= 3),
          t(
              "explorer",
              "Realm Explorer",
              "Visit 8 Dream biomes · 3% shorter cooldowns",
              b(0, 0, 0, 0, .03, 0, 0, 0, 0, 0),
              d -> d.visitedBiomes.size() >= 8),
          t(
              "unyielding",
              "Unyielding",
              "Endure 100 damage after initiation · +2 maximum health",
              b(2, 0, 0, 0, 0, 0, 0, 0, 0, 0),
              d -> d.damageEndured >= 100),
          t(
              "awakened_soul",
              "Awakened Soul",
              "First Gateway return · +5% Essence recovery",
              b(0, 0, 0, 0, 0, 0, 0, .05, 0, 0),
              d -> d.soulRank >= 2),
          t(
              "ascendant",
              "Ascendant",
              "Become Master · +3% ability strength",
              b(0, 0, .03, 0, 0, 0, 0, 0, 0, 0),
              d -> d.soulRank >= 3),
          t(
              "transcendent",
              "Transcendent",
              "Become Saint · +2 maximum health",
              b(2, 0, 0, 0, 0, 0, 0, 0, 0, 0),
              d -> d.soulRank >= 4),
          t(
              "soul_bond",
              "Soul Bond",
              "Acquire an Echo · +5% Essence recovery",
              b(0, 0, 0, 0, 0, 0, 0, .05, 0, 0),
              d -> !d.echoes.isEmpty()));

  public static Trait get(String id) {
    return ALL.stream().filter(t -> t.id.equals(id)).findFirst().orElse(null);
  }

  public static Stats stats(SoulData d) {
    var result = b(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    for (String id : d.traits) {
      var t = get(id);
      if (t != null) result = result.plus(t.bonus);
    }
    return result;
  }

  public static boolean grant(ServerPlayer p, String id) {
    var t = get(id);
    var d = ShadowSlaveMod.soul(p);
    if (t == null || !d.traits.add(id)) return false;
    p.sendSystemMessage(
        Component.literal("[The Spell] Trait earned: " + t.name + " · " + t.description));
    return true;
  }

  public static void check(ServerPlayer p) {
    var d = ShadowSlaveMod.soul(p);
    if (!d.initiated()) return;
    if (com.shadowslave.world.DreamRealm.isDream(p.level()))
      p.level()
          .getBiome(p.blockPosition())
          .unwrapKey()
          .ifPresent(k -> d.visitedBiomes.add(k.location().toString()));
    boolean affinity =
        d.traits.stream().anyMatch(id -> ALL.subList(0, 7).stream().anyMatch(t -> t.id.equals(id)));
    for (int i = 0; i < ALL.size(); i++) {
      var t = ALL.get(i);
      if (i < 7 && affinity) continue;
      if (t.earned.test(d) && grant(p, t.id) && i < 7) affinity = true;
    }
  }
}
