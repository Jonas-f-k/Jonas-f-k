package com.shadowslave.test;

import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.network.SpellNetwork;
import com.shadowslave.world.*;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(ShadowSlaveMod.ID)
@PrefixGameTestTemplate(false)
public final class ReworkGameTests {
  private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}

  @GameTest(template="empty")
  public static void allVisualPacketsRoundTrip(GameTestHelper h){
    for(int family=0;family<7;family++)for(int kind=0;kind<7;kind++){
      var packet=new SpellNetwork.Visual(h.getLevel().dimension().location(),family,kind,12,80,-18,4.5f,30);
      var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
      try{SpellNetwork.Visual.CODEC.encode(buffer,packet);check(packet.equals(SpellNetwork.Visual.CODEC.decode(buffer)),"Visual packet lost values");}
      finally{buffer.release();}
    }
    boolean rejected=false;
    try{new SpellNetwork.Visual(h.getLevel().dimension().location(),0,0,Double.NaN,0,0,4,20);}
    catch(IllegalArgumentException e){rejected=true;}
    check(rejected,"Invalid visual position accepted");h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=1200)
  public static void realDreamChunksUseNativeGeology(GameTestHelper h){
    var level=h.getLevel().getServer().getLevel(DreamRealm.KEY);
    check(level!=null,"Dream dimension unavailable");
    check(level.getChunkSource().getGenerator().getBiomeSource() instanceof DreamBiomeSource,"Dream atlas biome source not active");
    BlockPos target=null;
    for(int x=-2304;x<=2304&&target==null;x+=192)for(int z=-2304;z<=2304;z+=192){
      var sample=RealmMap.sample(x,z);
      if(!sample.isle()&&sample.height()>100&&sample.height()<180){target=new BlockPos(x,80,z);break;}
    }
    check(target!=null,"No terrestrial test column found");
    level.getChunkAt(target);int solid=0;
    for(int y=-48;y<80;y+=4){
      var state=level.getBlockState(new BlockPos(target.getX(),y,target.getZ()));
      var id=BuiltInRegistries.BLOCK.getKey(state.getBlock());
      check(!id.toString().equals("minecraft:stone")&&!id.getPath().contains("ore"),"Vanilla geology: "+id);
      if(state.isSolid())solid++;
    }
    check(solid>20,"Terrestrial profile produced no solid geology");h.succeed();
  }
}
