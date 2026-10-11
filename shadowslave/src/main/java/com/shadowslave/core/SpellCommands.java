package com.shadowslave.core;

import com.mojang.brigadier.arguments.*;
import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.content.Content;
import com.shadowslave.memory.Memories;
import com.shadowslave.network.SpellNetwork;
import com.shadowslave.nightmare.Nightmares;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = ShadowSlaveMod.ID)
public final class SpellCommands {
  @SubscribeEvent
  public static void register(RegisterCommandsEvent event) {
    var root =
        Commands.literal("spell")
            .executes(
                c -> {
                  c.getSource()
                      .sendSuccess(
                          () ->
                              Component.literal(
                                  "Nightmare Spell: O Soul Sea, R Dormant, G Awakened, V weapon, B"
                                      + " Memories. /spell status, retry, abandon. Operators:"
                                      + " /spell start <player> <family 0-6>, /spell memory"
                                      + " <player> <id>."),
                          false);
                  return 1;
                });
    root.then(
        Commands.literal("status")
            .executes(
                c -> {
                  var p = c.getSource().getPlayerOrException();
                  var d = ShadowSlaveMod.soul(p);
                  c.getSource()
                      .sendSuccess(
                          () ->
                              Component.literal(
                                  "Stage: "
                                      + d.stage
                                      + " | Aspect: "
                                      + Rules.RANKS[d.aspectRank]
                                      + " | Essence: "
                                      + d.essence
                                      + "/"
                                      + d.maxEssence()
                                      + " | Fragments: "
                                      + d.fragments),
                          false);
                  SpellNetwork.sync(p);
                  return 1;
                }));
    for (String action : new String[] {"retry", "abandon", "dream"})
      root.then(
          Commands.literal(action)
              .executes(
                  c -> {
                    SpellNetwork.handle(c.getSource().getPlayerOrException(), action, "");
                    return 1;
                  }));
    root.then(
        Commands.literal("start")
            .requires(s -> s.hasPermission(2))
            .then(
                Commands.argument("player", EntityArgument.player())
                    .then(
                        Commands.argument("family", IntegerArgumentType.integer(0, 6))
                            .executes(
                                c -> {
                                  Nightmares.start(
                                      EntityArgument.getPlayer(c, "player"),
                                      IntegerArgumentType.getInteger(c, "family"));
                                  return 1;
                                }))));
    root.then(
        Commands.literal("memory")
            .requires(s -> s.hasPermission(2))
            .then(
                Commands.argument("player", EntityArgument.player())
                    .then(
                        Commands.argument("id", StringArgumentType.word())
                            .suggests(
                                (c, b) -> {
                                  Content.memories.keySet().forEach(b::suggest);
                                  return b.buildFuture();
                                })
                            .executes(
                                c -> {
                                  var p = EntityArgument.getPlayer(c, "player");
                                  Memories.grant(p, StringArgumentType.getString(c, "id"));
                                  SpellNetwork.sync(p);
                                  return 1;
                                }))));
    root.then(
        Commands.literal("cast")
            .executes(
                c -> {
                  com.shadowslave.combat.Aspects.cast(c.getSource().getPlayerOrException());
                  return 1;
                }));
    root.then(
        Commands.literal("awakened")
            .executes(
                c -> {
                  com.shadowslave.combat.Aspects.castAwakened(c.getSource().getPlayerOrException());
                  return 1;
                }));
    root.then(
        Commands.literal("refill")
            .requires(a -> a.hasPermission(2))
            .executes(
                c -> {
                  var p = c.getSource().getPlayerOrException();
                  com.shadowslave.combat.BeastForms.end(p);
                  var d = ShadowSlaveMod.soul(p);
                  d.essence = d.maxEssence();
                  d.cooldownUntil =
                      d.awakenedCooldownUntil =
                          d.ascendedCooldownUntil =
                              d.transformCooldownUntil = d.beastCooldownUntil = 0;
                  SpellNetwork.sync(p);
                  return 1;
                }));
    root.then(
        Commands.literal("profile")
            .requires(a -> a.hasPermission(2))
            .then(
                Commands.argument("family", IntegerArgumentType.integer(0, 6))
                    .then(
                        Commands.argument("soulRank", IntegerArgumentType.integer(1, 4))
                            .then(
                                Commands.argument("aspectRank", IntegerArgumentType.integer(0, 6))
                                    .executes(
                                        c -> {
                                          var p = c.getSource().getPlayerOrException();
                                          var d = ShadowSlaveMod.soul(p);
                                          if (d.inventoryStored || d.stage.equals("preparing")) {
                                            c.getSource()
                                                .sendFailure(
                                                    Component.literal(
                                                        "Finish or abandon the active trial"
                                                            + " first."));
                                            return 0;
                                          }
                                          com.shadowslave.combat.BeastForms.end(p);
                                          d.family = IntegerArgumentType.getInteger(c, "family");
                                          d.soulRank =
                                              IntegerArgumentType.getInteger(c, "soulRank");
                                          d.aspectRank =
                                              IntegerArgumentType.getInteger(c, "aspectRank");
                                          d.stage =
                                              new String[] {
                                                    "aspirant",
                                                    "sleeper",
                                                    "awakened",
                                                    "master",
                                                    "saint"
                                                  }
                                                  [d.soulRank];
                                          d.cooldownUntil =
                                              d.awakenedCooldownUntil =
                                                  d.ascendedCooldownUntil =
                                                      d.transformCooldownUntil =
                                                          d.beastCooldownUntil = 0;
                                          com.shadowslave.player.Traits.check(p);
                                          d.essence = d.maxEssence();
                                          Memories.grant(p, "ashen_shroud");
                                          Memories.grant(p, "soul_edge");
                                          SpellNetwork.sync(p);
                                          c.getSource()
                                              .sendSuccess(
                                                  () ->
                                                      Component.literal(
                                                          "Test profile applied. R: "
                                                              + com.shadowslave.combat.Aspects
                                                                  .DORMANT[d.family]
                                                              + "; G: "
                                                              + (d.soulRank >= 2
                                                                  ? com.shadowslave.combat.Aspects
                                                                      .AWAKENED[d.family]
                                                                  : "locked")
                                                              + "; H: "
                                                              + (d.soulRank >= 3
                                                                  ? com.shadowslave.combat
                                                                      .HigherAspects.ASCENDED[
                                                                      d.family]
                                                                  : "locked")
                                                              + "; J: "
                                                              + (d.soulRank >= 4
                                                                  ? com.shadowslave.combat
                                                                      .BeastForms.ALL
                                                                      .get(d.family)
                                                                      .name()
                                                                  : "locked")),
                                                  false);
                                          return 1;
                                        })))));
    root.then(
        Commands.literal("spawn")
            .requires(a -> a.hasPermission(2))
            .then(
                Commands.argument("species", StringArgumentType.word())
                    .suggests(
                        (c, b) -> {
                          com.shadowslave.entity.CreatureEcology.ALL.forEach(
                              m -> b.suggest(m.id()));
                          return b.buildFuture();
                        })
                    .executes(
                        c -> {
                          var p = c.getSource().getPlayerOrException();
                          var s =
                              com.shadowslave.entity.CreatureEcology.get(
                                  StringArgumentType.getString(c, "species"));
                          if (s == null) return 0;
                          var pos =
                              p.blockPosition()
                                  .offset(
                                      (int) (p.getLookAngle().x * 10),
                                      0,
                                      (int) (p.getLookAngle().z * 10));
                          pos =
                              p.serverLevel()
                                  .getHeightmapPos(
                                      net.minecraft.world.level.levelgen.Heightmap.Types
                                          .MOTION_BLOCKING_NO_LEAVES,
                                      pos);
                          if (com.shadowslave.world.Sanctuary.isSafe(p.level(), pos)) {
                            c.getSource()
                                .sendFailure(
                                    Component.literal(
                                        "Move outside a Sanctuary before spawning hostile"
                                            + " creatures."));
                            return 0;
                          }
                          var mob = ShadowSlaveMod.CREATURE.get().create(p.serverLevel());
                          if (mob == null) return 0;
                          mob.configureSpecies(s);
                          if (!mob.canStandAt(pos)) {
                            c.getSource()
                                .sendFailure(
                                    Component.literal(
                                        "This creature needs more ground or headroom."));
                            return 0;
                          }
                          mob.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
                          p.serverLevel().addFreshEntity(mob);
                          return 1;
                        })));
    root.then(
        Commands.literal("reward")
            .requires(a -> a.hasPermission(2))
            .then(
                Commands.argument("id", StringArgumentType.word())
                    .suggests(
                        (c, b) -> {
                          Content.memories.keySet().forEach(b::suggest);
                          return b.buildFuture();
                        })
                    .then(
                        Commands.argument("rank", IntegerArgumentType.integer(0, 6))
                            .then(
                                Commands.argument("tier", IntegerArgumentType.integer(1, 7))
                                    .executes(
                                        c -> {
                                          var p = c.getSource().getPlayerOrException();
                                          Memories.grant(
                                              p,
                                              StringArgumentType.getString(c, "id"),
                                              IntegerArgumentType.getInteger(c, "rank"),
                                              IntegerArgumentType.getInteger(c, "tier"));
                                          SpellNetwork.sync(p);
                                          return 1;
                                        })))));
    root.then(
        Commands.literal("trialwarp")
            .requires(a -> a.hasPermission(2))
            .then(
                Commands.argument("site", IntegerArgumentType.integer(0, 11))
                    .executes(
                        c -> {
                          var p = c.getSource().getPlayerOrException();
                          var t = Nightmares.current(p);
                          if (t == null || !t.state.equals("active")) return 0;
                          int site = IntegerArgumentType.getInteger(c, "site");
                          var pos =
                              site < 6
                                  ? t.altar(Math.min(site, t.seals() - 1))
                                  : site < 11 ? t.arena(site - 6) : t.exit();
                          p.teleportTo(
                              p.server.getLevel(Nightmares.dimension(t.family)),
                              pos.getX() + .5,
                              pos.getY(),
                              pos.getZ() + 3.5,
                              180,
                              0);
                          return 1;
                        })));
    root.then(
        Commands.literal("trialfinish")
            .requires(a -> a.hasPermission(2))
            .executes(
                c -> {
                  var p = c.getSource().getPlayerOrException();
                  var t = Nightmares.current(p);
                  if (t == null || !t.state.equals("active")) return 0;
                  t.altars = (1 << t.seals()) - 1;
                  Nightmares.finish(p, t.exit());
                  return 1;
                }));
    root.then(
        Commands.literal("bestiary")
            .executes(
                c -> {
                  for (var m : com.shadowslave.entity.CreatureEcology.ALL)
                    c.getSource()
                        .sendSuccess(
                            () ->
                                Component.literal(
                                    m.id()
                                        + " — "
                                        + com.shadowslave.entity.CreatureEcology.RANKS[m.rank()]
                                        + " "
                                        + com.shadowslave.entity.CreatureEcology.CLASSES[m.cores()]
                                        + " · "
                                        + m.regions()
                                        + (m.canon() ? "" : " [original]")),
                            false);
                  return 1;
                }));
    root.then(
        Commands.literal("echo")
            .then(
                Commands.argument("species", StringArgumentType.word())
                    .suggests(
                        (c, b) -> {
                          var p = c.getSource().getPlayer();
                          if (p != null) ShadowSlaveMod.soul(p).echoes.forEach(b::suggest);
                          return b.buildFuture();
                        })
                    .executes(
                        c -> {
                          com.shadowslave.memory.Echoes.summon(
                              c.getSource().getPlayerOrException(),
                              StringArgumentType.getString(c, "species"));
                          return 1;
                        })));
    root.then(
        Commands.literal("echoadd")
            .requires(a -> a.hasPermission(2))
            .then(
                Commands.argument("species", StringArgumentType.word())
                    .suggests(
                        (c, b) -> {
                          com.shadowslave.entity.CreatureEcology.ALL.forEach(
                              m -> b.suggest(m.id()));
                          return b.buildFuture();
                        })
                    .executes(
                        c -> {
                          var p = c.getSource().getPlayerOrException();
                          var id = StringArgumentType.getString(c, "species");
                          if (com.shadowslave.entity.CreatureEcology.get(id) == null) return 0;
                          var d = ShadowSlaveMod.soul(p);
                          if (!d.echoes.contains(id)) d.echoes.add(id);
                          SpellNetwork.sync(p);
                          return 1;
                        })));
    root.then(
        Commands.literal("ascended")
            .executes(
                c -> {
                  com.shadowslave.combat.HigherAspects.cast(c.getSource().getPlayerOrException());
                  return 1;
                }));
    root.then(
        Commands.literal("transform")
            .executes(
                c -> {
                  com.shadowslave.combat.HigherAspects.transform(
                      c.getSource().getPlayerOrException());
                  return 1;
                }));
    root.then(
        Commands.literal("beast")
            .executes(
                c -> {
                  com.shadowslave.combat.BeastForms.attack(c.getSource().getPlayerOrException());
                  return 1;
                }));
    root.then(
        Commands.literal("traits")
            .executes(
                c -> {
                  var p = c.getSource().getPlayerOrException();
                  var d = ShadowSlaveMod.soul(p);
                  for (String id : d.traits) {
                    var t = com.shadowslave.player.Traits.get(id);
                    if (t != null)
                      c.getSource()
                          .sendSuccess(
                              () -> Component.literal(t.name() + " · " + t.description()), false);
                  }
                  return 1;
                }));
    root.then(
        Commands.literal("trait")
            .requires(a -> a.hasPermission(2))
            .then(
                Commands.argument("id", StringArgumentType.word())
                    .suggests(
                        (c, b) -> {
                          com.shadowslave.player.Traits.ALL.forEach(t -> b.suggest(t.id()));
                          return b.buildFuture();
                        })
                    .executes(
                        c -> {
                          var p = c.getSource().getPlayerOrException();
                          boolean granted =
                              com.shadowslave.player.Traits.grant(
                                  p, StringArgumentType.getString(c, "id"));
                          SpellNetwork.sync(p);
                          return granted ? 1 : 0;
                        })));
    root.then(
        Commands.literal("fragments")
            .requires(a -> a.hasPermission(2))
            .then(
                Commands.argument("amount", IntegerArgumentType.integer(0, 2_000_000))
                    .executes(
                        c -> {
                          var p = c.getSource().getPlayerOrException();
                          var d = ShadowSlaveMod.soul(p);
                          d.fragments = IntegerArgumentType.getInteger(c, "amount");
                          d.cores();
                          com.shadowslave.player.Traits.check(p);
                          d.essence = d.maxEssence();
                          SpellNetwork.sync(p);
                          return 1;
                        })));
    root.then(
        Commands.literal("cores")
            .requires(a -> a.hasPermission(2))
            .then(
                Commands.argument("count", IntegerArgumentType.integer(1, 7))
                    .executes(
                        c -> {
                          var p = c.getSource().getPlayerOrException();
                          var d = ShadowSlaveMod.soul(p);
                          if (d.aspectRank != 6) {
                            c.getSource()
                                .sendFailure(
                                    Component.literal(
                                        "Only Divine Aspects form additional cores."));
                            return 0;
                          }
                          d.formedCores = IntegerArgumentType.getInteger(c, "count");
                          d.fragments = Rules.coreFloor(d.formedCores);
                          d.essence = d.maxEssence();
                          SpellNetwork.sync(p);
                          return 1;
                        })));
    root.then(
        Commands.literal("growth")
            .executes(
                c -> {
                  var p = c.getSource().getPlayerOrException();
                  var d = ShadowSlaveMod.soul(p);
                  c.getSource()
                      .sendSuccess(
                          () ->
                              Component.literal(
                                  String.format(
                                      java.util.Locale.ROOT,
                                      "Cores %d · saturation %.1f%% · ability ×%.2f · health %.1f ·"
                                          + " Essence %d · cooldown R/G/H %.1f/%.1f/%.1fs",
                                      d.cores(),
                                      Rules.saturation(d.cores(), d.fragments) * 100,
                                      com.shadowslave.player.SoulGrowth.ability(d),
                                      p.getMaxHealth(),
                                      d.maxEssence(),
                                      com.shadowslave.player.SoulGrowth.cooldown(
                                              d, d.soulRank >= 2 ? 140 : 280)
                                          / 20.0,
                                      com.shadowslave.player.SoulGrowth.cooldown(d, 240) / 20.0,
                                      com.shadowslave.player.SoulGrowth.cooldown(d, 360) / 20.0)),
                          false);
                  return 1;
                }));
    event.getDispatcher().register(root);
  }
}
