package com.shadowslave.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public record IslandDensity() implements DensityFunction.SimpleFunction {
  public static final MapCodec<IslandDensity> CODEC=MapCodec.unit(new IslandDensity());
  public static final int SPACING=Archipelago.SPACING;
  public record Island(int x,int y,int z,int radius) {}
  /** Legacy developer API selects the first island of the cluster. */
  public static Island island(int cx,int cz){var a=Archipelago.cluster(RealmMap.layout().seed,cx,cz).getFirst();return new Island(a.x(),a.y(),a.z(),a.rx());}
  @Override public double compute(FunctionContext p){return Archipelago.density(RealmMap.layout().seed,p.blockX(),p.blockY(),p.blockZ());}
  @Override public double minValue(){return -1;}
  @Override public double maxValue(){return 1;}
  @Override public KeyDispatchDataCodec<IslandDensity> codec(){return KeyDispatchDataCodec.of(CODEC);}
}
