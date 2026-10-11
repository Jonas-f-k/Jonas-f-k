package com.shadowslave.player;

import com.shadowslave.ShadowSlaveMod;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;

/** All combat slots consume the same progression curve; core formation never resets strength. */
public final class SoulGrowth {
  public static double ability(SoulData d) {
    return Math.pow(1.65, Math.max(0, d.soulRank - 1))
        * (1 + .20 * d.growth())
        * (1 + .07 * d.aspectRank)
        * (1 + Traits.stats(d).power());
  }

  public static int cooldown(SoulData d, int base) {
    return Math.max(
        20,
        (int)
            Math.round(
                base
                    * Math.max(
                        .35,
                        (1 - Traits.stats(d).cooldown())
                            / (1 + .15 * Math.max(0, d.soulRank - 1) + .10 * d.growth()))));
  }

  public static int duration(SoulData d, int base) {
    return (int) Math.round(base * (1 + .12 * Math.max(0, d.soulRank - 1) + .06 * d.growth()));
  }

  public static double reach(SoulData d) {
    return 1 + Math.min(1.2, .09 * Math.max(0, d.soulRank - 1) + .05 * d.growth());
  }

  public static int amplifier(SoulData d) {
    return Math.min(3, Math.max(0, d.soulRank - 1) + (int) (d.growth() * .3));
  }

  public static double health(SoulData d) {
    return d.initiated()
        ? 20 + 8 * Math.max(0, d.soulRank - 1) + 8 * d.growth() + Traits.stats(d).health()
        : 20;
  }

  public static double recovery(SoulData d) {
    return (1 + .65 * Math.max(0, d.soulRank - 1) + .30 * d.growth())
        * (1 + Traits.stats(d).regen());
  }

  public static void gain(ServerPlayer p, int fragments) {
    var d = ShadowSlaveMod.soul(p);
    d.fragments = (int) Math.min(2_000_000L, (long) d.fragments + Math.max(0, fragments));
    int before = d.formedCores;
    d.cores();
    if (d.formedCores > before)
      p.sendSystemMessage(
          net.minecraft.network.chat.Component.literal(
              "[The Spell] A new Soul Core is born. " + d.cores() + " / 7 cores."));
    Traits.check(p);
    apply(p);
  }

  public static void apply(ServerPlayer p) {
    var d = ShadowSlaveMod.soul(p);
    var t = Traits.stats(d);
    attribute(
        p,
        Attributes.MAX_HEALTH,
        "soul_health",
        health(d) - 20,
        AttributeModifier.Operation.ADD_VALUE);
    attribute(
        p,
        Attributes.ATTACK_DAMAGE,
        "soul_melee",
        d.initiated() ? .25 * Math.max(0, d.soulRank - 1) + .12 * d.growth() + t.melee() : 0,
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    attribute(
        p,
        Attributes.MOVEMENT_SPEED,
        "soul_speed",
        d.initiated()
            ? Math.min(.32, .025 * Math.max(0, d.soulRank - 1) + .015 * d.growth() + t.speed())
            : 0,
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    attribute(
        p,
        Attributes.ARMOR,
        "trait_armor",
        d.initiated() ? t.armor() : 0,
        AttributeModifier.Operation.ADD_VALUE);
    if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    d.essence = Math.min(d.essence, d.maxEssence());
  }

  public static void attribute(
      ServerPlayer p,
      Holder<Attribute> type,
      String name,
      double amount,
      AttributeModifier.Operation op) {
    var a = p.getAttribute(type);
    if (a == null) return;
    var id = ShadowSlaveMod.id(name);
    var old = a.getModifier(id);
    if (old != null && Math.abs(old.amount() - amount) < .00001) return;
    a.removeModifier(id);
    if (amount != 0) a.addTransientModifier(new AttributeModifier(id, amount, op));
  }
}
