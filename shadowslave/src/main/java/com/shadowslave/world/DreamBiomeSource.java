package com.shadowslave.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;

/** Direct cartographic biome lookup; temperature and humidity no longer select Dream regions. */
public final class DreamBiomeSource extends BiomeSource {
  public static final MapCodec<DreamBiomeSource> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
      Biome.CODEC.listOf().fieldOf("biomes").forGetter(s->s.biomes)).apply(i,DreamBiomeSource::new));
  private final List<Holder<Biome>> biomes;
  public DreamBiomeSource(List<Holder<Biome>> biomes){
    if(biomes.size()!=RealmMap.BIOMES.size())throw new IllegalArgumentException("Dream biome table must follow RealmMap.BIOMES");
    this.biomes=List.copyOf(biomes);
  }
  @Override protected MapCodec<? extends BiomeSource> codec(){return CODEC;}
  @Override protected Stream<Holder<Biome>> collectPossibleBiomes(){return biomes.stream();}
  @Override public Holder<Biome> getNoiseBiome(int x,int y,int z,Climate.Sampler sampler){return biomes.get(RealmMap.sample(x*4+2,z*4+2).region());}
}
