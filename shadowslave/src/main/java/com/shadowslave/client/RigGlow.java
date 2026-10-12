package com.shadowslave.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Emissive markers keep every parent joint's transform, including animated tails and cores. */
public final class RigGlow {
  private RigGlow() {}
  public static void render(ModelPart rig,PoseStack pose,VertexConsumer vertices,int tint,boolean runes){
    rig.visit(pose,(entry,path,index,cube)->{
      if(path.endsWith("/eyes")||(runes&&path.endsWith("/runes")))
        cube.compile(entry,vertices,0xF000F0,OverlayTexture.NO_OVERLAY,tint);
    });
  }
}
