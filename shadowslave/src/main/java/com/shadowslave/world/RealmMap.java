package com.shadowslave.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;

/**
 * The map of the Dream Realm: a different continent for every world, the same one for the same
 * world seed.
 *
 * <p>This class is deliberately free of Minecraft imports. It is plain math, so it can be compiled,
 * tested and rendered to a preview image outside the game ({@code tools/preview_realm.py}). The
 * thin adapter {@link RealmMapDensity} exposes its channels to Minecraft's density functions, and
 * {@code tools/build_worldgen.py} reads {@link #BIOMES} to generate the biome source, so the Java
 * and the data cannot drift apart.
 *
 * <h2>How a map is made</h2>
 *
 * <ol>
 *   <li>A fixed <em>footprint</em> of land slots, coastal slots and ocean sites is jittered by the
 *       world seed. Biomes are dealt onto the slots along a random climate gradient (so cold lands
 *       cluster together, in a different direction every world), with rules that keep deadly
 *       regions away from the safe hub and mountains away from the fortress sites.
 *   <li>The plane is split into regions by a <em>weighted Voronoi diagram</em> of the sites, after
 *       the coordinates were bent by noise so borders wander like real coastlines.
 *   <li>Every region has a {@link Profile}. Terrain parameters are blended between regions with
 *       soft weights, so coasts and mountain feet are slopes, never walls. The biome itself is a
 *       hard lookup of the nearest site.
 *   <li>Rivers are carved along noise zero-crossings in the lowlands; a few small seas are dropped
 *       inland; the Chained Isles float over open water on a random side of the continent.
 *   <li>The fortress sites (first landing, first Gateway, three Citadels) keep fixed coordinates
 *       because the game builds on them. They are levelled into whatever terrain the seed produced.
 * </ol>
 */
public final class RealmMap {
  private RealmMap() {}

  public static final int SEA_LEVEL = 63;

  /** Must equal the archipelago cluster-cell spacing; each cell holds irregular islands. */
  public static final int ISLE_SPACING = Archipelago.SPACING;

  /** Soft-blend distance in weighted blocks; larger means broader, gentler transitions. */
  private static final double BLEND = 110.0;

  // ------------------------------------------------------------------ biomes

  /**
   * Terrain character of a region. Heights are relative to {@link #SEA_LEVEL}.
   *
   * @param biome biome path in the {@code shadowslave} namespace
   * @param base typical ground elevation above the sea
   * @param hills amplitude of broad rolling hills
   * @param ridges amplitude of ridged mountain chains
   * @param rough 3D overhang strength, 0..1 (cliffs, arches)
   * @param cave cavern frequency, 0..1
   * @param terrace how strongly the ground is cut into steps, 0..1
   * @param river how readily rivers cut through, 0..1
   * @param dunes amplitude of wind-shaped dunes
   * @param canyon depth of narrow ravines cut into the ground
   * @param climate -1 (cold) to 1 (hot): used to deal biomes onto the map
   */
  public record Profile(
      String biome,
      double base,
      double hills,
      double ridges,
      double rough,
      double cave,
      double terrace,
      double river,
      double dunes,
      double canyon,
      double climate) {}

  private static Profile p(
      String biome,
      double base,
      double hills,
      double ridges,
      double rough,
      double cave,
      double terrace,
      double river,
      double dunes,
      double canyon,
      double climate) {
    return new Profile(
        biome, base, hills, ridges, rough, cave, terrace, river, dunes, canyon, climate);
  }

  /**
   * Region table. The index is the region code: it is written into the biome source as a
   * temperature value of {@code -0.9 + 0.1 * index}, so Minecraft's multi-noise lookup returns
   * exactly the biome the map decided on.
   */
  public static final List<Profile> BIOMES =
      List.of(
          //   biome              base hills ridge rough cave terr river dune canyon climate
          p("dark_sea", -48, 10, 0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0.0),
          p("ember_haven", 9, 3, 0, 0.0, 0.0, 0.0, 0.4, 0, 0, 0.2),
          p("sunpetal_meadows", 11, 8, 0, 0.1, 0.0, 0.0, 0.8, 0, 0, 0.3),
          p("verdant_gardens", 10, 6, 0, 0.1, 0.0, 0.0, 0.9, 0, 0, 0.15),
          p("blossom_vale", 13, 12, 4, 0.2, 0.1, 0.0, 0.8, 0, 0, 0.4),
          p("golden_grove", 15, 10, 3, 0.2, 0.1, 0.0, 0.8, 0, 0, 0.5),
          p("moonlit_grove", 10, 12, 3, 0.2, 0.2, 0.0, 1.0, 0, 0, -0.1),
          p("azure_woods", 13, 12, 4, 0.2, 0.2, 0.0, 0.9, 0, 0, -0.2),
          p("mosslight_ravine", 20, 10, 6, 0.4, 0.3, 0.0, 0.3, 0, 30, 0.0),
          p("aurora_fields", 17, 14, 6, 0.2, 0.1, 0.0, 0.8, 0, 0, -0.7),
          p("frostglass_taiga", 24, 18, 10, 0.4, 0.2, 0.0, 0.6, 0, 0, -1.0),
          p("amethyst_heights", 44, 22, 24, 0.6, 0.5, 0.2, 0.0, 0, 0, -0.35),
          p("hollow_mountains", 62, 30, 78, 0.8, 1.0, 0.3, 0.0, 0, 0, -0.5),
          p("death_zone", 15, 14, 24, 0.9, 0.6, 0.8, 0.1, 0, 20, 0.9),
          p("nightmare_desert", 9, 3, 0, 0.1, 0.0, 0.0, 0.0, 9, 0, 1.0),
          p("scarlet_wilds", 15, 14, 10, 0.3, 0.2, 0.0, 0.7, 0, 0, 0.8),
          p("forgotten_shore", 7, 6, 3, 0.4, 0.2, 0.6, 0.5, 0, 0, 0.1),
          p("chained_isles", -48, 10, 0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0.0));

  public static final int DARK_SEA = 0;
  public static final int EMBER_HAVEN = 1;
  public static final int FORGOTTEN_SHORE = 16;
  public static final int CHAINED_ISLES = 17;

  /** Biomes dealt onto inland slots, one each. */
  private static final int[] INLAND_BIOMES = {
    indexOf("frostglass_taiga"), indexOf("aurora_fields"), indexOf("hollow_mountains"),
    indexOf("amethyst_heights"), indexOf("azure_woods"), indexOf("moonlit_grove"),
    indexOf("mosslight_ravine"), indexOf("verdant_gardens"), indexOf("sunpetal_meadows"),
    indexOf("blossom_vale"), indexOf("golden_grove"), indexOf("scarlet_wilds"),
    indexOf("death_zone"), indexOf("nightmare_desert")
  };

  /** Deadly regions: kept away from the hub. */
  private static boolean dangerous(int region) {
    return region == indexOf("death_zone") || region == indexOf("hollow_mountains");
  }

  /** Regions that cannot hold a levelled fortress. */
  private static boolean unbuildable(int region) {
    return dangerous(region)
        || region == indexOf("amethyst_heights")
        || region == indexOf("mosslight_ravine");
  }

  /** Region code written to the biome source's temperature axis. */
  public static double code(int region) {
    return -0.9 + 0.1 * region;
  }

  public static int indexOf(String biome) {
    for (int i = 0; i < BIOMES.size(); i++) if (BIOMES.get(i).biome().equals(biome)) return i;
    throw new IllegalArgumentException(biome);
  }

  // ------------------------------------------------------------------ footprint

  /** A place where a biome can be dealt. */
  private record Slot(double x, double z, double weight) {}

  private static Slot slot(double x, double z, double weight) {
    return new Slot(x, z, weight);
  }

  /*
   * x grows east, z grows south. The hub is the origin. These are only the *slots*: which biome
   * lands on which slot, and exactly where the slots sit, depends on the world seed.
   */
  private static final Slot HUB = slot(0, 0, 1.15);

  private static final Slot[] INLAND = {
    slot(-330, 380, 0.95), slot(170, 480, 0.9), slot(580, -140, 1.0), slot(920, 260, 1.0),
    slot(1380, -120, 1.0), slot(1330, 440, 0.95), slot(1000, -900, 1.1), slot(380, -1300, 1.15),
    slot(-90, -770, 0.95), slot(-640, -980, 1.2), slot(-430, -330, 0.9), slot(-540, 1010, 1.2),
    slot(450, 1060, 1.35), slot(1000, 800, 1.0)
  };

  /** Slots on the western coast, where the ruined shore usually lies. */
  private static final Slot[] COAST = {
    slot(-1000, -380, 1.1), slot(-1020, 140, 1.1), slot(-1060, 650, 1.0), slot(-1120, -850, 0.9)
  };

  /** Open water all round the continent. */
  private static final Slot[] OCEAN = {
    slot(-1950, -250, 1.5), slot(-1850, 820, 1.2), slot(-1750, -1150, 1.2), slot(-1000, 1650, 1.2),
    slot(300, 1800, 1.3), slot(1500, 1600, 1.2), slot(-500, -1850, 1.2), slot(600, -1950, 1.3),
    slot(1650, -1800, 1.2), slot(1850, 130, 0.9), slot(1900, -700, 0.9), slot(1800, 800, 0.9)
  };

  /** Chained Isles sites for the east; rotated by multiples of 90 degrees for other sides. */
  private static final Slot[] ISLES = {
    slot(2450, -250, 1.3), slot(2480, 520, 1.1), slot(2300, -1000, 1.1)
  };

  // ------------------------------------------------------------------ landmarks

  /**
   * Places where the game builds fixed structures. They keep these coordinates in every world and
   * are levelled into the terrain.
   *
   * @param flat radius of perfectly level ground
   * @param blend radius at which the terrain is fully natural again
   * @param up height of the level ground above the sea
   * @param citadel whether a fortress stands here (the region under it must be buildable)
   */
  public record Landmark(String name, int x, int z, int flat, int blend, int up, boolean citadel) {
    public int height() {
      return SEA_LEVEL + up;
    }
  }

  public static final List<Landmark> LANDMARKS =
      List.of(
          new Landmark("first_landing", 0, 0, 110, 260, 9, false),
          new Landmark("first_gateway", 160, 0, 60, 160, 9, false),
          new Landmark("citadel_bastion", 480, 0, 90, 230, 10, true),
          new Landmark("citadel_shore", -1024, 640, 90, 230, 8, true),
          new Landmark("citadel_ivory", 1024, -768, 90, 230, 12, true));

  // ------------------------------------------------------------------ layout

  /** One Voronoi site of a concrete map. */
  public record Site(int region, double x, double z, double weight) {}

  /** A complete map for one world seed. */
  public static final class Layout {
    public final long seed;
    public final List<Site> sites;

    /** Region forced under each entry of {@link #LANDMARKS}. */
    public final int[] landmarkRegion;

    public final int noiseSeed;
    final double[] sx, sz, sw;
    final int[] sr;
    private final double[] ix, iz;

    Layout(long seed, List<Site> sites, int[] landmarkRegion) {
      this.seed = seed;
      this.sites = List.copyOf(sites);
      this.landmarkRegion = landmarkRegion;
      this.noiseSeed = (int) (seed ^ (seed >>> 32)) * 0x2545F491;
      int n = sites.size();
      sx = new double[n];
      sz = new double[n];
      sw = new double[n];
      sr = new int[n];
      for (int i = 0; i < n; i++) {
        Site s = sites.get(i);
        sx[i] = s.x;
        sz[i] = s.z;
        sw[i] = s.weight;
        sr[i] = s.region;
      }
      ix = sites.stream().filter(s -> s.region == CHAINED_ISLES).mapToDouble(s -> s.x).toArray();
      iz = sites.stream().filter(s -> s.region == CHAINED_ISLES).mapToDouble(s -> s.z).toArray();
    }

    /** Cheap pre-check so that only columns near the Chained Isles pay for the cell vote. */
    boolean nearIsles(double x, double z) {
      for (int i = 0; i < ix.length; i++) if (Math.hypot(x - ix[i], z - iz[i]) < 1600) return true;
      return false;
    }

    /** Nearest site by plain (un-warped) weighted distance. */
    int nearestRegion(double x, double z) {
      double best = Double.MAX_VALUE;
      int region = 0;
      for (int i = 0; i < sx.length; i++) {
        double d = Math.hypot(x - sx[i], z - sz[i]) / sw[i];
        if (d < best) {
          best = d;
          region = sr[i];
        }
      }
      return region;
    }
  }

  private static volatile Layout layout = generate(0L);

  /** Selects the map for a world. Called once when a server starts, before any chunk exists. */
  public static void useSeed(long worldSeed) {
    layout = generate(worldSeed);
  }

  public static Layout layout() {
    return layout;
  }

  /** Builds the map for a seed. Deterministic. Retries with derived seeds until all rules hold. */
  public static Layout generate(long seed) {
    Layout last = null;
    for (int attempt = 0; attempt < 80; attempt++) {
      SplittableRandom rnd =
          new SplittableRandom(seed * 0x9E3779B97F4A7C15L + attempt * 0xBF58476D1CE4E5B9L + 17);
      last = tryGenerate(seed, rnd);
      if (last != null) return last;
    }
    // Practically unreachable; fall back to an unconstrained map rather than failing world
    // creation.
    return tryGenerateUnchecked(seed, new SplittableRandom(seed));
  }

  private static Layout tryGenerate(long seed, SplittableRandom rnd) {
    Layout l = tryGenerateUnchecked(seed, rnd);
    for (int i = 0; i < LANDMARKS.size(); i++) {
      int r = l.landmarkRegion[i];
      if (r == DARK_SEA || r == CHAINED_ISLES) return null;
      if (LANDMARKS.get(i).citadel() && unbuildable(r)) return null;
    }
    return l;
  }

  private static Layout tryGenerateUnchecked(long seed, SplittableRandom rnd) {
    double angle = rnd.nextDouble() * Math.PI * 2;
    double dirX = Math.cos(angle), dirZ = Math.sin(angle);

    // --- deal inland biomes along a random climate gradient
    int n = INLAND.length;
    Integer[] slotOrder = new Integer[n];
    double[] slotKey = new double[n];
    for (int i = 0; i < n; i++) {
      slotOrder[i] = i;
      slotKey[i] =
          (INLAND[i].x * dirX + INLAND[i].z * dirZ) / 1400.0 + (rnd.nextDouble() - 0.5) * 0.7;
    }
    Arrays.sort(slotOrder, (a, b) -> Double.compare(slotKey[a], slotKey[b]));
    Integer[] biomeOrder = new Integer[n];
    double[] biomeKey = new double[n];
    for (int i = 0; i < n; i++) {
      biomeOrder[i] = i;
      biomeKey[i] = BIOMES.get(INLAND_BIOMES[i]).climate() + (rnd.nextDouble() - 0.5) * 0.7;
    }
    Arrays.sort(biomeOrder, (a, b) -> Double.compare(biomeKey[a], biomeKey[b]));
    int[] inlandRegion = new int[n];
    for (int i = 0; i < n; i++) inlandRegion[slotOrder[i]] = INLAND_BIOMES[biomeOrder[i]];

    // --- jitter every slot
    double[][] pos = new double[n][3];
    for (int i = 0; i < n; i++) {
      pos[i][0] = INLAND[i].x + (rnd.nextDouble() - 0.5) * 300;
      pos[i][1] = INLAND[i].z + (rnd.nextDouble() - 0.5) * 300;
      pos[i][2] = INLAND[i].weight * (0.88 + rnd.nextDouble() * 0.24);
    }

    // --- rules: deadly regions stay out of the hub's neighbourhood
    for (int i = 0; i < n; i++) {
      if (!dangerous(inlandRegion[i]) || Math.hypot(pos[i][0], pos[i][1]) > 650) continue;
      for (int tries = 0; tries < 40; tries++) {
        int j = rnd.nextInt(n);
        if (Math.hypot(pos[j][0], pos[j][1]) > 650 && !dangerous(inlandRegion[j])) {
          int t = inlandRegion[i];
          inlandRegion[i] = inlandRegion[j];
          inlandRegion[j] = t;
          break;
        }
      }
    }

    List<Site> sites = new ArrayList<>();
    sites.add(new Site(EMBER_HAVEN, HUB.x, HUB.z, HUB.weight * (0.95 + rnd.nextDouble() * 0.2)));
    for (int i = 0; i < n; i++)
      sites.add(new Site(inlandRegion[i], pos[i][0], pos[i][1], pos[i][2]));

    // --- the western coast: usually the ruined shore, sometimes another land meets the sea
    int shoreLeft = 0;
    boolean[] converted = new boolean[COAST.length];
    for (int i = 0; i < COAST.length; i++) converted[i] = rnd.nextInt(100) < 35;
    for (int i = 0; i < COAST.length; i++) if (!converted[i]) shoreLeft++;
    if (shoreLeft < 2) Arrays.fill(converted, false);
    for (int i = 0; i < COAST.length; i++) {
      int region = FORGOTTEN_SHORE;
      if (converted[i]) {
        do region = INLAND_BIOMES[rnd.nextInt(INLAND_BIOMES.length)];
        while (unbuildable(region));
      }
      sites.add(
          new Site(
              region,
              COAST[i].x + (rnd.nextDouble() - 0.5) * 200,
              COAST[i].z + (rnd.nextDouble() - 0.5) * 200,
              COAST[i].weight * (0.9 + rnd.nextDouble() * 0.2)));
    }

    // --- open water
    for (Slot o : OCEAN)
      sites.add(
          new Site(
              DARK_SEA,
              o.x + (rnd.nextDouble() - 0.5) * 240,
              o.z + (rnd.nextDouble() - 0.5) * 240,
              o.weight * (0.9 + rnd.nextDouble() * 0.2)));

    // --- the Chained Isles float beyond the open water, on a random side
    int side = rnd.nextInt(4);
    for (Slot iso : ISLES) {
      double x = iso.x, z = iso.z;
      for (int k = 0; k < side; k++) {
        double t = x;
        x = -z;
        z = t;
      }
      sites.add(
          new Site(
              CHAINED_ISLES,
              x + (rnd.nextDouble() - 0.5) * 200,
              z + (rnd.nextDouble() - 0.5) * 200,
              iso.weight));
    }

    // --- a few small inland seas and bays
    int lakes = 1 + rnd.nextInt(3);
    for (int tries = 0; tries < 60 && lakes > 0; tries++) {
      double x = -800 + rnd.nextDouble() * 2200, z = -900 + rnd.nextDouble() * 1800;
      boolean ok = Math.hypot(x, z) > 750;
      for (Landmark m : LANDMARKS) ok &= Math.hypot(x - m.x, z - m.z) > 520;
      for (Site s : sites)
        if (s.region == DARK_SEA || s.region == CHAINED_ISLES)
          ok &= Math.hypot(x - s.x, z - s.z) > 650;
      for (Site s : sites)
        if (s.region != DARK_SEA && s.region != CHAINED_ISLES)
          ok &= Math.hypot(x - s.x, z - s.z) > 160;
      if (!ok) continue;
      sites.add(new Site(DARK_SEA, x, z, 0.34 + rnd.nextDouble() * 0.14));
      lakes--;
    }

    // --- landmarks take the region of the nearest site; the region under a Citadel must be
    // buildable
    Layout draft = new Layout(seed, sites, new int[LANDMARKS.size()]);
    int[] landmarkRegion = new int[LANDMARKS.size()];
    for (int i = 0; i < landmarkRegion.length; i++) {
      Landmark m = LANDMARKS.get(i);
      landmarkRegion[i] = i < 2 ? EMBER_HAVEN : draft.nearestRegion(m.x, m.z);
    }
    return new Layout(seed, sites, landmarkRegion);
  }

  // ------------------------------------------------------------------ sampling

  /** Everything the map knows about one column. */
  public record Sample(double height, int region, double rough, double cave, boolean isle) {}

  private static final ThreadLocal<Object[]> LAST = ThreadLocal.withInitial(() -> new Object[3]);

  /** Cached per thread: several density channels ask for the same column in a row. */
  public static Sample sample(int x, int z) {
    Layout l = layout;
    long key = ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    Object[] last = LAST.get();
    if (last[0] == l && last[1] != null && (Long) last[1] == key) return (Sample) last[2];
    Sample result = compute(l, x, z);
    last[0] = l;
    last[1] = key;
    last[2] = result;
    return result;
  }

  private static final int N = BIOMES.size();

  private static Sample compute(Layout l, int ix, int iz) {
    double x = ix, z = iz;
    int ns = l.noiseSeed;

    // Bend the plane so Voronoi borders become wandering coastlines and ridgelines.
    double wx = warpX(l, x, z), wz = warpZ(l, x, z);

    double[] weight = new double[N];
    int nearest = voronoi(l, wx, wz, weight);

    // Fortress sites: force their region and blend their own ground in.
    double level = 0, levelHeight = 0;
    for (int k = 0; k < LANDMARKS.size(); k++) {
      Landmark m = LANDMARKS.get(k);
      double d = Math.hypot(x - m.x, z - m.z);
      double lv = 1 - smoothstep(m.flat, m.blend, d);
      if (lv <= 0) continue;
      int region = l.landmarkRegion[k];
      double claim = 1 - smoothstep(m.flat, m.flat + 120, d);
      if (claim > 0) {
        for (int i = 0; i < N; i++) weight[i] *= 1 - claim;
        weight[region] += claim;
      }
      if (d <= m.flat + 40) nearest = region;
      if (lv > level) {
        level = lv;
        levelHeight = m.height();
      }
    }

    double base = 0,
        hills = 0,
        ridges = 0,
        rough = 0,
        cave = 0,
        terrace = 0,
        river = 0,
        dunes = 0,
        canyon = 0;
    for (int i = 0; i < N; i++) {
      double w = weight[i];
      if (w < 1e-4) continue;
      Profile f = BIOMES.get(i);
      base += w * f.base;
      hills += w * f.hills;
      ridges += w * f.ridges;
      rough += w * f.rough;
      cave += w * f.cave;
      terrace += w * f.terrace;
      river += w * f.river;
      dunes += w * f.dunes;
      canyon += w * f.canyon;
    }

    double hill = fbm(x / 230, z / 230, 4, 31 + ns);
    double detail = fbm(x / 37, z / 37, 3, 32 + ns);
    double ridgeNoise = fbm(x / 310 + 90, z / 310 - 40, 4, 33 + ns);
    double ridge = Math.pow(1 - Math.min(1, Math.abs(ridgeNoise) * 1.35), 2);

    double h = base + hills * hill + ridges * (ridge - 0.3) + (0.12 * hills + rough * 3) * detail;

    // Terraces: stepped, weathered ground (the ruined shore, the Death Zone mesas).
    if (terrace > 0.01) {
      double step = 7;
      double u = h / step, f = Math.floor(u);
      double stepped = (f + smoothstep(0.35, 0.65, u - f)) * step;
      h += terrace * (stepped - h);
    }

    // Dunes: long ridges, stretched along the wind.
    if (dunes > 0.01) {
      double d = fbm(x / 95 + z / 260, z / 60 - x / 300, 2, 41 + ns);
      h += dunes * (1 - 2 * Math.abs(d)) * 0.9 + dunes * 0.25 * fbm(x / 22, z / 22, 2, 42 + ns);
    }

    // Ravines and sinkholes: narrow cuts where a noise field crosses zero.
    if (canyon > 0.5) {
      double v = Math.abs(fbm(x / 240 + 17, z / 240 + 5, 3, 51 + ns));
      h -= canyon * (1 - smoothstep(0.0, 0.07, v)) * Math.min(1, canyon / 14);
    }

    // Rivers: only in the lowlands; they fade out as the ground climbs. The banks are shallow so
    // they can be walked, and the bed sits just under the sea so the channel always holds water.
    if (river > 0.05) {
      double v = Math.abs(fbm(x / 430 + 200, z / 430 - 70, 3, 61 + ns));
      double c = Math.pow(1 - smoothstep(0.0, 0.10, v), 1.6) * river;
      double lowland = 1 - smoothstep(SEA_LEVEL + 10, SEA_LEVEL + 38, SEA_LEVEL + h);
      double bed = -2.5;
      if (h > bed) h -= Math.min(1, c * lowland) * (h - bed);
    }

    double height = SEA_LEVEL + h;
    if (level > 0) height += level * (levelHeight - height);

    // Floating islands are never split by a biome border: a whole grid cell is either Chained Isles
    // or it is not, decided once for the cell.
    int cellX = Math.floorDiv(ix, ISLE_SPACING), cellZ = Math.floorDiv(iz, ISLE_SPACING);
    boolean isle = isleCell(l, cellX, cellZ);
    int region = isle ? CHAINED_ISLES : (nearest == CHAINED_ISLES ? DARK_SEA : nearest);

    return new Sample(height, region, Math.clamp(rough, 0, 1), Math.clamp(cave, 0, 1), isle);
  }

  private static final ThreadLocal<Object[]> LAST_CELL =
      ThreadLocal.withInitial(() -> new Object[3]);

  /**
   * True when the island cell (cx, cz) lies in the Chained Isles: its centre and corners all do.
   */
  private static boolean isleCell(Layout l, int cx, int cz) {
    long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
    Object[] last = LAST_CELL.get();
    if (last[0] == l && last[1] != null && (Long) last[1] == key) return (Boolean) last[2];
    double x0 = cx * (double) ISLE_SPACING, z0 = cz * (double) ISLE_SPACING, s = ISLE_SPACING;
    if (!l.nearIsles(x0 + s / 2, z0 + s / 2)) {
      last[0] = l;
      last[1] = key;
      last[2] = Boolean.FALSE;
      return false;
    }
    double[][] probe = {
      {x0 + s / 2, z0 + s / 2}, {x0, z0}, {x0 + s, z0}, {x0, z0 + s}, {x0 + s, z0 + s}
    };
    boolean result = true;
    for (double[] q : probe) {
      int r = voronoi(l, warpX(l, q[0], q[1]), warpZ(l, q[0], q[1]), new double[N]);
      if (r != CHAINED_ISLES && (r != DARK_SEA || q == probe[0])) {
        result = false;
        break;
      }
    }
    last[0] = l;
    last[1] = key;
    last[2] = result;
    return result;
  }

  private static double warpX(Layout l, double x, double z) {
    int ns = l.noiseSeed;
    return x + 230 * fbm(x / 900, z / 900, 3, 11 + ns) + 60 * fbm(x / 230, z / 230, 2, 12 + ns);
  }

  private static double warpZ(Layout l, double x, double z) {
    int ns = l.noiseSeed;
    return z + 230 * fbm(x / 900, z / 900, 3, 21 + ns) + 60 * fbm(x / 230, z / 230, 2, 22 + ns);
  }

  /** Fills soft weights per region and returns the nearest region. */
  private static int voronoi(Layout l, double wx, double wz, double[] weight) {
    int n = l.sx.length;
    double[] dist = new double[n];
    double plain = Double.MAX_VALUE;
    for (int i = 0; i < n; i++) {
      dist[i] = Math.hypot(wx - l.sx[i], wz - l.sz[i]) / l.sw[i];
      plain = Math.min(plain, dist[i]);
    }
    // Every site's reach wobbles a little, so borders meander instead of running straight.
    double best = Double.MAX_VALUE;
    int nearest = 0;
    for (int i = 0; i < n; i++) {
      if (dist[i] < plain * 1.9 + 450) {
        dist[i] *= 1 + 0.2 * fbm(wx / 270 + i * 7.3, wz / 270 - i * 3.1, 2, 70 + i + l.noiseSeed);
      }
      if (dist[i] < best) {
        best = dist[i];
        nearest = l.sr[i];
      }
    }
    double total = 0;
    Arrays.fill(weight, 0);
    for (int i = 0; i < n; i++) {
      double gap = dist[i] - best;
      if (gap > BLEND * 5) continue;
      double w = Math.exp(-gap / BLEND);
      weight[l.sr[i]] += w;
      total += w;
    }
    for (int i = 0; i < weight.length; i++) weight[i] /= total;
    return nearest;
  }

  // ------------------------------------------------------------------ noise

  private static double smoothstep(double a, double b, double v) {
    double t = Math.clamp((v - a) / (b - a), 0, 1);
    return t * t * (3 - 2 * t);
  }

  private static final double[] GX = new double[256];
  private static final double[] GZ = new double[256];

  static {
    long state = 0x9E3779B97F4A7C15L;
    for (int i = 0; i < 256; i++) {
      state = state * 6364136223846793005L + 1442695040888963407L;
      double angle = ((state >>> 11) / (double) (1L << 53)) * Math.PI * 2;
      GX[i] = Math.cos(angle);
      GZ[i] = Math.sin(angle);
    }
  }

  private static int hash(int x, int z, int seed) {
    int h = x * 374761393 + z * 668265263 + seed * 1274126177;
    h = (h ^ (h >>> 13)) * 1274126177;
    return (h ^ (h >>> 16)) & 255;
  }

  /** Classic 2D gradient noise, roughly in [-1, 1]. */
  private static double noise(double x, double z, int seed) {
    int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
    double fx = x - x0, fz = z - z0;
    double u = fx * fx * fx * (fx * (fx * 6 - 15) + 10);
    double v = fz * fz * fz * (fz * (fz * 6 - 15) + 10);
    double n00 = dot(hash(x0, z0, seed), fx, fz);
    double n10 = dot(hash(x0 + 1, z0, seed), fx - 1, fz);
    double n01 = dot(hash(x0, z0 + 1, seed), fx, fz - 1);
    double n11 = dot(hash(x0 + 1, z0 + 1, seed), fx - 1, fz - 1);
    double a = n00 + u * (n10 - n00);
    double b = n01 + u * (n11 - n01);
    return Math.clamp((a + v * (b - a)) * 1.5, -1, 1);
  }

  private static double dot(int g, double dx, double dz) {
    return GX[g] * dx + GZ[g] * dz;
  }

  /** Fractal sum of {@code octaves} noise layers, normalised to about [-1, 1]. */
  private static double fbm(double x, double z, int octaves, int seed) {
    double sum = 0, amp = 1, norm = 0, f = 1;
    for (int i = 0; i < octaves; i++) {
      sum += amp * noise(x * f + i * 17.3, z * f - i * 9.1, seed * 31 + i);
      norm += amp;
      amp *= 0.5;
      f *= 2.03;
    }
    return sum / norm;
  }
}
