package com.shadowslave.world;

import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.entity.NightmareCreature;
import com.shadowslave.memory.Memories;
import com.shadowslave.network.SpellNetwork;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;

public final class DreamRealm {
  public static final ResourceKey<Level> KEY =
      ResourceKey.create(Registries.DIMENSION, ShadowSlaveMod.id("dream_realm"));
  private static final Map<UUID, Long> SPAWN_CLOCK = new HashMap<>();

  public static boolean isDream(Level level) {
    return level.dimension().equals(KEY);
  }

  public static void clear() {
    SPAWN_CLOCK.clear();
  }

  public static CompoundTag location(ServerPlayer p) {
    var n = new CompoundTag();
    n.putString("dimension", p.level().dimension().location().toString());
    n.putDouble("x", p.getX());
    n.putDouble("y", p.getY());
    n.putDouble("z", p.getZ());
    n.putFloat("yaw", p.getYRot());
    n.putFloat("pitch", p.getXRot());
    return n;
  }

  public static void returnToSaved(ServerPlayer p) {
    var n = ShadowSlaveMod.soul(p).returnPoint;
    ServerLevel target = null;
    try {
      target =
          p.server.getLevel(
              ResourceKey.create(
                  Registries.DIMENSION, ResourceLocation.parse(n.getString("dimension"))));
    } catch (Exception ignored) {
    }
    if (target == null) {
      target = p.server.overworld();
      var b = target.getSharedSpawnPos();
      p.teleportTo(target, b.getX() + .5, b.getY() + 1, b.getZ() + .5, 0, 0);
    } else
      p.teleportTo(
          target,
          n.getDouble("x"),
          n.getDouble("y"),
          n.getDouble("z"),
          n.getFloat("yaw"),
          n.getFloat("pitch"));
    p.fallDistance = 0;
  }

  public static void enter(ServerPlayer p) {
    var d = ShadowSlaveMod.soul(p);
    if (!d.initiated()
        || isDream(p.level())
        || com.shadowslave.nightmare.Nightmares.isTrial(p.level())) return;
    if (p.server.overworld().getGameTime() - d.lastCombatTick < 200) {
      p.displayClientMessage(
          Component.literal("Rest for 10 seconds outside combat before entering the Dream Realm."),
          true);
      return;
    }
    var level = p.server.getLevel(KEY);
    if (level == null) return;
    var saved = com.shadowslave.nightmare.SpellSavedData.get(p.server);
    if (!saved.dreamInitialized) {
      Sanctuary.build(level, safeSurface(level, 0, 0), false);
      Sanctuary.build(level, safeSurface(level, 160, 0), true);
      saved.dreamInitialized = true;
      saved.setDirty();
    }
    d.returnPoint = location(p);
    BlockPos arrival;
    if (d.hasDreamAnchor) arrival = new BlockPos(d.dreamAnchorX, d.dreamAnchorY, d.dreamAnchorZ);
    else {
      // Each world has a shared, safe landing. A separate gateway remains an exploration goal.
      arrival = safeSurface(level, 0, 0);
    }
    p.teleportTo(level, arrival.getX() + .5, arrival.getY(), arrival.getZ() + .5, 0, 0);
    p.fallDistance = 0;
    p.sendSystemMessage(
        Component.literal(
            "[The Spell] The Dream Realm. An ancient Gateway lies east of the first landing. A"
                + " grand Citadel stands farther east at x480, z0; two others stand at -1024,640"
                + " and 1024,-768."));
    SpellNetwork.sync(p);
  }

  public static BlockPos safeSurface(ServerLevel level, int x, int z) {
    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    return new BlockPos(x, Math.clamp(y, 66, 220), z);
  }

  /** Natural creatures use the real ground height, including elevated floating islands. */
  public static BlockPos naturalSpawnSurface(ServerLevel level, int x, int z) {
    return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
  }

  public static boolean canAttemptNaturalSpawning(ServerPlayer p) {
    return isDream(p.level())
        && !p.isCreative()
        && !p.isSpectator()
        && ShadowSlaveMod.soul(p).initiated()
        && p.level().getDifficulty() != Difficulty.PEACEFUL
        && p.level().getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
        && !Sanctuary.isSafe(p.level(), p.blockPosition());
  }

  private static void landing(ServerLevel level, BlockPos b) {
    for (int x = -4; x <= 4; x++)
      for (int z = -4; z <= 4; z++) {
        level.setBlock(
            b.offset(x, -1, z), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 2);
        for (int y = 0; y < 3; y++)
          level.setBlock(b.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
      }
    for (int x : new int[] {-4, 4})
      for (int z : new int[] {-4, 4})
        level.setBlock(b.offset(x, 0, z), Blocks.SOUL_LANTERN.defaultBlockState(), 2);
  }

  public static void gateway(ServerPlayer p, BlockPos pos) {
    var d = ShadowSlaveMod.soul(p);
    if (!d.initiated()) return;
    if (!isDream(p.level())) {
      enter(p);
      return;
    }
    d.hasDreamAnchor = true;
    d.dreamAnchorX = pos.getX() + 2;
    d.dreamAnchorY = pos.getY();
    d.dreamAnchorZ = pos.getZ();
    if (d.soulRank == 1) {
      d.soulRank = 2;
      d.stage = "awakened";
      d.refill(200);
      Memories.grant(p, "awakened_mantle");
      p.sendSystemMessage(
          Component.literal(
              "[The Spell] You have Awakened. Your second Aspect ability is unlocked. Press G: "
                  + com.shadowslave.combat.Aspects.AWAKENED[d.family]
                  + ". You can now control Soul Essence."));
    }
    returnToSaved(p);
    SpellNetwork.sync(p);
  }

  public static void tick(ServerPlayer p) {
    if (p.tickCount % 100 != 0 || !canAttemptNaturalSpawning(p)) return;
    var level = p.serverLevel();
    long now = level.getGameTime();
    // Vanilla mob-cap-like local ceiling; no block scanning or forced distant chunks.
    if (level.getEntitiesOfClass(NightmareCreature.class, p.getBoundingBox().inflate(64),
            mob -> !mob.isEcho() && !mob.isBoss() && mob.trialId == null).size()
        >= 8) return;
    if (now - SPAWN_CLOCK.getOrDefault(p.getUUID(), 0L) < 160) return;
    SPAWN_CLOCK.put(p.getUUID(), now);
    // A few bounded candidates tolerate foliage and uneven islands without increasing the cap.
    for (int attempt = 0; attempt < 6; attempt++) {
      double angle = p.getRandom().nextDouble() * Math.PI * 2;
      double distance = 24 + p.getRandom().nextInt(17);
      int x = p.getBlockX() + (int) Math.round(Math.cos(angle) * distance),
          z = p.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
      if (!level.hasChunk(x >> 4, z >> 4)) continue;
      var pos = naturalSpawnSurface(level, x, z);
      if (pos.getY() <= level.getMinBuildHeight()
          || pos.getY() >= level.getMaxBuildHeight()
          || Sanctuary.isSafe(level, pos)) continue;
      if (!level.getBlockState(pos.below()).isSolid()
          || !level.getBlockState(pos).isAir()
          || !level.getBlockState(pos.above()).isAir()) continue;
      if (level.players().stream().anyMatch(other -> !other.isSpectator()
              && other.distanceToSqr(x + .5, pos.getY(), z + .5) < 24 * 24)) continue;
      var biome = level.getBiome(pos).unwrapKey().map(k -> k.location().getPath()).orElse("");
      var species =
          com.shadowslave.entity.CreatureEcology.choose(biome, level.isNight(), p.getRandom());
      if (species == null || pos.getY() + species.height() >= level.getMaxBuildHeight()) continue;
      // Collision checks on a broad creature must not pull in unloaded neighbouring chunks.
      int margin = (int) Math.ceil(species.width() / 2);
      if (!level.hasChunk((x - margin) >> 4, (z - margin) >> 4)
          || !level.hasChunk((x + margin) >> 4, (z - margin) >> 4)
          || !level.hasChunk((x - margin) >> 4, (z + margin) >> 4)
          || !level.hasChunk((x + margin) >> 4, (z + margin) >> 4)) continue;
      var mob = ShadowSlaveMod.CREATURE.get().create(level);
      if (mob == null) return;
      mob.configureSpecies(species);
      if (!mob.canStandAt(pos)) continue;
      mob.moveTo(x + .5, pos.getY(), z + .5, 0, 0);
      level.addFreshEntity(mob);
      return;
    }
  }
}
