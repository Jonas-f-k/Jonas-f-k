package com.shadowslave.core;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Settings {
  public static final ModConfigSpec SPEC;
  public static final ModConfigSpec.IntValue COUNTDOWN_DAYS;
  public static final ModConfigSpec.IntValue ENEMIES_PER_TRIAL;
  public static final ModConfigSpec.IntValue KILL_POINTS;
  public static final ModConfigSpec.BooleanValue KEEP_DREAM_INVENTORY;
  public static final ModConfigSpec.BooleanValue NATURAL_REGEN;
  public static final ModConfigSpec.BooleanValue AUTO_START;

  static {
    var b = new ModConfigSpec.Builder();
    AUTO_START =
        b.comment("Automatically begin a First Nightmare when the personal countdown ends.")
            .define("autoStartNightmare", true);
    COUNTDOWN_DAYS =
        b.comment("Online Minecraft days, including nights skipped by sleep.")
            .defineInRange("countdownDays", 7, 1, 100);
    ENEMIES_PER_TRIAL =
        b.comment("Finite eligible encounters per trial. 36 * 25 permits Sacred.")
            .defineInRange("enemiesPerTrial", 36, 34, 80);
    KILL_POINTS = b.defineInRange("pointsPerCreature", 25, 1, 100);
    KEEP_DREAM_INVENTORY =
        b.comment("Preserve ordinary inventory on Dream Realm death; Memories always persist.")
            .define("keepDreamInventory", true);
    NATURAL_REGEN =
        b.comment("Allow slow Essence regeneration outside combat.")
            .define("naturalEssenceRegen", true);
    SPEC = b.build();
  }
}
