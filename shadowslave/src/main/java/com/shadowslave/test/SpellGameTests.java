package com.shadowslave.test;

import com.mojang.authlib.GameProfile;
import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.content.Content;
import com.shadowslave.core.Rules;
import com.shadowslave.entity.NightmareCreature;
import com.shadowslave.memory.*;
import com.shadowslave.nightmare.*;
import com.shadowslave.player.SoulData;
import com.shadowslave.world.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(ShadowSlaveMod.ID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
  /**
   * FakePlayer has no Netty channel. Simulate relocation while exercising real escrow/reward code.
   */
  private static final class TestPlayer extends FakePlayer {
    TestPlayer(net.minecraft.server.level.ServerLevel level, String name) {
      super(level, new GameProfile(UUID.randomUUID(), name));
    }

    @Override
    public void teleportTo(
        net.minecraft.server.level.ServerLevel level,
        double x,
        double y,
        double z,
        float yaw,
        float pitch) {
      setServerLevel(level);
      moveTo(x, y, z, yaw, pitch);
    }
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  @GameTest(template = "empty", timeoutTicks = 1200)
  public static void chainedArchipelagoHasGapsAndLinks(GameTestHelper h) {
    var key =
        net.minecraft.resources.ResourceKey.create(
            Registries.DIMENSION, ShadowSlaveMod.id("archipelago_test"));
    var l = h.getLevel().getServer().getLevel(key);
    check(l != null, "Isolated archipelago test dimension available");
    var edge = com.shadowslave.world.Archipelago.links(com.shadowslave.world.Archipelago.cluster(com.shadowslave.world.RealmMap.layout().seed,40,40)).getFirst();
    var a = edge.a();
    var b = edge.b();
    var pa = new BlockPos(a.x(), a.y() - 5, a.z());
    var pb = new BlockPos(b.x(), b.y() - 5, b.z());
    l.getChunkAt(pa);
    l.getChunkAt(pb);
    check(
        l.getBlockState(pa).isSolid() && l.getBlockState(pb).isSolid(),
        "Two distinct islands have solid cores");
    int mx = (a.x() + b.x()) / 2, mz = (a.z() + b.z()) / 2;
    double distance=Math.hypot(b.x()-a.x(),b.z()-a.z());
    var gap = new BlockPos(mx+(int)((a.z()-b.z())/distance*24),110,mz+(int)((b.x()-a.x())/distance*24));
    l.getChunkAt(gap);
    for (int y = 75; y < 195; y++)
      check(
          l.getBlockState(new BlockPos(gap.getX(), y, gap.getZ())).isAir(),
          "True open gap between island bodies at " + y);
    boolean link = false;
    for (int dx = -2; dx <= 2; dx++)
      for (int dz = -2; dz <= 2; dz++)
        for (int y = 95; y < 180; y++) {
          var p = new BlockPos(mx + dx, y, mz + dz);
          l.getChunkAt(p);
          if (l.getBlockState(p).is(RealmBlocks.IRON.get())) link = true;
        }
    check(link, "Generated iron catenary crosses the gap");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void expandedMemoriesAndLandmarks(GameTestHelper h) {
    check(Content.memories.size() == 128, "128 collectible Memories");
    check(
        Content.memories.values().stream().map(Content.Memory::design).distinct().count() == 128,
        "Every Memory has its own stable visual ID");
    check(
        Content.memories.values().stream().filter(m -> m.type().equals("armor")).count() == 16,
        "16 full armor sets");
    check(RealmPalette.BIOMES.size() == 17, "17 distinct biome palettes");
    for (var m : Content.memories.values()) {
      check(m.design() > 0 && m.design() <= 128, "Bounded model ID");
      check(
          RealmPalette.BIOMES.containsKey(m.biome()),
          "Every Memory has an existing discovery region");
      check(m.rank() <= 2, "Every new Memory is reachable with current progression");
    }
    var l = h.getLevel();
    int x = 11000;
    for (String biome :
        List.of("blossom_vale", "verdant_gardens", "mosslight_ravine", "golden_grove")) {
      var p = new BlockPos(x, 80, 0);
      l.getChunkAt(p);
      FantasyLandmarks.build(l, p, biome, true, net.minecraft.util.RandomSource.create(9));
      check(
          l.getBlockState(p.offset(0, 0, -2)).is(ShadowSlaveMod.GATEWAY.get()),
          "Landmark has a working gateway");
      check(
          l.getBlockEntity(p.offset(2, 0, 2))
              instanceof net.minecraft.world.level.block.entity.ChestBlockEntity,
          "Landmark has a real loot chest");
      x += 32;
    }
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void evaluationAndClock(GameTestHelper h) {
    int[] boundary = {0, 99, 100, 224, 225, 399, 400, 599, 600, 849, 850, 999999};
    int[] ranks = {0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5};
    for (int i = 0; i < boundary.length; i++)
      check(Rules.aspectRank(boundary[i], false) == ranks[i], "Appraisal boundary " + boundary[i]);
    check(Rules.aspectRank(0, true) == 6, "Only the boss unlocks Divine, even at zero points");
    check(Rules.countdownDelta(13000, 24000) == 11000, "Skipped night must count");
    check(Rules.countdownDelta(24000, 0) == 1, "Clock reset must not subtract progress");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void memoryLoadoutAndProtection(GameTestHelper h) {
    var d = new SoulData();
    d.soulRank = 2;
    d.stage = "awakened";
    d.owned.addAll(Content.memories.keySet());
    check(Memories.equip(d, "ashen_shroud"), "Equip whole-body armor");
    check(!Memories.equip(d, "awakened_mantle"), "Second whole-body armor rejected");
    check(Memories.equip(d, "fading_ember"), "Equip charm");
    check(!Memories.equip(d, "divine_spark"), "Second charm rejected");
    for (String id :
        List.of("silent_steps", "tide_crown", "iron_heart", "thorn_greaves", "soul_edge"))
      check(Memories.equip(d, id), "Equip " + id);
    check(d.equipped.size() == 7 && !Memories.equip(d, "mirror_knife"), "Seven-slot ceiling");
    check(
        Math.abs(Rules.protection(.4, Map.of("feet", .2)) - .35) < .00001,
        "Special armor replaces its coverage instead of stacking");
    d.activeMemories = false;
    check(Memories.protection(d, true) == 0, "Dismissed armor gives no protection");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void persistence(GameTestHelper h) {
    var d = new SoulData();
    d.family = 6;
    d.aspectRank = 6;
    d.soulRank = 2;
    d.fragments = 2020;
    d.essence = 153;
    d.elapsed = 34567;
    d.instance = UUID.randomUUID();
    d.owned.add("soul_edge");
    d.equipped.add("soul_edge");
    var saved = d.serializeNBT(h.getLevel().registryAccess());
    var reloaded = new SoulData();
    reloaded.deserializeNBT(h.getLevel().registryAccess(), saved);
    check(
        reloaded.family == 6 && reloaded.instance.equals(d.instance) && reloaded.elapsed == 34567,
        "Player state survives NBT round-trip");
    check(
        reloaded.cores() == 2 && reloaded.essence == 153 && reloaded.equipped.equals(d.equipped),
        "Essence, cores and ownership survive reload");
    for (long seed = 0; seed < 100; seed++) {
      var t = new TrialInstance(UUID.randomUUID(), 3, 11, seed, 0);
      t.points = 600;
      t.altars = 5;
      t.bossId = UUID.randomUUID();
      var copy = TrialInstance.load(t.save());
      check(
          copy.bossSite == t.bossSite
              && copy.bossId.equals(t.bossId)
              && copy.points == 600
              && copy.altars == 5,
          "Trial reload must not reroll or reset");
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 400)
  public static void dimensionsAndTerrain(GameTestHelper h) {
    var server = h.getLevel().getServer();
    for (int i = 0; i < 7; i++)
      check(server.getLevel(Nightmares.dimension(i)) != null, "Missing trial dimension " + i);
    var dream = server.getLevel(DreamRealm.KEY);
    check(dream != null, "Missing Dream Realm");
    dream.getChunk(0, 0);
    for (String name :
        List.of(
            "forgotten_shore",
            "chained_isles",
            "hollow_mountains",
            "nightmare_desert",
            "death_zone",
            "ember_haven"))
      check(
          dream
              .registryAccess()
              .registryOrThrow(Registries.BIOME)
              .containsKey(ShadowSlaveMod.id(name)),
          "Missing biome " + name);
    check(dream.getChunk(0, 0).getMinBuildHeight() == -64, "Noise chunks generated");
    var fresh = dream.getChunk(73, 29);
    boolean originalSurface = false;
    outer:
    for (int x = 0; x < 16; x++)
      for (int z = 0; z < 16; z++)
        for (int y = -63; y < 240; y++) {
          var block = fresh.getBlockState(new BlockPos(73 * 16 + x, y, 29 * 16 + z)).getBlock();
          if (net.minecraft.core.registries.BuiltInRegistries.BLOCK
              .getKey(block)
              .getNamespace()
              .equals(ShadowSlaveMod.ID)) {
            originalSurface = true;
            break outer;
          }
        }
    check(originalSurface, "Noise generation uses original Dream Realm materials");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void allTrialBlueprints(GameTestHelper h) {
    for (int family = 0; family < 7; family++) {
      var t = new TrialInstance(UUID.randomUUID(), family, 0, 1, 0);
      var blueprint = TrialBuilder.blueprint(t);
      var blocks = new HashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
      blueprint.forEach(p -> blocks.put(p.pos(), p.state()));
      for (int i = 0; i < 5; i++) {
        var site = t.arena(i);
        check(blocks.get(site.below()).isSolid(), "Arena " + i + " has a solid floor");
        var bounds =
            NightmareCreature.bodyDimensions(family, true)
                .makeBoundingBox(site.getX() + .5, site.getY(), site.getZ() + .5);
        for (var pos :
            BlockPos.betweenClosed(
                (int) Math.floor(bounds.minX),
                (int) Math.floor(bounds.minY),
                (int) Math.floor(bounds.minZ),
                (int) Math.ceil(bounds.maxX) - 1,
                (int) Math.ceil(bounds.maxY) - 1,
                (int) Math.ceil(bounds.maxZ) - 1))
          check(
              !blocks.containsKey(pos) || blocks.get(pos).isAir(),
              "Full boss body fits arena " + i + " in family " + family + " at " + pos);
      }
      for (int i = 0; i < t.seals(); i++)
        check(
            blocks.get(t.altar(i)).is(ShadowSlaveMod.ALTAR.get()),
            "Objective accessible in family " + family);
      check(blocks.get(t.exit()).is(ShadowSlaveMod.EXIT.get()), "Exit placed");
    }
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void creatureSizesSurviveSaveAndSync(GameTestHelper h) {
    for (int family = 0; family < 7; family++)
      for (boolean boss : new boolean[] {false, true}) {
        var mob = ShadowSlaveMod.CREATURE.get().create(h.getLevel());
        mob.configure(family, boss, 1, null, null, -1);
        var size = NightmareCreature.bodyDimensions(family, boss);
        check(
            size.width() > 1 && size.height() > 3, "Nightmares must exceed ordinary humanoid size");
        check(
            Math.abs(mob.getBoundingBox().getXsize() - size.width()) < .0001
                && Math.abs(mob.getBoundingBox().getYsize() - size.height()) < .0001,
            "Physical box matches family/boss dimensions");
        check(
            Math.abs(mob.getEyeHeight() - size.eyeHeight()) < .0001,
            "Targeting eye height scales with body");
        var copy = ShadowSlaveMod.CREATURE.get().create(h.getLevel());
        copy.load(mob.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
        check(
            copy.family() == family
                && copy.isBoss() == boss
                && Math.abs(copy.getBbHeight() - size.height()) < .0001,
            "Save/reload preserves hitbox");
        var synced = ShadowSlaveMod.CREATURE.get().create(h.getLevel());
        var values = mob.getEntityData().getNonDefaultValues();
        if (values != null) synced.getEntityData().assignValues(values);
        check(
            Math.abs(synced.getBbWidth() - size.width()) < .0001
                && Math.abs(synced.getBbHeight() - size.height()) < .0001,
            "Synced family/boss flags refresh receiving entity dimensions");
        copy.getAttribute(Attributes.SCALE).setBaseValue(1.25);
        copy.refreshDimensions();
        check(
            Math.abs(copy.getBbHeight() - size.height() * 1.25) < .0001,
            "Vanilla scale attribute remains compatible");
      }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 200)
  public static void oversizedSpawnChecksFullBody(GameTestHelper h) {
    var level = h.getLevel().getServer().getLevel(Nightmares.dimension(0));
    var o = new BlockPos(9000, 81, 0);
    for (int x = -3; x <= 3; x++)
      for (int z = -3; z <= 3; z++)
        for (int y = -1; y <= 7; y++)
          level.setBlock(
              o.offset(x, y, z), (y == -1 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
    var mob = ShadowSlaveMod.CREATURE.get().create(level);
    mob.configure(6, true, 1, null, null, -1);
    check(mob.canStandAt(o), "Giant can spawn on clear platform");
    level.setBlock(o.above(5), Blocks.STONE.defaultBlockState(), 2);
    check(!mob.canStandAt(o), "5.7-block boss rejected under low ceiling");
    level.setBlock(o.above(5), Blocks.AIR.defaultBlockState(), 2);
    level.setBlock(o.east().above(), Blocks.STONE.defaultBlockState(), 2);
    check(!mob.canStandAt(o), "Boss shoulders cannot spawn inside adjacent wall");
    level.setBlock(o.east().above(), Blocks.AIR.defaultBlockState(), 2);
    level.setBlock(o.below(), Blocks.AIR.defaultBlockState(), 2);
    check(!mob.canStandAt(o), "Unsupported spawn rejected");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 300)
  public static void sanctuaryProtectsAndHasWarmShelter(GameTestHelper h) {
    var level = h.getLevel().getServer().getLevel(DreamRealm.KEY);
    var o = new BlockPos(0, 240, 0);
    Sanctuary.build(level, o, false);
    check(
        Sanctuary.isSafe(level, o) && Sanctuary.isSafe(level, new BlockPos(160, 240, 0)),
        "Both guaranteed refuges are protected");
    check(!Sanctuary.isSafe(h.getLevel(), o), "Protection never leaks into other dimensions");
    check(
        level.getBlockState(o.below()).is(RealmBlocks.LIMESTONE.get()),
        "Arrival has solid limestone footing");
    check(
        level.getBlockState(o.offset(4, 0, 5)).getLightEmission() == 15,
        "Warm sanctuary lantern emits real block light");
    var player = new TestPlayer(level, "SanctuaryTest");
    player.moveTo(.5, 240, .5, 0, 0);
    var mob = ShadowSlaveMod.CREATURE.get().create(level);
    mob.configure(3, true, 1, null, null, -1);
    check(
        !mob.doHurtTarget(player) && player.getHealth() == player.getMaxHealth(),
        "Nightmare cannot attack a sheltered player");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 400)
  public static void completeTrialRestoresInventory(GameTestHelper h) {
    var p = new TestPlayer(h.getLevel(), "SpellCompletionTest");
    p.getInventory().add(new ItemStack(Items.DIAMOND, 7));
    p.experienceLevel = 12;
    var d = ShadowSlaveMod.soul(p);
    d.family = 0;
    var saved = SpellSavedData.get(p.server);
    var t = new TrialInstance(p.getUUID(), 0, saved.nextSlot++, 42, 0);
    t.state = "active";
    saved.instances.put(t.id, t);
    d.instance = t.id;
    d.stage = "preparing";
    Nightmares.enter(p, t, p.server.getLevel(Nightmares.dimension(0)));
    check(
        d.inventoryStored && p.getInventory().countItem(Items.DIAMOND) == 0,
        "Ordinary inventory escrowed");
    check(p.getInventory().countItem(Items.STONE_SWORD) == 1, "Trial kit granted once");
    t.altars = 63;
    t.bossDefeated = true;
    Nightmares.finish(p, t.exit());
    check(
        d.aspectRank == 6 && d.soulRank == 1 && d.stage.equals("sleeper"),
        "Divine awarded on completion");
    check(
        p.getInventory().countItem(Items.DIAMOND) == 7
            && p.getInventory().countItem(Items.IRON_SWORD) == 0
            && p.experienceLevel == 12,
        "Original inventory and XP restored without trial items");
    check(d.equipped.size() == 3 && !d.inventoryStored, "Starter loadout and consumed escrow");
    saved.instances.remove(t.id);
    saved.setDirty();
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 400)
  public static void failedTrialRestoresInventory(GameTestHelper h) {
    var p = new TestPlayer(h.getLevel(), "SpellFailureTest");
    p.getInventory().add(new ItemStack(Items.EMERALD, 3));
    var d = ShadowSlaveMod.soul(p);
    d.family = 2;
    var saved = SpellSavedData.get(p.server);
    var t = new TrialInstance(p.getUUID(), 2, saved.nextSlot++, 13, 0);
    t.state = "active";
    saved.instances.put(t.id, t);
    d.instance = t.id;
    d.stage = "preparing";
    Nightmares.enter(p, t, p.server.getLevel(Nightmares.dimension(2)));
    t.bossDefeated = true;
    t.points = 900;
    Nightmares.fail(p);
    check(
        d.stage.equals("retry") && d.soulRank == 0 && d.aspectRank == 0,
        "Failure cannot award an Aspect, even after the boss");
    check(
        p.getInventory().countItem(Items.EMERALD) == 3 && !d.inventoryStored,
        "Failure restores belongings");
    int site = t.bossSite;
    Nightmares.start(p, -1);
    var retry = Nightmares.current(p);
    check(
        retry.family == 2 && retry.bossSite == site && retry.points == 0 && !retry.bossDefeated,
        "Retry resets rewards but preserves assignment and arena");
    saved.instances.remove(t.id);
    saved.instances.remove(retry.id);
    saved.setDirty();
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void killCreditIsIdempotent(GameTestHelper h) {
    var level = h.getLevel().getServer().getLevel(Nightmares.dimension(1));
    var p = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "SpellCreditTest"));
    var d = ShadowSlaveMod.soul(p);
    d.family = 1;
    d.stage = "nightmare";
    var saved = SpellSavedData.get(p.server);
    var t = new TrialInstance(p.getUUID(), 1, saved.nextSlot++, 55, 0);
    t.state = "active";
    saved.instances.put(t.id, t);
    d.instance = t.id;
    var mob = ShadowSlaveMod.CREATURE.get().create(level);
    mob.configure(1, false, 1, t.id, p.getUUID(), 0);
    t.liveEntities.put(0, mob.getUUID());
    Nightmares.kill(p, mob);
    int points = t.points;
    Nightmares.kill(p, mob);
    check(points > 0 && t.points == points, "Same encounter cannot award twice");
    var impostor = ShadowSlaveMod.CREATURE.get().create(level);
    impostor.configure(1, true, 1, t.id, p.getUUID(), -1);
    t.bossId = UUID.randomUUID();
    Nightmares.kill(p, impostor);
    check(!t.bossDefeated, "Unregistered boss cannot grant Divine");
    saved.instances.remove(t.id);
    saved.setDirty();
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void rankedMemoriesPersistAndArmorWorks(GameTestHelper h) {
    var p = new TestPlayer(h.getLevel(), "RankedMemoryTest");
    var d = ShadowSlaveMod.soul(p);
    d.soulRank = 2;
    d.stage = "awakened";
    Memories.grant(p, "ashen_shroud", 2, 4);
    check(
        d.owned.contains("ashen_shroud")
            && Memories.rank(d, "ashen_shroud") == 2
            && Memories.tier(d, "ashen_shroud") == 4,
        "Killed Fallen Devil yields Ascended IV exactly");
    check(Memories.equip(d, "ashen_shroud"), "Ranked armor equips");
    Memories.updateArmor(p);
    check(p.getAttributeValue(Attributes.ARMOR) > 19, "Memory gives real armor points");
    check(p.getAttributeValue(Attributes.ARMOR_TOUGHNESS) > 0, "Memory gives real toughness");
    var copy = new SoulData();
    copy.deserializeNBT(
        h.getLevel().registryAccess(), d.serializeNBT(h.getLevel().registryAccess()));
    check(
        Memories.rank(copy, "ashen_shroud") == 2 && Memories.tier(copy, "ashen_shroud") == 4,
        "Rank and tier survive reload");
    d.activeMemories = false;
    Memories.updateArmor(p);
    check(
        p.getAttributeValue(Attributes.ARMOR) == 0
            && p.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 0,
        "Dismissal removes armor attributes");
    var token = MemoryTokenItem.create("soul_edge", 3, 6);
    check(
        MemoryTokenItem.rank(token) == 3 && MemoryTokenItem.tier(token) == 6,
        "Crystallized Memory preserves both axes");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void gatewayUnlocksSecondAbility(GameTestHelper h) {
    var level = h.getLevel().getServer().getLevel(DreamRealm.KEY);
    var p = new TestPlayer(level, "AwakeningTest");
    var d = ShadowSlaveMod.soul(p);
    d.family = 1;
    d.soulRank = 1;
    d.stage = "sleeper";
    check(!com.shadowslave.combat.Aspects.canUseAwakened(d), "Sleeper cannot cast second ability");
    DreamRealm.gateway(p, new BlockPos(160, 240, 0));
    check(
        d.soulRank == 2
            && d.stage.equals("awakened")
            && com.shadowslave.combat.Aspects.canUseAwakened(d),
        "First Gateway return unlocks second ability");
    var old = d.cooldownUntil;
    com.shadowslave.combat.Aspects.castAwakened(p);
    check(
        d.awakenedCooldownUntil > 0 && d.cooldownUntil == old,
        "Awakened ability uses independent cooldown");
    check(
        Rules.cores(6, 999) == 1
            && Rules.cores(6, 1000) == 2
            && Rules.cores(6, 2999) == 2
            && Rules.cores(6, 3000) == 3
            && Rules.cores(6, 21000) == 7,
        "Divine cores use progressive saturation costs");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void ecologyHasDistinctBodiesAndSavedIdentity(GameTestHelper h) {
    var all = com.shadowslave.entity.CreatureEcology.ALL;
    check(all.size() == 32, "32 creature species");
    check(
        all.stream().map(com.shadowslave.entity.CreatureEcology.Species::body).distinct().count()
            == 12,
        "12 body archetypes");
    for (var species : all) {
      var mob = ShadowSlaveMod.CREATURE.get().create(h.getLevel());
      mob.configureSpecies(species);
      check(
          mob.rank == species.rank() && mob.creatureClass == species.cores(),
          "Species rank and class exact");
      check(
          Math.abs(mob.getBbWidth() - species.width()) < .001
              && Math.abs(mob.getBbHeight() - species.height()) < .001,
          "Species has matching custom hitbox");
      var copy = ShadowSlaveMod.CREATURE.get().create(h.getLevel());
      copy.load(mob.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
      check(
          copy.speciesId().equals(species.id())
              && Math.abs(copy.getBbHeight() - species.height()) < .001,
          "Saved species restores dimensions");
      var synced = ShadowSlaveMod.CREATURE.get().create(h.getLevel());
      var values = mob.getEntityData().getNonDefaultValues();
      if (values != null) synced.getEntityData().assignValues(values);
      check(
          synced.speciesId().equals(species.id())
              && Math.abs(synced.getBbWidth() - species.width()) < .001,
          "Client identity controls matching hitbox");
      check(
          !com.shadowslave.entity.CreatureEcology.rewards(species, species.regions().split(",")[0])
              .isEmpty(),
          "Every species has a Memory pool");
    }
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void earnedEquipmentIsCopiedAndRestored(GameTestHelper h) {
    var p = new TestPlayer(h.getLevel(), "EarnedGearTest");
    p.getInventory().add(new ItemStack(Items.DIAMOND_SWORD));
    p.setItemSlot(
        net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
    var d = ShadowSlaveMod.soul(p);
    d.family = 3;
    var saved = SpellSavedData.get(p.server);
    var t = new TrialInstance(p.getUUID(), 3, saved.nextSlot++, 77, 0);
    t.state = "active";
    saved.instances.put(t.id, t);
    d.instance = t.id;
    d.stage = "preparing";
    Nightmares.enter(p, t, p.server.getLevel(Nightmares.dimension(3)));
    check(
        p.getInventory().countItem(Items.DIAMOND_SWORD) == 1
            && p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST)
                .is(Items.DIAMOND_CHESTPLATE),
        "Earned weapon and armor have trial equivalents");
    Nightmares.fail(p);
    check(
        p.getInventory().countItem(Items.DIAMOND_SWORD) == 1
            && p.getInventory().countItem(Items.DIAMOND_CHESTPLATE) == 1,
        "Escrow restores originals with no duplicate copies");
    saved.instances.remove(t.id);
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void citadelAndLegacyLayouts(GameTestHelper h) {
    var center = new BlockPos(480, 100, 0);
    var blocks = new HashMap<BlockPos, net.minecraft.world.level.block.Block>();
    GrandCitadels.blueprint(center, 0).forEach(a -> blocks.put(a.pos(), a.block()));
    check(
        blocks.get(center.offset(0, 2, -10)) == ShadowSlaveMod.GATEWAY.get(),
        "Grand citadel has a functional Gateway");
    check(
        blocks.get(center.offset(-35, 24, -35)) == RealmBlocks.LANTERN.get(),
        "Watchtower reaches 24 blocks");
    check(blocks.get(center.offset(0, 4, 40)) == Blocks.AIR, "Southern gate is open");
    check(
        blocks.get(center.offset(2, 1, -10)) != Blocks.AIR
            && blocks.get(center.offset(2, 2, -10)) == Blocks.AIR,
        "Gateway anchor has floor and clear feet");
    var t = new TrialInstance(UUID.randomUUID(), 0, 0, 1, 0);
    var tag = t.save();
    tag.remove("layout");
    var old = TrialInstance.load(tag);
    check(
        old.seals() == 3 && old.start().equals(new BlockPos(0, 81, 50)),
        "Old trial layout remains loadable");
    check(
        t.seals() == 6 && t.contains(new BlockPos(120, 81, 120)),
        "Expanded trial has six objectives and wider bounds");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void echoOwnershipAndTargets(GameTestHelper h) {
    var p = new TestPlayer(h.getLevel(), "EchoOwnerTest");
    var echo = ShadowSlaveMod.CREATURE.get().create(h.getLevel());
    echo.configureSpecies(com.shadowslave.entity.CreatureEcology.get("carapace_scavenger"));
    echo.makeEcho(p.getUUID());
    var hostile = ShadowSlaveMod.CREATURE.get().create(h.getLevel());
    hostile.configureSpecies(com.shadowslave.entity.CreatureEcology.get("iron_spider"));
    check(
        !echo.canAttack(p) && echo.canAttack(hostile), "Echo attacks creatures, never its player");
    var copy = ShadowSlaveMod.CREATURE.get().create(h.getLevel());
    copy.load(echo.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
    check(copy.isEcho() && copy.owner.equals(p.getUUID()), "Echo identity and ownership persist");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 400)
  public static void laterNightmaresKeepAspectAndWakingAnchor(GameTestHelper h) {
    var dream = h.getLevel().getServer().getLevel(DreamRealm.KEY);
    var p = new TestPlayer(dream, "AscensionTest");
    var d = ShadowSlaveMod.soul(p);
    d.family = 1;
    d.aspectRank = 5;
    d.soulRank = 2;
    d.stage = "awakened";
    d.owned.add("soul_edge");
    d.equipped.add("soul_edge");
    d.returnPoint.putString("dimension", "minecraft:overworld");
    d.returnPoint.putDouble("x", 31);
    d.returnPoint.putDouble("y", 81);
    d.returnPoint.putDouble("z", 12);
    check(Nightmares.ascend(p), "Awakened can challenge Second Nightmare");
    var t = Nightmares.current(p);
    check(t.targetRank == 3, "Second Nightmare targets Ascended rank");
    t.state = "active";
    Nightmares.enter(p, t, p.server.getLevel(Nightmares.dimension(1)));
    t.altars = 63;
    Nightmares.finish(p, t.exit());
    check(d.soulRank == 2, "Cannot ascend before resolving ruler conflict");
    t.bossDefeated = true;
    Nightmares.finish(p, t.exit());
    check(
        d.soulRank == 3 && d.aspectRank == 5 && d.equipped.contains("soul_edge"),
        "Ascension preserves Aspect rank and Memory loadout");
    check(
        p.level().dimension().equals(DreamRealm.KEY)
            && d.returnPoint.getString("dimension").equals("minecraft:overworld")
            && d.returnPoint.getDouble("x") == 31,
        "Trial returns to Dream Realm and preserves waking destination");
    check(Nightmares.ascend(p), "Master can challenge Third Nightmare");
    var third = Nightmares.current(p);
    third.state = "active";
    Nightmares.enter(p, third, p.server.getLevel(Nightmares.dimension(1)));
    Nightmares.fail(p);
    check(
        d.soulRank == 3
            && d.aspectRank == 5
            && d.returnPoint.getString("dimension").equals("minecraft:overworld"),
        "Failed higher Nightmare preserves rank and waking anchor");
    Nightmares.start(p, -1);
    check(Nightmares.current(p).targetRank == 4, "Retry preserves higher Nightmare target");
    var saved = SpellSavedData.get(p.server);
    saved.instances.remove(t.id);
    saved.instances.remove(third.id);
    saved.instances.remove(d.instance);
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void higherAbilitiesAndBattleFormExpire(GameTestHelper h) {
    var p = new TestPlayer(h.getLevel(), "BattleFormTest");
    p.moveTo(50000, 240, 50000, 0, 0);
    var d = ShadowSlaveMod.soul(p);
    d.family = 3;
    d.soulRank = 2;
    d.essence = 250;
    com.shadowslave.combat.HigherAspects.cast(p);
    check(d.ascendedCooldownUntil == 0, "Awakened cannot cast Master ability");
    d.soulRank = 3;
    com.shadowslave.combat.HigherAspects.cast(p);
    check(d.ascendedCooldownUntil > 0, "Master ability unlocks on Ascension");
    d.soulRank = 4;
    d.essence = d.maxEssence();
    double width = p.getBbWidth();
    com.shadowslave.combat.HigherAspects.transform(p);
    check(d.transformUntil > 0, "Saint form activates in clear space");
    check(p.getAttributeValue(Attributes.ARMOR) >= 8, "Saint form has physical armor");
    check(p.getBbWidth() > width, "Saint form expands real player hitbox");
    d.transformUntil = 0;
    com.shadowslave.combat.HigherAspects.passive(p);
    check(
        p.getAttributeValue(Attributes.ARMOR) == 0 && Math.abs(p.getBbWidth() - width) < .001,
        "Expired transformation removes modifiers and restores body size");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void soulShardsRequireInitiationAndPreserveValue(GameTestHelper h) {
    var p = new TestPlayer(h.getLevel(), "ShardTest");
    var d = ShadowSlaveMod.soul(p);
    var stack = SoulShardItem.create(2, 27);
    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
    ShadowSlaveMod.SOUL_SHARD
        .get()
        .use(h.getLevel(), p, net.minecraft.world.InteractionHand.MAIN_HAND);
    check(d.fragments == 0 && stack.getCount() == 1, "Aspirant cannot absorb a shard");
    d.soulRank = 1;
    ShadowSlaveMod.SOUL_SHARD
        .get()
        .use(h.getLevel(), p, net.minecraft.world.InteractionHand.MAIN_HAND);
    check(
        d.fragments == 27 && stack.isEmpty(),
        "Initiated player absorbs recorded shard value exactly once");
    h.succeed();
  }
}
