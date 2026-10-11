package com.shadowslave.client;

import com.mojang.blaze3d.vertex.*;
import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.entity.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;

/** One complete articulated rig per species and a safe rig for each legacy trial family. */
public final class NightmareModel extends EntityModel<NightmareCreature> {
  public static final ModelLayerLocation LAYER=new ModelLayerLocation(ShadowSlaveMod.id("nightmare"),"main");
  private static final String[] LEGACY={"dragon","moonveil_wraith","leviathan","colossus","stag","griffin","wyvern"};
  private final ModelPart root;
  private final ModelPart[] species=new ModelPart[CreatureEcology.ALL.size()], legacy=new ModelPart[7];
  private int skinSpecies=-1,skinFamily;
  private boolean echo;
  private ModelPart selected;
  public NightmareModel(ModelPart root) {
    this.root=root;
    for(int i=0;i<species.length;i++)species[i]=root.getChild("species_"+i);
    for(int i=0;i<7;i++)legacy[i]=root.getChild("legacy_"+i);
    selected=legacy[0];
  }
  public static LayerDefinition createLayer() {
    var mesh=new MeshDefinition();var root=mesh.getRoot();
    for(int i=0;i<CreatureEcology.ALL.size();i++) {
      var s=CreatureEcology.ALL.get(i);
      CreatureRig.build(root,"species_"+i,s.id(),s.family(),s.cores());
    }
    for(int i=0;i<7;i++)CreatureRig.build(root,"legacy_"+i,LEGACY[i],i,3);
    return LayerDefinition.create(mesh,64,64);
  }
  /** Kept separate from entity state so all species and windup poses can be verified on the CPU. */
  public void animate(int index,int family,float swing,float stride,float age,float yaw,float pitch,float strike,boolean charging) {
    root.getAllParts().forEach(ModelPart::resetPose);
    skinFamily=Math.clamp(family,0,6);skinSpecies=index>=0&&index<species.length?index:-1;
    for(int i=0;i<species.length;i++)species[i].visible=i==skinSpecies;
    for(int i=0;i<7;i++)legacy[i].visible=skinSpecies<0&&i==skinFamily;
    selected=skinSpecies<0?legacy[skinFamily]:species[skinSpecies];
    String id=skinSpecies<0?LEGACY[skinFamily]:CreatureEcology.ALL.get(skinSpecies).id();
    CreatureRig.animate(selected,id,swing,stride,age,yaw,pitch,strike,charging,id.equals("aurora_moth")||id.equals("spire_messenger"));
  }
  @Override public void setupAnim(NightmareCreature mob,float swing,float stride,float age,float yaw,float pitch) {
    echo=mob.isEcho();
    animate(CreatureEcology.renderIndex(mob.species()),mob.family(),swing,stride,age,yaw,pitch,attackTime,mob.isCharging());
  }
  @Override public void renderToBuffer(PoseStack pose,VertexConsumer buffer,int light,int overlay,int tint) {
    var sprite=net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
      .apply(ShadowSlaveMod.id(skinSpecies<0?"entity/nightmare_"+skinFamily:"entity/species_"+skinSpecies));
    root.render(pose,sprite.wrap(buffer),light,overlay,echo?0xFFBCDCE5:tint);
  }
  public void renderGlow(PoseStack pose,VertexConsumer buffer,int tint,boolean boss) {
    pose.pushPose();selected.translateAndRotate(pose);
    var head=selected.getChild("head");pose.pushPose();head.translateAndRotate(pose);
    var eyes=head.getChild("eyes");eyes.visible=true;
    eyes.render(pose,buffer,0xF000F0,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,tint);
    eyes.visible=false;pose.popPose();
    if(boss){var runes=selected.getChild("runes");runes.visible=true;runes.render(pose,buffer,0xF000F0,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,tint);runes.visible=false;}
    pose.popPose();
  }
}
