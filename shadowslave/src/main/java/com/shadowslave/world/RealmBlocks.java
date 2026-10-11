package com.shadowslave.world;

import com.shadowslave.ShadowSlaveMod;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Original materials shared by terrain, ruins, sanctuaries and the seven trials. */
public final class RealmBlocks {
  public static final Map<String, DeferredBlock<Block>> MATERIALS = new LinkedHashMap<>();
  public static final DeferredBlock<Block> ASH_STONE = material("ash_stone", Blocks.DEEPSLATE, 0);
  public static final DeferredBlock<Block> ASH_BRICKS =
      material("ash_bricks", Blocks.DEEPSLATE_BRICKS, 0);
  public static final DeferredBlock<Block> FRACTURED_STONE =
      material("fractured_stone", Blocks.CRACKED_DEEPSLATE_BRICKS, 0);
  public static final DeferredBlock<Block> ASH_TILES =
      material("ash_tiles", Blocks.DEEPSLATE_TILES, 0);
  public static final DeferredBlock<Block> LIMESTONE =
      material("sanctuary_limestone", Blocks.CALCITE, 0);
  public static final DeferredBlock<Block> BRONZE = material("aged_bronze", Blocks.COPPER_BLOCK, 0);
  public static final DeferredBlock<Block> AMBER_GLASS =
      material("amber_glass", Blocks.SEA_LANTERN, 15);
  public static final DeferredBlock<Block> SILVER_RUNE =
      material("silver_rune", Blocks.CHISELED_STONE_BRICKS, 6);
  public static final DeferredBlock<Block> SOIL = material("ashen_soil", Blocks.COARSE_DIRT, 0);
  public static final DeferredBlock<Block> ASH_GRASS =
      material("ashen_grass", Blocks.MOSS_BLOCK, 0);
  public static final DeferredBlock<Block> MOSS = material("sanctuary_moss", Blocks.MOSS_BLOCK, 0);
  public static final DeferredBlock<Block> BARK = material("petrified_bark", Blocks.DEEPSLATE, 0);
  public static final DeferredBlock<Block> IRON = material("black_iron", Blocks.IRON_BLOCK, 0);
  public static final DeferredBlock<Block> BONE = material("fossil_bone", Blocks.BONE_BLOCK, 0);
  public static final DeferredBlock<Block> EMBER = material("ember_obsidian", Blocks.OBSIDIAN, 3);
  public static final DeferredBlock<Block> DROWNED =
      material("drowned_stone", Blocks.PRISMARINE, 0);
  public static final DeferredBlock<Block> LANTERN =
      ShadowSlaveMod.BLOCKS.register(
          "amber_lantern",
          () ->
              new CarvedBlock(
                  BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN)
                      .lightLevel(s -> 15)
                      .noOcclusion(),
                  Shapes.or(
                      Block.box(3, 0, 3, 13, 2, 13),
                      Block.box(4, 2, 4, 12, 13, 12),
                      Block.box(3, 13, 3, 13, 15, 13),
                      Block.box(7, 15, 7, 9, 16, 9))));
  public static final DeferredBlock<SlabBlock> SLAB =
      ShadowSlaveMod.BLOCKS.register(
          "ash_slab",
          () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_TILE_SLAB)));
  public static final DeferredBlock<StairBlock> STAIRS =
      ShadowSlaveMod.BLOCKS.register(
          "ash_stairs",
          () ->
              new StairBlock(
                  ASH_TILES.get().defaultBlockState(),
                  BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_TILE_STAIRS)));

  // EXPANSION START
  static {
    material("meadow_grass", Blocks.DIRT, 0);
    material("blossom_grass", Blocks.DIRT, 0);
    material("lavender_grass", Blocks.DIRT, 0);
    material("autumn_litter", Blocks.DIRT, 0);
    material("turquoise_moss", Blocks.DIRT, 0);
    material("frost_grass", Blocks.DIRT, 0);
    material("scarlet_litter", Blocks.DIRT, 0);
    material("warm_sand", Blocks.STONE, 0);
    material("dream_loam", Blocks.DIRT, 0);
    material("rose_clay", Blocks.STONE, 0);
    material("snow_soil", Blocks.DIRT, 0);
    material("golden_sandstone", Blocks.STONE, 0);
    material("blue_shale", Blocks.STONE, 0);
    material("crystal_rock", Blocks.STONE, 5);
    material("red_sandstone", Blocks.STONE, 0);
    material("pale_marble", Blocks.STONE, 0);
    material("emerald_leaves", Blocks.OAK_LEAVES, 0);
    material("pink_leaves", Blocks.OAK_LEAVES, 0);
    material("lavender_leaves", Blocks.OAK_LEAVES, 0);
    material("golden_leaves", Blocks.OAK_LEAVES, 0);
    material("cyan_leaves", Blocks.OAK_LEAVES, 0);
    material("teal_leaves", Blocks.OAK_LEAVES, 5);
    material("frost_leaves", Blocks.OAK_LEAVES, 0);
    material("crimson_leaves", Blocks.OAK_LEAVES, 0);
    material("dream_oak", Blocks.OAK_PLANKS, 0);
    material("dream_birch", Blocks.OAK_PLANKS, 0);
    material("blue_bark", Blocks.OAK_PLANKS, 0);
    material("rose_bark", Blocks.OAK_PLANKS, 0);
    material("redwood_bark", Blocks.OAK_PLANKS, 0);
    material("runic_timber", Blocks.OAK_PLANKS, 0);
    material("honey_planks", Blocks.OAK_PLANKS, 0);
    material("plum_planks", Blocks.OAK_PLANKS, 0);
    material("emerald_crystal", Blocks.STONE, 5);
    material("amethyst_crystal", Blocks.STONE, 5);
    material("cyan_crystal", Blocks.STONE, 5);
    material("rose_crystal", Blocks.STONE, 5);
    material("peach_bricks", Blocks.STONE, 0);
    material("mossy_ruin_stone", Blocks.DIRT, 0);
    material("moon_brass", Blocks.STONE, 0);
    material("navy_cloth", Blocks.STONE, 0);
    material("silver_plate", Blocks.STONE, 0);
    material("charcoal_plate", Blocks.STONE, 0);
    material("leaf_plate", Blocks.STONE, 0);
    material("plum_cloth", Blocks.STONE, 0);
    material("gold_plate", Blocks.STONE, 0);
    material("ice_plate", Blocks.STONE, 0);
    material("scarlet_plate", Blocks.STONE, 0);
    material("pearl_plate", Blocks.STONE, 0);
    DeferredBlock<Block> rose_bloom =
        ShadowSlaveMod.BLOCKS.register(
            "rose_bloom",
            () ->
                new CarvedBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY)
                        .noCollission()
                        .lightLevel(s -> 3),
                    Block.box(2, 0, 2, 14, 12, 14)));
    MATERIALS.put("rose_bloom", rose_bloom);
    ShadowSlaveMod.ITEMS.registerSimpleBlockItem(rose_bloom);
    DeferredBlock<Block> sun_bloom =
        ShadowSlaveMod.BLOCKS.register(
            "sun_bloom",
            () ->
                new CarvedBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY)
                        .noCollission()
                        .lightLevel(s -> 3),
                    Block.box(2, 0, 2, 14, 12, 14)));
    MATERIALS.put("sun_bloom", sun_bloom);
    ShadowSlaveMod.ITEMS.registerSimpleBlockItem(sun_bloom);
    DeferredBlock<Block> azure_bloom =
        ShadowSlaveMod.BLOCKS.register(
            "azure_bloom",
            () ->
                new CarvedBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY)
                        .noCollission()
                        .lightLevel(s -> 3),
                    Block.box(2, 0, 2, 14, 12, 14)));
    MATERIALS.put("azure_bloom", azure_bloom);
    ShadowSlaveMod.ITEMS.registerSimpleBlockItem(azure_bloom);
    DeferredBlock<Block> jade_bloom =
        ShadowSlaveMod.BLOCKS.register(
            "jade_bloom",
            () ->
                new CarvedBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY)
                        .noCollission()
                        .lightLevel(s -> 3),
                    Block.box(2, 0, 2, 14, 12, 14)));
    MATERIALS.put("jade_bloom", jade_bloom);
    ShadowSlaveMod.ITEMS.registerSimpleBlockItem(jade_bloom);
    DeferredBlock<Block> violet_bloom =
        ShadowSlaveMod.BLOCKS.register(
            "violet_bloom",
            () ->
                new CarvedBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY)
                        .noCollission()
                        .lightLevel(s -> 3),
                    Block.box(2, 0, 2, 14, 12, 14)));
    MATERIALS.put("violet_bloom", violet_bloom);
    ShadowSlaveMod.ITEMS.registerSimpleBlockItem(violet_bloom);
    DeferredBlock<Block> ember_bloom =
        ShadowSlaveMod.BLOCKS.register(
            "ember_bloom",
            () ->
                new CarvedBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY)
                        .noCollission()
                        .lightLevel(s -> 3),
                    Block.box(2, 0, 2, 14, 12, 14)));
    MATERIALS.put("ember_bloom", ember_bloom);
    ShadowSlaveMod.ITEMS.registerSimpleBlockItem(ember_bloom);
    DeferredBlock<Block> moon_bloom =
        ShadowSlaveMod.BLOCKS.register(
            "moon_bloom",
            () ->
                new CarvedBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY)
                        .noCollission()
                        .lightLevel(s -> 3),
                    Block.box(2, 0, 2, 14, 12, 14)));
    MATERIALS.put("moon_bloom", moon_bloom);
    ShadowSlaveMod.ITEMS.registerSimpleBlockItem(moon_bloom);
    DeferredBlock<Block> cloud_bloom =
        ShadowSlaveMod.BLOCKS.register(
            "cloud_bloom",
            () ->
                new CarvedBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY)
                        .noCollission()
                        .lightLevel(s -> 3),
                    Block.box(2, 0, 2, 14, 12, 14)));
    MATERIALS.put("cloud_bloom", cloud_bloom);
    ShadowSlaveMod.ITEMS.registerSimpleBlockItem(cloud_bloom);
  }

  static {
    DeferredBlock<Block> moon_lantern =
        ShadowSlaveMod.BLOCKS.register(
            "moon_lantern",
            () ->
                new CarvedBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                        .noOcclusion()
                        .lightLevel(s -> 15),
                    Block.box(1, 0, 1, 15, 14, 15)));
    MATERIALS.put("moon_lantern", moon_lantern);
    ShadowSlaveMod.ITEMS.registerSimpleBlockItem(moon_lantern);
    DeferredBlock<Block> runic_basin =
        ShadowSlaveMod.BLOCKS.register(
            "runic_basin",
            () ->
                new CarvedBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                        .noOcclusion()
                        .lightLevel(s -> 4),
                    Block.box(1, 0, 1, 15, 14, 15)));
    MATERIALS.put("runic_basin", runic_basin);
    ShadowSlaveMod.ITEMS.registerSimpleBlockItem(runic_basin);
    DeferredBlock<Block> amethyst_cluster =
        ShadowSlaveMod.BLOCKS.register(
            "amethyst_cluster",
            () ->
                new CarvedBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                        .noOcclusion()
                        .lightLevel(s -> 4),
                    Block.box(1, 0, 1, 15, 14, 15)));
    MATERIALS.put("amethyst_cluster", amethyst_cluster);
    ShadowSlaveMod.ITEMS.registerSimpleBlockItem(amethyst_cluster);
    DeferredBlock<Block> potion_shelf =
        ShadowSlaveMod.BLOCKS.register(
            "potion_shelf",
            () ->
                new CarvedBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                        .noOcclusion()
                        .lightLevel(s -> 4),
                    Block.box(1, 0, 1, 15, 14, 15)));
    MATERIALS.put("potion_shelf", potion_shelf);
    ShadowSlaveMod.ITEMS.registerSimpleBlockItem(potion_shelf);
    DeferredBlock<Block> star_books =
        ShadowSlaveMod.BLOCKS.register(
            "star_books",
            () ->
                new CarvedBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                        .noOcclusion()
                        .lightLevel(s -> 4),
                    Block.box(1, 0, 1, 15, 14, 15)));
    MATERIALS.put("star_books", star_books);
    ShadowSlaveMod.ITEMS.registerSimpleBlockItem(star_books);
    DeferredBlock<Block> bloom_pot =
        ShadowSlaveMod.BLOCKS.register(
            "bloom_pot",
            () ->
                new CarvedBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                        .noOcclusion()
                        .lightLevel(s -> 4),
                    Block.box(1, 0, 1, 15, 14, 15)));
    MATERIALS.put("bloom_pot", bloom_pot);
    ShadowSlaveMod.ITEMS.registerSimpleBlockItem(bloom_pot);
    DeferredBlock<Block> lantern_pedestal =
        ShadowSlaveMod.BLOCKS.register(
            "lantern_pedestal",
            () ->
                new CarvedBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                        .noOcclusion()
                        .lightLevel(s -> 4),
                    Block.box(1, 0, 1, 15, 14, 15)));
    MATERIALS.put("lantern_pedestal", lantern_pedestal);
    ShadowSlaveMod.ITEMS.registerSimpleBlockItem(lantern_pedestal);
  }

  static {
    for (String id :
        List.of(
            "crimson_bloom",
            "hibiscus_bloom",
            "crocus_bloom",
            "frost_bloom",
            "lotus_bloom",
            "marigold_bloom",
            "peony_bloom",
            "foxglove_bloom")) {
      DeferredBlock<Block> flower =
          ShadowSlaveMod.BLOCKS.register(
              id,
              () ->
                  new CarvedBlock(
                      BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY)
                          .noCollission()
                          .noOcclusion()
                          .lightLevel(state -> 2),
                      Block.box(2, 0, 2, 14, 14, 14)));
      MATERIALS.put(id, flower);
      ShadowSlaveMod.ITEMS.registerSimpleBlockItem(flower);
    }
  }

  public static BlockState state(String name) {
    var state = MATERIALS.get(name).get().defaultBlockState();
    return state.getBlock() instanceof LeavesBlock
        ? state.setValue(LeavesBlock.PERSISTENT, true)
        : state;
  }

  // EXPANSION END
  private static DeferredBlock<Block> material(String id, Block source, int light) {
    var block =
        ShadowSlaveMod.BLOCKS.register(
            id,
            () ->
                id.endsWith("_leaves")
                    ? new LeavesBlock(
                        BlockBehaviour.Properties.ofFullCopy(source).lightLevel(s -> light))
                    : new Block(
                        BlockBehaviour.Properties.ofFullCopy(source).lightLevel(s -> light)));
    MATERIALS.put(id, block);
    ShadowSlaveMod.ITEMS.registerSimpleBlockItem(block);
    return block;
  }

  public static void init() {
    ShadowSlaveMod.ITEMS.registerSimpleBlockItem(LANTERN);
    ShadowSlaveMod.ITEMS.registerSimpleBlockItem(SLAB);
    ShadowSlaveMod.ITEMS.registerSimpleBlockItem(STAIRS);
  }

  private static final class CarvedBlock extends Block {
    private final VoxelShape shape;

    CarvedBlock(Properties p, VoxelShape shape) {
      super(p);
      this.shape = shape;
    }

    @Override
    protected VoxelShape getShape(
        BlockState s, BlockGetter level, BlockPos pos, CollisionContext context) {
      return shape;
    }
  }
}
