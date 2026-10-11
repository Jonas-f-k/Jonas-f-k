package com.shadowslave.core;

import java.util.*;

/** Pure, deterministic rules shared by the game and executable regression tests. */
public final class Rules {
  private Rules() {}

  public static final String[] RANKS = {
    "Dormant", "Awakened", "Ascended", "Transcendent", "Supreme", "Sacred", "Divine"
  };
  public static final int[] THRESHOLDS = {0, 100, 225, 400, 600, 850};
  public static final int MEMORY_SLOTS = 7;

  public static int aspectRank(int score, boolean bossDefeated) {
    if (bossDefeated) return 6;
    for (int i = THRESHOLDS.length - 1; i >= 0; i--) if (score >= THRESHOLDS[i]) return i;
    return 0;
  }

  public static int nextThreshold(int score) {
    for (int threshold : THRESHOLDS) if (threshold > score) return threshold;
    return 850;
  }

  public static int bossSite(long seed, int count) {
    if (count < 1) throw new IllegalArgumentException("A trial must have a boss arena");
    return new Random(seed ^ 0x534841444F574CL).nextInt(count);
  }

  public static int maximumEssence(int soulRank, int fragments, int cores) {
    return (int)
        Math.round(
            100 * Math.pow(1.65, Math.max(0, soulRank - 1)) * (1 + .45 * growth(cores, fragments)));
  }

  public static int coreFloor(int cores) {
    int c = Math.clamp(cores, 1, 7);
    return c * (c - 1) * 500;
  }

  public static int coreCapacity(int cores) {
    return Math.clamp(cores, 1, 7) * 1000;
  }

  public static double saturation(int cores, int fragments) {
    return Math.clamp((fragments - coreFloor(cores)) / (double) coreCapacity(cores), 0, 1);
  }

  /** Continuous across every core boundary, including saturation of the seventh core. */
  public static double growth(int cores, int fragments) {
    return Math.clamp(cores, 1, 7) - 1 + saturation(cores, fragments);
  }

  public static int cores(int aspectRank, int fragments) {
    if (aspectRank != 6) return 1;
    int cores = 1, remaining = Math.max(0, fragments);
    while (cores < 7 && remaining >= cores * 1000) {
      remaining -= cores * 1000;
      cores++;
    }
    return cores;
  }

  public static boolean canEquip(List<String> equippedTypes, String type) {
    if (equippedTypes.size() >= MEMORY_SLOTS) return false;
    long same = equippedTypes.stream().filter(type::equals).count();
    return switch (type) {
      case "armor", "charm", "head", "chest", "legs", "feet" -> same == 0;
      case "weapon" -> same < 2;
      default -> true;
    };
  }

  /** Each specialty replaces precisely one quarter of full-body protection. */
  public static double protection(double fullBody, Map<String, Double> overrides) {
    double result = 0;
    for (String part : List.of("head", "chest", "legs", "feet"))
      result += overrides.getOrDefault(part, fullBody) / 4.0;
    return Math.clamp(result, 0, .75);
  }

  public static long countdownDelta(long previousDayTime, long dayTime) {
    long difference = dayTime - previousDayTime;
    return difference > 0 && difference <= 24000 ? difference : 1;
  }
}
