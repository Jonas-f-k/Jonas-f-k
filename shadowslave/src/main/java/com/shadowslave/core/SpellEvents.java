package com.shadowslave.core;

import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.combat.Aspects;
import com.shadowslave.content.Content;
import com.shadowslave.entity.NightmareCreature;
import com.shadowslave.memory.*;
import com.shadowslave.network.SpellNetwork;
import com.shadowslave.nightmare.*;
import com.shadowslave.world.DreamRealm;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.*;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.*;

@EventBusSubscriber(modid = ShadowSlaveMod.ID)
public final class SpellEvents {
  @SubscribeEvent
  public static void reload(AddReloadListenerEvent e) {
    e.addListener(new Content());
  }

  @SubscribeEvent
  public static void serverTick(ServerTickEvent.Post e) {
    Nightmares.tick(e.getServer());
    com.shadowslave.world.GrandCitadels.tick(e.getServer());
  }

  /**
   * The Dream Realm map is derived from the world seed; it must be chosen before any Dream chunk
   * exists.
   */
  @SubscribeEvent
  public static void aboutToStart(ServerAboutToStartEvent e) {
    com.shadowslave.world.RealmMap.useSeed(e.getServer().getWorldData().worldGenOptions().seed());
  }

  @SubscribeEvent
  public static void stopped(ServerStoppedEvent e) {
    TrialBuilder.clear();
    DreamRealm.clear();
    com.shadowslave.world.GrandCitadels.clear();
  }

  @SubscribeEvent
  public static void login(PlayerEvent.PlayerLoggedInEvent e) {
    if (!(e.getEntity() instanceof ServerPlayer p)) return;
    var d = ShadowSlaveMod.soul(p);
    d.lastDayTime = p.server.overworld().getDayTime();
    d.lastActionTick = -100;
    if (!d.welcomed) {
      d.welcomed = true;
      p.sendSystemMessage(
          Component.literal(
              "[The Spell] You have been chosen. Your First Nightmare begins after "
                  + Settings.COUNTDOWN_DAYS.get()
                  + " personal days. Press O for your Spell status."));
    }
    if (d.stage.equals("nightmare") && Nightmares.current(p) == null) {
      d.stage = "retry";
      Nightmares.restore(p);
    }
    var pending = Nightmares.current(p);
    if (d.stage.equals("preparing") && pending != null && pending.state.equals("active")) {
      var level = p.server.getLevel(Nightmares.dimension(pending.family));
      if (level != null) Nightmares.enter(p, pending, level);
    }
    com.shadowslave.player.Traits.check(p);
    com.shadowslave.player.SoulGrowth.apply(p);
    com.shadowslave.combat.BeastForms.tick(p);
    Memories.removeManifested(p);
    SpellNetwork.sync(p);
  }

  @SubscribeEvent
  public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
    if (e.getEntity() instanceof ServerPlayer p) Memories.removeManifested(p);
  }

  @SubscribeEvent
  public static void openContainer(PlayerContainerEvent.Open e) {
    if (e.getEntity() instanceof ServerPlayer p) Memories.removeManifested(p);
  }

  @SubscribeEvent
  public static void tick(PlayerTickEvent.Post e) {
    if (!(e.getEntity() instanceof ServerPlayer p)) return;
    var d = ShadowSlaveMod.soul(p);
    long daytime = p.server.overworld().getDayTime();
    if (d.stage.equals("aspirant")) {
      d.elapsed += d.lastDayTime < 0 ? 1 : Rules.countdownDelta(d.lastDayTime, daytime);
      if (d.elapsed >= Settings.COUNTDOWN_DAYS.get() * 24000L
          && Settings.AUTO_START.get()
          && !p.isSpectator()
          && p.level().getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL)
        Nightmares.start(p, -1);
    }
    d.lastDayTime = daytime;
    com.shadowslave.combat.BeastForms.tick(p);
    if (p.tickCount % 20 == 0) {
      com.shadowslave.player.Traits.check(p);
      com.shadowslave.player.SoulGrowth.apply(p);
      Aspects.passive(p);
      Memories.updateArmor(p);
      if (d.initiated()
          && Settings.NATURAL_REGEN.get()
          && p.server.overworld().getGameTime() - d.lastCombatTick > 120) {
        d.regenRemainder +=
            com.shadowslave.player.SoulGrowth.recovery(d) + (Memories.has(d, "essence") ? 1 : 0);
        int gained = (int) d.regenRemainder;
        d.regenRemainder -= gained;
        d.refill(gained);
      }
      for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
        var s = p.getInventory().getItem(i);
        if (s.is(ShadowSlaveMod.MANIFESTED_BLADE.get()) && !Memories.validBlade(p, s))
          p.getInventory().setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
      }
      SpellNetwork.sync(p);
    }
    DreamRealm.tick(p);
  }

  @SubscribeEvent
  public static void sleep(CanPlayerSleepEvent e) {
    var p = e.getEntity();
    var d = ShadowSlaveMod.soul(p);
    if (d.initiated()
        && p.level().dimension().equals(Level.OVERWORLD)
        && e.getVanillaProblem() == null) {
      e.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
      DreamRealm.enter(p);
    }
  }

  @SubscribeEvent
  public static void damage(LivingIncomingDamageEvent e) {
    if (e.getSource().getEntity() instanceof ServerPlayer p) {
      var d = ShadowSlaveMod.soul(p);
      d.lastCombatTick = p.server.overworld().getGameTime();
      if (p.getMainHandItem().is(ShadowSlaveMod.MANIFESTED_BLADE.get())) {
        if (!Memories.validBlade(p, p.getMainHandItem())) {
          e.setAmount(Math.min(1, e.getAmount()));
          return;
        }
        var def = Content.memories.get(MemoryTokenItem.memoryId(p.getMainHandItem()));
        if (def != null) {
          e.setAmount(e.getAmount() + Memories.weaponPower(d, def));
          if (e.getSource().getDirectEntity() == p) com.shadowslave.network.SpellNetwork.visual(p.serverLevel(),Math.max(0,d.family),6,e.getEntity().position(),1.5*com.shadowslave.player.SoulGrowth.reach(d),12);
        }
      }
    }
    if (e.getEntity() instanceof ServerPlayer p) {
      var d = ShadowSlaveMod.soul(p);
      d.lastCombatTick = p.server.overworld().getGameTime();
      if (e.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)
          || e.getSource().is(DamageTypes.GENERIC_KILL)) return;
      boolean magic = e.getSource().is(NightmareCreature.NIGHTMARE_DAMAGE);
      double memory = magic ? Memories.protection(d, true) : 0;
      // A full-body Memory replaces vanilla physical armor reduction while active.

      if (magic && memory == 0) {
        int netherite = 0, diamond = 0;
        for (var s : p.getArmorSlots()) {
          if (s.getItem().toString().contains("netherite")) netherite++;
          else if (s.getItem().toString().contains("diamond")) diamond++;
        }
        memory = netherite * .025 + diamond * .0125;
      }
      double flaw = d.initiated() && d.family == 5 ? 1.15 : 1;
      var traits = com.shadowslave.player.Traits.stats(d);
      double reduction =
          e.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
              ? 1 - traits.fire()
              : e.getSource().is(DamageTypes.FALL) ? 1 - traits.fall() : 1;
      e.setAmount((float) (e.getAmount() * (1 - memory) * flaw * reduction));
    }
  }

  @SubscribeEvent
  public static void damaged(LivingDamageEvent.Post e) {
    if (e.getEntity() instanceof ServerPlayer p && ShadowSlaveMod.soul(p).initiated())
      ShadowSlaveMod.soul(p).damageEndured += Math.max(0, e.getNewDamage());
  }

  @SubscribeEvent
  public static void death(LivingDeathEvent e) {
    if (e.getEntity() instanceof ServerPlayer p) {
      var d = ShadowSlaveMod.soul(p);
      com.shadowslave.combat.BeastForms.end(p);
      if (d.stage.equals("nightmare")) {
        e.setCanceled(true);
        p.setHealth(p.getMaxHealth());
        Nightmares.fail(p);
        return;
      }
      if (DreamRealm.isDream(p.level()) && Settings.KEEP_DREAM_INVENTORY.get()) {
        e.setCanceled(true);
        p.setHealth(p.getMaxHealth());
        p.removeAllEffects();
        p.clearFire();
        d.essence = 0;
        d.fragments = Math.max(0, d.fragments - d.fragments / 10);
        DreamRealm.returnToSaved(p);
        p.displayClientMessage(
            Component.literal(
                "[The Spell] You awaken wounded. Essence drained; 10% of Soul Fragments lost."),
            false);
        SpellNetwork.sync(p);
        return;
      }
    }
    if (e.getEntity() instanceof NightmareCreature mob) {
      if (mob.isEcho()) {
        var master =
            mob.owner == null ? null : mob.getServer().getPlayerList().getPlayer(mob.owner);
        if (master != null) {
          ShadowSlaveMod.soul(master).echoes.remove(mob.speciesId());
          master.sendSystemMessage(
              Component.literal(
                  "[The Spell] Your Echo has been destroyed: " + mob.species().name()));
          SpellNetwork.sync(master);
        }
        return;
      }
      // Attribution is to the real killer (including player-fired projectiles), never client
      // reports.
      if (e.getSource().getEntity() instanceof ServerPlayer p) Nightmares.kill(p, mob);
      else if (e.getSource().getEntity() instanceof NightmareCreature echo
          && echo.isEcho()
          && echo.owner != null) {
        var master = mob.getServer().getPlayerList().getPlayer(echo.owner);
        if (master != null) Nightmares.kill(master, mob);
      } else if (mob.trialId != null && mob.isBoss()) {
        // Environmental death cannot permanently lock the Divine encounter.
        var t = SpellSavedData.get(mob.getServer()).instances.get(mob.trialId);
        if (t != null && !t.bossDefeated && mob.getUUID().equals(t.bossId)) {
          t.bossId = null;
          SpellSavedData.get(mob.getServer()).setDirty();
        }
      }
    }
  }

  @SubscribeEvent
  public static void drops(LivingDropsEvent e) {
    e.getDrops().removeIf(item -> item.getItem().is(ShadowSlaveMod.MANIFESTED_BLADE.get()));
  }

  @SubscribeEvent
  public static void entityJoin(EntityJoinLevelEvent e) {
    if (e.getEntity() instanceof ItemEntity item
        && item.getItem().is(ShadowSlaveMod.MANIFESTED_BLADE.get())) e.setCanceled(true);
  }

  @SubscribeEvent
  public static void breakBlock(BlockEvent.BreakEvent e) {
    if (Nightmares.isTrial(e.getPlayer().level()) && !e.getPlayer().isCreative())
      e.setCanceled(true);
  }

  @SubscribeEvent
  public static void changeDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
    if (e.getEntity() instanceof ServerPlayer p) {
      com.shadowslave.combat.BeastForms.end(p);
      SpellNetwork.sync(p);
    }
  }

  @SubscribeEvent
  public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
    if (e.getEntity() instanceof ServerPlayer p) {
      com.shadowslave.combat.BeastForms.end(p);
      com.shadowslave.player.SoulGrowth.apply(p);
      SpellNetwork.sync(p);
    }
  }
}
