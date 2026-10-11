package com.shadowslave.test;

import com.mojang.authlib.GameProfile;
import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.combat.BeastForms;
import com.shadowslave.entity.CreatureEcology;
import com.shadowslave.player.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(ShadowSlaveMod.ID)
@PrefixGameTestTemplate(false)
public final class GrowthGameTests {
  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static FakePlayer player(GameTestHelper h, String name) {
    return new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
  }

  @GameTest(template = "empty")
  public static void trialVisualLookupHandlesMissingSpecies(GameTestHelper h) {
    var mob = ShadowSlaveMod.CREATURE.get().create(h.getLevel());
    mob.configure(0, false, 0, null, null, 0);
    check(
        mob.species() == null && CreatureEcology.renderIndex(mob.species()) == -1,
        "Trial creature resolves its legacy texture without List.of.indexOf(null)");
    mob.configure(6, true, 1, null, null, 0);
    check(CreatureEcology.renderIndex(mob.species()) == -1, "Trial boss uses safe fallback");
    for (var species : CreatureEcology.ALL) {
      mob.configureSpecies(species);
      check(
          CreatureEcology.renderIndex(mob.species()) >= 0, "Every species has an indexed material");
    }
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void growthIsContinuousAndRanked(GameTestHelper h) {
    var d = new SoulData();
    d.soulRank = 2;
    d.aspectRank = 6;
    double power = 0, health = 0;
    int essence = 0, cooldown = 1000;
    for (int fragments = 0; fragments <= 28000; fragments += 10) {
      d.fragments = fragments;
      double next = SoulGrowth.ability(d), hp = SoulGrowth.health(d);
      int cap = d.maxEssence(), cd = SoulGrowth.cooldown(d, 240);
      check(
          next >= power && hp >= health && cap >= essence && cd <= cooldown,
          "Each saturation increment improves/preserves power, health, Essence and cooldown");
      power = next;
      health = hp;
      essence = cap;
      cooldown = cd;
    }
    check(d.cores() == 7 && d.growth() == 7, "Seventh core saturates without an eighth core");
    d.fragments = 2_000_000;
    check(d.cores() == 7 && d.growth() == 7, "Seven-core cap");
    d.fragments = 0;
    check(d.cores() == 7, "Formed cores survive fragment loss");
    var copy = new SoulData();
    copy.deserializeNBT(
        h.getLevel().registryAccess(), d.serializeNBT(h.getLevel().registryAccess()));
    check(copy.cores() == 7, "Core formation persists");
    var single = new SoulData();
    single.soulRank = 1;
    single.aspectRank = 5;
    single.fragments = 100000;
    check(single.cores() == 1 && single.growth() == 1, "Non-Divine retains one saturated core");
    double previous = 0;
    for (int rank = 1; rank <= 4; rank++) {
      single.soulRank = rank;
      double next = SoulGrowth.ability(single);
      check(next > previous, "Soul advancement strengthens every ability slot");
      previous = next;
    }
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void traitsArePermanentAndDoNotStackOnReplay(GameTestHelper h) {
    var p = player(h, "TraitsTest");
    var d = ShadowSlaveMod.soul(p);
    d.family = 4;
    d.soulRank = 2;
    d.totalKills = 100;
    d.eliteKills = 10;
    d.fragments = 1000;
    Traits.check(p);
    check(
        d.traits.containsAll(
            List.of(
                "thorn_blessed", "survivor", "hunter", "predator", "ancient_slayer", "saturated")),
        "Combat and cultivation milestones grant traits");
    int count = d.traits.size();
    var bonus = Traits.stats(d);
    Traits.check(p);
    check(
        d.traits.size() == count && bonus.equals(Traits.stats(d)),
        "Repeated checks cannot stack permanent bonuses");
    d.fragments = 0;
    d.totalKills = 0;
    var copy = new SoulData();
    copy.deserializeNBT(
        h.getLevel().registryAccess(), d.serializeNBT(h.getLevel().registryAccess()));
    check(copy.traits.equals(d.traits), "Fragment loss and save/load retain traits");
    SoulGrowth.apply(p);
    double hp = p.getMaxHealth();
    SoulGrowth.apply(p);
    check(
        p.getMaxHealth() == hp && hp > 20,
        "Health modifier changes actual maximum health exactly once");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void sevenBeastBodiesMatchPhysicsAndCleanFlight(GameTestHelper h) {
    var p = player(h, "BeastBodiesTest");
    p.moveTo(60000, 250, 60000, 0, 0);
    var d = ShadowSlaveMod.soul(p);
    d.soulRank = 4;
    d.aspectRank = 6;
    double humanWidth = p.getBbWidth(), humanHeight = p.getBbHeight();
    for (int family = 0; family < 7; family++) {
      d.family = family;
      d.transformCooldownUntil = 0;
      d.essence = d.maxEssence();
      BeastForms.transform(p);
      var form = BeastForms.ALL.get(family);
      check(BeastForms.active(p) == family, "Form activated");
      check(
          Math.abs(p.getBbWidth() - form.width()) < .001
              && Math.abs(p.getBbHeight() - form.height()) < .001,
          "Actual box matches beast proportions");
      check(
          Math.abs(
                  p.getDimensions(net.minecraft.world.entity.Pose.CROUCHING).width() - form.width())
              < .001,
          "Crouching cannot shrink a beast through narrow gaps");
      check(p.getAbilities().mayfly == form.flies(), "Flight is family-specific");
      BeastForms.end(p);
      check(
          Math.abs(p.getBbWidth() - humanWidth) < .001
              && Math.abs(p.getBbHeight() - humanHeight) < .001
              && !p.getAbilities().mayfly
              && !p.getAbilities().flying,
          "Dismissal restores normal body and removes granted flight");
    }
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void transformationCannotClipThroughCeiling(GameTestHelper h) {
    var p = player(h, "BeastRoomTest");
    var pos = h.absolutePos(new BlockPos(1, 1, 1));
    p.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
    var d = ShadowSlaveMod.soul(p);
    d.soulRank = 4;
    d.family = 3;
    d.essence = d.maxEssence();
    int before = d.essence;
    h.getLevel()
        .setBlockAndUpdate(
            pos.above(3), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
    BeastForms.transform(p);
    check(
        BeastForms.active(p) < 0 && d.essence == before && d.transformCooldownUntil == 0,
        "Insufficient headroom fails without spending Essence or cooldown");
    h.succeed();
  }
}
