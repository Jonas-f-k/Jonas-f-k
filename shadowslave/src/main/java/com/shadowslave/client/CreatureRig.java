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
        || name.equals("jaw") || name.equals("segment") || name.equals("tail")) band = 1;
    if (name.contains("plate") || name.contains("carapace") || name.contains("pauldron")
        || name.contains("shield") || name.contains("ridge") || name.contains("band")
        || name.contains("mane") || name.contains("feather") || name.contains("petal")
        || name.contains("moss") || name.contains("veil") || name.equals("cap")
        || name.equals("base") || name.contains("mantle") || name.equals("fin")) band = 2;
    if (name.contains("horn") || name.contains("fang") || name.contains("claw")
        || name.contains("tusk") || name.contains("tooth") || name.contains("weapon")
        || name.contains("spine") || name.contains("crown") || name.contains("tine")
        || name.contains("beak") || name.contains("shard") || name.contains("prism")
        || name.equals("tip") || name.equals("point") || name.contains("skeletal")) band = 3;
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
    var f=empty(parent,name);
    switch(id) {
      case "carapace_scavenger","carapace_centurion","carapace_demon" -> crab(f,cores);
      case "iron_spider","iron_spider_matriarch","glass_huntsman" -> spider(f,id,cores);
      case "black_ravager","dread_wolf","rime_stalker","moss_hart","cinder_hound","wolf","stag" -> quadruped(f,id);
      case "chain_worm","ivory_burrower","azure_serpent","leviathan" -> serpent(f,id);
      case "spire_messenger","aurora_moth","griffin" -> flyer(f,id);
      case "blood_flower","briar_maw" -> flower(f,id);
      case "mordant_mimic" -> mimic(f);
      case "soul_devouring_tree" -> tree(f);
      case "rolling_stone" -> boulder(f);
      case "gilded_mantis","flesh_reaver" -> reaver(f,id);
      case "dragon","wyvern" -> dragon(f,id);
      default -> humanoid(f,id,family,cores);
    }
    // Every model keeps a readable center; emissive geometry has its own render pass.
    part(f,"runes",-1,6,-3.7f,2,2,.16f,0,0,0);
    return f;
  }

  private static void crab(PartDefinition f,int cores) {
    var b=part(f,"body",-4.8f,-2,-4.5f,9.6f,cores==1?6:8,9,0,cores==1?10:8,0);
    part(b,"carapace",-4,-4,-3.5f,8,cores==1?2:3,7,0,0,0);
    if(cores==2)part(b,"centurion_crest",-1,-7,-3,2,4,6,0,0,0);
    if(cores>=3) {
      part(b,"demon_spine",-.7f,-7,-2,1.4f,5,6,0,0,0);
      for(int sign:new int[]{-1,1})part(b,"demon_flange_"+sign,sign<0?-6:4,-3,1,2,4,5,0,0,0);
    }
    for(int s:new int[]{-1,1})for(int n=0;n<cores;n++)part(b,"plate_"+s+"_"+n,s<0?-5.3f:4.2f,-2+n*1.2f,-2.8f,1.1f,1.5f,5.6f,0,0,0);
    var head=part(f,"head",-2.7f,-2,-3,5.4f,4,4,0,11,-4);
    eye(head,.8f,-.6f,-3.18f,1.3f);jaw(head,4,3);
    for(int i=0;i<6;i++)limb(f,i,(i%2==0?-1:1)*4,11,-3+(i/2)*3,10,true,false);
    for(int s:new int[]{-1,1}) {
      var claw=part(f,"arm_"+s,-1,-1,-1,2,3,2,s*4,10,-4);
      var fore=part(claw,"elbow",-1.5f,-1,-4,3,3,4,0,2,-1);
      part(fore,"pincer",-.8f,0,-4,1.6f,2,3,0,0,-2);
      part(fore,"thumb",-1.2f,-1,-3,1.4f,2.4f,3,1.5f,0,-1);
    }
    if(cores>=3)for(int s:new int[]{-1,1})horn(head,s,false,false);
  }
  private static void spider(PartDefinition f,String id,int cores) {
    boolean glass=id.equals("glass_huntsman");
    var b=part(f,"body",-3,-2,-2,6,4,4,0,11,-1);
    part(b,"abdomen",cores>=5?-4.6f:-3.5f,cores>=5?-5:-3,1,cores>=5?9.2f:7,cores>=5?8:6,cores>=5?8:6,0,0,0);
    if(cores>=5)part(b,"brood",-2.5f,-4,3,5,2,4,0,0,0);
    var head=part(f,"head",-2,-1.6f,-2.5f,4,3.2f,3,0,12,-3);
    eye(head,.2f,-.6f,-2.7f,.8f);jaw(head,3.2f,2);
    for(int i=0;i<8;i++)limb(f,i,(i%2==0?-1:1)*2.8f,11,-3+(i/2)*1.8f,11.5f,true,false);
    for(int s:new int[]{-1,1})part(b,"ridge_"+s,s<0?-3.6f:2.8f,-3,2,.8f,2,4,0,0,0);
    if(glass)for(int i=0;i<4;i++)part(b,"prism_"+i,-2+i*1.2f,-5,3,1,3,1,0,0,0);
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
  private static void flower(PartDefinition f,String id) {
    var b=part(f,"body",-1,-1,-1,2,18,2,0,6,0);
    var h=part(f,"head",-2,-1.5f,-2,4,3,4,0,-2,0);
    eye(h,.4f,0,-2.2f,.8f);jaw(h,3,2);
    for(int i=0;i<6;i++) {
      float a=i*Mth.PI/3;
      var petal=part(h,"petal_"+i,-1.4f,-4,-.5f,2.8f,5,1,(float)Math.sin(a)*2,0,(float)Math.cos(a)*2);
      petal= h.addOrReplaceChild("petal_"+i,surface("petal_"+i,-1.4f,-4,-.5f,2.8f,5,1),PartPose.offsetAndRotation((float)Math.sin(a)*2,0,(float)Math.cos(a)*2,-.6f,a,0));
      for(int t=0;t<2;t++)part(petal,"thorn_"+t,-.3f,-3+t*2,-1,.6f,1,1,0,0,0);
    }
    for(int i=0;i<4;i++)limb(f,i,(i%2==0?-1:1)*2,15,i<2?-2:2,7,true,false);
    if(id.equals("briar_maw"))for(int i=0;i<3;i++)part(b,"briar_"+i,-3,4+i*4,-.5f,6,1,1,0,0,0);
  }
  private static void mimic(PartDefinition f) {
    part(f,"body",-5,0,-4.5f,10,8,9,0,12,0);
    var h=f.addOrReplaceChild("head",box(-5,-6,-9,10,6,9),PartPose.offset(0,10,4.5f));
    eye(h,1,-3,-9.2f,1.4f);
    part(h,"jaw",-4,0,-8.5f,8,1,8,0,0,0);
    for(int i=0;i<6;i++)part(h,"tooth_"+i,-3.5f+i*1.3f,-.3f,-9,.7f,2,1,0,0,0);
    var tongue=part(f,"tail",-1,-.5f,-5,2,1,5,0,13,-3);
    part(tongue,"segment",-.7f,0,-3,1.4f,1,3,0,0,-4);
    for(int i=0;i<4;i++)limb(f,i,(i%2==0?-1:1)*4,19,i<2?-3:3,4,false,false);
  }
  private static void tree(PartDefinition f) {
    var b=part(f,"body",-2.6f,0,-2.6f,5.2f,22,5.2f,0,2,0);
    var h=part(f,"head",-2.5f,-2,-3,5,7,3,0,7,-1);
    eye(h,.7f,0,-3.2f,1.2f);jaw(h,3.5f,1.4f);
    for(int i=0;i<6;i++) {
      float a=i*Mth.PI/3;
      var branch=part(f,"arm_"+i,-.6f,-8,-.6f,1.2f,8,1.2f,(float)Math.sin(a)*2,5+i%2*3,(float)Math.cos(a)*2);
      part(branch,"elbow",-2.7f,-4,-2.7f,5.4f,3,5.4f,(float)Math.sin(a)*3,-6,(float)Math.cos(a)*3);
      var twig=part(branch,"twig",-.4f,0,-.4f,.8f,5,.8f,(float)Math.sin(a)*3,-5,(float)Math.cos(a)*3);
      part(twig,"fruit",-1,4,-1,2,2,2,0,0,0);
      limb(f,i,(float)Math.sin(a)*2,18,(float)Math.cos(a)*2,5,true,true);
    }
  }
  private static void boulder(PartDefinition f) {
    var b=part(f,"body",-4.5f,-5,-4.5f,9,10,9,0,14,0);
    part(b,"cap",-3.5f,-7,-3.5f,7,2,7,0,0,0);
    part(b,"base",-3.5f,5,-3.5f,7,2,7,0,0,0);
    var h=part(f,"head",-2.8f,-1,-1,5.6f,2,1,0,11,-4.3f);eye(h,.7f,0,-1.2f,1.1f);
    for(int s:new int[]{-1,1})part(b,"shard_"+s,s<0?-6:4,-2,-3,2,5,6,0,0,0);
  }
  private static void reaver(PartDefinition f,String id) {
    boolean mantis=id.equals("gilded_mantis");
    var b=part(f,"body",-2,-1,-2,4,10,4,0,3,0);
    part(b,"abdomen",-2,7,1,4,6,5,0,0,0);
    var h=part(f,"head",-2.8f,-2,-2,5.6f,4,4,0,0,-1);eye(h,.9f,0,-2.2f,1.2f);jaw(h,3,2);
    for(int i=0;i<4;i++)limb(f,i,(i%2==0?-1:1)*2,13,i<2?-1:3,9,true,false);
    for(int s:new int[]{-1,1}) {
      arm(f,s,false,false);
      var a=f.getChild("arm_"+s).getChild("elbow");
      part(a,"scythe",-.6f,-3,-5,1.2f,9,2,0,0,0);
      if(mantis)part(h,"antenna_"+s,-.3f,-5,-.3f,.6f,5,.6f,s*1.8f,-1,0);
      else for(int i=0;i<3;i++)part(b,"rib_"+s+"_"+i,s<0?-2.8f:1.8f,1+i*2,-2.3f,1,1,4.6f,0,0,0);
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

  public static void animate(ModelPart f,String id,float swing,float stride,float age,float yaw,float pitch,float strike,boolean charging,boolean flying) {
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
