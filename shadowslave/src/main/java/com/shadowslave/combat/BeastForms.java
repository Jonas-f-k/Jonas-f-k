package com.shadowslave.combat;

import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.entity.NightmareCreature;
import com.shadowslave.network.SpellNetwork;
import com.shadowslave.player.SoulGrowth;
import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** The player remains the damageable/control entity. No rider/proxy or duplicated inventory. */
public final class BeastForms {
  public record Form(String name, String attack, float width, float height, boolean flies) {
    public EntityDimensions dimensions() {
      return EntityDimensions.scalable(width, height).withEyeHeight(height * .72f);
    }
  }

  public static final List<Form> ALL =
      List.of(
          new Form("Cinder Dragon", "Cinder Breath", 3.4f, 3.8f, true),
          new Form("Dread Wolf", "Predator's Pounce", 2.8f, 2.4f, false),
          new Form("Tide Leviathan", "Tidal Maw", 3.2f, 2.6f, false),
          new Form("Iron Colossus", "Seismic Stomp", 3.0f, 5.6f, false),
          new Form("Worldroot Stag", "Rootbound Sanctuary", 3.0f, 3.9f, false),
          new Form("Mirror Griffin", "Prismatic Wingbeat", 3.8f, 3.4f, true),
          new Form("Astral Wyvern", "Gravity Roar", 3.4f, 4.2f, true));

  public static int active(Player p) {
    if (p == null || p.getUUID() == null || p.level() == null) return -1;
    if (p.level().isClientSide) return SpellNetwork.forms.getOrDefault(p.getUUID(), -1);
    if (!(p instanceof ServerPlayer sp) || sp.server == null) return -1;
    var d = ShadowSlaveMod.soul(sp);
    return d.soulRank >= 4
            && d.family >= 0
            && d.family < 7
            && d.transformUntil > sp.server.overworld().getGameTime()
        ? d.family
        : -1;
  }

  public static void transform(ServerPlayer p) {
    var d = ShadowSlaveMod.soul(p);
    long now = p.server.overworld().getGameTime();
    if (active(p) >= 0) {
      end(p);
      return;
    }
    if (d.soulRank < 4 || d.family < 0) {
      p.displayClientMessage(
          Component.literal("Conquer your Third Nightmare to unlock a beast Transformation."),
          true);
      return;
    }
    if (now < d.transformCooldownUntil) {
      p.displayClientMessage(
          Component.literal(
              "Transformation recovering: " + ((d.transformCooldownUntil - now + 19) / 20) + "s"),
          true);
      return;
    }
    if (p.isPassenger() || p.isSleeping()) {
      p.displayClientMessage(Component.literal("Stand in a clear space to transform."), true);
      return;
    }
    var f = ALL.get(d.family);
    var bounds = f.dimensions().scale(p.getScale()).makeBoundingBox(p.position());
    if (!p.level().noCollision(p, bounds)) {
      p.displayClientMessage(
          Component.literal(
              f.name
                  + " needs "
                  + f.width
                  + " blocks of width and "
                  + f.height
                  + " blocks of headroom."),
          true);
      return;
    }
    if (!d.spend(60)) {
      p.displayClientMessage(Component.literal("You need 60 Essence."), true);
      return;
    }
    int duration = SoulGrowth.duration(d, 500);
    d.transformUntil = now + duration;
    SpellNetwork.visual(p.serverLevel(),d.family,3,p.position(),p.getBbWidth()+2,36);
    d.transformCooldownUntil = d.transformUntil + SoulGrowth.cooldown(d, 400);
    tick(p);
    p.displayClientMessage(
        Component.literal(f.name + " · R: " + f.attack + (f.flies ? " · double Space to fly" : "")),
        true);
    SpellNetwork.sync(p);
  }

  public static void end(ServerPlayer p) {
    var d = ShadowSlaveMod.soul(p);
    d.transformUntil = 0;
    d.beastAttackUntil = 0;
    tick(p);
  }

  public static void tick(ServerPlayer p) {
    var d = ShadowSlaveMod.soul(p);
    long now = p.server.overworld().getGameTime();
    int form = active(p);
    if (form >= 0 && (p.isPassenger() || p.isSleeping() || p.tickCount % 20 == 0 && !d.spend(2))) {
      d.transformUntil = 0;
      form = -1;
    }
    boolean changed = d.activeBeast != form;
    for (var type :
        List.of(
            Attributes.SCALE,
            Attributes.ARMOR,
            Attributes.ATTACK_DAMAGE,
            Attributes.KNOCKBACK_RESISTANCE)) {
      var a = p.getAttribute(type);
      if (a != null)
        for (String name :
            List.of("avatar_scale", "avatar_armor", "avatar_damage", "avatar_stability"))
          if (a.hasModifier(ShadowSlaveMod.id(name))) a.removeModifier(ShadowSlaveMod.id(name));
    }
    if (changed
        && form >= 0
        && !p.level()
            .noCollision(
                p, ALL.get(form).dimensions().scale(p.getScale()).makeBoundingBox(p.position()))) {
      d.transformUntil = 0;
      form = -1;
      changed = d.activeBeast != form;
    }
    boolean enabled = form >= 0;
    double growth = d.growth();
    SoulGrowth.attribute(
        p,
        Attributes.ARMOR,
        "beast_armor",
        enabled ? (form == 3 ? 16 : 8) + growth * .5 : 0,
        AttributeModifier.Operation.ADD_VALUE);
    SoulGrowth.attribute(
        p,
        Attributes.MAX_HEALTH,
        "beast_health",
        enabled ? 14 + growth * 4 : 0,
        AttributeModifier.Operation.ADD_VALUE);
    SoulGrowth.attribute(
        p,
        Attributes.ATTACK_DAMAGE,
        "beast_claws",
        enabled ? (form == 3 ? 15 : 10) * (1 + .10 * growth) : 0,
        AttributeModifier.Operation.ADD_VALUE);
    SoulGrowth.attribute(
        p,
        Attributes.KNOCKBACK_RESISTANCE,
        "beast_stability",
        enabled ? (form == 3 ? .85 : .5) : 0,
        AttributeModifier.Operation.ADD_VALUE);
    SoulGrowth.attribute(
        p,
        Attributes.MOVEMENT_SPEED,
        "beast_stride",
        enabled ? (form == 1 ? .35 : form == 3 ? -.12 : form == 4 ? .18 : .10) : 0,
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    boolean flying = enabled && ALL.get(form).flies;
    if (flying && !p.getAbilities().mayfly) {
      d.formGrantedFlight = true;
      p.getAbilities().mayfly = true;
      p.onUpdateAbilities();
    }
    if (!flying && d.formGrantedFlight) {
      d.formGrantedFlight = false;
      p.getAbilities().mayfly = p.isCreative() || p.isSpectator();
      if (!p.getAbilities().mayfly) p.getAbilities().flying = false;
      p.onUpdateAbilities();
      p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80));
    }
    if (changed) {
      d.activeBeast = form;
      p.refreshDimensions();
      SpellNetwork.sync(p);
    }
    if (!enabled) return;
    if (p.tickCount % 20 == 0)
      switch (form) {
        case 0 -> p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 45));
        case 1 -> p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240));
        case 2 -> {
          p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 45));
          p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 45, 2));
        }
        case 3 -> p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 45, 1));
        case 4 -> p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 45));
        case 5, 6 -> p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 45));
      }
    if (d.beastAttackUntil > now && form == 0 && p.tickCount % 4 == 0) {
      double range = 18 * SoulGrowth.reach(d);
      var look = p.getLookAngle();
      for (var e : enemies(p, range)) {
        var delta = e.getBoundingBox().getCenter().subtract(p.getEyePosition());
        if (delta.normalize().dot(look) > .88) {
          e.hurt(p.damageSources().playerAttack(p), (float) (2.6 * SoulGrowth.ability(d)));
          e.igniteForSeconds(7);
        }
      }
      for (int i = 1; i <= range; i++) {
        var a = p.getEyePosition().add(look.scale(i));
        var pos = net.minecraft.core.BlockPos.containing(a);
        if (!p.level().hasChunkAt(pos) || !p.level().getBlockState(pos).isAir()) break;
        p.serverLevel()
            .sendParticles(ParticleTypes.SOUL_FIRE_FLAME, a.x, a.y, a.z, 3, .14, .14, .14, .01);
      }
    }
  }

  private static List<Monster> enemies(ServerPlayer p, double range) {
    return p.serverLevel()
        .getEntitiesOfClass(
            Monster.class,
            p.getBoundingBox().inflate(range),
            e ->
                e.isAlive()
                    && !(e instanceof NightmareCreature n && n.isEcho())
                    && p.hasLineOfSight(e)
                    && e.distanceToSqr(p) <= range * range);
  }

  public static void attack(ServerPlayer p) {
    var d = ShadowSlaveMod.soul(p);
    int form = active(p);
    if (form < 0) return;
    long now = p.server.overworld().getGameTime();
    if (now < d.beastCooldownUntil) {
      p.displayClientMessage(Component.literal("Beast attack recovering."), true);
      return;
    }
    if (!d.spend(20)) {
      p.displayClientMessage(Component.literal("You need 20 Essence."), true);
      return;
    }
    d.beastCooldownUntil = now + SoulGrowth.cooldown(d, 160);
    SpellNetwork.visual(p.serverLevel(),d.family,4,p.position(),4*SoulGrowth.reach(d),30);
    double power = 12 * SoulGrowth.ability(d), range = 9 * SoulGrowth.reach(d);
    var targets = enemies(p, range);
    switch (form) {
      case 0 -> d.beastAttackUntil = now + 40;
      case 1 -> {
        Vec3 from = p.position(),
            direction = p.getLookAngle().multiply(1, 0, 1).normalize(),
            to = from;
        for (double step = .5; step <= range; step += .5) {
          var candidate = from.add(direction.scale(step));
          var pos = net.minecraft.core.BlockPos.containing(candidate);
          if (!p.level().hasChunkAt(pos)
              || !p.level().noCollision(p, p.getBoundingBox().move(candidate.subtract(from))))
            break;
          if (p.level().getBlockState(pos.below()).isSolid()) to = candidate;
        }
        for (var e : targets)
          if (e.position().subtract(from).normalize().dot(direction) > .65) {
            e.hurt(p.damageSources().playerAttack(p), (float) power);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
          }
        p.teleportTo(to.x, to.y, to.z);
        p.serverLevel()
            .sendParticles(ParticleTypes.SQUID_INK, from.x, from.y + 1, from.z, 70, 2, 1, 2, .02);
      }
      case 2 -> {
        for (var e : targets) {
          var pull = p.position().subtract(e.position()).normalize();
          e.push(pull.x * 1.3, .35, pull.z * 1.3);
          e.hurt(p.damageSources().playerAttack(p), (float) (power * (p.isInWater() ? 1.3 : .8)));
          e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 2));
        }
        p.heal((float) (4 * Math.sqrt(SoulGrowth.ability(d))));
        p.serverLevel()
            .sendParticles(
                ParticleTypes.BUBBLE,
                p.getX(),
                p.getY() + 1,
                p.getZ(),
                110,
                range / 2,
                1,
                range / 2,
                .02);
      }
      case 3 -> {
        for (var e : targets) {
          e.hurt(p.damageSources().playerAttack(p), (float) (power * 1.25));
          e.knockback(2.2, p.getX() - e.getX(), p.getZ() - e.getZ());
          e.push(0, .5, 0);
          e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 2));
        }
        p.serverLevel()
            .sendParticles(
                ParticleTypes.CRIT,
                p.getX(),
                p.getY() + .2,
                p.getZ(),
                160,
                range / 2,
                .15,
                range / 2,
                .15);
      }
      case 4 -> {
        for (var e : targets) {
          e.hurt(p.damageSources().playerAttack(p), (float) (power * .65));
          e.addEffect(
              new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SoulGrowth.duration(d, 160), 6));
          e.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 2));
        }
        for (var ally :
            p.serverLevel()
                .getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(range / 2)))
          if (ally != p) ally.heal((float) (5 * Math.sqrt(SoulGrowth.ability(d))));
        p.heal((float) (5 * Math.sqrt(SoulGrowth.ability(d))));
        p.serverLevel()
            .sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                p.getX(),
                p.getY() + 1,
                p.getZ(),
                130,
                range / 2,
                1,
                range / 2,
                .01);
      }
      case 5 -> {
        for (var e : targets) {
          e.hurt(p.damageSources().playerAttack(p), (float) power);
          e.knockback(2.5, p.getX() - e.getX(), p.getZ() - e.getZ());
          e.setTarget(null);
          e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120));
        }
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 180, 2));
        p.serverLevel()
            .sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                p.getX(),
                p.getY() + 2,
                p.getZ(),
                140,
                range / 2,
                2,
                range / 2,
                .03);
      }
      case 6 -> {
        for (var e : targets) {
          e.hurt(p.damageSources().playerAttack(p), (float) power);
          var pull = p.position().subtract(e.position()).normalize();
          e.push(pull.x, .9, pull.z);
          e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 70, 2));
        }
        p.serverLevel()
            .sendParticles(
                ParticleTypes.END_ROD,
                p.getX(),
                p.getY() + 3,
                p.getZ(),
                130,
                range / 2,
                3,
                range / 2,
                .02);
      }
    }
    p.displayClientMessage(Component.literal(ALL.get(form).attack), true);
    SpellNetwork.sync(p);
  }
}
