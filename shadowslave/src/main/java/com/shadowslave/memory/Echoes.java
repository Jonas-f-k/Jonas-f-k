package com.shadowslave.memory;

import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.entity.*;
import com.shadowslave.network.SpellNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * One manifested companion at a time; acquired copies retain their creature's fixed rank and class.
 */
public final class Echoes {
  public static void summon(ServerPlayer p, String speciesId) {
    var d = ShadowSlaveMod.soul(p);
    if (!d.initiated() || !d.echoes.contains(speciesId)) return;
    var s = CreatureEcology.get(speciesId);
    if (s == null) return;
    for (var level : p.server.getAllLevels())
      for (var e : level.getAllEntities())
        if (e instanceof NightmareCreature n && n.isEcho() && p.getUUID().equals(n.owner))
          n.discard();
    if (!d.spend(40)) {
      p.displayClientMessage(Component.literal("You need 40 Essence to manifest an Echo."), true);
      return;
    }
    var mob = ShadowSlaveMod.CREATURE.get().create(p.serverLevel());
    if (mob == null) {
      d.refill(40);
      return;
    }
    mob.configureSpecies(s);
    mob.makeEcho(p.getUUID());
    for (int distance = 3; distance <= 8; distance++)
      for (int sign : new int[] {-1, 1}) {
        var pos = p.blockPosition().offset(distance * sign, 0, 2);
        if (mob.canStandAt(pos)) {
          mob.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
          p.serverLevel().addFreshEntity(mob);
          p.sendSystemMessage(Component.literal("[The Spell] Echo manifested: " + s.name()));
          SpellNetwork.sync(p);
          return;
        }
      }
    d.refill(40);
    p.displayClientMessage(Component.literal("Not enough space for this Echo."), true);
  }

  public static void reward(ServerPlayer p, NightmareCreature mob) {
    var d = ShadowSlaveMod.soul(p);
    if (mob.species() == null || mob.isEcho() || d.echoes.contains(mob.speciesId())) return;
    d.echoes.add(mob.speciesId());
    p.sendSystemMessage(
        Component.literal(
            "[The Spell] You have received an Echo: "
                + mob.species().name()
                + ". Summon with /spell echo "
                + mob.speciesId()));
  }
}
