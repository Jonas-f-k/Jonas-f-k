package com.shadowslave;

import com.mojang.logging.LogUtils;
import com.shadowslave.content.Content;
import com.shadowslave.core.Settings;
import com.shadowslave.entity.NightmareCreature;
import com.shadowslave.memory.MemoryTokenItem;
import com.shadowslave.network.SpellNetwork;
import com.shadowslave.player.SoulData;
import com.shadowslave.world.RealmBlocks;
import com.shadowslave.world.SpellBlock;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.*;
import org.slf4j.Logger;

@Mod(ShadowSlaveMod.ID)
public final class ShadowSlaveMod {
  public static final String ID = "shadowslave";
  public static final Logger LOGGER = LogUtils.getLogger();

  public static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath(ID, path);
  }

  public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
  public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
  public static final DeferredRegister<EntityType<?>> ENTITIES =
      DeferredRegister.create(Registries.ENTITY_TYPE, ID);
  public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
      DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ID);
  public static final Supplier<AttachmentType<SoulData>> SOUL =
      ATTACHMENTS.register(
          "soul", () -> AttachmentType.serializable(SoulData::new).copyOnDeath().build());
  public static final DeferredBlock<SpellBlock> ALTAR =
      BLOCKS.register(
          "trial_altar",
          () ->
              new SpellBlock(
                  SpellBlock.Kind.ALTAR,
                  BlockBehaviour.Properties.ofFullCopy(Blocks.CRYING_OBSIDIAN)
                      .strength(-1, 3600000)
                      .lightLevel(s -> 10)));
  public static final DeferredBlock<SpellBlock> EXIT =
      BLOCKS.register(
          "trial_exit",
          () ->
              new SpellBlock(
                  SpellBlock.Kind.EXIT,
                  BlockBehaviour.Properties.ofFullCopy(Blocks.CRYING_OBSIDIAN)
                      .strength(-1, 3600000)
                      .lightLevel(s -> 13)));
  public static final DeferredBlock<SpellBlock> GATEWAY =
      BLOCKS.register(
          "gateway",
          () ->
              new SpellBlock(
                  SpellBlock.Kind.GATEWAY,
                  BlockBehaviour.Properties.ofFullCopy(Blocks.CRYING_OBSIDIAN)
                      .strength(-1, 3600000)
                      .lightLevel(s -> 15)));
  public static final DeferredItem<com.shadowslave.memory.SoulShardItem> SOUL_SHARD =
      ITEMS.register(
          "soul_shard",
          () -> new com.shadowslave.memory.SoulShardItem(new Item.Properties().stacksTo(64)));
  public static final DeferredItem<com.shadowslave.nightmare.NightmareSeedItem> NIGHTMARE_SEED =
      ITEMS.register(
          "nightmare_seed",
          () -> new com.shadowslave.nightmare.NightmareSeedItem(new Item.Properties().stacksTo(1)));
  public static final DeferredItem<MemoryTokenItem> MEMORY =
      ITEMS.register("memory", () -> new MemoryTokenItem(new Item.Properties().stacksTo(1)));
  public static final DeferredItem<SwordItem> MANIFESTED_BLADE =
      ITEMS.register(
          "manifested_blade",
          () ->
              new SwordItem(
                  Tiers.IRON,
                  new Item.Properties()
                      .stacksTo(1)
                      .attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4f))));
  public static final Supplier<EntityType<NightmareCreature>> CREATURE =
      ENTITIES.register(
          "nightmare_creature",
          () ->
              EntityType.Builder.of(NightmareCreature::new, MobCategory.MONSTER)
                  .sized(2.10f, 4.20f)
                  .clientTrackingRange(12)
                  .build(ID + ":nightmare_creature"));
  public static final DeferredRegister<CreativeModeTab> TABS =
      DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
  public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>>
      FEATURES = DeferredRegister.create(Registries.FEATURE, ID);
  public static final Supplier<com.shadowslave.world.DreamRuinFeature> RUIN =
      FEATURES.register("dream_ruin", com.shadowslave.world.DreamRuinFeature::new);
  public static final Supplier<com.shadowslave.world.RealmDecorationFeature> DECORATION =
      FEATURES.register("realm_decoration", com.shadowslave.world.RealmDecorationFeature::new);
  public static final DeferredRegister<
          com.mojang.serialization.MapCodec<
              ? extends net.minecraft.world.level.levelgen.DensityFunction>>
      DENSITIES = DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, ID);
  public static final Supplier<
          com.mojang.serialization.MapCodec<com.shadowslave.world.IslandDensity>>
      ISLANDS = DENSITIES.register("islands", () -> com.shadowslave.world.IslandDensity.CODEC);
  public static final Supplier<
          com.mojang.serialization.MapCodec<com.shadowslave.world.RealmMapDensity>>
      REALM_MAP =
          DENSITIES.register("realm_map", () -> com.shadowslave.world.RealmMapDensity.CODEC);
  public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.biome.BiomeSource>> BIOME_SOURCES = DeferredRegister.create(Registries.BIOME_SOURCE, ID);
  public static final Supplier<com.mojang.serialization.MapCodec<com.shadowslave.world.DreamBiomeSource>> DREAM_BIOMES = BIOME_SOURCES.register("dream_map", () -> com.shadowslave.world.DreamBiomeSource.CODEC);
  public static final Supplier<com.shadowslave.world.RealmWatersFeature> WATERS = FEATURES.register("realm_waters", com.shadowslave.world.RealmWatersFeature::new);
  public static final Supplier<com.shadowslave.world.IslandChainsFeature> CHAINS =
      FEATURES.register("island_chains", com.shadowslave.world.IslandChainsFeature::new);

  static {
    RealmBlocks.init();
    ITEMS.registerSimpleBlockItem(ALTAR);
    ITEMS.registerSimpleBlockItem(EXIT);
    ITEMS.registerSimpleBlockItem(GATEWAY);
    TABS.register(
        "spell",
        () ->
            CreativeModeTab.builder()
                .title(net.minecraft.network.chat.Component.literal("Nightmare Spell"))
                .icon(() -> MEMORY.get().getDefaultInstance())
                .displayItems(
                    (p, out) -> {
                      out.accept(GATEWAY);
                      out.accept(ALTAR);
                      out.accept(EXIT);
                      out.accept(SOUL_SHARD);
                      out.accept(NIGHTMARE_SEED);
                      RealmBlocks.MATERIALS.values().forEach(out::accept);
                      out.accept(RealmBlocks.LANTERN);
                      out.accept(RealmBlocks.SLAB);
                      out.accept(RealmBlocks.STAIRS);
                      Content.memories
                          .keySet()
                          .forEach(id -> out.accept(MemoryTokenItem.create(id)));
                    })
                .build());
  }

  public ShadowSlaveMod(IEventBus bus, ModContainer container) {
    Content.bootstrap();
    BLOCKS.register(bus);
    ITEMS.register(bus);
    ENTITIES.register(bus);
    ATTACHMENTS.register(bus);
    TABS.register(bus);
    FEATURES.register(bus);
    DENSITIES.register(bus);
    BIOME_SOURCES.register(bus);
    bus.addListener(SpellNetwork::register);
    bus.addListener(
        (EntityAttributeCreationEvent e) ->
            e.put(CREATURE.get(), NightmareCreature.attributes().build()));
    container.registerConfig(ModConfig.Type.SERVER, Settings.SPEC);
  }

  public static SoulData soul(net.minecraft.world.entity.player.Player player) {
    return player.getData(SOUL);
  }
}
