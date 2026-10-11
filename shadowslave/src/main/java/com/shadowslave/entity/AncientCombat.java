package com.shadowslave.entity;

import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.world.Sanctuary;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Windup, locked aim and recovery distinguish creatures; special attacks allow a dodge window. */
public final class AncientCombat {
  public static void tick(NightmareCreature mob, ServerLevel level) {
    var species = mob.species();
    if (species == null || mob.isNoAi() || mob.isEcho()) return;
    if (mob.specialFollowThrough > 0) {
      mob.specialFollowThrough--;
      if (mob.tickCount % 2 == 0)
        for (var e : targets(mob, 4 + mob.getBbWidth() / 2))
          if (mob.specialHits.add(e.getUUID())) {
            physical(mob, e, 1.5f);
            e.knockback(1.1, mob.getX() - e.getX(), mob.getZ() - e.getZ());
          }
    }
    var target = mob.getTarget();
    if (target == null || !target.isAlive() || Sanctuary.isSafe(level, target.blockPosition())) {
      mob.specialWindup = 0;
      mob.setCharging(false);
      return;
    }
    if (mob.specialWindup > 0) {
      mob.getNavigation().stop();
      mob.specialWindup--;
      mob.setCharging(true);
      if (mob.specialWindup == 0) {
        release(mob, level);
        mob.specialCooldown = Math.max(60, 170 - species.rank() * 15 - species.cores() * 6);
        mob.setCharging(false);
      }
      return;
    }
    if (mob.specialCooldown > 0) {
      mob.specialCooldown--;
      return;
    }
    if (!mob.hasLineOfSight(target) || mob.distanceToSqr(target) > 576) return;
    mob.specialTarget = species.body() == 10 ? mob.position() : target.position();
    mob.specialWindup = species.body() == 10 || species.body() == 11 ? 36 : 26;
    mob.specialHits.clear();
    com.shadowslave.network.SpellNetwork.visual(level,mob.family(),5,mob.specialTarget,species.body()==10?7:species.body()==11?9:3.5,mob.specialWindup);
    mob.setCharging(true);
    mob.playSound(
        net.minecraft.sounds.SoundEvents.WARDEN_SONIC_CHARGE, .3f, .55f + species.body() * .03f);
  }

  private static java.util.List<LivingEntity> targets(NightmareCreature mob, double range) {
    return mob.level()
        .getEntitiesOfClass(
            LivingEntity.class,
            mob.getBoundingBox().inflate(range),
            e ->
                e != mob
                    && e.isAlive()
                    && mob.canAttack(e)
                    && !(e instanceof NightmareCreature n && !n.isEcho())
                    && (e == mob.getTarget() || e instanceof ServerPlayer)
                    && !Sanctuary.isSafe(e.level(), e.blockPosition())
                    && mob.hasLineOfSight(e));
  }

  private static void physical(NightmareCreature mob, LivingEntity target, float factor) {
    if (target.hurt(
        mob.damageSources().mobAttack(mob),
        (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * factor)) {
      var s = mob.species();
      if (s != null && s.behavior().equals("fire")) target.igniteForSeconds(4);
      if (s != null && s.behavior().equals("frost"))
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
    }
  }

  private static void magic(NightmareCreature mob, LivingEntity target, float amount) {
    target.hurt(
        new DamageSource(
            mob.level()
                .registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getHolderOrThrow(NightmareCreature.NIGHTMARE_DAMAGE),
            mob),
        amount);
  }

  private static void release(NightmareCreature mob, ServerLevel level) {
    var s = mob.species();
    double radius = s.body() == 10 ? 7 : s.body() == 11 ? 9 : 3.5;
    switch (s.body()) {
      case 3, 8 -> {
        Vec3 delta = mob.specialTarget.subtract(mob.position()).normalize();
        mob.setDeltaMovement(delta.x * 1.35, .45, delta.z * 1.35);
        mob.hasImpulse = true;
        mob.specialFollowThrough = 12;
      }
      case 1 -> {
        for (var e : targets(mob, mob.getBbWidth() + 3))
          if (e.position()
                  .subtract(mob.position())
                  .normalize()
                  .dot(mob.specialTarget.subtract(mob.position()).normalize())
              > .30) {
            physical(mob, e, 1.9f);
            e.knockback(1.4, mob.getX() - e.getX(), mob.getZ() - e.getZ());
          }
        level.sendParticles(
            ParticleTypes.CRIT,
            mob.getX(),
            mob.getY() + 1,
            mob.getZ(),
            50,
            mob.getBbWidth(),
            .7,
            mob.getBbWidth(),
            .08);
      }
      case 10 -> {
        for (var e : targets(mob, 10))
          if (e.position().distanceToSqr(mob.specialTarget) < radius * radius) {
            physical(mob, e, 1.9f);
            e.push(0, .7, 0);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
          }
        level.sendParticles(
            ParticleTypes.CRIT,
            mob.specialTarget.x,
            mob.specialTarget.y + .2,
            mob.specialTarget.z,
            120,
            radius,
            .2,
            radius,
            .12);
      }
      case 2, 9 -> {
        for (var e : targets(mob, 24))
          if (e.position().distanceToSqr(mob.specialTarget) < radius * radius) {
            magic(mob, e, 4 + s.rank() * 3);
            e.addEffect(
                new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, s.body() == 2 ? 4 : 2));
            if (s.body() == 9) e.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
          }
        level.sendParticles(
            ParticleTypes.SPORE_BLOSSOM_AIR,
            mob.specialTarget.x,
            mob.specialTarget.y + 1,
            mob.specialTarget.z,
            60,
            radius,
            1,
            radius,
            .03);
      }
      case 4, 5, 6, 11 -> {
        for (var e : targets(mob, 24)) {
          boolean area =
              s.body() == 11
                  ? e.position().distanceToSqr(mob.specialTarget) < radius * radius
                  : e.position().distanceToSqr(mob.specialTarget) < 16;
          if (!area) continue;
          magic(mob, e, 5 + s.rank() * 4);
          e.addEffect(
              new MobEffectInstance(
                  s.behavior().equals("frost")
                      ? MobEffects.MOVEMENT_SLOWDOWN
                      : s.body() == 4 ? MobEffects.POISON : MobEffects.DARKNESS,
                  100,
                  s.behavior().equals("frost") ? 2 : 1));
          if (s.body() == 11)
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4));
          if (s.body() == 6 || s.body() == 11) {
            mob.heal(8);
            if (e instanceof ServerPlayer p) ShadowSlaveMod.soul(p).refill(-12 - s.rank() * 8);
          }
        }
        for (int i = 0; i < 18; i++) {
          var point = mob.getEyePosition().lerp(mob.specialTarget.add(0, 1, 0), i / 18.0);
          level.sendParticles(ParticleTypes.SOUL, point.x, point.y, point.z, 2, .13, .13, .13, .01);
        }
      }
      case 7 -> {
        for (var e : targets(mob, 5))
          if (e.position().distanceToSqr(mob.position()) < 25) {
            physical(mob, e, 2.2f);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
          }
      }
      default -> {
        for (var e : targets(mob, 6))
          if (e.position().distanceToSqr(mob.specialTarget) < 9) {
            physical(mob, e, 1.65f);
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
          }
      }
    }
  }
}
