package com.shadowslave.combat;

import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.entity.NightmareCreature;
import com.shadowslave.network.SpellNetwork;
import com.shadowslave.player.SoulGrowth;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;

/**
 * Original Aspect families obey the canon unlock order; their combat expressions are Minecraft
 * adaptations.
 */
public final class HigherAspects {
  public static final String[] ASCENDED = {
    "Phoenix Sanctuary",
    "Shadow Lances",
    "Tidal Bastion",
    "Iron Legion",
    "Worldroot",
    "Prismatic Rupture",
    "Starfall"
  };
  public static final String[] FORMS =
      BeastForms.ALL.stream().map(BeastForms.Form::name).toArray(String[]::new);

  public static void cast(ServerPlayer p) {
    var d = ShadowSlaveMod.soul(p);
    long now = p.server.overworld().getGameTime();
    if (d.soulRank < 3 || d.family < 0) {
      p.displayClientMessage(
          Component.literal("Conquer your Second Nightmare to unlock an Ascended ability."), true);
      return;
    }
    if (now < d.ascendedCooldownUntil) {
      p.displayClientMessage(Component.literal("Ascended ability recovering."), true);
      return;
    }
    if (!d.spend(45)) {
      p.displayClientMessage(Component.literal("You need 45 Essence."), true);
      return;
    }
    d.ascendedCooldownUntil = now + SoulGrowth.cooldown(d, 360);
    com.shadowslave.network.SpellNetwork.visual(p.serverLevel(),d.family,2,p.position(),6*SoulGrowth.reach(d),36);
    double reach = SoulGrowth.reach(d);
    int amp = SoulGrowth.amplifier(d);
    var level = p.serverLevel();
    var enemies =
        level.getEntitiesOfClass(
            Monster.class,
            p.getBoundingBox().inflate(16 * reach),
            e -> !(e instanceof NightmareCreature n && n.isEcho()) && p.hasLineOfSight(e));
    float power = (float) (14 * SoulGrowth.ability(d));
    switch (d.family) {
      case 0 -> {
        for (var e : enemies)
          if (e.distanceToSqr(p) < 100 * reach * reach) {
            e.hurt(p.damageSources().playerAttack(p), power);
            e.igniteForSeconds(8);
          }
        for (var ally :
            level.getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(8 * reach))) {
          if (ally != p) ally.heal((float) (6 * Math.sqrt(SoulGrowth.ability(d))));
          ally.addEffect(
              new MobEffectInstance(MobEffects.FIRE_RESISTANCE, SoulGrowth.duration(d, 300)));
        }
        p.heal((float) (6 * Math.sqrt(SoulGrowth.ability(d))));
        level.sendParticles(
            ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1, p.getZ(), 120, 5, 1, 5, .04);
      }
      case 1 -> {
        var look = p.getLookAngle();
        for (var e : enemies)
          if (e.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize().dot(look)
              > .72) {
            e.hurt(p.damageSources().playerAttack(p), power);
            e.addEffect(
                new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, SoulGrowth.duration(d, 100), 4));
            for (int i = 0; i < 12; i++) {
              var point = p.getEyePosition().lerp(e.getBoundingBox().getCenter(), i / 12.0);
              level.sendParticles(
                  ParticleTypes.SQUID_INK, point.x, point.y, point.z, 3, .1, .1, .1, 0);
            }
          }
      }
      case 2 -> {
        p.heal((float) (8 * Math.sqrt(SoulGrowth.ability(d))));
        p.addEffect(
            new MobEffectInstance(MobEffects.ABSORPTION, SoulGrowth.duration(d, 400), 2 + amp));
        p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, SoulGrowth.duration(d, 600)));
        for (var e : enemies)
          if (e.distanceToSqr(p) < 100 * reach * reach)
            e.knockback(2, p.getX() - e.getX(), p.getZ() - e.getZ());
        level.sendParticles(
            ParticleTypes.BUBBLE, p.getX(), p.getY() + 1, p.getZ(), 100, 4, 1, 4, .02);
      }
      case 3 -> {
        p.addEffect(
            new MobEffectInstance(
                MobEffects.DAMAGE_BOOST, SoulGrowth.duration(d, 240), Math.min(3, 1 + amp / 2)));
        p.addEffect(
            new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE,
                SoulGrowth.duration(d, 240),
                Math.min(3, 1 + amp / 2)));
        for (var e : enemies)
          if (e.distanceToSqr(p) < 64 * reach * reach) {
            e.hurt(p.damageSources().playerAttack(p), power * .8f);
            e.knockback(2, p.getX() - e.getX(), p.getZ() - e.getZ());
          }
        level.sendParticles(
            ParticleTypes.ENCHANTED_HIT, p.getX(), p.getY() + 1, p.getZ(), 90, 2, 1, 2, .04);
      }
      case 4 -> {
        p.heal((float) (10 * Math.sqrt(SoulGrowth.ability(d))));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, SoulGrowth.duration(d, 240), 1));
        for (var e : enemies) {
          e.addEffect(
              new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SoulGrowth.duration(d, 180), 6));
          e.addEffect(new MobEffectInstance(MobEffects.POISON, SoulGrowth.duration(d, 180), 2));
        }
        level.sendParticles(
            ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 100, 6, 1, 6, .01);
      }
      case 5 -> {
        for (var e : enemies) {
          e.setTarget(null);
          e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, SoulGrowth.duration(d, 180)));
          e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, SoulGrowth.duration(d, 180), 2));
          e.hurt(p.damageSources().playerAttack(p), power * .7f);
        }
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, SoulGrowth.duration(d, 160)));
        level.sendParticles(
            ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 120, 6, 1, 6, .04);
      }
      case 6 -> {
        for (var e : enemies) {
          e.hurt(p.damageSources().playerAttack(p), power);
          e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, SoulGrowth.duration(d, 70), 2));
        }
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SoulGrowth.duration(d, 400)));
        p.addEffect(
            new MobEffectInstance(
                MobEffects.JUMP, SoulGrowth.duration(d, 240), Math.min(3, 1 + amp / 2)));
        level.sendParticles(
            ParticleTypes.END_ROD, p.getX(), p.getY() + 6, p.getZ(), 130, 7, 4, 7, .05);
      }
    }
    p.displayClientMessage(Component.literal(ASCENDED[d.family]), true);
    SpellNetwork.sync(p);
  }

  public static void transform(ServerPlayer p) {
    BeastForms.transform(p);
  }

  public static void passive(ServerPlayer p) {
    BeastForms.tick(p);
  }
}
