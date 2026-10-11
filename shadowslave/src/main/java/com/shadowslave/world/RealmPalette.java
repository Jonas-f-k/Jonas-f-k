package com.shadowslave.world;

import java.util.Map;

public final class RealmPalette {
  public record Palette(String ground, String leaves, String trunk, String flower) {}

  public static final Map<String, Palette> BIOMES =
      Map.ofEntries(
          Map.entry(
              "frostglass_taiga",
              new Palette("frost_grass", "frost_leaves", "blue_bark", "frost_bloom")),
          Map.entry(
              "hollow_mountains", new Palette("ash_stone", "", "petrified_bark", "moon_bloom")),
          Map.entry(
              "aurora_fields",
              new Palette("lavender_grass", "lavender_leaves", "dream_birch", "violet_bloom")),
          Map.entry("death_zone", new Palette("ashen_soil", "", "petrified_bark", "moon_bloom")),
          Map.entry(
              "azure_woods",
              new Palette("turquoise_moss", "cyan_leaves", "blue_bark", "lotus_bloom")),
          Map.entry(
              "moonlit_grove",
              new Palette("turquoise_moss", "teal_leaves", "blue_bark", "lotus_bloom")),
          Map.entry(
              "forgotten_shore", new Palette("ashen_grass", "", "petrified_bark", "moon_bloom")),
          Map.entry(
              "blossom_vale",
              new Palette("blossom_grass", "pink_leaves", "rose_bark", "peony_bloom")),
          Map.entry(
              "mosslight_ravine",
              new Palette("sanctuary_moss", "emerald_leaves", "dream_oak", "jade_bloom")),
          Map.entry(
              "amethyst_heights",
              new Palette("lavender_grass", "lavender_leaves", "rose_bark", "crocus_bloom")),
          Map.entry(
              "golden_grove",
              new Palette("autumn_litter", "golden_leaves", "dream_birch", "marigold_bloom")),
          Map.entry(
              "verdant_gardens",
              new Palette("meadow_grass", "emerald_leaves", "runic_timber", "foxglove_bloom")),
          Map.entry(
              "nightmare_desert", new Palette("warm_sand", "", "redwood_bark", "crimson_bloom")),
          Map.entry(
              "sunpetal_meadows",
              new Palette("meadow_grass", "golden_leaves", "dream_oak", "sun_bloom")),
          Map.entry(
              "scarlet_wilds",
              new Palette("scarlet_litter", "crimson_leaves", "redwood_bark", "crimson_bloom")),
          Map.entry(
              "ember_haven",
              new Palette("meadow_grass", "golden_leaves", "dream_oak", "sun_bloom")),
          Map.entry(
              "chained_isles",
              new Palette("meadow_grass", "cyan_leaves", "dream_oak", "cloud_bloom")));
}
