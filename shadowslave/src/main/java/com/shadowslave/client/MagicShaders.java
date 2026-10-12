package com.shadowslave.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import com.shadowslave.ShadowSlaveMod;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.slf4j.Logger;

/** Mod-owned GLSL effect shader, managed by Minecraft's shader resource-reload lifecycle. */
@EventBusSubscriber(modid=ShadowSlaveMod.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class MagicShaders extends RenderType {
  private static final Logger LOG=LogUtils.getLogger();
  private static ShaderInstance spell;
  private static boolean warned;
  private static final RenderType MAGIC=create("shadowslave_arcane",DefaultVertexFormat.POSITION_COLOR_TEX,
      VertexFormat.Mode.QUADS,262144,false,false,CompositeState.builder()
          .setShaderState(new RenderStateShard.ShaderStateShard(()->spell))
          .setTransparencyState(ADDITIVE_TRANSPARENCY)
          .setDepthTestState(LEQUAL_DEPTH_TEST)
          .setCullState(NO_CULL)
          .setWriteMaskState(COLOR_WRITE)
          .createCompositeState(false));
  private static final RenderType SHADOW=create("shadowslave_shadow_wisps",DefaultVertexFormat.POSITION_COLOR_TEX,
      VertexFormat.Mode.QUADS,65536,false,true,CompositeState.builder()
          .setShaderState(new RenderStateShard.ShaderStateShard(()->spell))
          .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
          .setDepthTestState(LEQUAL_DEPTH_TEST)
          .setCullState(NO_CULL)
          .setWriteMaskState(COLOR_WRITE)
          .createCompositeState(false));
  private MagicShaders(){super("shadowslave_unused",DefaultVertexFormat.POSITION_COLOR_TEX,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
  @SubscribeEvent public static void register(RegisterShadersEvent event){
    spell=null;
    try {
      event.registerShader(new ShaderInstance(event.getResourceProvider(),
          ResourceLocation.fromNamespaceAndPath(ShadowSlaveMod.ID,"arcane"),DefaultVertexFormat.POSITION_COLOR_TEX),loaded->{
        spell=loaded;
        LOG.info("Shadow Slave bundled arcane GLSL shader loaded (nine procedural materials)");
      });
    }catch(java.io.IOException|RuntimeException error){
      if(!warned){warned=true;LOG.warn("Shadow Slave arcane shader unavailable; using vanilla effect rendering",error);}
    }
  }
  public static boolean isLoaded(){return spell!=null;}
  public static RenderType renderType(){return isLoaded()?MAGIC:RenderType.lightning();}
  public static RenderType renderTypeForMaterial(int material){return isLoaded()&&material==SpellGeometry.WISP?SHADOW:renderType();}
  public static void viewRange(float blocks){if(spell!=null){var uniform=spell.getUniform("MagicViewRange");if(uniform!=null)uniform.set(Math.max(1,blocks));}}
  public static void time(float seconds){if(spell!=null){var uniform=spell.getUniform("MagicTime");if(uniform!=null)uniform.set(seconds);}}
}
