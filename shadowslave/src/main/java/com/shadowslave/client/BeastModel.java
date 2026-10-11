package com.shadowslave.client;

import com.mojang.blaze3d.vertex.*;
import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.combat.BeastForms;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.player.AbstractClientPlayer;

/** Seven distinct anatomies with hierarchical knees, wing fingers, jaws and tail chains. */
public final class BeastModel extends EntityModel<AbstractClientPlayer> {
  public static final ModelLayerLocation LAYER=new ModelLayerLocation(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ShadowSlaveMod.ID,"beasts"),"main");
  private static final String[] IDS={"dragon","wolf","leviathan","colossus","stag","griffin","wyvern"};
  private final ModelPart root;
  private final ModelPart[] forms=new ModelPart[7];
  private int selected;
  public BeastModel(ModelPart root){this.root=root;for(int i=0;i<7;i++)forms[i]=root.getChild("beast_"+i);}
  public static LayerDefinition createLayer(){var mesh=new MeshDefinition();for(int i=0;i<7;i++)CreatureRig.build(mesh.getRoot(),"beast_"+i,IDS[i],i,4);return LayerDefinition.create(mesh,CreatureRig.TEXTURE_WIDTH,CreatureRig.TEXTURE_HEIGHT);}
  public void animate(int form,float swing,float amount,float age,float yaw,float pitch,float strike,boolean flying,boolean breathing){
    selected=Math.clamp(form,0,6);root.getAllParts().forEach(ModelPart::resetPose);
    for(int i=0;i<7;i++)forms[i].visible=i==selected;
    CreatureRig.animate(forms[selected],IDS[selected],swing,amount,age,yaw,pitch,strike,breathing,flying);
  }
  @Override public void setupAnim(AbstractClientPlayer p,float swing,float amount,float age,float yaw,float pitch){animate(BeastForms.active(p),swing,amount,age,yaw,pitch,attackTime,p.getAbilities().flying,false);}
  @Override public void renderToBuffer(PoseStack pose,VertexConsumer buffer,int light,int overlay,int tint){
    var sprite=net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(ShadowSlaveMod.id("entity/beast_"+selected));
    root.render(pose,sprite.wrap(buffer),light,overlay,tint);
  }
  public void glow(PoseStack pose,VertexConsumer buffer,int tint){
    var f=forms[selected];pose.pushPose();f.translateAndRotate(pose);
    var head=f.getChild("head");head.translateAndRotate(pose);var eyes=head.getChild("eyes");eyes.visible=true;
    eyes.render(pose,buffer,0xF000F0,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,tint);eyes.visible=false;pose.popPose();
  }
  public ModelPart claw(int sign){
    var f=forms[selected];
    if(f.hasChild("arm_"+sign)){var claw=f.getChild("arm_"+sign);claw.visible=true;return claw;}
    if(f.hasChild("leg_"+(sign<0?0:1)))return f.getChild("leg_"+(sign<0?0:1));
    return f.getChild("wing_"+sign);
  }
}
