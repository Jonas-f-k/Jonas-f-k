package com.shadowslave.combat;

import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.entity.NightmareCreature;
import com.shadowslave.memory.Memories;
import com.shadowslave.network.SpellNetwork;
import com.shadowslave.player.SoulGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;

public final class Aspects {
  private static boolean hostile(Monster e) {
    return !(e instanceof NightmareCreature n && n.isEcho());
  }

  public static final String[] DORMANT = {
    "Ember Burst",
    "Shadow Mantle",
    "Tide Sight",
    "Iron Resolve",
    "Life Bloom",
    "Veil of Reflections",
    "Weightless Touch"
  };
  public static final String[] AWAKENED = {
    "Whitefire Lance",
    "Shadow Step",
    "Undertow",
    "Siegebreaker",
    "Briar Prison",
    "Looking Glass",
    "Gravitic Collapse"
  };

  public static boolean canUseAwakened(com.shadowslave.player.SoulData d) {
    return d.soulRank >= 2 && d.family >= 0;
  }

  public static void castAwakened(ServerPlayer p) {
    var d = ShadowSlaveMod.soul(p);
    long now = p.server.overworld().getGameTime();
    if (!canUseAwakened(d)) {
      p.displayClientMessage(
          Component.literal(
              "Return through a Dream Realm Gateway to unlock your Awakened ability."),
          true);
      return;
    }
    if (now < d.awakenedCooldownUntil) {
      p.displayClientMessage(
          Component.literal(
              "Awakened ability recovering: " + ((d.awakenedCooldownUntil - now + 19) / 20) + "s"),
          true);
      return;
    }
    var level = p.serverLevel();
    int cost = 30;
    double power = 8 * SoulGrowth.ability(d), reach = SoulGrowth.reach(d);
    int amp = SoulGrowth.amplifier(d);
    var targets =
        level.getEntitiesOfClass(
            Monster.class,
            p.getBoundingBox().inflate(14 * reach),
            e -> e.isAlive() && hostile(e) && p.hasLineOfSight(e));
    if (d.family == 5 && targets.isEmpty()) {
      p.displayClientMessage(Component.literal("Look toward a creature within 14 blocks."), true);
      return;
    }
    if (!d.spend(cost)) {
      p.displayClientMessage(Component.literal("You need 30 Essence."), true);
      return;
    }
    d.awakenedCooldownUntil = now + SoulGrowth.cooldown(d, 240);
    SpellNetwork.visual(level,d.family,1,p.position(),4*SoulGrowth.reach(d),28);
    switch (d.family) {
      case 0 -> {
        var look = p.getLookAngle();
        for (var e : targets) {
          var delta = e.getBoundingBox().getCenter().subtract(p.getEyePosition());
          if (delta.normalize().dot(look) > .88) {
            e.hurt(p.damageSources().playerAttack(p), (float) power);
            e.igniteForSeconds(7);
          }
        }
        for (int i = 1; i <= 14 * reach; i++) {
          var a = p.getEyePosition().add(look.scale(i));
          if (!level.getBlockState(BlockPos.containing(a)).isAir()) break;
          level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, a.x, a.y, a.z, 5, .12, .12, .12, 0);
        }
        p.heal((float) (3 * Math.sqrt(SoulGrowth.ability(d))));
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, SoulGrowth.duration(d, 180)));
      }
      case 1 -> {
        Vec3 direction = p.getLookAngle().multiply(1, 0, 1).normalize(),
            start = p.position(),
            target = start;
        for (double distance = .5; distance <= (12 + d.aspectRank) * reach; distance += .5) {
          var candidate = start.add(direction.scale(distance));
          var bp = BlockPos.containing(candidate);
          if (!level.hasChunkAt(bp)
              || !level.noCollision(p, p.getBoundingBox().move(candidate.subtract(start)))) break;
          if (level.getBlockState(bp.below()).isSolid()) target = candidate;
        }
        p.teleportTo(target.x, target.y, target.z);
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, SoulGrowth.duration(d, 100)));
        p.addEffect(
            new MobEffectInstance(MobEffects.DAMAGE_BOOST, SoulGrowth.duration(d, 100), amp));
        level.sendParticles(ParticleTypes.SMOKE, start.x, start.y + 1, start.z, 50, .5, 1, .5, .04);
      }
      case 2 -> {
        for (var e : targets) {
          var pull = p.position().subtract(e.position()).normalize().scale(.8 * reach);
          e.push(pull.x, .18, pull.z);
          e.hurt(p.damageSources().playerAttack(p), (float) power * .6f);
          e.addEffect(
              new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SoulGrowth.duration(d, 160), 2));
        }
        p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, SoulGrowth.duration(d, 400)));
      }
      case 3 -> {
        for (var e : targets)
          if (e.distanceToSqr(p) < 49 * reach * reach) {
            e.hurt(p.damageSources().playerAttack(p), (float) power);
            e.knockback(1.8 * reach, p.getX() - e.getX(), p.getZ() - e.getZ());
          }
        p.addEffect(
            new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE,
                SoulGrowth.duration(d, 160),
                Math.min(3, 1 + amp / 2)));
        level.sendParticles(
            ParticleTypes.CRIT, p.getX(), p.getY() + .4, p.getZ(), 90, 4, .5, 4, .1);
      }
      case 4 -> {
        for (var e : targets) {
          e.addEffect(
              new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SoulGrowth.duration(d, 140), 5));
          e.addEffect(
              new MobEffectInstance(
                  MobEffects.POISON, SoulGrowth.duration(d, 160), Math.min(3, amp)));
          e.hurt(p.damageSources().playerAttack(p), (float) power * .4f);
          level.sendParticles(
              ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + 1, e.getZ(), 18, .5, .7, .5, 0);
        }
        p.heal((float) (6 * Math.sqrt(SoulGrowth.ability(d))));
      }
      case 5 -> {
        var look = p.getLookAngle();
        var target =
            targets.stream()
                .filter(e -> e.position().subtract(p.position()).normalize().dot(look) > .75)
                .min(java.util.Comparator.comparingDouble(p::distanceToSqr))
                .orElse(null);
        if (target == null) {
          d.refill(cost);
          d.awakenedCooldownUntil = now;
          p.displayClientMessage(Component.literal("Face a creature to exchange positions."), true);
          return;
        }
        var from = p.position();
        var to = target.position();
        if (level.noCollision(p, p.getBoundingBox().move(to.subtract(from)))
            && level.noCollision(target, target.getBoundingBox().move(from.subtract(to)))) {
          target.teleportTo(from.x, from.y, from.z);
          p.teleportTo(to.x, to.y, to.z);
          target.addEffect(
              new MobEffectInstance(
                  MobEffects.WEAKNESS, SoulGrowth.duration(d, 180), Math.min(3, 1 + amp)));
          target.hurt(p.damageSources().playerAttack(p), (float) power * .5f);
          target.setTarget(null);
        } else {
          d.refill(cost);
          d.awakenedCooldownUntil = now;
          return;
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, to.x, to.y + 1, to.z, 80, 1, 1, 1, .1);
      }
      case 6 -> {
        for (var e : targets) {
          var delta = p.position().subtract(e.position()).normalize();
          e.push(delta.x * .7 * reach, .65, delta.z * .7 * reach);
          e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, SoulGrowth.duration(d, 50), 1));
          e.hurt(p.damageSources().playerAttack(p), (float) power * .8f);
        }
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SoulGrowth.duration(d, 240)));
        level.sendParticles(
            ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 100, 5, 1, 5, .1);
      }
    }
    p.displayClientMessage(Component.literal(AWAKENED[d.family]), true);
    SpellNetwork.sync(p);
  }

  public static void cast(ServerPlayer p) {
    var d = ShadowSlaveMod.soul(p);
    long now = p.server.overworld().getGameTime();
    if (!d.initiated() || d.family < 0) return;
    if (BeastForms.active(p) >= 0) {
      BeastForms.attack(p);
      return;
    }
    if (now < d.cooldownUntil) {
      p.displayClientMessage(
          Component.literal("Aspect recovering: " + ((d.cooldownUntil - now + 19) / 20) + "s"),
          true);
      return;
    }
    int cost = d.soulRank >= 2 ? 20 : 0;
    if (!d.spend(cost)) {
      p.displayClientMessage(Component.literal("You need 20 Essence."), true);
      return;
    }
    d.cooldownUntil = now + SoulGrowth.cooldown(d, d.soulRank >= 2 ? 140 : 280);
    SpellNetwork.visual(p.serverLevel(),d.family,0,p.position(),3*SoulGrowth.reach(d),22);
    double power = 5 * SoulGrowth.ability(d), reach = SoulGrowth.reach(d);
    int amp = SoulGrowth.amplifier(d);
    var level = p.serverLevel();
    switch (d.family) {
      case 0 -> {
        for (var e :
            level.getEntitiesOfClass(
                Monster.class,
                p.getBoundingBox().inflate(6 * reach),
                e -> hostile(e) && p.hasLineOfSight(e))) {
          e.hurt(p.damageSources().playerAttack(p), (float) power);
          e.igniteForSeconds(4 + amp);
        }
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, SoulGrowth.duration(d, 100)));
        level.sendParticles(
            ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 60, 2, .6, 2, .1);
      }
      case 1 -> {
        p.addEffect(
            new MobEffectInstance(MobEffects.DAMAGE_BOOST, SoulGrowth.duration(d, 160), amp));
        p.addEffect(
            new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED, SoulGrowth.duration(d, 160), Math.min(2, amp)));
        level.sendParticles(
            ParticleTypes.SMOKE, p.getX(), p.getY() + 1, p.getZ(), 40, .5, 1, .5, .01);
      }
      case 2 -> {
        for (var e :
            level.getEntitiesOfClass(
                Monster.class, p.getBoundingBox().inflate(18 * reach), Aspects::hostile)) {
          e.addEffect(new MobEffectInstance(MobEffects.GLOWING, SoulGrowth.duration(d, 160)));
          e.addEffect(
              new MobEffectInstance(
                  MobEffects.MOVEMENT_SLOWDOWN,
                  SoulGrowth.duration(d, 100),
                  Math.min(3, 1 + amp / 2)));
        }
        p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, SoulGrowth.duration(d, 400)));
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, SoulGrowth.duration(d, 400)));
        level.sendParticles(
            ParticleTypes.BUBBLE, p.getX(), p.getY() + 1, p.getZ(), 40, 1, 1, 1, .03);
      }
      case 3 -> {
        p.addEffect(
            new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE,
                SoulGrowth.duration(d, 100),
                Math.min(3, 1 + amp / 2)));
        p.addEffect(
            new MobEffectInstance(
                MobEffects.ABSORPTION,
                SoulGrowth.duration(d, 140),
                (d.aspectRank >= 4 ? 1 : 0) + amp));
        level.sendParticles(
            ParticleTypes.ENCHANTED_HIT, p.getX(), p.getY() + 1, p.getZ(), 35, .7, 1, .7, .03);
      }
      case 4 -> {
        p.heal((float) (4 * Math.sqrt(SoulGrowth.ability(d))));
        for (var e :
            level.getEntitiesOfClass(
                Monster.class, p.getBoundingBox().inflate(5 * reach), Aspects::hostile))
          e.addEffect(new MobEffectInstance(MobEffects.POISON, SoulGrowth.duration(d, 100), amp));
        level.sendParticles(
            ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 35, 1, .7, 1, .01);
      }
      case 5 -> {
        for (var e :
            level.getEntitiesOfClass(
                Monster.class, p.getBoundingBox().inflate(8 * reach), Aspects::hostile)) {
          e.setTarget(null);
          e.addEffect(
              new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SoulGrowth.duration(d, 100), 3));
          e.addEffect(
              new MobEffectInstance(
                  MobEffects.WEAKNESS, SoulGrowth.duration(d, 100), Math.min(3, 1 + amp / 2)));
        }
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, SoulGrowth.duration(d, 100)));
        level.sendParticles(
            ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 60, 1, 1, 1, .1);
      }
      case 6 -> {
        for (var e :
            level.getEntitiesOfClass(
                Monster.class,
                p.getBoundingBox().inflate(7 * reach),
                e -> hostile(e) && p.hasLineOfSight(e))) {
          e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, SoulGrowth.duration(d, 40)));
          e.hurt(p.damageSources().playerAttack(p), (float) power * .7f);
        }
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SoulGrowth.duration(d, 160)));
        level.sendParticles(
            ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 70, 2, .6, 2, .1);
      }
    }
    p.displayClientMessage(Component.literal(DORMANT[d.family]), true);
    SpellNetwork.sync(p);
  }

  public static void passive(ServerPlayer p) {
    var d = ShadowSlaveMod.soul(p);
    if (!d.initiated()) return;
    if (d.family == 0 && p.isInWaterRainOrBubble())
      p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50));
    if (d.family == 1 && p.level().isDay() && p.level().canSeeSky(p.blockPosition()))
      p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50));
    if (d.family == 2 && p.isOnFire()) d.refill(-2);
    if (d.family == 3)
      p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0, false, false));
    if (d.family == 4 && p.tickCount % 100 == 0) p.causeFoodExhaustion(.8f);
    if (d.family == 5 && p.server.overworld().getGameTime() - d.lastCombatTick > 100)
      p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false));
    if (d.family == 6 && p.getY() < 48) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50));
    if (Memories.has(d, "speed"))
      p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 0, false, false));
    if (Memories.has(d, "vision"))
      p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false));
    if (Memories.has(d, "breathing"))
      p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 50, 0, false, false));
    if (Memories.has(d, "fire"))
      p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 50, 0, false, false));
    if (Memories.has(d, "feather"))
      p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 50, 0, false, false));
  }
}
