package com.shadowslave.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Explicit surface basins; no global fluid plane or implicit lava under the sky islands. */
public final class RealmWatersFeature extends Feature<NoneFeatureConfiguration> {
  public RealmWatersFeature(){super(NoneFeatureConfiguration.CODEC);}
  @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
    var l=c.level();int x0=(c.origin().getX()>>4)<<4,z0=(c.origin().getZ()>>4)<<4;
    for(int x=x0;x<x0+16;x++)for(int z=z0;z<z0+16;z++){
      var sample=RealmMap.sample(x,z);
      if(sample.isle()||l.getBiome(new BlockPos(x,80,z)).is(com.shadowslave.ShadowSlaveMod.id("chained_isles"))||sample.height()>=RealmMap.SEA_LEVEL)continue;
      for(int y=Math.max(l.getMinBuildHeight(),(int)Math.floor(sample.height())-3);y<=RealmMap.SEA_LEVEL;y++){
        var pos=new BlockPos(x,y,z);if(l.isEmptyBlock(pos))l.setBlock(pos,Blocks.WATER.defaultBlockState(),2);
      }
    }
    return true;
  }
}
