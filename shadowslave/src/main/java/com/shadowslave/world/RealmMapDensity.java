package com.shadowslave.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Exposes one channel of the fixed {@link RealmMap} to Minecraft's density-function system, so the
 * noise router in {@code noise_settings/dream.json} can build the terrain from it.
 *
 * <p>JSON usage: {@code {"type": "shadowslave:realm_map", "channel": "height"}}.
 */
public record RealmMapDensity(Channel channel) implements DensityFunction.SimpleFunction {

  /** The quantities the map can answer for a column. */
  public enum Channel {
    /** Absolute ground height in blocks. */
    HEIGHT("height", -64, 400),
    /** Region code; matches the temperature axis of the biome source. */
    REGION("region", -1, 1),
    /** 3D overhang strength, 0..1. */
    ROUGH("rough", 0, 1),
    /** Cavern frequency, 0..1. */
    CAVE("cave", 0, 1),
    /** 1 inside a Chained Isles island cell, otherwise 0. */
    ISLE("isle", 0, 1);

    public final String id;
    public final double min;
    public final double max;

    Channel(String id, double min, double max) {
      this.id = id;
      this.min = min;
      this.max = max;
    }

    static final Codec<Channel> CODEC =
        Codec.STRING.comapFlatMap(
            name -> {
              for (Channel c : values()) if (c.id.equals(name)) return DataResult.success(c);
              return DataResult.error(() -> "Unknown realm_map channel: " + name);
            },
            c -> c.id);
  }

  public static final MapCodec<RealmMapDensity> CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(Channel.CODEC.fieldOf("channel").forGetter(RealmMapDensity::channel))
                  .apply(instance, RealmMapDensity::new));

  @Override
  public double compute(FunctionContext context) {
    RealmMap.Sample sample = RealmMap.sample(context.blockX(), context.blockZ());
    return switch (channel) {
      case HEIGHT -> sample.height();
      case REGION -> RealmMap.code(sample.region());
      case ROUGH -> sample.rough();
      case CAVE -> sample.cave();
      case ISLE -> sample.isle() ? 1 : 0;
    };
  }

  @Override
  public double minValue() {
    return channel.min;
  }

  @Override
  public double maxValue() {
    return channel.max;
  }

  @Override
  public KeyDispatchDataCodec<RealmMapDensity> codec() {
    return KeyDispatchDataCodec.of(CODEC);
  }
}
