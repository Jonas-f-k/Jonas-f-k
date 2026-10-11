package com.shadowslave.entity;

import com.shadowslave.content.Content;
import java.util.*;
import net.minecraft.util.RandomSource;

/** Rank is zero-based; class is the number of cores. Original species are explicitly marked. */
public final class CreatureEcology {
  public record Species(
      String id,
      String name,
      int rank,
      int cores,
      int family,
      int body,
      float width,
      float height,
      String behavior,
      String regions,
      String rewards,
      boolean canon) {}

  public static final String[] RANKS = {
    "Dormant", "Awakened", "Fallen", "Corrupted", "Great", "Cursed", "Unholy"
  };
  public static final String[] CLASSES = {
    "", "Beast", "Monster", "Demon", "Devil", "Tyrant", "Terror", "Titan"
  };
  public static final List<Species> ALL =
      List.of(
          s(
              "carapace_scavenger",
              "Carapace Scavenger",
              1,
              1,
              2,
              1,
              2.2f,
              1.6f,
              "armor",
              "forgotten_shore",
              "weapon,head",
              true),
          s(
              "carapace_centurion",
              "Carapace Centurion",
              1,
              2,
              2,
              1,
              2.8f,
              2.2f,
              "charge",
              "forgotten_shore",
              "armor,chest",
              true),
          s(
              "carapace_demon",
              "Carapace Demon",
              1,
              3,
              2,
              1,
              3.4f,
              2.8f,
              "regenerate",
              "forgotten_shore",
              "weapon,feet",
              true),
          s(
              "iron_spider",
              "Iron Spider",
              1,
              1,
              3,
              2,
              2.7f,
              1.8f,
              "web",
              "forgotten_shore,scarlet_wilds",
              "armor,utility",
              true),
          s(
              "iron_spider_matriarch",
              "Iron Spider Matriarch",
              1,
              5,
              3,
              2,
              4.8f,
              3.1f,
              "brood",
              "forgotten_shore",
              "weapon,chest",
              true),
          s(
              "flesh_reaver",
              "Flesh Reaver",
              1,
              1,
              0,
              8,
              1.9f,
              2.1f,
              "leap",
              "forgotten_shore",
              "weapon,legs",
              true),
          s(
              "rolling_stone",
              "Rolling Stone",
              1,
              2,
              3,
              10,
              2.3f,
              2.3f,
              "charge",
              "forgotten_shore,hollow_mountains",
              "head,charm",
              true),
          s(
              "stone_saint",
              "Stone Saint",
              1,
              2,
              3,
              0,
              1.2f,
              2.7f,
              "armor",
              "forgotten_shore",
              "armor,weapon",
              true),
          s(
              "forsaken_knight",
              "Forsaken Knight",
              2,
              4,
              1,
              0,
              1.5f,
              3.2f,
              "dark_regenerate",
              "forgotten_shore,hollow_mountains",
              "armor,weapon",
              true),
          s(
              "spire_messenger",
              "Spire Messenger",
              2,
              2,
              1,
              5,
              2.3f,
              3.3f,
              "screech",
              "forgotten_shore",
              "chest,utility",
              true),
          s(
              "lord_of_dead",
              "Lord of the Dead",
              2,
              5,
              3,
              10,
              4.4f,
              5.8f,
              "brood",
              "forgotten_shore,death_zone",
              "armor,charm",
              true),
          s(
              "soul_devouring_tree",
              "Soul Devouring Tree",
              1,
              6,
              4,
              11,
              4.8f,
              7.2f,
              "lure",
              "forgotten_shore",
              "charm,utility",
              true),
          s(
              "black_ravager",
              "Black Ravager",
              2,
              1,
              1,
              3,
              2.4f,
              2.3f,
              "leap",
              "chained_isles",
              "weapon,feet",
              true),
          s(
              "dread_wolf",
              "Dread Wolf",
              2,
              2,
              3,
              3,
              3.1f,
              2.5f,
              "charge",
              "chained_isles,frostglass_taiga",
              "armor,head",
              true),
          s(
              "chain_worm",
              "Chain Worm",
              2,
              3,
              6,
              4,
              2.6f,
              3.6f,
              "leap",
              "chained_isles",
              "weapon,charm",
              true),
          s(
              "chalice_maiden",
              "Chalice Maiden",
              2,
              3,
              5,
              6,
              1.2f,
              3.1f,
              "drain",
              "chained_isles,moonlit_grove",
              "charm,utility",
              true),
          s(
              "mordant_mimic",
              "Mordant Mimic",
              2,
              4,
              3,
              7,
              2.2f,
              2.0f,
              "ambush",
              "chained_isles",
              "utility,weapon",
              true),
          s(
              "sailor_doll",
              "Sailor Doll",
              2,
              1,
              4,
              0,
              1.0f,
              2.8f,
              "slash",
              "chained_isles",
              "weapon,legs",
              true),
          s(
              "blood_flower",
              "Blood Flower",
              1,
              2,
              0,
              9,
              1.8f,
              2.4f,
              "poison",
              "forgotten_shore,scarlet_wilds",
              "charm,head",
              true),
          s(
              "rime_stalker",
              "Rime Stalker",
              1,
              2,
              3,
              3,
              2.2f,
              2.0f,
              "frost",
              "frostglass_taiga,aurora_fields",
              "armor,feet",
              false),
          s(
              "aurora_moth",
              "Aurora Moth",
              1,
              1,
              6,
              5,
              2.5f,
              1.7f,
              "screech",
              "aurora_fields,amethyst_heights",
              "utility,chest",
              false),
          s(
              "glass_huntsman",
              "Glass Huntsman",
              2,
              3,
              5,
              2,
              2.8f,
              2.2f,
              "web",
              "amethyst_heights,nightmare_desert",
              "head,weapon",
              false),
          s(
              "dune_revenant",
              "Dune Revenant",
              2,
              2,
              0,
              0,
              1.1f,
              2.7f,
              "ambush",
              "nightmare_desert",
              "armor,legs",
              false),
          s(
              "ivory_burrower",
              "Ivory Burrower",
              2,
              3,
              0,
              4,
              2.8f,
              2.9f,
              "poison",
              "nightmare_desert",
              "weapon,charm",
              false),
          s(
              "hollow_watcher",
              "Hollow Watcher",
              3,
              2,
              1,
              6,
              1.9f,
              4.0f,
              "screech",
              "hollow_mountains,death_zone",
              "head,utility",
              false),
          s(
              "ashen_colossus",
              "Ashen Colossus",
              3,
              4,
              0,
              10,
              3.6f,
              5.5f,
              "charge",
              "death_zone,hollow_mountains",
              "armor,weapon",
              false),
          s(
              "moss_hart",
              "Moss Hart",
              1,
              1,
              4,
              3,
              2.0f,
              2.4f,
              "regenerate",
              "mosslight_ravine,verdant_gardens,golden_grove",
              "head,feet",
              false),
          s(
              "azure_serpent",
              "Azure Serpent",
              1,
              2,
              2,
              4,
              1.9f,
              2.5f,
              "frost",
              "azure_woods",
              "weapon,utility",
              false),
          s(
              "moonveil_wraith",
              "Moonveil Wraith",
              1,
              3,
              5,
              6,
              1.3f,
              3.0f,
              "drain",
              "moonlit_grove",
              "armor,charm",
              false),
          s(
              "gilded_mantis",
              "Gilded Mantis",
              1,
              2,
              4,
              8,
              1.7f,
              2.8f,
              "slash",
              "golden_grove,verdant_gardens",
              "weapon,legs",
              false),
          s(
              "briar_maw",
              "Briar Maw",
              1,
              3,
              4,
              9,
              2.1f,
              2.9f,
              "poison",
              "mosslight_ravine,scarlet_wilds",
              "armor,charm",
              false),
          s(
              "cinder_hound",
              "Cinder Hound",
              1,
              2,
              0,
              3,
              2.1f,
              2.0f,
              "fire",
              "scarlet_wilds",
              "weapon,feet",
              false));

  private static Species s(
      String id,
      String name,
      int rank,
      int cores,
      int family,
      int body,
      float w,
      float h,
      String behavior,
      String regions,
      String rewards,
      boolean canon) {
    return new Species(
        id, name, rank, cores, family, body, w * 1.25f, h * 1.30f, behavior, regions, rewards,
        canon);
  }

  public static int renderIndex(Species species) {
    return species == null ? -1 : ALL.indexOf(species);
  }

  public static Species get(String id) {
    return ALL.stream().filter(s -> s.id.equals(id)).findFirst().orElse(null);
  }

  public static Species choose(String biome, boolean night, RandomSource random) {
    var pool =
        ALL.stream()
            .filter(s -> Arrays.asList(s.regions.split(",")).contains(biome))
            .filter(
                s ->
                    night
                        || !List.of("chalice_maiden", "dune_revenant", "moonveil_wraith")
                            .contains(s.id))
            .toList();
    if (pool.isEmpty()) return null;
    // High-class encounters are scarce. Rank and class themselves never change in a random roll.
    var candidates = new ArrayList<Species>();
    for (var s : pool) {
      int weight = s.cores >= 5 ? 1 : s.rank >= 3 ? 2 : s.cores >= 3 ? 4 : 12;
      for (int i = 0; i < weight; i++) candidates.add(s);
    }
    return candidates.get(random.nextInt(candidates.size()));
  }

  public static List<Content.Memory> rewards(Species s, String biome) {
    var regional =
        Content.memories.values().stream()
            .filter(
                m ->
                    m.biome().equals(biome)
                        && Arrays.asList(s.rewards.split(",")).contains(m.type())
                        && !m.id().equals("divine_spark"))
            .toList();
    if (!regional.isEmpty()) return regional;
    return Content.memories.values().stream()
        .filter(
            m ->
                Arrays.asList(s.rewards.split(",")).contains(m.type())
                    && !m.id().equals("divine_spark"))
        .toList();
  }
}
