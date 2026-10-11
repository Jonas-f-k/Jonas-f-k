import com.shadowslave.world.RealmMap;
import java.io.*;
import java.util.*;

/**
 * Developer tool (not part of the mod).
 *
 * <pre>
 *   regions                           region table as JSON (for tools/build_worldgen.py)
 *   layout seed                       sites and landmark regions for one seed as JSON
 *   preview seed x0 z0 w h step file  raw height/region grid (for tools/preview_realm.py)
 * </pre>
 */
public final class RealmMapTool {
  public static void main(String[] args) throws IOException {
    if (args.length == 1 && args[0].equals("regions")) {
      regions();
    } else if (args.length == 2 && args[0].equals("layout")) {
      layout(Long.parseLong(args[1]));
    } else if (args.length == 8 && args[0].equals("preview")) {
      RealmMap.useSeed(Long.parseLong(args[1]));
      preview(
          Integer.parseInt(args[2]),
          Integer.parseInt(args[3]),
          Integer.parseInt(args[4]),
          Integer.parseInt(args[5]),
          Integer.parseInt(args[6]),
          args[7]);
    } else {
      System.err.println("usage: regions | layout seed | preview seed x0 z0 w h step file");
      System.exit(2);
    }
  }

  private static void regions() {
    StringBuilder b = new StringBuilder("{\"sea_level\":" + RealmMap.SEA_LEVEL + ",\"biomes\":[");
    for (int i = 0; i < RealmMap.BIOMES.size(); i++) {
      var p = RealmMap.BIOMES.get(i);
      if (i > 0) b.append(',');
      b.append(
          String.format(
              Locale.ROOT,
              "{\"index\":%d,\"biome\":\"%s\",\"code\":%.1f,\"base\":%s,\"hills\":%s,\"ridges\":%s,\"rough\":%s,\"cave\":%s,\"terrace\":%s,\"river\":%s,\"dunes\":%s,\"canyon\":%s,\"climate\":%s}",
              i, p.biome(), RealmMap.code(i), p.base(), p.hills(), p.ridges(), p.rough(), p.cave(), p.terrace(), p.river(), p.dunes(), p.canyon(), p.climate()));
    }
    b.append("],\"landmarks\":[");
    for (int i = 0; i < RealmMap.LANDMARKS.size(); i++) {
      var l = RealmMap.LANDMARKS.get(i);
      if (i > 0) b.append(',');
      b.append(String.format(Locale.ROOT, "{\"name\":\"%s\",\"x\":%d,\"z\":%d,\"flat\":%d,\"blend\":%d,\"up\":%d,\"citadel\":%b}", l.name(), l.x(), l.z(), l.flat(), l.blend(), l.up(), l.citadel()));
    }
    b.append("]}");
    System.out.println(b);
  }

  private static void layout(long seed) {
    var l = RealmMap.generate(seed);
    StringBuilder b = new StringBuilder("{\"sites\":[");
    for (int i = 0; i < l.sites.size(); i++) {
      var s = l.sites.get(i);
      if (i > 0) b.append(',');
      b.append(String.format(Locale.ROOT, "{\"region\":%d,\"x\":%.1f,\"z\":%.1f,\"weight\":%.3f}", s.region(), s.x(), s.z(), s.weight()));
    }
    b.append("],\"landmark_regions\":").append(Arrays.toString(l.landmarkRegion)).append("}");
    System.out.println(b);
  }

  private static void preview(int x0, int z0, int w, int h, int step, String file) throws IOException {
    try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
      out.writeInt(w);
      out.writeInt(h);
      for (int j = 0; j < h; j++) {
        for (int i = 0; i < w; i++) {
          var s = RealmMap.sample(x0 + i * step, z0 + j * step);
          out.writeFloat((float) s.height());
          out.writeByte(s.region());
          out.writeBoolean(s.isle());
        }
      }
    }
  }
}
