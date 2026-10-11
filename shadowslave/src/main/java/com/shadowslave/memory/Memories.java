package com.shadowslave.memory;

import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.content.Content;
import com.shadowslave.core.Rules;
import com.shadowslave.player.SoulData;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;

public final class Memories {
  public static int rank(SoulData d, String id) {
    var m = Content.memories.get(id);
    return d.memoryRanks.getOrDefault(id, m == null ? 0 : m.rank());
  }

  public static int tier(SoulData d, String id) {
    return d.memoryTiers.getOrDefault(id, 1);
  }

  public static double potency(SoulData d, String id) {
    var m = Content.memories.get(id);
    return Math.max(
        .5, 1 + (rank(d, id) - (m == null ? 0 : m.rank())) * .3 + (tier(d, id) - 1) * .12);
  }

  public static float weaponPower(SoulData d, Content.Memory m) {
    return (float) (m.power() * potency(d, m.id()));
  }

  public static String quality(SoulData d, String id) {
    return Rules.RANKS[rank(d, id)] + " · Tier " + tier(d, id);
  }

  public static void grant(ServerPlayer p, String id, int rank, int tier) {
    var d = ShadowSlaveMod.soul(p);
    if (!Content.memories.containsKey(id)) return;
    if (!d.owned.contains(id)) {
      d.owned.add(id);
      d.memoryRanks.put(id, Math.clamp(rank, 0, 6));
      d.memoryTiers.put(id, Math.clamp(tier, 1, 7));
      p.sendSystemMessage(
          Component.literal(
              "[The Spell] Memory received: "
                  + Content.memories.get(id).name()
                  + " ["
                  + quality(d, id)
                  + "]"));
    } else p.spawnAtLocation(MemoryTokenItem.create(id, rank, tier));
  }

  /**
   * Physical armor uses Minecraft's real armor calculation and HUD. Specialty pieces replace their
   * quarter.
   */
  public static double armorPoints(SoulData d) {
    double full = 0;
    Map<String, Double> parts = new HashMap<>();
    for (var m : active(d)) {
      double value = Math.min(32, (8 + rank(d, m.id()) * 4 + (tier(d, m.id()) - 1) * 1.2));
      if (m.type().equals("armor")) full = value;
      else if (List.of("head", "chest", "legs", "feet").contains(m.type()))
        parts.put(m.type(), value);
    }
    double total = 0;
    for (String part : List.of("head", "chest", "legs", "feet"))
      total += parts.getOrDefault(part, full) / 4;
    return total;
  }

  public static void updateArmor(ServerPlayer p) {
    var d = ShadowSlaveMod.soul(p);
    var armor = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
    var toughness =
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
    var id = ShadowSlaveMod.id("memory_armor");
    var toughId = ShadowSlaveMod.id("memory_toughness");
    double points = armorPoints(d);
    double delta = points;
    if (active(d).stream().anyMatch(m -> m.type().equals("armor")))
      for (var stack : p.getArmorSlots())
        if (stack.getItem() instanceof ArmorItem a) delta -= a.getDefense();
    applyAttribute(armor, id, delta);
    applyAttribute(toughness, toughId, points > 0 ? Math.min(8, points / 6) : 0);
  }

  private static void applyAttribute(
      net.minecraft.world.entity.ai.attributes.AttributeInstance a,
      net.minecraft.resources.ResourceLocation id,
      double value) {
    if (a == null) return;
    var old = a.getModifier(id);
    if (old != null && old.amount() == value) return;
    a.removeModifier(id);
    if (value != 0)
      a.addTransientModifier(
          new net.minecraft.world.entity.ai.attributes.AttributeModifier(
              id,
              value,
              net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
  }

  public static boolean equip(SoulData d, String id) {
    if (d.equipped.contains(id)) {
      d.equipped.remove(id);
      return true;
    }
    var def = Content.memories.get(id);
    if (!d.initiated() || def == null || !d.owned.contains(id)) return false;
    var types =
        d.equipped.stream()
            .map(Content.memories::get)
            .filter(Objects::nonNull)
            .map(Content.Memory::type)
            .toList();
    if (!Rules.canEquip(types, def.type())) return false;
    d.equipped.add(id);
    return true;
  }

  public static List<Content.Memory> active(SoulData d) {
    if (!d.activeMemories || !d.initiated()) return List.of();
    return d.equipped.stream().map(Content.memories::get).filter(Objects::nonNull).toList();
  }

  public static boolean has(SoulData d, String effect) {
    return active(d).stream().anyMatch(m -> effect.equals(m.effect()));
  }

  public static double protection(SoulData d, boolean magic) {
    double base = 0;
    Map<String, Double> overrides = new HashMap<>();
    for (var m : active(d)) {
      double amount = (magic ? m.magic() : m.physical()) * potency(d, m.id());
      if (m.type().equals("armor")) base = amount;
      else if (List.of("head", "chest", "legs", "feet").contains(m.type()))
        overrides.put(m.type(), amount);
    }
    double value = Rules.protection(base, overrides);
    if (magic && has(d, "ward")) value += .05;
    return Math.min(.75, value);
  }

  public static void grant(ServerPlayer p, String id) {
    var d = ShadowSlaveMod.soul(p);
    if (Content.memories.containsKey(id) && !d.owned.contains(id)) {
      d.owned.add(id);
      p.sendSystemMessage(
          Component.literal(
              "[The Spell] You have received a Memory: " + Content.memories.get(id).name()));
    }
  }

  public static void manifest(ServerPlayer p, String id) {
    var d = ShadowSlaveMod.soul(p);
    var def = Content.memories.get(id);
    if (!d.initiated()
        || !d.activeMemories
        || !d.equipped.contains(id)
        || def == null
        || !def.type().equals("weapon")) return;
    // Never overwrite a real held item; require a free selected slot or storage slot.
    if (!p.getMainHandItem().isEmpty()
        && !p.getMainHandItem().is(ShadowSlaveMod.MANIFESTED_BLADE.get())
        && p.getInventory().getFreeSlot() < 0) {
      p.displayClientMessage(Component.literal("Free an inventory slot before summoning."), true);
      return;
    }
    int cost = 5 * Math.max(1, rank(d, id) - d.soulRank + 2);
    if (!d.spend(cost)) {
      p.displayClientMessage(Component.literal("You need " + cost + " Essence."), true);
      return;
    }
    removeManifested(p);
    var held = p.getMainHandItem().copy();
    p.getInventory().setItem(p.getInventory().selected, ItemStack.EMPTY);
    if (!held.isEmpty()) p.getInventory().add(held);
    var blade = new ItemStack(ShadowSlaveMod.MANIFESTED_BLADE.get());
    var tag = new CompoundTag();
    tag.putString("memory", id);
    tag.putUUID("owner", p.getUUID());
    blade.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    blade.set(DataComponents.CUSTOM_NAME, Component.literal(def.name()));
    blade.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
    p.getInventory().setItem(p.getInventory().selected, blade);
  }

  public static boolean validBlade(ServerPlayer p, ItemStack stack) {
    var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    var d = ShadowSlaveMod.soul(p);
    return stack.is(ShadowSlaveMod.MANIFESTED_BLADE.get())
        && tag.hasUUID("owner")
        && tag.getUUID("owner").equals(p.getUUID())
        && d.activeMemories
        && d.equipped.contains(tag.getString("memory"))
        && d.owned.contains(tag.getString("memory"));
  }

  public static void removeManifested(ServerPlayer p) {
    for (int i = 0; i < p.getInventory().getContainerSize(); i++)
      if (p.getInventory().getItem(i).is(ShadowSlaveMod.MANIFESTED_BLADE.get()))
        p.getInventory().setItem(i, ItemStack.EMPTY);
  }

  public static void crystallize(ServerPlayer p, String id) {
    var d = ShadowSlaveMod.soul(p);
    if (!d.initiated()
        || !d.owned.contains(id)
        || d.equipped.contains(id)
        || !Content.memories.containsKey(id)
        || p.getInventory().getFreeSlot() < 0) return;
    var token = MemoryTokenItem.create(id, rank(d, id), tier(d, id));
    d.owned.remove(id);
    d.memoryRanks.remove(id);
    d.memoryTiers.remove(id);
    p.getInventory().add(token);
  }
}
