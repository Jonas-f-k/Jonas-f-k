package com.shadowslave.test;

import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.entity.*;
import com.shadowslave.world.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(ShadowSlaveMod.ID)
@PrefixGameTestTemplate(false)
public final class CreaturePresentationGameTests {
  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  @GameTest(template = "empty")
  public static void allSpeciesAreReachableInTheirEcology(GameTestHelper h) {
    var seen = new HashSet<String>();
    var random = RandomSource.create(71506);
    for (String biome : RealmPalette.BIOMES.keySet()) {
      for (int roll = 0; roll < 2000; roll++) {
        var species = CreatureEcology.choose(biome, true, random);
        if (species == null) continue;
        check(Arrays.asList(species.regions().split(",")).contains(biome),
            "Species escaped its declared habitat: " + species.id());
        seen.add(species.id());
      }
    }
    check(seen.size() == CreatureEcology.ALL.size(), "Some species cannot spawn: " + seen);
    for (String biome : List.of("chained_isles", "nightmare_desert", "moonlit_grove"))
      for (int roll = 0; roll < 200; roll++) {
        var species = CreatureEcology.choose(biome, false, random);
        check(species == null || !List.of("chalice_maiden", "dune_revenant", "moonveil_wraith")
            .contains(species.id()), "Nocturnal species spawned during daytime");
      }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 200)
  public static void elevatedGroundAndWideFootingAreValid(GameTestHelper h) {
    var level = h.getLevel().getServer().getLevel(com.shadowslave.nightmare.Nightmares.dimension(0));
    var ground = new BlockPos(9700, 239, 0);
    for (int x = -4; x <= 4; x++)
      for (int z = -4; z <= 4; z++)
        for (int y = 0; y <= 12; y++)
          level.setBlock(ground.offset(x, y, z),
              (y == 0 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
    var surface = DreamRealm.naturalSpawnSurface(level, ground.getX(), ground.getZ());
    check(surface.equals(ground.above()), "Elevated ground was clamped away from the island");
    var mob = ShadowSlaveMod.CREATURE.get().create(level);
    mob.configureSpecies(CreatureEcology.get("iron_spider_matriarch"));
    check(mob.canStandAt(surface), "Large creature rejected on its supported elevated platform");
    level.setBlock(ground.offset(-3, 0, -3), Blocks.AIR.defaultBlockState(), 2);
    check(!mob.canStandAt(surface), "Large creature accepted with a foot over the void");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 40)
  public static void attackPhasesReachTrackedClientsAndRecover(GameTestHelper h) {
    var mob = ShadowSlaveMod.CREATURE.get().create(h.getLevel());
    mob.configureSpecies(CreatureEcology.get("carapace_scavenger"));
    mob.setNoAi(true);
    mob.setTarget(EntityType.ZOMBIE.create(h.getLevel()));
    mob.doHurtTarget(mob.getTarget());
    check(mob.swinging && mob.attackPhase() == NightmareCreature.ATTACK_STRIKE,
        "Melee damage never started the vanilla swing and synchronized strike");
    mob.specialWindup = 26;
    mob.setCharging(true);
    check(mob.attackPhase() == NightmareCreature.ATTACK_WINDUP, "Windup not animated");
    var receiver = ShadowSlaveMod.CREATURE.get().create(h.getLevel());
    receiver.getEntityData().assignValues(mob.getEntityData().getNonDefaultValues());
    check(receiver.attackPhase() == NightmareCreature.ATTACK_WINDUP && receiver.isCharging(),
        "Client tracking mid-windup lost the animation state");
    mob.specialWindup = 0;
    mob.setCharging(false);
    check(mob.attackPhase() == NightmareCreature.ATTACK_STRIKE, "Special release has no strike");
    check(mob.attackAnimationProgress(.5f) > 0, "Strike progress does not interpolate");
    h.runAtTickTime(11, () -> {
      mob.tick();
      check(mob.attackPhase() == NightmareCreature.ATTACK_RECOVERY, "Strike never recovers");
    });
    h.runAtTickTime(20, () -> {
      mob.tick();
      check(mob.attackPhase() == NightmareCreature.ATTACK_IDLE, "Recovery never returns to idle");
      h.succeed();
    });
  }
  @GameTest(template="empty")
  public static void allBeastAbilitiesPublishTheirAttackPose(GameTestHelper h){
    var player=h.makeMockServerPlayerInLevel();var data=ShadowSlaveMod.soul(player);
    data.soulRank=4;data.aspectRank=6;data.stage="saint";
    long now=player.server.overworld().getGameTime();
    for(int form=0;form<7;form++){
      data.family=form;data.transformUntil=now+500;data.beastCooldownUntil=0;
      data.beastAttackUntil=0;data.essence=10000;
      com.shadowslave.combat.BeastForms.attack(player);
      check(data.beastAttackUntil==now+(form==0?40:18),"Beast ability omitted its tracked pose: "+form);
    }
    h.succeed();
  }
}
