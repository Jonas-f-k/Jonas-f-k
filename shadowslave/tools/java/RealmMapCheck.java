import com.shadowslave.world.RealmMap;
import java.util.*;

/**
 * Invariant checks for the generated Dream Realm map over many world seeds.
 *
 * <pre>javac -d out src/main/java/com/shadowslave/world/RealmMap.java tools/java/RealmMapCheck.java
 * java -cp out RealmMapCheck [seeds]</pre>
 */
public final class RealmMapCheck {
  static int failures = 0;

  static void check(boolean ok, String message) {
    if (!ok) {
      failures++;
      if (failures < 40) System.out.println("FAIL: " + message);
    }
  }

  public static void main(String[] args) {
    int seeds = args.length > 0 ? Integer.parseInt(args[0]) : 120;
    Set<String> biomesSeen = new TreeSet<>();
    Map<String, Integer> citadelBiomes = new TreeMap<>();
    double minLand = 1, maxLand = 0;
    for (int s = 0; s < seeds; s++) {
      long seed = s * 7919L - 31;
      RealmMap.useSeed(seed);
      var layout = RealmMap.layout();

      // determinism
      var again = RealmMap.generate(seed);
      check(again.sites.equals(layout.sites), "layout not deterministic, seed " + seed);

      // landmarks: forced region, levelled ground, buildable region under Citadels
      for (int i = 0; i < RealmMap.LANDMARKS.size(); i++) {
        var m = RealmMap.LANDMARKS.get(i);
        var sample = RealmMap.sample(m.x(), m.z());
        int region = layout.landmarkRegion[i];
        check(sample.region() == region, m.name() + " region " + sample.region() + " != " + region + " seed " + seed);
        check(Math.abs(sample.height() - m.height()) < 0.6, m.name() + " height " + sample.height() + " seed " + seed);
        String b = RealmMap.BIOMES.get(region).biome();
        check(!b.equals("dark_sea") && !b.equals("chained_isles"), m.name() + " in water biome, seed " + seed);
        if (m.citadel()) {
          citadelBiomes.merge(b, 1, Integer::sum);
          check(!b.equals("hollow_mountains") && !b.equals("death_zone") && !b.equals("amethyst_heights")
              && !b.equals("mosslight_ravine"), "citadel in " + b + " seed " + seed);
          // the whole flat court must be dry land
          for (int dx = -m.flat(); dx <= m.flat(); dx += 15)
            for (int dz = -m.flat(); dz <= m.flat(); dz += 15)
              if (Math.hypot(dx, dz) <= m.flat())
                check(RealmMap.sample(m.x() + dx, m.z() + dz).height() > RealmMap.SEA_LEVEL + 3, m.name() + " court is wet, seed " + seed);
        }
      }
      var hub = RealmMap.sample(0, 0);
      check(RealmMap.BIOMES.get(hub.region()).biome().equals("ember_haven"), "hub is not ember_haven, seed " + seed);

      // deadly regions keep their distance from the hub
      for (var site : layout.sites) {
        String b = RealmMap.BIOMES.get(site.region()).biome();
        if ((b.equals("death_zone") || b.equals("hollow_mountains")) && Math.hypot(site.x(), site.z()) < 600)
          check(false, b + " site too close to hub, seed " + seed);
      }

      // statistics over a coarse grid
      int land = 0, total = 0;
      for (int x = -2800; x <= 2800; x += 40)
        for (int z = -2800; z <= 2800; z += 40) {
          var q = RealmMap.sample(x, z);
          biomesSeen.add(RealmMap.BIOMES.get(q.region()).biome());
          check(q.height() > -64 && q.height() < 300, "height out of range " + q.height() + " seed " + seed);
          check(q.rough() >= 0 && q.rough() <= 1 && q.cave() >= 0 && q.cave() <= 1, "channel range, seed " + seed);
          if (Math.abs(x) < 2000 && Math.abs(z) < 2000) {
            total++;
            if (q.height() > RealmMap.SEA_LEVEL) land++;
          }
        }
      double frac = land / (double) total;
      minLand = Math.min(minLand, frac);
      maxLand = Math.max(maxLand, frac);

      // floating islands: a whole 104-block cell is one region, never split
      if (s < 25)
        for (int cx = -30; cx <= 30; cx++)
          for (int cz = -30; cz <= 30; cz++) {
            int x0 = cx * RealmMap.ISLE_SPACING, z0 = cz * RealmMap.ISLE_SPACING;
            if (!RealmMap.sample(x0 + 52, z0 + 52).isle()) continue;
            for (int dx = 0; dx < RealmMap.ISLE_SPACING; dx += 13)
              for (int dz = 0; dz < RealmMap.ISLE_SPACING; dz += 13)
                check(RealmMap.sample(x0 + dx, z0 + dz).region() == RealmMap.CHAINED_ISLES, "isle cell split at " + x0 + "," + z0 + " seed " + seed);
          }
    }
    System.out.printf("seeds=%d land fraction in 4000x4000 window: %.2f..%.2f%n", seeds, minLand, maxLand);
    System.out.println("biomes seen: " + biomesSeen.size() + " " + biomesSeen);
    System.out.println("biomes under citadels: " + citadelBiomes);
    System.out.println(failures == 0 ? "ALL OK" : failures + " FAILURES");
    System.exit(failures == 0 ? 0 : 1);
  }
}
