package com.shadowslave.content;

import com.google.gson.*;
import com.shadowslave.ShadowSlaveMod;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;

/** Pack-reloadable gameplay catalogue. Invalid reloads leave the last valid catalogue intact. */
public final class Content extends SimplePreparableReloadListener<JsonObject> {
  public record Trial(
      String id,
      String name,
      String boss,
      String family,
      String aspect,
      String flaw,
      String objective,
      String primary,
      String accent,
      String light,
      int color) {}

  public record Memory(
      String id,
      String name,
      String type,
      String description,
      int rank,
      double physical,
      double magic,
      int power,
      String effect,
      int design,
      String biome,
      String silhouette,
      int color,
      int trim) {}

  public static List<Trial> trials = List.of();
  public static Map<String, Memory> memories = Map.of();
  private static final Gson GSON = new Gson();
  private static final String FILE = "data/shadowslave/spell/catalogue.json";

  public static void bootstrap() {
    try (var stream = Content.class.getClassLoader().getResourceAsStream(FILE)) {
      if (stream == null) throw new IllegalStateException("Missing spell catalogue");
      install(
          JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
              .getAsJsonObject());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  protected JsonObject prepare(ResourceManager resources, ProfilerFiller profiler) {
    try (var reader = resources.openAsReader(ShadowSlaveMod.id("spell/catalogue.json"))) {
      return JsonParser.parseReader(reader).getAsJsonObject();
    } catch (Exception e) {
      ShadowSlaveMod.LOGGER.error("Cannot read Spell catalogue; retaining previous content", e);
      return null;
    }
  }

  @Override
  protected void apply(JsonObject json, ResourceManager resources, ProfilerFiller profiler) {
    if (json == null) return;
    try {
      install(json);
    } catch (Exception e) {
      ShadowSlaveMod.LOGGER.error("Invalid Spell catalogue; retaining previous content", e);
    }
  }

  private static void install(JsonObject json) {
    var nextTrials = new ArrayList<Trial>();
    var nextMemories = new LinkedHashMap<String, Memory>();
    for (var e : json.getAsJsonArray("trials")) nextTrials.add(GSON.fromJson(e, Trial.class));
    if (nextTrials.size() != 7)
      throw new IllegalArgumentException("Exactly seven ordered trial definitions are required");
    var ids = new HashSet<String>();
    var designs = new HashSet<Integer>();
    for (var t : nextTrials)
      if (t.id == null || !ids.add(t.id) || t.boss == null || t.flaw == null)
        throw new IllegalArgumentException("Invalid trial");
    for (var e : json.getAsJsonArray("memories")) {
      var m = GSON.fromJson(e, Memory.class);
      if (m.id == null
          || m.design < 1
          || m.design > 128
          || !designs.add(m.design)
          || m.biome == null
          || m.silhouette == null
          || m.rank < 0
          || m.rank > 6
          || m.magic < 0
          || m.magic > .75
          || m.physical < 0
          || m.physical > .75
          || nextMemories.put(m.id, m) != null)
        throw new IllegalArgumentException("Invalid memory definition");
    }
    trials = List.copyOf(nextTrials);
    memories = Collections.unmodifiableMap(nextMemories);
  }

  public static Trial trial(int family) {
    return trials.get(Math.clamp(family, 0, 6));
  }
}
