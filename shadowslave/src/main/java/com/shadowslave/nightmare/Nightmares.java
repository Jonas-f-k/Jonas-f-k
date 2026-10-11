package com.shadowslave.nightmare;

import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.content.Content;
import com.shadowslave.core.*;
import com.shadowslave.entity.NightmareCreature;
import com.shadowslave.memory.Memories;
import com.shadowslave.network.SpellNetwork;
import com.shadowslave.world.DreamRealm;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.AABB;

public final class Nightmares {
  public static ResourceKey<Level> dimension(int family) {
    return ResourceKey.create(
        Registries.DIMENSION, ShadowSlaveMod.id("nightmare_" + Content.trial(family).id()));
  }

  public static boolean isTrial(Level level) {
    return level.dimension().location().getNamespace().equals(ShadowSlaveMod.ID)
        && level.dimension().location().getPath().startsWith("nightmare_");
  }

  public static TrialInstance current(ServerPlayer p) {
    var id = ShadowSlaveMod.soul(p).instance;
    return id == null ? null : SpellSavedData.get(p.server).instances.get(id);
  }

  public static void start(ServerPlayer p, int forcedFamily) {
    var d = ShadowSlaveMod.soul(p);
    if (d.stage.equals("nightmare") || d.stage.equals("preparing")) return;
    if (d.initiated()
        && (!d.stage.equals("retry") || current(p) == null || current(p).targetRank == 1)) return;
    if (p.level().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
      p.sendSystemMessage(
          Component.literal("Choose Easy, Normal or Hard difficulty for Nightmare combat."));
      return;
    }
    if (d.family < 0)
      d.family = forcedFamily >= 0 ? Math.clamp(forcedFamily, 0, 6) : p.getRandom().nextInt(7);
    if (p.server.getLevel(dimension(d.family)) == null) {
      p.sendSystemMessage(
          Component.literal("Nightmare dimension is missing. Check enabled data packs."));
      return;
    }
    var saved = SpellSavedData.get(p.server);
    var old = current(p);
    int attempt = old == null ? 0 : old.attempt + 1;
    if (saved.nextSlot >= 1000000) {
      p.sendSystemMessage(
          Component.literal("The server's trial allocation limit has been reached."));
      return;
    }
    // Retry keeps the same kind and selected arena; it resets score, encounters and objectives.
    long seed = old == null ? p.getRandom().nextLong() : old.seed;
    var t = new TrialInstance(p.getUUID(), d.family, saved.nextSlot++, seed, attempt);
    if (old != null && old.targetRank > 1) t.targetRank = old.targetRank;
    saved.instances.put(t.id, t);
    saved.setDirty();
    d.instance = t.id;
    d.stage = "preparing";
    p.sendSystemMessage(
        Component.literal("[The Spell] Preparing " + Content.trial(d.family).name() + "..."));
    SpellNetwork.sync(p);
  }

  public static boolean ascend(ServerPlayer p) {
    var d = ShadowSlaveMod.soul(p);
    if (!DreamRealm.isDream(p.level())
        || d.soulRank < 2
        || d.soulRank > 3
        || d.inventoryStored
        || d.stage.equals("preparing")) {
      p.displayClientMessage(
          Component.literal("Use a Nightmare Seed in the Dream Realm as an Awakened or Master."),
          true);
      return false;
    }
    if (p.level().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return false;
    var saved = SpellSavedData.get(p.server);
    if (saved.nextSlot >= 1000000) return false;
    var t = new TrialInstance(p.getUUID(), d.family, saved.nextSlot++, p.getRandom().nextLong(), 0);
    t.targetRank = d.soulRank + 1;
    t.wakingReturn = d.returnPoint.copy();
    saved.instances.put(t.id, t);
    saved.setDirty();
    d.instance = t.id;
    d.stage = "preparing";
    p.sendSystemMessage(
        Component.literal(
            "[The Spell] Preparing your "
                + (t.targetRank == 3 ? "Second" : "Third")
                + " Nightmare. Defeat the trial's ruler and restore every conduit to Ascend."));
    SpellNetwork.sync(p);
    return true;
  }

  public static void tick(MinecraftServer server) {
    var saved = SpellSavedData.get(server);
    int builds = 0;
    for (var t : saved.instances.values()) {
      if (!t.state.equals("building") && !t.state.equals("active")) continue;
      var p = server.getPlayerList().getPlayer(t.owner);
      if (p == null || !p.isAlive()) continue;
      if (!Objects.equals(ShadowSlaveMod.soul(p).instance, t.id)) continue;
      var level = server.getLevel(dimension(t.family));
      if (level == null) continue;
      if (t.state.equals("building")) {
        if (builds++ >= 2) continue;
        if (TrialBuilder.buildStep(level, t)) {
          t.state = "active";
          enter(p, t, level);
        }
        saved.setDirty();
        continue;
      }
      if (!p.level().dimension().equals(level.dimension())) continue;
      if (!t.contains(p.blockPosition()))
        p.teleportTo(level, t.start().getX() + .5, t.start().getY(), t.start().getZ() + .5, 180, 0);
      if (server.getTickCount() % 20 != 0) continue;
      // Limit active regular enemies; encounter IDs can score at most once.
      long active =
          level
              .getEntitiesOfClass(
                  NightmareCreature.class,
                  new AABB(t.origin()).inflate(180),
                  e -> t.id.equals(e.trialId) && !e.isBoss())
              .size();
      int budget = Settings.ENEMIES_PER_TRIAL.get();
      if (active < 4)
        for (int i = 0; i < budget; i++) {
          if (t.spawned.contains(i)) continue;
          var anchor = t.altar(i % t.seals());
          if (anchor.distSqr(p.blockPosition()) > 26 * 26) continue;
          var mob = ShadowSlaveMod.CREATURE.get().create(level);
          if (mob == null) break;
          mob.configure(t.family, false, t.targetRank > 1 ? t.targetRank - 1 : 0, t.id, t.owner, i);
          int offset = (i / t.seals()) % 4;
          mob.moveTo(
              anchor.getX() + new int[] {-7, 7, -5, 5}[offset] + .5,
              anchor.getY(),
              anchor.getZ() + new int[] {8, 8, -8, -8}[offset] + .5,
              0,
              0);
          if (mob.canStandAt(mob.blockPosition()) && level.addFreshEntity(mob)) {
            t.spawned.add(i);
            t.liveEntities.put(i, mob.getUUID());
            saved.setDirty();
          }
          break;
        }
      if (!t.bossDefeated
          && t.bossId == null
          && t.bossPosition().distSqr(p.blockPosition()) < 17 * 17) {
        var boss = ShadowSlaveMod.CREATURE.get().create(level);
        if (boss != null) {
          boss.configure(
              t.family, true, t.targetRank > 1 ? t.targetRank - 1 : 1, t.id, t.owner, -1);
          var b = t.bossPosition();
          boss.moveTo(b.getX() + .5, b.getY(), b.getZ() + .5, 0, 0);
          if (boss.canStandAt(b) && level.addFreshEntity(boss)) {
            t.bossId = boss.getUUID();
            saved.setDirty();
            p.sendSystemMessage(
                Component.literal(
                    "[The Spell] A Divine challenge stirs: " + Content.trial(t.family).boss()));
          }
        }
      }
    }
  }

  public static void enter(ServerPlayer p, TrialInstance t, ServerLevel level) {
    if (!t.owner.equals(p.getUUID())
        || !Objects.equals(ShadowSlaveMod.soul(p).instance, t.id)
        || !t.state.equals("active")) return;
    var d = ShadowSlaveMod.soul(p);
    p.closeContainer();
    if (!d.inventoryStored) {
      if (t.targetRank > 1) t.wakingReturn = d.returnPoint.copy();
      d.returnPoint = DreamRealm.location(p);
      d.previousMode = p.gameMode.getGameModeForPlayer().getName();
      d.inventoryBackup = p.getInventory().save(new ListTag());
      d.selectedSlot = p.getInventory().selected;
      d.xpLevel = p.experienceLevel;
      d.xpProgress = p.experienceProgress;
      d.xpTotal = p.totalExperience;
      d.inventoryStored = true;
      p.getInventory().clearContent();
      p.experienceLevel = 0;
      p.totalExperience = 0;
      p.experienceProgress = 0;
    }
    p.getInventory().clearContent();
    d.stage = "nightmare";
    p.removeAllEffects();
    p.setHealth(p.getMaxHealth());
    p.getFoodData().setFoodLevel(20);
    p.getFoodData().setSaturation(10);
    p.setGameMode(GameType.ADVENTURE);
    TrialKit.supply(p);
    p.teleportTo(level, t.start().getX() + .5, t.start().getY(), t.start().getZ() + .5, 180, 0);
    p.sendSystemMessage(
        Component.literal(
            "[The Spell] "
                + Content.trial(t.family).objective()
                + " Explore the districts. Restore the western and eastern seals in pairs from"
                + " south to north, then return to the exit. "
                + (t.targetRank == 1
                    ? "Seek the optional boss for Divine."
                    : "Defeat the ruler to resolve this Nightmare.")
                + ""));
    SpellSavedData.get(p.server).setDirty();
    p.server.overworld().getDataStorage().save();
    p.server.getPlayerList().saveAll();
    SpellNetwork.sync(p);
  }

  public static void altar(ServerPlayer p, BlockPos pos) {
    var t = current(p);
    if (t == null
        || !t.state.equals("active")
        || !p.getUUID().equals(t.owner)
        || !p.level().dimension().equals(dimension(t.family))) return;
    for (int i = 0; i < t.seals(); i++)
      if (t.altar(i).equals(pos)) {
        if ((t.altars & (1 << i)) != 0) {
          p.displayClientMessage(Component.literal("This seal is already restored."), true);
          return;
        }
        if (!p.serverLevel()
            .getEntitiesOfClass(
                NightmareCreature.class,
                new AABB(pos).inflate(7),
                e -> t.id.equals(e.trialId) && e.isAlive())
            .isEmpty()) {
          p.displayClientMessage(
              Component.literal("Draw the nearby creatures away or defeat them first."), true);
          return;
        }
        if (t.layoutVersion > 0) {
          if (i >= 2 && (t.altars & (1 << (i - 2))) == 0) {
            p.displayClientMessage(
                Component.literal("This conduit is dormant. Restore its southern seal first."),
                true);
            return;
          }
          if (t.activatedWaves.add(i)) {
            for (int n = 0; n < 2; n++) {
              int encounter = 100 + i * 2 + n;
              var guardian = ShadowSlaveMod.CREATURE.get().create(p.serverLevel());
              if (guardian == null) continue;
              guardian.configure(
                  t.family,
                  false,
                  t.targetRank > 1 ? t.targetRank - 1 : 0,
                  t.id,
                  t.owner,
                  encounter);
              var spawn = pos.offset(n == 0 ? -5 : 5, 0, 4);
              if (guardian.canStandAt(spawn)) {
                guardian.moveTo(spawn.getX() + .5, spawn.getY(), spawn.getZ() + .5, 0, 0);
                p.serverLevel().addFreshEntity(guardian);
                t.liveEntities.put(encounter, guardian.getUUID());
                t.spawned.add(encounter);
              }
            }
            SpellSavedData.get(p.server).setDirty();
            p.sendSystemMessage(
                Component.literal(
                    "[The Spell] The seal summons its wardens. Defeat them, then restore the"
                        + " conduit."));
            return;
          }
          for (int n = 0; n < 2; n++) {
            int index = 100 + i * 2 + n;
            if (t.spawned.contains(index) && !t.killed.contains(index)) {
              var entity = p.serverLevel().getEntity(t.liveEntities.get(index));
              if (entity != null && entity.isAlive()) {
                p.displayClientMessage(Component.literal("A seal warden still lives."), true);
                return;
              }
              t.killed.add(index);
            }
          }
        }
        t.altars |= 1 << i;
        SpellSavedData.get(p.server).setDirty();
        p.sendSystemMessage(
            Component.literal(
                "[The Spell] Seal restored ("
                    + Integer.bitCount(t.altars)
                    + "/"
                    + t.seals()
                    + ")."));
        p.serverLevel()
            .sendParticles(
                net.minecraft.core.particles.ParticleTypes.END_ROD,
                pos.getX() + .5,
                pos.getY() + 1,
                pos.getZ() + .5,
                40,
                .7,
                1,
                .7,
                .04);
        SpellNetwork.sync(p);
        return;
      }
  }

  public static void finish(ServerPlayer p, BlockPos pos) {
    var t = current(p);
    if (t == null
        || !t.exit().equals(pos)
        || !p.level().dimension().equals(dimension(t.family))
        || !t.state.equals("active")) return;
    if (!t.complete()) {
      p.displayClientMessage(Component.literal("Restore every seal before leaving."), true);
      return;
    }
    if (t.targetRank > 1) {
      if (!t.bossDefeated) {
        p.displayClientMessage(
            Component.literal("Resolve this Nightmare's conflict by defeating its ruler."), true);
        return;
      }
      var d = ShadowSlaveMod.soul(p);
      d.soulRank = t.targetRank;
      d.stage = d.soulRank == 3 ? "master" : "saint";
      d.essence = d.maxEssence();
      t.state = "completed";
      restore(p);
      SpellSavedData.get(p.server).setDirty();
      p.sendSystemMessage(
          Component.literal(
              "[The Spell] You have "
                  + (d.soulRank == 3
                      ? "Ascended. Press H: "
                          + com.shadowslave.combat.HigherAspects.ASCENDED[d.family]
                      : "Transcended. Press J: "
                          + com.shadowslave.combat.HigherAspects.FORMS[d.family])));
      SpellNetwork.sync(p);
      return;
    }
    var d = ShadowSlaveMod.soul(p);
    d.aspectRank = Rules.aspectRank(t.points, t.bossDefeated);
    d.soulRank = 1;
    d.stage = "sleeper";
    d.essence = d.maxEssence();
    t.state = "completed";
    restore(p);
    Memories.grant(p, "ashen_shroud");
    Memories.grant(p, "soul_edge");
    Memories.grant(p, "fading_ember");
    if (d.aspectRank >= 4) Memories.grant(p, "silent_steps");
    if (d.aspectRank == 6) Memories.grant(p, "divine_spark");
    d.equipped.clear();
    d.equipped.addAll(List.of("ashen_shroud", "soul_edge", "fading_ember"));
    SpellSavedData.get(p.server).setDirty();
    p.sendSystemMessage(
        Component.literal(
            "[The Spell] First Nightmare conquered. Aspect: "
                + Content.trial(d.family).aspect()
                + " ("
                + Rules.RANKS[d.aspectRank]
                + "). Flaw: "
                + Content.trial(d.family).flaw()));
    p.sendSystemMessage(
        Component.literal(
            "Press O to open your Soul Sea. Sleep in a bed to enter the Dream Realm and find a"
                + " Gateway."));
    SpellNetwork.sync(p);
  }

  public static void fail(ServerPlayer p) {
    var d = ShadowSlaveMod.soul(p);
    var t = current(p);
    if (t == null) return;
    if (!d.stage.equals("nightmare") && !d.stage.equals("preparing")) return;
    t.state = "failed";
    d.stage = "retry";
    restore(p);
    SpellSavedData.get(p.server).setDirty();
    p.sendSystemMessage(
        Component.literal(
            "[The Spell] The trial has ended. Your belongings are restored. Press O and choose"
                + " Retry Nightmare."));
    SpellNetwork.sync(p);
  }

  public static void restore(ServerPlayer p) {
    var d = ShadowSlaveMod.soul(p);
    if (d.inventoryStored) {
      p.closeContainer();
      p.getInventory().clearContent();
      p.getInventory().load(d.inventoryBackup);
      p.getInventory().selected = d.selectedSlot;
      p.experienceLevel = d.xpLevel;
      p.totalExperience = d.xpTotal;
      p.experienceProgress = d.xpProgress;
      d.inventoryStored = false;
      d.inventoryBackup = new ListTag();
    }
    p.setGameMode(GameType.byName(d.previousMode, GameType.SURVIVAL));
    p.removeAllEffects();
    p.clearFire();
    p.setHealth(p.getMaxHealth());
    p.getFoodData().setFoodLevel(20);
    p.fallDistance = 0;
    DreamRealm.returnToSaved(p);
    var t = current(p);
    if (t != null && t.targetRank > 1 && !t.wakingReturn.isEmpty())
      d.returnPoint = t.wakingReturn.copy();
    p.inventoryMenu.broadcastChanges();
    SpellSavedData.get(p.server).setDirty();
    p.server.overworld().getDataStorage().save();
    p.server.getPlayerList().saveAll();
  }

  public static void kill(ServerPlayer p, NightmareCreature mob) {
    if (mob.isEcho()) return;
    var d = ShadowSlaveMod.soul(p);
    if (mob.trialId != null) {
      var t = current(p);
      if (t == null
          || !t.id.equals(mob.trialId)
          || !t.owner.equals(p.getUUID())
          || !t.state.equals("active")) return;
      if (mob.isBoss()) {
        if (t.bossDefeated || !mob.getUUID().equals(t.bossId)) return;
        t.bossDefeated = true;
        p.sendSystemMessage(
            Component.literal(
                t.targetRank == 1
                    ? "[The Spell] DIVINE secured. Complete the remaining objective to claim it."
                    : "[The Spell] Nightmare ruler defeated. Restore every conduit to Ascend."));
      } else {
        if (!mob.getUUID().equals(t.liveEntities.get(mob.encounter))
            || !t.killed.add(mob.encounter)) return;
        t.points += Settings.KILL_POINTS.get();
      }
      SpellSavedData.get(p.server).setDirty();
    } else if (d.initiated()) {
      int fragments = Math.max(1, mob.rank * mob.creatureClass * 3);
      if (d.family == 1) com.shadowslave.player.SoulGrowth.gain(p, fragments);
      else p.spawnAtLocation(com.shadowslave.memory.SoulShardItem.create(mob.rank, fragments));
      if (mob.creatureClass >= 3 && mob.rank >= 1 && p.getRandom().nextFloat() < .12f)
        p.spawnAtLocation(ShadowSlaveMod.NIGHTMARE_SEED.get());
      if (p.getRandom().nextFloat() < .02f) com.shadowslave.memory.Echoes.reward(p, mob);
      if (p.getRandom().nextFloat() < .18f) {
        String region =
            p.level()
                .getBiome(mob.blockPosition())
                .unwrapKey()
                .map(k -> k.location().getPath())
                .orElse("");
        var species = mob.species();
        var pool =
            species != null
                ? com.shadowslave.entity.CreatureEcology.rewards(species, region)
                : Content.memories.values().stream().filter(m -> m.biome().equals(region)).toList();
        if (!pool.isEmpty()) {
          var reward = pool.get(p.getRandom().nextInt(pool.size()));
          Memories.grant(p, reward.id(), mob.rank, mob.creatureClass);
        }
      }
    }
    d.totalKills++;
    if (mob.rank >= 2) d.eliteKills++;
    if (mob.isBoss()) d.bossKills++;
    com.shadowslave.player.Traits.check(p);
    d.refill(mob.isBoss() ? 40 : 10);
    SpellNetwork.sync(p);
  }
}
