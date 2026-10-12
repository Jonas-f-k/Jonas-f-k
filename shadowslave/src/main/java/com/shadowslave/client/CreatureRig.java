package com.shadowslave.client;

import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Native, hierarchical rigs. All dimensions are in model pixels, not world blocks. */
public final class CreatureRig {
  private CreatureRig() {}
  /** Four 64x32 material regions: skin, flexible joints, raised plates, hard tips. */
  public static final int TEXTURE_WIDTH = 64, TEXTURE_HEIGHT = 128;
  private static CubeListBuilder box(float x,float y,float z,float w,float h,float d) {
    return CubeListBuilder.create().texOffs(0,0).addBox(x,y,z,w,h,d);
  }
  private static CubeListBuilder surface(String name,float x,float y,float z,float w,float h,float d) {
    int band = 0;
    if (name.startsWith("leg_") || name.startsWith("arm_") || name.equals("knee")
        || name.equals("elbow") || name.equals("hand") || name.startsWith("finger_")
        || name.equals("jaw") || name.equals("segment") || name.equals("tail")
        || name.startsWith("socket_")) band = 1;
    if (name.contains("plate") || name.contains("carapace") || name.contains("pauldron")
        || name.contains("shield") || name.contains("ridge") || name.contains("band")
        || name.contains("mane") || name.contains("feather") || name.contains("petal")
        || name.contains("moss") || name.contains("veil") || name.contains("foliage")
        || name.contains("leaf") || name.contains("helmet") || name.equals("cap")
        || name.equals("base") || name.startsWith("stone_") || name.contains("mantle") || name.equals("fin")) band = 2;
    if (name.contains("horn") || name.contains("fang") || name.contains("claw")
        || name.contains("tusk") || name.contains("tooth") || name.contains("weapon")
        || name.contains("spine") || name.contains("crown") || name.contains("tine")
        || name.contains("beak") || name.contains("shard") || name.contains("prism")
        || name.equals("tip") || name.equals("point") || name.equals("fruit")
        || name.startsWith("skull_") || name.contains("skeletal")) band = 3;
    int spanX = (int)Math.ceil(2*(w+d)), spanY = (int)Math.ceil(h+d);
    int hash = name.hashCode();
    int u = Math.floorMod(hash, Math.max(1, TEXTURE_WIDTH-spanX+1));
    int v = band*32 + Math.floorMod(hash/31, Math.max(1, 32-spanY+1));
    return CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,w,h,d);
  }
  private static PartDefinition part(PartDefinition p,String name,float x,float y,float z,float w,float h,float d,float px,float py,float pz) {
    return p.addOrReplaceChild(name,surface(name,x,y,z,w,h,d),PartPose.offset(px,py,pz));
  }
  private static PartDefinition empty(PartDefinition p,String name) {
    return p.addOrReplaceChild(name,CubeListBuilder.create(),PartPose.ZERO);
  }
  private static void eye(PartDefinition head,float x,float y,float z,float width) {
    head.addOrReplaceChild("eyes",box(-x-width,y,z,width,.65f,.18f).addBox(x,y,z,width,.65f,.18f),PartPose.ZERO);
  }
  private static void jaw(PartDefinition head,float width,float depth) {
    var j=part(head,"jaw",-width/2,0,-depth,width,1.5f,depth+1,0,2,-1);
    for(int s:new int[]{-1,1}) part(j,"fang_"+s,s*(width/2-1),-1,-depth+.5f,.7f,1.6f,.7f,0,0,0);
  }
  private static void limb(PartDefinition rig,int i,float x,float y,float z,float length,boolean insect,boolean hoof) {
    int sign=x<0?-1:1;
    var upper=part(rig,"leg_"+i,-1,0,-1,2,length*.52f,2,x,y,z);
    if(insect) upper=rig.addOrReplaceChild("leg_"+i,surface("leg_"+i,-.65f,0,-.65f,1.3f,length*.52f,1.3f),PartPose.offsetAndRotation(x,y,z,0,0,-sign*.85f));
    var lower=part(upper,"knee",-.7f,0,-.8f,1.4f,length*.5f,1.6f,0,length*.49f,0);
    lower.addOrReplaceChild("foot",surface("plate_foot",hoof?-1.1f:-1.4f,0,-2.6f,hoof?2.2f:2.8f,1.4f,3.5f),PartPose.offset(0,length*.46f,0));
    if(!hoof)for(int c=0;c<3;c++)part(lower,"claw_"+c,-1.3f+c*.9f,.7f,-3.4f,.5f,.7f,1.4f,0,length*.46f,0);
  }
  private static void arm(PartDefinition rig,int sign,boolean blade,boolean massive) {
    var a=part(rig,"arm_"+sign,massive?-1.8f:-1.3f,-1,-1.7f,massive?3.6f:2.6f,6,3.4f,sign*(massive?5.7f:4.4f),3,0);
    var fore=part(a,"elbow",massive?-1.8f:-1.2f,0,-1.5f,massive?3.6f:2.4f,6,3,0,5,0);
    part(fore,"hand",massive?-2.2f:-1.6f,4.7f,-2,massive?4.4f:3.2f,2.6f,4,0,0,0);
    if(blade)part(fore,"weapon",-.5f,-7,-3.5f,1,15,1,0,5,0);
    else for(int i=0;i<3;i++)part(fore,"finger_"+i,-1.2f+i*.8f,6,-2.7f,.6f,2,1.8f,0,0,0);
  }
  private static void wing(PartDefinition rig,int sign,boolean feathers,boolean moth) {
    var w=part(rig,"wing_"+sign,sign<0?-3:0,-.6f,-.6f,3,1.2f,1.2f,sign*2.5f,moth?4:2,1);
    var elbow=part(w,"elbow",sign<0?-3:0,0,-.4f,3,2, .8f,sign*2.7f,0,0);
    var tip=part(elbow,"tip",sign<0?-2.5f:0,0,-.3f,2.5f,1,.6f,sign*2.7f,0,0);
    for(int i=0;i<5;i++) {
      float xx=sign<0?-1.4f*(i+1):i*1.4f;
      var feather=part(w,"feather_"+i,xx,1+i*.35f,-.4f,1.8f,moth?10-i*.8f:8-i*.85f,.8f,0,0,0);
      if(feathers)part(feather,"point",.35f,6-i*.6f,-.35f,1,3,.7f,0,0,0);
    }
    if(!feathers&&!moth)for(int i=0;i<3;i++)part(w,"rib_"+i,sign<0?-6+i*1.5f:1+i*1.5f,0,-.7f,.5f,8-i,1.4f,0,0,0);
  }
  private static void tail(PartDefinition rig,int count,boolean fin) {
    PartDefinition p=part(rig,"tail",-1,0,0,2,2,2,0,12,5);
    for(int i=1;i<count;i++)p=part(p,"segment",-.9f+i*.12f,0,0,Math.max(.6f,1.8f-i*.24f),2,2,0,1,1.6f);
    if(fin)part(p,"fin",-2,-1,0,4,3,.6f,0,0,1.2f);
  }
  private static void horn(PartDefinition head,int sign,boolean antlers,boolean ice) {
    var h=part(head,"horn_"+sign,-.55f,-4,-.6f,1.1f,4,1.2f,sign*2.7f,-2,1);
    h=part(h,"tip",-.45f,-3,-.5f,.9f,3,1,sign*.9f,-3,.6f);
    h=part(h,"crown",-.3f,-2,-.3f,.6f,2,.6f,sign*.5f,-2,.4f);
    if(antlers)for(int i=0;i<3;i++)part(h,"tine_"+i,sign<0?-2.3f:0,-i*1.4f,-.3f,2.3f,.6f,.6f,0,0,0);
    if(ice)part(h,"shard",-.8f,-2,-1,1.6f,4,2,0,0,0);
  }

  public static PartDefinition build(PartDefinition parent,String name,String id,int family,int cores) {
    if(BeastRig.isForm(id))return BeastRig.build(parent,name,id,family,cores);
    var f=empty(parent,name);
    switch(id) {
      case "carapace_scavenger","carapace_centurion","carapace_demon" -> nativeCrab(f,id);
      case "iron_spider","iron_spider_matriarch","glass_huntsman" -> nativeSpider(f,id);
      case "black_ravager","dread_wolf","rime_stalker","moss_hart","cinder_hound" -> nativeQuadruped(f,id);
      case "chain_worm","ivory_burrower","azure_serpent" -> nativeWorm(f,id);
      case "spire_messenger","aurora_moth" -> nativeFlyer(f,id);
      case "blood_flower","briar_maw" -> nativeFlower(f,id);
      case "mordant_mimic" -> nativeMimic(f);
      case "soul_devouring_tree" -> nativeTree(f);
      case "rolling_stone" -> nativeBoulder(f);
      case "gilded_mantis" -> nativeMantis(f);
      case "flesh_reaver" -> nativeReaver(f);
      case "lord_of_dead" -> nativeBoneMound(f);
      case "stone_saint","forsaken_knight","chalice_maiden","sailor_doll",
           "dune_revenant","hollow_watcher","ashen_colossus","moonveil_wraith" -> nativeBiped(f,id);
      case "wolf","stag" -> quadruped(f,id);
      case "leviathan" -> serpent(f,id);
      case "griffin" -> flyer(f,id);
      case "dragon","wyvern" -> dragon(f,id);
      default -> humanoid(f,id,family,cores);
    }
    // Every model keeps a readable center; emissive geometry has its own render pass.
    // Eyes and the optional boss sigil keep the renderer's explicit fullbright contract.
    part(f,"runes",-.5f,6,-3.7f,1,2,.16f,0,0,0);
    return f;
  }

  private static void quadruped(PartDefinition f,String id) {
    boolean deer=id.equals("moss_hart")||id.equals("stag");
    boolean boar=id.equals("black_ravager");
    var b=part(f,"body",-3.4f,-3,-4,6.8f,6,10,0,10,0);
    part(b,"shoulders",-3.8f,-4,-3.5f,7.6f,5,4,0,0,0);
    if(!deer)for(int i=0;i<4;i++)part(b,"mane_"+i,-2.8f,-4-i*.15f,-3+i*1.6f,5.6f,2.2f,2,0,0,0);
    var head=part(f,"head",-2.4f,-3,-3,4.8f,5,5,0,deer?3:7,-3);
    part(head,"muzzle",-1.7f,-.3f,-5,3.4f,2.4f,3,0,0,0);
    eye(head,.7f,-1.2f,-3.2f,1.1f);jaw(head,3.4f,4);
    for(int s:new int[]{-1,1}) {
      part(head,"ear_"+s,-.7f,-2,-.6f,1.4f,2.8f,1.2f,s*2,-2,.4f);
      if(deer||id.equals("rime_stalker"))horn(head,s,deer,!deer);
      if(boar)part(head,"tusk_"+s,-.5f,-1,-.5f,1,4,1,s*2,1,-4);
    }
    for(int i=0;i<4;i++)limb(f,i,(i%2==0?-1:1)*2.7f,12,i<2?-3:4,10,false,deer);
    tail(f,deer?2:4,false);
    if(id.equals("cinder_hound"))for(int i=0;i<4;i++)part(b,"ember_"+i,-.5f,-6,-2+i*2,1,3,1.2f,0,0,0);
    if(deer)for(int s:new int[]{-1,1})part(b,"moss_"+s,s<0?-3.8f:2.8f,-1,0,1,4,4,0,0,0);
  }
  private static void serpent(PartDefinition f,String id) {
    boolean leviathan=id.equals("leviathan");
    var b=part(f,"body",-2.3f,-3,-2.3f,4.6f,7,4.6f,0,9,0);
    PartDefinition p=b;
    for(int i=0;i<5;i++) {
      p=part(p,"segment",-2+i*.18f,0,-1.8f,4-i*.36f,3.5f,3.6f,0,3.1f,i<2?0:1.2f);
      part(p,"band",-2.2f+i*.18f,.4f,-2,4.4f-i*.36f,.8f,4,0,0,0);
    }
    var head=part(f,"head",-3.2f,-3.5f,-3.5f,6.4f,5.5f,5.5f,0,1,-1);
    eye(head,1,-1,-3.7f,1.4f);jaw(head,5,3.5f);
    for(int s:new int[]{-1,1}) {
      horn(head,s,false,false);
      if(leviathan||id.equals("azure_serpent"))part(head,"gill_"+s,s<0?-4.8f:3,-2,0,1.8f,5,.8f,0,0,0);
      else part(head,"mandible_"+s,-.7f,-1,-3.5f,1.4f,3,4,s*2,1,-1);
    }
    if(id.equals("ivory_burrower")) {
      for(int sign:new int[]{-1,1})part(head,"ivory_tusk_"+sign,-.55f,-4,-.55f,1.1f,7,1.1f,sign*2.6f,2,-4);
      part(b,"bone_keel",-.7f,-4,2,1.4f,8,1.4f,0,0,0);
    }
    if(id.equals("chain_worm"))for(int i=0;i<3;i++)part(b,"chain_lock_"+i,-2.6f,-1+i*2,-2.7f,5.2f,1.3f,5.4f,0,0,0);
    tail(f,5,true);
    if(leviathan)for(int s:new int[]{-1,1})wing(f,s,false,true);
  }
  private static void flyer(PartDefinition f,String id) {
    boolean moth=id.equals("aurora_moth");
    if(id.equals("griffin"))quadruped(f,"griffin_body");
    else {
      var b=part(f,"body",-1.7f,-2,-1.8f,3.4f,12,3.6f,0,3,0);
      if(moth)for(int i=0;i<4;i++)part(b,"band_"+i,-1.8f,i*2, -.2f,3.6f,1,2,0,0,0);
      var h=part(f,"head",-2,-2,-2,4,4,4,0,1,-1);
      if(!moth)part(h,"beak",-1,-.5f,-5,2,2,3,0,0,0);
      eye(h,.4f,-.4f,-2.2f,.9f);
      for(int s:new int[]{-1,1})part(h,"antenna_"+s,-.25f,-5,0,.5f,5,.5f,s*1.7f,-1,0);
      for(int i=0;i<2;i++)limb(f,i,(i==0?-1:1)*1.3f,15,0,7,false,false);
      tail(f,3,false);
    }
    wing(f,-1,!moth,moth);wing(f,1,!moth,moth);
    if(id.equals("griffin")) {
      var h=f.getChild("head");
      part(h,"beak",-1.3f,-.2f,-6,2.6f,3,3,0,0,0);
      for(int s:new int[]{-1,1})horn(h,s,false,false);
    }
  }
  private static void humanoid(PartDefinition f,String id,int family,int cores) {
    boolean giant=id.contains("colossus")||id.equals("lord_of_dead");
    boolean ghost=id.contains("wraith")||id.equals("chalice_maiden")||id.equals("hollow_watcher");
    var b=part(f,"body",-3.5f,0,-2.5f,7,11,5,0,2,0);
    if(id.equals("lord_of_dead")) {
      b=f.addOrReplaceChild("body",box(-1,0,-1.5f,2,11,3),PartPose.offset(0,2,0));
      for(int i=0;i<4;i++)part(b,"skeletal_rib_"+i,-3.6f,1+i*2.5f,-2.2f,7.2f,.9f,4.4f,0,0,0);
    }
    var h=part(f,"head",-2.8f,-5,-2.8f,5.6f,5.5f,5.6f,0,0,0);
    eye(h,.7f,-2.5f,-3,1.2f);jaw(h,3.5f,2);
    for(int s:new int[]{-1,1}) {
      arm(f,s,id.equals("forsaken_knight")||id.equals("stone_saint")||id.equals("dune_revenant"),giant);
      if(giant||id.contains("knight")||id.equals("stone_saint"))part(f.getChild("arm_"+s),"pauldron",-2,-2,-2.5f,4,4,5,0,0,0);
      if(cores>=4||family==0||family==4)horn(h,s,family==4,false);
    }
    if(!ghost)for(int i=0;i<2;i++)limb(f,i,(i==0?-1:1)*2,13,0,9,false,giant);
    else for(int i=0;i<5;i++)part(b,"veil_"+i,-3.4f+i*1.4f,9,-2.4f,1.3f,10+i%2*3,.7f,0,0,0);
    if(id.equals("hollow_watcher"))h.addOrReplaceChild("eyes",box(-1.2f,-3,-3.1f,2.4f,2,.2f),PartPose.ZERO);
    if(id.equals("forsaken_knight")||id.equals("stone_saint")) {
      part(b,"breastplate",-3.7f,1,-2.8f,7.4f,6,.6f,0,0,0);
      part(f.getChild("arm_1").getChild("elbow"),"shield",-2,0,-3,4,8,1,0,0,0);
    }
    if(id.equals("sailor_doll")) {
      part(h,"hat",-3.4f,-5.6f,-3.4f,6.8f,1,6.8f,0,0,0);
      part(b,"collar",-4,0,-3,8,2,6,0,0,0);
    }
    if(giant)for(int i=0;i<3;i++)part(b,"rib_"+i,-3.8f,2+i*3,-2.9f,7.6f,1,.8f,0,0,0);
    if(id.equals("moonveil_wraith")||id.equals("chalice_maiden"))part(h,"halo",-3.7f,-6,0,7.4f,.6f,1,0,0,0);
    if(id.equals("chalice_maiden")) {
      var hand=f.getChild("arm_1").getChild("elbow");
      part(hand,"chalice_cup",-1.6f,-1,-1.6f,3.2f,2,3.2f,0,7,-2);
      part(hand,"chalice_stem",-.35f,0,-.35f,.7f,3,.7f,0,8,-2);
      part(b,"skirt",-4.4f,9,-3,8.8f,5,6,0,0,0);
    }
    if(id.equals("lord_of_dead")) {
      part(h,"crown",-3.2f,-6,-3.2f,6.4f,1,6.4f,0,0,0);
      for(int i=0;i<3;i++)part(h,"crown_spike_"+i,-2.2f+i*2,-8,-2.5f,.7f,3,.7f,0,0,0);
      for(int sign:new int[]{-1,1})part(b,"mantle_"+sign,sign<0?-4.5f:2.8f,1,2,1.7f,15,1.2f,0,0,0);
    }
    if(id.equals("ashen_colossus"))for(int sign:new int[]{-1,1})part(b,"basalt_plate_"+sign,sign<0?-4.5f:2.5f,0,-3,2,5,6,0,0,0);
    if(id.equals("dune_revenant"))for(int i=0;i<3;i++)part(b,"bandage_"+i,-3.7f,2+i*3,-2.7f,7.4f,1.2f,5.4f,0,0,0);
  }
  private static void dragon(PartDefinition f,String id) {
    boolean wyvern=id.equals("wyvern");
    var b=part(f,"body",-3,-3,-3,6,12,7,0,7,0);
    part(b,"neck",-1.8f,-8,-2.4f,3.6f,8,3.6f,0,0,0);
    var h=part(f,"head",-2.7f,-3,-3,5.4f,5,5,0,-1,-1);
    part(h,"snout",-1.8f,-.2f,-5.5f,3.6f,2.2f,3,0,0,0);
    eye(h,.8f,-1,-3.2f,1.1f);jaw(h,3.6f,4.5f);
    for(int s:new int[]{-1,1}) {horn(h,s,false,false);wing(f,s,false,false);if(!wyvern)arm(f,s,false,false);}
    for(int i=0;i<2;i++)limb(f,i,(i==0?-1:1)*2.5f,16,3,6,false,false);
    tail(f,5,true);
    for(int i=0;i<5;i++)part(b,"spine_"+i,-.45f,-5+i*3,3, .9f,2.4f,1.4f,0,0,0);
    // First-person anatomy remains available for each form, including winged forelimbs.
    if(wyvern)for(int s:new int[]{-1,1})arm(f,s,false,false);
  }

  /* Species rigs use a restrained voxel vocabulary: one silhouette, visible joint hinges,
     and a few functional hard surfaces. Cubes are not stacked as arbitrary decoration. */
  private static PartDefinition posed(PartDefinition p,String n,float x,float y,float z,float w,float h,float d,
      float px,float py,float pz,float rx,float ry,float rz) {
    return p.addOrReplaceChild(n,surface(n,x,y,z,w,h,d),PartPose.offsetAndRotation(px,py,pz,rx,ry,rz));
  }
  private static void jointLeg(PartDefinition f,int i,float x,float y,float z,float length,
      float width,float hipX,float hipY,float hipZ,float kneeAngle,boolean hoof) {
    var l=posed(f,"leg_"+i,-width/2,0,-width/2,width,length*.48f,width,x,y,z,hipX,hipY,hipZ);
    var k=posed(l,"knee",-width*.42f,0,-width*.42f,width*.84f,length*.5f,width*.84f,
        0,length*.46f,0,kneeAngle,0,0);
    part(k,"foot",-width*.65f,0,-width*.9f,width*1.3f,1,width*1.6f,0,length*.46f,0);
    if(!hoof)for(int c=0;c<2;c++)part(k,"claw_"+c,-width*.5f+c*width*.7f,.2f,-width*1.3f,
        .45f,.5f,width*.8f,0,length*.46f,0);
  }
  private static void radialLeg(PartDefinition f,int i,float x,float y,float z,float length,float yaw,float spread) {
    int s=x<0?-1:1;
    var l=posed(f,"leg_"+i,-.55f,0,-.55f,1.1f,length*.48f,1.1f,x,y,z,.08f,yaw,-s*spread);
    var k=posed(l,"knee",-.42f,0,-.42f,.84f,length*.58f,.84f,0,length*.46f,0,0,0,s*(spread-.18f));
    posed(k,"foot",-.3f,0,-.65f,.6f,1.8f,1.3f,0,length*.55f,0,.12f,0,0);
    part(k,"claw",-.2f,1.1f,-1,.4f,.7f,1.3f,0,length*.55f,0);
  }
  private static void nativeCrab(PartDefinition f,String id) {
    boolean centurion=id.equals("carapace_centurion"),demon=id.equals("carapace_demon");
    float lift=demon?11:centurion?12:14;
    var b=part(f,"body",-4,-2,-3,8,5,7,0,lift,0);
    part(b,"carapace_top",-3.5f,-3,-2.5f,7,2,6,0,0,0);
    part(b,"carapace_back",-3,-1,3,6,3,2,0,0,0);
    for(int s:new int[]{-1,1})part(b,"carapace_rim_"+s,s<0?-4.5f:3.5f,-1,-2,1,2,6,0,0,0);
    // Upright front mass and eight laterally hinged scythe legs, not a humanoid with horns.
    var breast=part(b,"front_plate",-3.2f,-5,-4,6.4f,6,3,0,0,0);
    if(centurion) {
      part(breast,"crest_plate",-2.5f,-2,-.4f,5,2,1,0,-3,-1);
      part(breast,"crest_tip",-1.5f,-1,-.3f,3,1,.6f,0,-4,-1);
    }
    if(demon)for(int s:new int[]{-1,1}) {
      posed(b,"demon_carapace_flange_"+s,-.8f,-2,-2,1.6f,4,4,s*3.8f,-3,0,0,0,s*.32f);
      posed(b,"demon_spine_"+s,-.5f,-3,-.5f,1,3,1,s*2,-3,2,-.35f,0,s*.3f);
    }
    var h=part(f,"head",-2,-1.5f,-2,4,3,3,0,lift-3,-4);
    eye(h,.55f,-.3f,-2.16f,.6f);
    var j=part(h,"jaw",-1.6f,0,-2,3.2f,1.2f,2.2f,0,1,-.4f);
    for(int s:new int[]{-1,1})posed(j,"mandible_"+s,-.35f,-.4f,-2,.7f,1.4f,2,s*1.2f,0,-.6f,0,s*.3f,0);
    if(demon)for(int s:new int[]{-1,1}) {
      var crown=posed(h,"horn_"+s,-.45f,-2,-.45f,.9f,2,.9f,s*1.4f,-1.3f,0,.2f,0,s*.35f);
      posed(crown,"horn_tip",-.3f,-1.5f,-.3f,.6f,1.5f,.6f,0,-1.8f,0,-.4f,0,-s*.35f);
    }
    for(int i=0;i<8;i++) {
      int s=i%2==0?-1:1,n=i/2;
      radialLeg(f,i,s*3.8f,lift+1,-2+n*1.8f,centurion||demon?11:9,(n-1.5f)*s*.35f,.88f);
    }
    for(int s:new int[]{-1,1}) {
      float reach=demon?5:4;
      var a=posed(f,"arm_"+s,-1,0,-1,2,reach,2,s*3,lift-3,-3,-.5f,s*-.25f,-s*.28f);
      var e=posed(a,"elbow",-1.25f,0,-1.25f,2.5f,reach,2.5f,0,reach-.3f,0,-1.12f,0,0);
      if(centurion) {
        var scythe=part(e,"weapon_scythe",-.6f,0,-.5f,1.2f,7,1,0,reach-.3f,0);
        var blade=posed(scythe,"weapon_blade",-.4f,0,-.5f,.8f,5,1,0,6.5f,0,.58f,0,s*.18f);
        posed(blade,"weapon_point",-.22f,0,-.3f,.44f,2,.6f,0,4.5f,0,.5f,0,0);
        continue;
      }
      part(e,"plate_palm",-1.8f,reach-1,-1.4f,3.6f,3,2.8f,0,0,0);
      var p=part(e,"pincer",-.65f,0,-1,1.3f,3.5f,2,0,reach+1,-.5f);
      posed(p,"pincer_tip",-.5f,0,-.6f,1,2,1.2f,0,3,0,.25f,0,s*.35f);
      posed(e,"thumb",-.6f,0,-.8f,1.2f,3.7f,1.6f,s*1.5f,reach+.8f,0,0,0,-s*.3f);
    }
  }
  private static void nativeSpider(PartDefinition f,String id) {
    boolean brood=id.equals("iron_spider_matriarch"),glass=id.equals("glass_huntsman");
    var b=part(f,"body",-2.6f,-1.5f,-2.8f,5.2f,3,4.6f,0,14,-1);
    float wide=brood?4.2f:glass?3.1f:3.5f;
    part(b,"abdomen",-wide,-2.5f,1.4f,wide*2,5,6,0,-.2f,0);
    part(b,"abdomen_cap",-wide+.8f,-3.5f,2.2f,(wide-.8f)*2,1.4f,4.4f,0,0,0);
    part(b,"abdomen_tip",-wide+.9f,-1.5f,6.5f,(wide-.9f)*2,3,1.4f,0,0,0);
    var h=part(f,"head",-2,-1.3f,-2,4,2.6f,3.5f,0,14,-3.6f);
    h.addOrReplaceChild("eyes",box(-1.65f,-.6f,-2.17f,.7f,.65f,.18f)
        .addBox(.95f,-.6f,-2.17f,.7f,.65f,.18f).addBox(-.55f,-.9f,-2.17f,.4f,.4f,.18f)
        .addBox(.15f,-.9f,-2.17f,.4f,.4f,.18f),PartPose.ZERO);
    var j=part(h,"jaw",-1.4f,0,-1.5f,2.8f,.8f,2,0,1,-.5f);
    for(int s:new int[]{-1,1})posed(j,"fang_"+s,-.4f,0,-1,.8f,2,1,s*.9f,0,-.7f,.45f,0,-s*.22f);
    for(int i=0;i<8;i++) {
      int s=i%2==0?-1:1,n=i/2;
      radialLeg(f,i,s*2.4f,14,-3+n*1.6f,11,(n-1.5f)*s*.48f,glass?1.12f:1.0f);
      if(glass)part(f.getChild("leg_"+i),"glass_joint_plate",-.7f,1,-.7f,1.4f,2,1.4f,0,0,0);
    }
    if(brood)for(int s:new int[]{-1,1}) {
      part(b,"brood_plate_"+s,s<0?-4.5f:3.5f,-1,3,1,3,3,0,0,0);
      part(b,"spinneret_"+s,-.5f,-.5f,0,1,1,2,s*1.5f,1.5f,7);
    }
    if(glass)for(int s:new int[]{-1,1})posed(b,"prism_ridge_"+s,-.45f,-2,-.45f,.9f,2,.9f,s*2,-2.5f,4,0,0,s*.4f);
  }
  private static void nativeQuadruped(PartDefinition f,String id) {
    boolean hart=id.equals("moss_hart"),ravager=id.equals("black_ravager"),rime=id.equals("rime_stalker");
    float y=hart?9:12;
    var b=part(f,"body",ravager?-2.2f:-2.7f,-2.4f,-3,ravager?4.4f:5.4f,ravager?4.2f:5,ravager?10:8,0,y,0);
    part(b,"shoulder_mane",ravager?-2.5f:-3,-3,-3.8f,ravager?5:6,ravager?4.5f:5.5f,3.5f,0,0,0);
    part(b,"haunch",-2.8f,-2,3,5.6f,5,3,0,0,0);
    part(b,"neck",-1.9f,-4,-5,3.8f,5,3.5f,0,0,0);
    var h=part(f,"head",-2,-2.5f,-2.5f,4,4,4.5f,0,y-3,-4.5f);
    part(h,"muzzle",ravager?-1.8f:-1.35f,-.25f,ravager?-4.3f:-5,ravager?3.6f:2.7f,1.9f,ravager?2.3f:3,0,0,0);
    part(h,"nose",-1,-.1f,-5.2f,2,.9f,.35f,0,0,0);
    // Side-set eyes read as a quadruped face rather than two human eye slits.
    h.addOrReplaceChild("eyes",box(-2.07f,-.7f,-1.9f,.18f,.65f,1.15f)
        .addBox(1.89f,-.7f,-1.9f,.18f,.65f,1.15f),PartPose.ZERO);
    var j=part(h,"jaw",-1.2f,0,-4.5f,2.4f,.9f,4,0,1,-.1f);
    if(!hart)for(int s:new int[]{-1,1})part(j,"fang_"+s,-.2f,-.8f,-.3f,.4f,1,.6f,s*.85f,0,-3.5f);
    for(int s:new int[]{-1,1}) {
      posed(h,"ear_"+s,-.65f,-2,-.45f,1.3f,2.4f,.9f,s*1.5f,-2,1,0,0,s*(hart?.9f:.18f));
      if(hart) {
        var antler=posed(h,"horn_"+s,-.4f,-4,-.4f,.8f,4,.8f,s*1.6f,-2,.3f,.25f,0,s*.25f);
        var crown=posed(antler,"crown",-.3f,-3.5f,-.3f,.6f,3.5f,.6f,0,-3.5f,0,-.3f,0,s*.28f);
        for(int t=0;t<2;t++)posed(crown,"tine_"+t,-.25f,-2,-.25f,.5f,2,.5f,0,-1.2f-t*1.5f,0,0,0,s*.95f);
      }
      if(ravager)part(h,"cheek_fur_"+s,s<0?-2.35f:1.85f,-1.4f,-1.4f,.5f,2.8f,2.6f,0,0,0);
    }
    for(int i=0;i<4;i++) {
      float length=hart?12:9;
      jointLeg(f,i,(i%2==0?-1:1)*2.2f,y+2,i<2?-2.3f:4,length,hart?1.2f:1.65f,
          i<2?-.08f:.32f,0,0,i<2?.08f:-.55f,hart);
    }
    var t=posed(f,"tail",-.7f,-.7f,0,1.4f,1.4f,hart?2:3,0,y-1,5, hart?-.25f:.25f,0,0);
    if(!hart)for(int n=0;n<3;n++)t=posed(t,"segment",-.6f,-.6f,0,1.2f-n*.2f,1.2f-n*.2f,2.5f,0,0,2.6f,-.13f,0,0);
    if(rime)for(int n=0;n<3;n++)posed(b,"ice_plate_"+n,-1.6f,-1,-.8f,3.2f,1.3f,1.6f,0,-2.4f,-1+n*2,0,0,n==1?.1f:-.1f);
    if(id.equals("dread_wolf"))for(int n=0;n<5;n++) {
      posed(b,"wire_mane_"+n,-2.3f,-.5f,-.45f,4.6f,1.3f,.9f,0,-3,-2+n*1.3f,-.18f,0,0);
    }
    if(hart)for(int s:new int[]{-1,1})part(b,"moss_"+s,s<0?-2.95f:2.45f,-1,0,.5f,2.8f,3.5f,0,0,0);
    if(id.equals("cinder_hound")) {
      for(int n=0;n<3;n++)part(b,"ember_ridge_"+n,-.45f,-.4f,-.5f,.9f,1.2f,1,0,-3.2f,-1+n*2);
      for(int s:new int[]{-1,1})part(b,"char_plate_"+s,s<0?-3.2f:2.3f,-2,-3,.9f,3.5f,2,0,0,0);
    }
  }
  private static void nativeWorm(PartDefinition f,String id) {
    boolean azure=id.equals("azure_serpent"),ivory=id.equals("ivory_burrower");
    var b=part(f,"body",-2.2f,-2,-2,4.4f,4,4,0,16,0);
    // Curved neck closes the gap between raised maw and grounded segmented body.
    part(b,"neck_low",-2,-5,-2,4,5,4,0,0,0);
    part(b,"neck_high",-1.9f,-9,-3,3.8f,5,3.8f,0,0,0);
    var h=part(f,"head",-2.7f,-2.5f,-3,5.4f,4.5f,4.5f,0,7,-1.3f);
    if(azure) {
      part(h,"snout",-1.8f,-.2f,-5,3.6f,2,2.5f,0,0,0);eye(h,.9f,-.7f,-3.15f,.7f);jaw(h,3.5f,4);
      for(int s:new int[]{-1,1})posed(h,"gill_fin_"+s,-.4f,-1.4f,-1,.8f,3,2,s*2.5f,0,1,0,s*.4f,s*.55f);
    } else {
      h=f.addOrReplaceChild("head",CubeListBuilder.create(),PartPose.offset(0,7,-1.3f));
      part(h,"maw_top",-2.8f,-2.5f,-2.5f,5.6f,1,4,0,0,0);
      part(h,"maw_side_left",-2.8f,-1.5f,-2.5f,1,3,4,0,0,0);
      part(h,"maw_side_right",1.8f,-1.5f,-2.5f,1,3,4,0,0,0);
      var j=part(h,"jaw",-2.8f,0,-4,5.6f,1,4,0,1.5f,1.5f);
      eye(h,.6f,-1.8f,-2.65f,.45f);
      for(int s:new int[]{-1,1}) {
        posed(h,ivory?"ivory_tusk_"+s:"mandible_"+s,-.4f,-.4f,-3,.8f,.8f,3,s*2.4f,.5f,-1.2f,0,-s*.3f,0);
        for(int n=0;n<2;n++)part(j,"tooth_"+s+"_"+n,-.25f,-.9f,-.3f,.5f,1,.6f,s*(.7f+n),0,-2);
      }
    }
    PartDefinition chain=b;
    for(int n=0;n<6;n++) {
      float w=4.4f-n*.4f;
      chain=posed(chain,"segment",-w/2,-w/2,0,w,w,3,0,n<2?1:0,2.7f,0,(n<3?.16f:-.24f),0);
      part(chain,"band_plate",-w/2-.12f,-w/2-.12f,.7f,w+.24f,.6f,1,0,0,0);
      if(!azure)for(int s:new int[]{-1,1})part(chain,"spine_"+s,-.3f,-.3f,-.4f,.6f,.6f,1.2f,s*(w/2+.3f),0,1.2f);
    }
    var t=part(f,"tail",-.65f,-.65f,0,1.3f,1.3f,3,0,21,2);
    if(azure)part(t,"fin",-1.5f,0,0,3,.4f,2.5f,0,0,2.4f);
    if(ivory)part(b,"ivory_keel",-.4f,-6,1.7f,.8f,5,.8f,0,0,0);
    if(id.equals("chain_worm")) {
      // Source creature combines a worm lower body, raised torso, grasping hands and wings.
      part(b,"torso",-2.7f,-10,-1.6f,5.4f,7,3.2f,0,0,0);
      for(int s:new int[]{-1,1}) {
        var a=posed(f,"arm_"+s,-.8f,0,-.8f,1.6f,5,1.6f,s*3,8,0,-.15f,0,-s*.12f);
        var e=posed(a,"elbow",-.85f,0,-.85f,1.7f,4.5f,1.7f,0,4.6f,0,-.4f,0,0);
        part(e,"hand",-1.2f,3.6f,-1.2f,2.4f,1.8f,2.4f,0,0,0);
        for(int n=0;n<3;n++)part(e,"finger_"+n,-.22f,0,-.2f,.44f,1.8f,.4f,-.8f+n*.8f,5,-.9f);
        var w=posed(f,"wing_"+s,s<0?-3.5f:0,-.35f,-.35f,3.5f,.7f,.7f,s*2.6f,9,1,0,s*.2f,s*.3f);
        var we=part(w,"elbow",s<0?-3:0,-.25f,-.25f,3,.5f,.5f,s*3.3f,0,0);
        part(we,"tip",s<0?-2:0,-.18f,-.18f,2,.36f,.36f,s*2.8f,0,0);
        part(w,"wing_membrane",s<0?-3.3f:0,0,-.18f,3.3f,4,.36f,0,0,0);
        part(we,"wing_membrane",s<0?-2.8f:0,0,-.15f,2.8f,3.5f,.3f,0,0,0);
      }
      part(h,"toothed_muzzle",-1.7f,-.8f,-4.8f,3.4f,1.3f,2.3f,0,0,0);
    }
  }
  private static void nativeFlyer(PartDefinition f,String id) {
    boolean moth=id.equals("aurora_moth");
    if(moth) {
      var b=part(f,"body",-1.2f,-1.2f,-2,2.4f,2.4f,6,0,13,0);
      part(b,"thorax_fur",-1.7f,-1.7f,-2.2f,3.4f,3.4f,2.4f,0,0,0);
      part(b,"abdomen_tip",-.8f,-.8f,3.5f,1.6f,1.6f,1.8f,0,0,0);
      var h=part(f,"head",-1.4f,-1.2f,-1.5f,2.8f,2.4f,2.5f,0,13,-2.5f);
      eye(h,.65f,-.2f,-1.65f,.65f);
      for(int s:new int[]{-1,1}) {
        var a=posed(h,"antenna_"+s,-.2f,-3,-.2f,.4f,3,.4f,s*.8f,-.8f,0,-.4f,0,s*.35f);
        part(a,"antenna_feather",-.55f,-2.6f,-.2f,1.1f,1.6f,.4f,0,0,0);
        var w=posed(f,"wing_"+s,s<0?-6:0,-.22f,-2.5f,6,.44f,6,s*1.5f,12.4f,0,0,s*-.08f,s*-.05f);
        part(w,"forewing_tip",s<0?-8.5f:5.5f,-.2f,-1.6f,3,.4f,4,0,0,0);
        var e=empty(w,"elbow");empty(e,"tip");
        posed(w,"hindwing",s<0?-5:0,-.18f,0,5,.36f,4,s*.3f,.15f,3,0,s*.12f,0);
      }
      for(int i=0;i<6;i++)radialLeg(f,i,(i%2==0?-1:1)*1.2f,14,-1+i/2*1.4f,4,0,.9f);
    } else {
      var b=part(f,"body",-2.7f,-1.5f,-2.1f,5.4f,7,4.2f,0,7,0);
      part(b,"neck",-1,-4,-1,2,4,2,0,0,-1);
      var h=part(f,"head",-1.8f,-1.8f,-2,3.6f,3.6f,3.5f,0,2,-1.8f);
      part(h,"beak",-1,-.1f,-4,2,1.4f,2.4f,0,0,0);eye(h,.7f,-.7f,-2.15f,.6f);jaw(h,2,3);
      for(int i=0;i<2;i++)jointLeg(f,i,(i==0?-1:1)*1.6f,12,1,10,1.1f,.18f,0,0,-.35f,false);
      for(int s:new int[]{-1,1}) {
        var w=posed(f,"wing_"+s,s<0?-4:0,-.45f,-.45f,4,.9f,.9f,s*2.5f,7,0,0,0,s*.2f);
        var e=posed(w,"elbow",s<0?-4:0,-.35f,-.35f,4,.7f,.7f,s*3.7f,0,0,0,s*.15f,0);
        var t=posed(e,"tip",s<0?-3:0,-.25f,-.25f,3,.5f,.5f,s*3.6f,0,0,0,s*.2f,0);
        for(int n=0;n<4;n++) {
          part(w,"feather_"+n,s<0?-3.6f+n*.85f:n*.85f,0,-.25f,.9f,4.5f-n*.3f,.5f,0,0,0);
          part(e,"feather_"+n,s<0?-3.6f+n*.85f:n*.85f,0,-.22f,.9f,6-n*.4f,.44f,0,0,0);
        }
        part(t,"feather_tip",s<0?-2.8f:0,0,-.2f,2.8f,4,.4f,0,0,0);
      }
      for(int n=0;n<6;n++) {
        int s=n%2==0?-1:1,row=n/2,idx=row==0?s:n;
        var a=posed(f,"arm_"+idx,-.55f,0,-.55f,1.1f,3.5f,1.1f,s*2.3f,7+row*2,-1,-.35f,0,-s*.5f);
        var e=posed(a,"elbow",-.5f,0,-.5f,1,3.5f,1,0,3.2f,0,-.5f,0,0);
        part(e,"hand",-.7f,3,-.8f,1.4f,1,1.6f,0,0,0);
        for(int c=0;c<2;c++)part(e,"claw_"+c,-.18f,0,-.2f,.36f,1.3f,.4f,-.4f+c*.8f,3.7f,-.5f);
      }
      part(b,"chest_feathers",-2.1f,-1.7f,-2.4f,4.2f,3,.6f,0,0,0);
      var t=part(f,"tail",-.55f,-.55f,0,1.1f,1.1f,4,0,13,2);part(t,"segment",-.4f,-.4f,0,.8f,.8f,3,0,0,3.5f);
    }
  }
  private static void nativeFlower(PartDefinition f,String id) {
    boolean briar=id.equals("briar_maw");
    var b=part(f,"body",-.8f,0,-.8f,1.6f,14,1.6f,0,10,0);
    part(b,"stem_knot",-1.3f,6,-1.3f,2.6f,3,2.6f,0,0,0);
    var h=empty(f,"head");
    h=f.addOrReplaceChild("head",CubeListBuilder.create(),PartPose.offset(0,7,0));
    part(h,"calyx",-2.6f,-1,-2.6f,5.2f,2,5.2f,0,0,0);
    eye(h,.65f,-1.4f,-1.45f,.65f);
    var maw=part(h,"jaw",-1.7f,-1,-1.7f,3.4f,2,3.4f,0,-.9f,0);
    for(int n=0;n<6;n++) {
      float a=n*Mth.PI/3;
      var p=posed(h,"petal_"+n,-1.3f,-4,-.4f,2.6f,4.5f,.8f,Mth.sin(a)*2.3f,0,Mth.cos(a)*2.3f,-.68f,a,0);
      part(p,"petal_tip",-.8f,-5.2f,-.3f,1.6f,1.5f,.6f,0,0,0);
      if(briar)for(int t=0;t<2;t++)part(p,"tooth_"+t,-.2f,-.7f,-.7f,.4f,1.2f,1,Mth.sin(t*2f)*.7f,-1.6f-t*1.5f,0);
    }
    for(int s:new int[]{-1,1}) {
      posed(b,"leaf_"+s,-1.2f,-.3f,-2.5f,2.4f,.6f,5,s*.6f,6,s*.4f,0,s*.8f,-s*.3f);
      var root=posed(f,"leg_"+(s<0?0:1),-.55f,0,-.55f,1.1f,4,1.1f,s*.8f,19,0,0,0,-s*.9f);
      part(root,"knee",-.4f,0,-.4f,.8f,3,.8f,0,3.7f,0);
      if(briar)posed(b,"briar_"+s,-.3f,0,-.3f,.6f,6,.6f,s*.6f,3,0,-.4f,0,-s*.55f);
    }
    for(int i=2;i<4;i++)posed(f,"leg_"+i,-.5f,0,-.5f,1,5,1,0,19,(i==2?-1:1)*.8f,(i==2?1:-1)*.9f,0,0);
  }
  private static void nativeMimic(PartDefinition f) {
    var b=part(f,"body",-4.5f,0,-3.8f,9,5.5f,7.6f,0,16,0);
    for(int s:new int[]{-1,1})part(b,"iron_band_"+s,-.45f,-.15f,-3.95f,.9f,5.8f,7.9f,s*2.8f,0,0);
    part(b,"latch_plate",-.7f,1,-4.05f,1.4f,2,.35f,0,0,0);
    // The lid rotates around its actual rear hinge; its mouth stays inside the chest.
    var h=part(f,"head",-4.5f,-2.5f,-7.6f,9,2.5f,7.6f,0,15.5f,3.8f);
    part(h,"lid_crown",-3.8f,-3.5f,-7,7.6f,1,6.4f,0,0,0);
    eye(h,1.1f,-1.2f,-7.77f,.65f);
    empty(h,"jaw");
    for(int n=0;n<6;n++) {
      part(h,"tooth_"+n,-.3f,0,-.5f,.6f,1.5f,1,-3.2f+n*1.3f,-.2f,-6.8f);
      part(b,"tooth_"+n,-.3f,-1.4f,-.5f,.6f,1.5f,1,-3.2f+n*1.3f,0,-3.1f);
    }
    var t=part(f,"tail",-.7f,-.3f,-3,1.4f,.6f,3,0,17.5f,-3);
    part(t,"segment",-.5f,-.25f,-3,1,.5f,3,0,0,-2.7f);
    for(int i=0;i<4;i++)jointLeg(f,i,(i%2==0?-1:1)*3.6f,20,i<2?-2.5f:2.5f,3,1.1f,-.25f,0,0,.4f,false);
  }
  private static void nativeTree(PartDefinition f) {
    var b=part(f,"body",-1.8f,0,-1.8f,3.6f,20,3.6f,0,4,0);
    part(b,"trunk_low",-2.4f,13,-2.4f,4.8f,7,4.8f,0,0,0);
    var h=empty(f,"head");
    h=f.addOrReplaceChild("head",CubeListBuilder.create(),PartPose.offset(0,8,-1.9f));
    // No cartoon face. The tiny bark seams are the existing renderer's emissive anchors.
    h.addOrReplaceChild("eyes",box(-.7f,-.1f,-.12f,.3f,.7f,.16f)
        .addBox(.4f,.2f,-.12f,.3f,.7f,.16f),PartPose.ZERO);
    for(int n=0;n<6;n++) {
      float a=n*Mth.PI/3;
      var branch=posed(f,"arm_"+n,-.6f,-6,-.6f,1.2f,6,1.2f,Mth.sin(a)*1.4f,6+n%2*2,Mth.cos(a)*1.4f,0,a,.7f);
      var tip=posed(branch,"elbow",-.45f,-4,-.45f,.9f,4,.9f,0,-5.5f,0,0,0,-.45f);
      part(tip,"foliage_mantle",-3,-1.8f,-3,6,2.8f,6,0,-1,0);
      part(tip,"foliage_cap",-2.1f,-3,-2.1f,4.2f,1.3f,4.2f,0,-1,0);
      var twig=part(tip,"twig",-.18f,0,-.18f,.36f,3,.36f,1.7f,.5f,-1.4f);
      part(twig,"fruit",-.65f,0,-.65f,1.3f,1.3f,1.3f,0,2.7f,0);
      var r=posed(f,"leg_"+n,-.55f,0,-.55f,1.1f,4,1.1f,Mth.sin(a)*1.6f,20,Mth.cos(a)*1.6f,0,a,.9f);
      posed(r,"knee",-.4f,0,-.4f,.8f,3,.8f,0,3.7f,0,0,0,-.5f);
    }
    part(b,"foliage_crown",-3.3f,-6,-3.3f,6.6f,3.2f,6.6f,0,0,0);
  }
  private static void nativeBoulder(PartDefinition f) {
    var b=part(f,"body",-4.2f,-4,-4.2f,8.4f,8,8.4f,0,18,0);
    part(b,"stone_top",-3.1f,-5.5f,-3.1f,6.2f,1.5f,6.2f,0,0,0);
    part(b,"stone_bottom",-3.2f,4,-3.2f,6.4f,1.8f,6.4f,0,0,0);
    for(int s:new int[]{-1,1})posed(b,"stone_facet_"+s,-.5f,-2,-2,1,4,4,s*4,0,0,0,0,s*.12f);
    var h=empty(f,"head");h=f.addOrReplaceChild("head",CubeListBuilder.create(),PartPose.offset(0,16,-4.3f));
    eye(h,.6f,0,-.1f,.5f);
  }
  private static void nativeMantis(PartDefinition f) {
    var b=posed(f,"body",-1.25f,0,-1.25f,2.5f,7,2.5f,0,6,0,.28f,0,0);
    part(b,"abdomen",-2,-1,0,4,3,7,0,6,1);
    var h=part(f,"head",-2.4f,-1.2f,-1.4f,4.8f,2.4f,2.8f,0,5,-1.5f);eye(h,1.35f,-.3f,-1.55f,.7f);jaw(h,1.8f,1.7f);
    for(int s:new int[]{-1,1}) {
      posed(h,"antenna_"+s,-.17f,-4,-.17f,.34f,4,.34f,s*1.1f,-1,0,-.4f,0,s*.35f);
      var a=posed(f,"arm_"+s,-.5f,0,-.5f,1,5,1,s*1.3f,7,-1,-.55f,0,-s*.18f);
      var e=posed(a,"elbow",-.6f,0,-.6f,1.2f,5.5f,1.2f,0,4.7f,0,-1.5f,0,0);
      part(e,"scythe",-.45f,-1.5f,-.9f,.9f,5,1.8f,0,4.5f,0);
      for(int t=0;t<3;t++)part(e,"claw_tooth_"+t,-.2f,-.3f,-.6f,.4f,.6f,1.2f,0,1+t*1.3f,-.6f);
    }
    for(int i=0;i<4;i++)radialLeg(f,i,(i%2==0?-1:1)*1.5f,13,i<2?1:5,10,(i<2?-.25f:.35f)*(i%2==0?-1:1),.7f);
  }
  private static void nativeReaver(PartDefinition f) {
    // Giant locust: segmented abdomen, folded wing blades and massive jumping femurs.
    var b=part(f,"body",-2,-2,-2.8f,4,4,5.6f,0,13,-1);
    var abdomen=part(b,"abdomen",-1.8f,-1.5f,2,3.6f,3,4,0,0,0);
    var a=part(abdomen,"segment",-1.4f,-1.2f,0,2.8f,2.4f,3,0,0,3.5f);
    part(a,"abdomen_tip",-1,-1,0,2,2,2,0,0,2.5f);
    var h=part(f,"head",-1.9f,-2,-1.7f,3.8f,4,3.4f,0,11.5f,-3.8f);
    eye(h,.9f,-.6f,-1.85f,.75f);jaw(h,2.2f,2);
    for(int s:new int[]{-1,1}) {
      posed(h,"antenna_"+s,-.2f,-4,-.2f,.4f,4,.4f,s*1,-1,0,-.5f,0,s*.16f);
      var hind=posed(f,"leg_"+(s<0?0:1),-.8f,0,-1.2f,1.6f,6,2.4f,s*1.8f,14,3,.4f,0,-s*.8f);
      var shin=posed(hind,"knee",-.45f,0,-.45f,.9f,8,.9f,0,5.6f,0,-.6f,0,s*.65f);
      part(shin,"foot",-.4f,0,-1.4f,.8f,.8f,2.4f,0,7.6f,0);
      for(int n=0;n<3;n++)part(shin,"spine_"+n,-.2f,-.3f,-.2f,.4f,.6f,.4f,s*.55f,2+n*1.7f,0);
      var w=posed(f,"wing_"+s,s<0?-1:0,-.25f,0,1,.5f,7,s*1.7f,11,0,0,-s*.08f,s*.08f);
      var e=part(w,"elbow",s<0?-1.2f:0,-.2f,0,1.2f,.4f,5,0,0,6.5f);
      part(e,"tip",s<0?-.8f:0,-.15f,0,.8f,.3f,2.5f,0,0,4.5f);
      part(w,"wing_plate",s<0?-1.7f:0,-.25f,.5f,1.7f,.5f,6,0,0,0);
    }
    for(int i=2;i<6;i++)radialLeg(f,i,(i%2==0?-1:1)*1.7f,14,i<4?-2:.5f,8,(i<4?-.4f:.3f)*(i%2==0?-1:1),.66f);
  }
  private static void nativeBoneMound(PartDefinition f) {
    // The Lord is an ossuary mound, not a humanoid skeleton king.
    var b=f.addOrReplaceChild("body",CubeListBuilder.create(),PartPose.offset(0,20,0));
    for(int n=0;n<6;n++) {
      float x=(n%3-1)*3.3f,z=n<3?-1.8f:2.4f,y=n%3==1?-4.5f:-1.8f;
      var cage=posed(b,"skeletal_cage_"+n,-.35f,-.35f,-2,.7f,.7f,4,x,y,z,n%2*.15f,(n-2)*.12f,0);
      for(int r=0;r<3;r++)for(int side:new int[]{-1,1}) {
        var rib=posed(cage,"skeletal_rib_"+side+"_"+r,side<0?-1.7f:0,-.3f,-.3f,1.7f,.6f,.6f,0,0,-1.3f+r*1.3f,0,0,side*.25f);
        posed(rib,"skeletal_curve",-.25f,0,-.25f,.5f,1.8f,.5f,side*1.5f,0,0,0,0,-side*.45f);
      }
      var skull=posed(b,"skull_"+n,-1,-1,-.9f,2,1.7f,1.8f,x,y-1.1f,z-2.2f,-.12f,(n-2)*.23f,(n%2==0?1:-1)*.12f);
      part(skull,"skeletal_jaw",-.8f,.7f,-1,1.6f,.6f,1.7f,0,0,0);
      for(int side:new int[]{-1,1})part(skull,"socket_"+side,-.25f,-.25f,-.2f,.5f,.5f,.4f,side*.5f,-.1f,-.9f);
    }
    for(int n=0;n<8;n++)posed(b,"skeletal_femur_"+n,-.35f,-.35f,-2.2f,.7f,.7f,4.4f,
        (n%4-1.5f)*2.4f,2.5f-(n%2)*.8f,(n<4?-2.5f:2.5f),.1f,n*.63f,0);
    var h=f.addOrReplaceChild("head",surface("skeletal_skull",-1.4f,-1.4f,-1.2f,2.8f,2.5f,2.4f),PartPose.offset(0,12.7f,-1.4f));
    eye(h,.3f,-.35f,-1.35f,.55f);
    part(h,"skeletal_jaw",-1.1f,.7f,-1.3f,2.2f,.8f,2.2f,0,0,0);
  }
  private static void nativeBiped(PartDefinition f,String id) {
    boolean saint=id.equals("stone_saint"),knight=id.equals("forsaken_knight"),doll=id.equals("sailor_doll");
    boolean colossus=id.equals("ashen_colossus"),maiden=id.equals("chalice_maiden");
    boolean watcher=id.equals("hollow_watcher"),wraith=id.equals("moonveil_wraith"),dune=id.equals("dune_revenant");
    boolean ghost=watcher||wraith||maiden;
    float width=colossus?8:saint?5.5f:maiden?4.4f:5;
    var b=part(f,"body",-width/2,0,-1.8f,width,8,3.6f,0,4,0);
    part(b,"neck",-.9f,-2,-.9f,1.8f,2.5f,1.8f,0,0,0);
    part(b,"waist",-width*.34f,7,-1.3f,width*.68f,2,2.6f,0,0,0);
    var h=part(f,"head",-2.3f,-4,-2.3f,4.6f,4.6f,4.6f,0,2,0);
    eye(h,.65f,-1.8f,-2.47f,.75f);
    if(saint||knight) {
      part(h,"helmet_cap",-2.6f,-4.4f,-2.6f,5.2f,.9f,5.2f,0,0,0);
      part(h,"helmet_brow",-2.5f,-2.8f,-2.65f,5,1,.6f,0,0,0);
      for(int s:new int[]{-1,1})part(h,"helmet_cheek_"+s,s<0?-2.55f:1.7f,-1.8f,-2.65f,.85f,2.5f,.6f,0,0,0);
      part(b,"breastplate",-width/2-.15f,.6f,-2.15f,width+.3f,5,.6f,0,0,0);
      part(b,"belt_plate",-width/2-.3f,6.5f,-2,width+.6f,1.2f,4,0,0,0);
      for(int s:new int[]{-1,1})posed(b,"hip_plate_"+s,-1.3f,0,-.3f,2.6f,3,.6f,s*1.45f,7.5f,-1.7f,-.08f,0,s*.12f);
      if(knight) {
        for(int side:new int[]{-1,1}) {
          var horn=posed(h,"horn_"+side,-.4f,-2.5f,-.5f,.8f,2.5f,1,side*2.3f,-2.6f,.7f,.2f,0,side*.6f);
          posed(horn,"horn_tip",-.25f,-1.8f,-.35f,.5f,1.8f,.7f,0,-2.2f,0,-.5f,0,-side*.6f);
        }
        part(b,"tattered_mantle",-2.3f,0,0,4.6f,10,.45f,0,1,2.1f);
      }
    }
    if(doll) {
      part(h,"sailor_hat_brim",-2.8f,-4.6f,-2.8f,5.6f,.7f,5.6f,0,0,0);
      part(h,"sailor_hat_cap",-2.2f,-5.5f,-2.2f,4.4f,1.3f,4.4f,0,0,0);
      part(b,"sailor_collar",-2.8f,-.15f,-2,5.6f,1.2f,4,0,0,0);
      part(b,"sailor_tie",-.5f,1,-2.05f,1,2.5f,.4f,0,0,0);
    }
    if(dune)for(int n=0;n<4;n++)part(b,"bandage_"+n,-2.6f,1+n*1.6f,-1.95f,5.2f,.7f,3.9f,0,0,0);
    if(colossus) {
      part(b,"basalt_breastplate",-3.2f,1,-2.15f,6.4f,5,.7f,0,0,0);
      for(int s:new int[]{-1,1})posed(b,"basalt_shoulder_plate_"+s,-1.5f,-1.5f,-2,3,3,4,s*3.8f,.5f,0,0,0,s*.18f);
      part(h,"basalt_brow",-2.5f,-2.9f,-2.6f,5,.9f,.5f,0,0,0);
    }
    for(int s:new int[]{-1,1}) {
      float w=colossus?2.8f:1.5f;
      var a=posed(f,"arm_"+s,-w/2,0,-w/2,w,5,w,s*(width/2+.6f),4.5f,0,0,0,-s*.06f);
      var e=posed(a,"elbow",-w*.45f,0,-w*.45f,w*.9f,5,w*.9f,0,4.6f,0,-.08f,0,0);
      part(e,"hand",-w*.55f,4.2f,-w*.6f,w*1.1f,1.6f,w*1.2f,0,0,0);
      if(saint||knight) {
        part(a,"pauldron",-1.25f,-.5f,-1.4f,2.5f,2.2f,2.8f,0,0,0);
        part(e,"gauntlet_plate",-.95f,3.4f,-1.05f,1.9f,2.5f,2.1f,0,0,0);
        if(s<0) {
          var sword=part(e,"weapon_hilt",-.25f,-.5f,-.25f,.5f,2,.5f,0,5,-1);
          part(sword,"weapon_guard",-1.4f,-.4f,-.35f,2.8f,.6f,.7f,0,1,0);
          part(sword,"weapon_blade",-.45f,0,-.12f,.9f,8,.24f,0,1.3f,0);
          part(sword,"weapon_tip",-.25f,0,-.1f,.5f,1,.2f,0,9.2f,0);
        } else part(e,"shield_plate",-2.8f,-2,-.7f,5.6f,8.5f,1.4f,0,3,-1.5f);
      }
      if(!ghost)jointLeg(f,s<0?0:1,s*(colossus?2:1.4f),13,0,10,colossus?2.2f:1.65f,0,0,0,0,colossus||saint||knight);
    }
    if(maiden) {
      part(b,"skirt",-3,7,-2.2f,6,8,4.4f,0,0,0);
      for(int s:new int[]{-1,1})posed(b,"veil_"+(s<0?0:1),-1.5f,0,-.3f,3,8,.6f,s*1.5f,8,-2.3f,0,0,s*.04f);
      part(h,"veil_cap",-2.6f,-4.3f,-2.4f,5.2f,.6f,4.8f,0,0,0);
    }
    if(ghost) {
      if(watcher)h.addOrReplaceChild("eyes",box(-.8f,-2.7f,-2.48f,1.6f,1.6f,.18f),PartPose.ZERO);
      for(int n=0;n<5;n++)posed(b,"veil_"+n,-.45f,0,-.2f,.9f,9+(n%2)*2,.4f,-2.2f+n*1.1f,7.5f,(n%2==0?-1.8f:1.7f),0,0,(n-2)*.035f);
      if(wraith)part(h,"veil_hood",-2.7f,-4.4f,-2.1f,5.4f,.7f,4.8f,0,0,0);
      if(watcher) {
        part(h,"hood_top",-2.7f,-4.5f,-2.6f,5.4f,.6f,5.2f,0,0,0);
        for(int s:new int[]{-1,1})part(h,"hood_side_"+s,s<0?-2.7f:2.2f,-4,-2.5f,.5f,5.3f,5,0,0,0);
      }
    }
  }

  public static void animate(ModelPart f,String id,float swing,float stride,float age,float yaw,float pitch,float strike,boolean charging,boolean flying) {
    if(BeastRig.isForm(id)){BeastRig.animate(f,id,swing,stride,age,yaw,pitch,strike,charging,flying);return;}
    if (!java.util.Set.of("dragon","wolf","leviathan","colossus","stag","griffin","wyvern").contains(id)) {
      CreatureMotion.animate(f,id,swing,stride,age,yaw,pitch,strike,charging,flying);
      return;
    }
    float hit=Mth.sin(Mth.clamp(strike,0,1)*Mth.PI);
    var head=f.getChild("head");
    head.yRot=Mth.clamp(yaw,-38,38)*Mth.DEG_TO_RAD;
    head.xRot=Mth.clamp(pitch,-24,24)*Mth.DEG_TO_RAD-.12f*hit;
    head.getChild("eyes").visible=false;
    f.getChild("runes").visible=false;
    if(head.hasChild("jaw"))head.getChild("jaw").xRot=(charging?.4f:hit*.6f)+.025f*Mth.sin(age*.08f);
    var b=f.getChild("body");
    b.y+=Mth.sin(age*.07f)*.12f+Mth.sin(swing*1.2f)*stride*.16f;
    b.xRot=charging?.08f:0;
    for(int i=0;i<8;i++) {
      if(!f.hasChild("leg_"+i))continue;
      var l=f.getChild("leg_"+i);
      float phase=swing*.7f+(i%4==0||i%4==3?0:Mth.PI);
      l.xRot+=Mth.cos(phase)*stride*.5f;
      if(charging)l.xRot-=.2f;
      if(l.hasChild("knee"))l.getChild("knee").xRot=.16f+Math.max(0,-Mth.sin(phase))*stride*.45f;
    }
    for(int i=-1;i<6;i++) {
      if(!f.hasChild("arm_"+i))continue;
      var a=f.getChild("arm_"+i);
      a.visible=!id.equals("wyvern");
      a.xRot=charging?-1.05f:-hit*1.35f+Mth.cos(swing*.7f+i)*stride*.2f;
      a.zRot+=Mth.sin(age*.045f+i)*.045f;
      if(a.hasChild("elbow"))a.getChild("elbow").xRot=-.1f-hit*.55f;
      if(a.hasChild("elbow")&&a.getChild("elbow").hasChild("thumb"))a.getChild("elbow").getChild("thumb").yRot=(charging?.35f:hit*.6f);
    }
    for(int s:new int[]{-1,1})if(f.hasChild("wing_"+s)) {
      var w=f.getChild("wing_"+s);
      float flap=Mth.sin(age*(flying?.36f:.09f))*(flying?.5f:.09f);
      w.zRot=s*((flying?.25f:.15f)+flap);
      w.yRot=s*(flying?.15f:.6f);
      w.getChild("elbow").yRot=s*(flying?.12f:.55f);
      w.getChild("elbow").getChild("tip").yRot=s*(flying?.12f:.5f);
    }
    if(f.hasChild("tail"))wave(f.getChild("tail"),age*.07f,.18f);
    if(b.hasChild("segment"))wave(b.getChild("segment"),age*.08f,.14f);
    if(id.equals("mordant_mimic"))head.xRot-=charging?.65f:hit*.55f;
    if(id.equals("rolling_stone"))b.xRot+=swing*.35f;
    if(id.contains("wraith")||id.equals("chalice_maiden")||id.equals("hollow_watcher")) {
      f.y+=Mth.sin(age*.055f)*.65f;
      for(int i=0;i<5;i++)if(b.hasChild("veil_"+i))b.getChild("veil_"+i).xRot=Mth.sin(age*.08f+i*.6f)*.09f;
    }
    for(int i=0;i<6;i++)if(head.hasChild("petal_"+i))head.getChild("petal_"+i).xRot+=Mth.sin(age*.07f+i)*.05f+(charging?.5f:hit*.3f);
  }
  private static void wave(ModelPart p,float phase,float strength) {
    p.yRot=Mth.sin(phase)*strength;
    if(p.hasChild("segment"))wave(p.getChild("segment"),phase-.55f,strength*1.08f);
  }
}
