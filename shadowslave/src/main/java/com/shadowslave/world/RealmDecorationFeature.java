package com.shadowslave.world;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Seeded connected tree crowns; biome placement controls density and supported ground. */
public final class RealmDecorationFeature extends Feature<NoneFeatureConfiguration> {
  public RealmDecorationFeature(){super(NoneFeatureConfiguration.CODEC);}
  @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
    var level=context.level();var origin=context.origin();var random=context.random();
    String biome=level.getBiome(origin).unwrapKey().map(k->k.location().getPath()).orElse("");
    var palette=RealmPalette.BIOMES.get(biome);
    if(palette==null||palette.leaves().isEmpty()||!level.isEmptyBlock(origin)
        ||level.getBlockState(origin.below()).getCollisionShape(level,origin.below()).isEmpty())return false;
    boolean conifer=biome.contains("taiga"),sparse=biome.contains("meadow")||biome.contains("fields")||biome.contains("isles");
    int height=(conifer?8:sparse?5:7)+random.nextInt(conifer?5:4);
    if(origin.getY()+height+3>=level.getMaxBuildHeight())return false;
    for(int y=0;y<=height+2;y++)if(!replaceable(level,origin.above(y)))return false;
    for(int y=0;y<height;y++)wood(level,origin.above(y),palette.trunk());
    if(conifer){
      int turn=random.nextInt(4);
      for(int y=3;y<height-1;y+=2+random.nextInt(2)){
        int reach=Math.max(1,(height-y)/3),arms=3+random.nextInt(2);
        for(int arm=0;arm<arms;arm++){
          int direction=(turn+arm)%4,dx=direction==0?1:direction==2?-1:0,dz=direction==1?1:direction==3?-1:0;
          int length=Math.max(1,reach-random.nextInt(2));
          for(int n=1;n<=length;n++)wood(level,origin.offset(dx*n,y,dz*n),palette.trunk());
          crown(level,origin.offset(dx*length,y+1,dz*length),1.5f+reach*.22f,1.45f,1.5f+reach*.22f,palette.leaves(),random);
        }
        turn=(turn+1+random.nextInt(2))%4;
      }
      crown(level,origin.above(height),1.55f,2,1.55f,palette.leaves(),random);
    }else{
      int bend=random.nextInt(4),dx=bend==0?1:bend==2?-1:0,dz=bend==1?1:bend==3?-1:0;
      crown(level,origin.offset(dx,height,dz),2.5f+random.nextFloat()*.8f,2.3f,2.4f+random.nextFloat()*.7f,palette.leaves(),random);
      int branches=sparse?2:3+random.nextInt(2);
      for(int b=0;b<branches;b++){
        int direction=(bend+b)%4,bx=direction==0?1:direction==2?-1:0,bz=direction==1?1:direction==3?-1:0;
        int base=Math.max(3,height-4+random.nextInt(2)),reach=2+random.nextInt(2);
        for(int n=1;n<=reach;n++)wood(level,origin.offset(bx*n,base+(n+1)/2,bz*n),palette.trunk());
        crown(level,origin.offset(bx*reach,base+(reach+1)/2+1,bz*reach),2.15f+random.nextFloat()*.7f,1.65f+random.nextFloat()*.55f,2.2f+random.nextFloat()*.6f,palette.leaves(),random);
        if((biome.contains("ravine")||biome.contains("moonlit"))&&b%2==0){
          BlockPos tip=origin.offset(bx*(reach+2),base,bz*(reach+2));
          for(int drop=0;drop<2+random.nextInt(2);drop++)leaf(level,tip.below(drop),palette.leaves());
        }
      }
    }
    return true;
  }
  private static boolean replaceable(WorldGenLevel level,BlockPos p){
    var state=level.getBlockState(p);return state.isAir()||state.is(BlockTags.LEAVES)||state.is(BlockTags.REPLACEABLE);
  }
  private static void wood(WorldGenLevel level,BlockPos p,String material){
    if(replaceable(level,p))level.setBlock(p,RealmBlocks.state(material),2);
  }
  private static void leaf(WorldGenLevel level,BlockPos p,String material){
    if(level.isEmptyBlock(p)||level.getBlockState(p).is(BlockTags.REPLACEABLE))level.setBlock(p,RealmBlocks.state(material),2);
  }
  private static void crown(WorldGenLevel level,BlockPos center,float rx,float ry,float rz,String material,RandomSource random){
    for(int x=-(int)Math.ceil(rx);x<=rx;x++)for(int y=-(int)Math.ceil(ry);y<=ry;y++)for(int z=-(int)Math.ceil(rz);z<=rz;z++){
      float distance=x*x/(rx*rx)+y*y/(ry*ry)+z*z/(rz*rz);
      if(distance>1.12f||(distance>.88f&&random.nextInt(5)==0))continue;
      leaf(level,center.offset(x,y,z),material);
    }
  }
}
