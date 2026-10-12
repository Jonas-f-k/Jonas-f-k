package com.shadowslave.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Player transformations have their own anatomy rather than reusing creature-family rigs.
 * Front is negative Z; feet are at Y=24. Four explicit material bands share a 64x128 skin.
 */
public final class BeastRig {
  public static final String[] IDS={"dragon","wolf","leviathan","colossus","stag","griffin","wyvern"};
  private static final int SKIN=0, JOINT=1, PLATE=2, TIP=3;
  private static final int[] SIGNS={-1,1};
  private BeastRig() {}
  public static boolean isForm(String id) {
    if(id==null)return false;
    return switch(id) {
      case "dragon","wolf","leviathan","colossus","stag","griffin","wyvern" -> true;
      default -> false;
    };
  }

  private static CubeListBuilder cube(String name,int material,float x,float y,float z,
      float w,float h,float d) {
    int spanX=(int)Math.ceil(2*(w+d)),spanY=(int)Math.ceil(h+d);
    int hash=name.hashCode();
    int u=Math.floorMod(hash,Math.max(1,65-spanX));
    int v=material*32+Math.floorMod(hash/31,Math.max(1,33-spanY));
    return CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,w,h,d);
  }
  private static PartDefinition box(PartDefinition p,String name,int material,
      float x,float y,float z,float w,float h,float d,float px,float py,float pz) {
    return p.addOrReplaceChild(name,cube(name,material,x,y,z,w,h,d),PartPose.offset(px,py,pz));
  }
  private static PartDefinition tilted(PartDefinition p,String name,int material,
      float x,float y,float z,float w,float h,float d,float px,float py,float pz,
      float rx,float ry,float rz) {
    return p.addOrReplaceChild(name,cube(name,material,x,y,z,w,h,d),
        PartPose.offsetAndRotation(px,py,pz,rx,ry,rz));
  }
  private static PartDefinition empty(PartDefinition p,String name) {
    return p.addOrReplaceChild(name,CubeListBuilder.create(),PartPose.ZERO);
  }
  private static void eyes(PartDefinition head,float halfGap,float y,float z,float width) {
    head.addOrReplaceChild("eyes",cube("eyes",TIP,-halfGap-width,y,z,width,.55f,.16f)
        .addBox(halfGap,y,z,width,.55f,.16f),PartPose.ZERO);
  }
  private static void rune(PartDefinition parent,String name,float x,float y,float z,
      float width,float height) {
    box(parent,name,TIP,-width/2,-height/2,0,width,height,.12f,x,y,z);
  }
  private static void jaw(PartDefinition head,float width,float length,float hingeY,float hingeZ,
      boolean fangs) {
    var jaw=box(head,"jaw",JOINT,-width/2,0,-length,width,1.15f,length,0,hingeY,hingeZ);
    if(fangs)for(int s:SIGNS)box(jaw,"fang_"+s,TIP,-.28f,-1.15f,-.35f,.56f,1.3f,.7f,
        s*(width/2-.6f),0,-length+.6f);
  }

  /** A connected shoulder/thigh, articulated shin and weight-bearing paw, never a dangling spike. */
  private static void paw(PartDefinition root,int index,float x,float y,float z,
      float upper,float lower,float width,boolean digitigrade,boolean hoof,boolean talon) {
    var leg=tilted(root,"leg_"+index,SKIN,-width/2,-.4f,-width/2,width,upper+.7f,width,
        x,y,z,digitigrade?-.23f:0,0,0);
    var knee=tilted(leg,"knee",JOINT,-width*.39f,-.35f,-width*.4f,width*.78f,lower+.6f,
        width*.8f,0,upper,0,digitigrade?.43f:0,0,0);
    float footWidth=hoof?width*.85f:width*1.35f;
    var foot=tilted(knee,"foot",hoof?TIP:SKIN,-footWidth/2,-.3f,-2.15f,footWidth,1.7f,3.1f,
        0,lower,0,digitigrade?-.20f:0,0,0);
    if(!hoof)for(int c=0;c<3;c++)box(foot,"claw_"+c,TIP,-.25f,0,-1.3f,.5f,
        talon?1.35f:.6f,talon?1.7f:1.2f,(c-1)*footWidth*.3f,.45f,-1.7f);
    else box(foot,"hoof_split",JOINT,-.14f,-.25f,-2.2f,.28f,1.75f,.15f,0,0,0);
  }
  private static PartDefinition tail(PartDefinition root,int segments,float y,float z,
      float width,float length,float rise) {
    PartDefinition current=box(root,"tail",SKIN,-width/2,-width/2,-.6f,width,width,length,
        0,y,z);
    for(int i=1;i<segments;i++) {
      float taper=1-i/(float)(segments+1),next=width*taper;
      current=tilted(current,"segment",SKIN,-next/2,-next/2,-.65f,next,next,length*.9f,
          0,rise,length-.7f,0,0,0);
    }
    return current;
  }
  private static void sweptHorn(PartDefinition head,int s,float x,float y,float z) {
    var h=tilted(head,"horn_"+s,TIP,-.65f,-3.1f,-.55f,1.3f,3.6f,1.1f,s*x,y,z,
        -.62f,0,s*.18f);
    box(h,"horn_tip",TIP,-.38f,-2,-.35f,.76f,2.3f,.7f,0,-2.8f,0);
  }

  /** Broad bat membrane panels sit between three ribs; folding happens at shoulder and wrist. */
  private static void batWing(PartDefinition root,int s,float y,float z,boolean wyvern) {
    var wing=box(root,"wing_"+s,SKIN,s<0?-4.3f:0,-.65f,-.65f,4.3f,1.3f,1.3f,
        s*3,y,z);
    var elbow=box(wing,"elbow",JOINT,s<0?-5.4f:0,-.5f,-.55f,5.4f,1,1.1f,s*4,0,0);
    for(int n=0;n<3;n++) {
      float length=5.6f-n*1.0f;
      tilted(elbow,"finger_"+n,TIP,-.28f,-.35f,-.25f,.56f,.7f,length,
          s*(n*1.4f+.2f),0,.1f,0,s*(.28f+n*.16f),0);
    }
    // Overlap at the bones gives one readable sail rather than comb-like isolated bars.
    box(wing,"membrane_inner",JOINT,s<0?-4:0,-.12f,.1f,4,.24f,4.7f,0,0,0);
    box(elbow,"membrane_outer",JOINT,s<0?-4.8f:0,-.1f,.1f,4.8f,.2f,4.4f,0,0,0);
    box(elbow,"membrane_tip",JOINT,s<0?-6.2f:4.4f,-.1f,.2f,1.8f,.2f,2.8f,0,0,0);
    if(wyvern) {
      var hook=tilted(elbow,"wrist_claw",TIP,-.5f,-2,-.5f,1,2.4f,1,0,0,0,0,0,s*.32f);
      box(hook,"claw_tip",TIP,-.35f,-.55f,-1.25f,.7f,.75f,1.5f,0,-1.7f,0);
    }
  }

  public static PartDefinition build(PartDefinition parent,String name,String id,int family,int cores) {
    var f=empty(parent,name);
    switch(id) {
      case "dragon" -> dragon(f);
      case "wolf" -> wolf(f);
      case "leviathan" -> leviathan(f);
      case "colossus" -> colossus(f);
      case "stag" -> stag(f);
      case "griffin" -> griffin(f);
      case "wyvern" -> wyvern(f);
      default -> throw new IllegalArgumentException("Unknown transformation anatomy: "+id);
    }
    return f;
  }

  private static void dragon(PartDefinition f) {
    var body=box(f,"body",SKIN,-3.2f,-3,-5,6.4f,6,10,0,11,0);
    box(body,"breast",PLATE,-3.6f,-3.3f,-5.2f,7.2f,6.4f,4,0,0,0);
    box(body,"belly",JOINT,-2.35f,2,-4.8f,4.7f,1.4f,9.2f,0,0,0);
    for(int i=0;i<3;i++)box(body,"dorsal_plate_"+i,PLATE,-.8f,-4.25f,-1.3f,1.6f,1.8f,2.5f,
        0,0,-2+i*2.6f);
    var head=box(f,"head",SKIN,-2.55f,-1.5f,-3.9f,5.1f,3.6f,4.5f,0,7,-6.1f);
    tilted(head,"neck",SKIN,-2.2f,-.5f,-1,4.4f,6,3.2f,0,1.2f,2.1f,.30f,0,0);
    box(head,"muzzle",PLATE,-1.95f,-.45f,-6.4f,3.9f,2.1f,3,0,0,0);
    box(head,"brow",PLATE,-2.8f,-1.2f,-4.25f,5.6f,1.0f,1.8f,0,0,0);
    eyes(head,1.85f,.0f,-4.16f,.7f);jaw(head,3.8f,5.3f,1.7f,-.7f,true);
    for(int s:SIGNS)sweptHorn(head,s,2,-1.1f,-.3f);
    for(int i=0;i<4;i++)paw(f,i,(i%2==0?-1:1)*2.7f,13,i<2?-3.4f:3.7f,
        4.0f,4.1f,i<2?2.4f:2.8f,true,false,false);
    batWing(f,-1,8,-1.6f,false);batWing(f,1,8,-1.6f,false);
    var tip=tail(f,5,11.4f,4.6f,2.8f,3.4f,.12f);
    box(tip,"tail_spade",PLATE,-1.3f,-.5f,.9f,2.6f,1,3.1f,0,0,0);
    rune(body,"runes",0,-1.1f,-5.35f,1.1f,2.3f);
  }
  private static void wolf(PartDefinition f) {
    var body=box(f,"body",SKIN,-2.5f,-2.7f,-4.9f,5,5.4f,10.7f,0,11.4f,0);
    box(body,"shoulder_ruff",PLATE,-3.4f,-3.9f,-5.2f,6.8f,6.5f,4.6f,0,0,0);
    box(body,"mane_back",PLATE,-2.5f,-3.6f,-1.4f,5,1.5f,5.3f,0,0,0);
    var head=box(f,"head",SKIN,-2.2f,-2.5f,-3.3f,4.4f,4.3f,4.3f,0,9.0f,-5.1f);
    box(head,"neck_ruff",PLATE,-2.45f,-.7f,.1f,4.9f,5.3f,3,0,0,0);
    box(head,"muzzle",SKIN,-1.35f,-.2f,-7.0f,2.7f,1.8f,4.2f,0,0,0);
    box(head,"nose",JOINT,-1.15f,-.1f,-7.25f,2.3f,1.2f,.5f,0,0,0);
    eyes(head,1.35f,-1,-3.47f,.72f);jaw(head,2.5f,6,1.35f,-.65f,true);
    for(int s:SIGNS) {
      var ear=tilted(head,"ear_"+s,SKIN,-.8f,-2.4f,-.6f,1.6f,2.9f,1.2f,
          s*1.65f,-2.2f,.15f,0,0,-s*.14f);
      box(ear,"ear_inner",JOINT,-.42f,-1.8f,-.65f,.84f,1.65f,.16f,0,0,0);
    }
    for(int i=0;i<4;i++)paw(f,i,(i%2==0?-1:1)*2.3f,13.4f,i<2?-3.5f:4.1f,
        i<2?4.1f:3.6f,i<2?4.1f:4.6f,i<2?1.8f:2.5f,true,false,false);
    var tip=tail(f,3,11.1f,5.2f,2.5f,3.1f,.45f);
    box(tip,"tail_brush",PLATE,-1.4f,-1.2f,-.6f,2.8f,2.4f,3.4f,0,0,0);
    rune(body,"runes",0,-1,-5.28f,.8f,1.6f);
  }
  private static void leviathan(PartDefinition f) {
    var body=box(f,"body",SKIN,-3.4f,-2.8f,-4.2f,6.8f,5.6f,8.4f,0,15.4f,0);
    box(body,"belly",JOINT,-2.8f,1.8f,-4.3f,5.6f,1.3f,8.8f,0,0,0);
    var head=box(f,"head",SKIN,-3.1f,-2.35f,-4.1f,6.2f,4.7f,5.2f,0,13.4f,-5.0f);
    box(head,"snout",PLATE,-2.5f,-1.1f,-6.8f,5,2.9f,3.6f,0,0,0);
    box(head,"brow",PLATE,-3.3f,-2.2f,-4.35f,6.6f,1.15f,1.7f,0,0,0);
    eyes(head,2.5f,-.75f,-4.3f,.6f);jaw(head,4.8f,5.8f,1.75f,-.9f,true);
    for(int s:SIGNS) {
      var gill=tilted(head,"gill_"+s,JOINT,s<0?-1.8f:0,-2.4f,-.2f,1.8f,4.8f,1.7f,
          s*3,0,.5f,0,0,-s*.28f);
      box(gill,"gill_ridge",PLATE,-.35f,-2.6f,-.2f,.7f,5.2f,.6f,s*.75f,0,0);
      tilted(head,"crest_"+s,PLATE,-.7f,-1.5f,-.6f,1.4f,3,3.5f,s*2.2f,-2.2f,-1.1f,
          -.20f,0,-s*.22f);
      var flipper=tilted(f,"arm_"+s,SKIN,s<0?-4.8f:0,-.4f,-1.4f,4.8f,.8f,3.6f,
          s*3.1f,16,-1.7f,.15f,-s*.3f,s*.22f);
      box(flipper,"fin_tip",PLATE,s<0?-3.4f:0,-.35f,-.75f,3.4f,.7f,2.5f,s*4.1f,0,.8f);
    }
    PartDefinition t=body;
    for(int i=0;i<5;i++) {
      float w=5.9f-i*.85f,h=4.7f-i*.55f;
      t=box(t,i==0?"tail":"segment",SKIN,-w/2,-h/2,-.7f,w,h,5.1f,0,0,i==0?3.7f:4.3f);
      box(t,"dorsal_fin",PLATE,-.28f,-h/2-2.4f,-.1f,.56f,2.7f,4.6f,0,0,0);
    }
    for(int s:SIGNS)tilted(t,"tail_fluke_"+s,PLATE,s<0?-4.8f:0,-.45f,-.4f,4.8f,.9f,4.7f,
        0,0,3.8f,0,-s*.4f,0);
    rune(head,"runes",0,-2.48f,-2.2f,.9f,1.4f);
  }
  private static void colossus(PartDefinition f) {
    var body=box(f,"body",PLATE,-4,-6,-2.7f,8,10.3f,5.4f,0,9,0);
    box(body,"hips",SKIN,-3.2f,3.4f,-2.4f,6.4f,3.2f,4.8f,0,0,0);
    box(body,"breastplate",PLATE,-4.2f,-5.1f,-3.2f,8.4f,5.6f,1.2f,0,0,0);
    box(body,"waist_joint",JOINT,-3.2f,.65f,-2.95f,6.4f,1.5f,5.9f,0,0,0);
    box(body,"core_socket",JOINT,-1.65f,-3.8f,-3.42f,3.3f,3.3f,.65f,0,0,0);
    rune(body,"runes",0,-2.2f,-3.83f,1.35f,2.2f);
    var head=box(f,"head",PLATE,-2.35f,-2.6f,-2.25f,4.7f,4.9f,4.5f,0,.6f,-.1f);
    box(head,"neck",JOINT,-1.8f,1.75f,-1.4f,3.6f,2.3f,2.8f,0,0,0);
    box(head,"brow",PLATE,-2.6f,-1.2f,-2.8f,5.2f,1.2f,1.3f,0,0,0);
    box(head,"chin",PLATE,-1.75f,.3f,-2.55f,3.5f,1.5f,.8f,0,0,0);
    eyes(head,.35f,.05f,-2.47f,1.5f);
    for(int s:SIGNS) {
      var arm=box(f,"arm_"+s,SKIN,-1.65f,-.9f,-1.75f,3.3f,5.3f,3.5f,s*5.25f,4.8f,0);
      box(arm,"pauldron",PLATE,-2.05f,-1.6f,-2.15f,4.1f,3.3f,4.3f,0,0,0);
      var elbow=box(arm,"elbow",JOINT,-1.3f,-.55f,-1.5f,2.6f,5.8f,3,0,4.8f,0);
      box(elbow,"gauntlet",PLATE,-1.9f,.65f,-1.95f,3.8f,4.4f,3.9f,0,0,0);
      box(elbow,"hand",SKIN,-1.85f,4.7f,-2.15f,3.7f,2.8f,4.3f,0,0,0);
      box(elbow,"knuckle",PLATE,-1.65f,5,-2.75f,3.3f,1.25f,.9f,0,0,0);
    }
    for(int i=0;i<2;i++) {
      var leg=box(f,"leg_"+i,SKIN,-1.5f,-.6f,-1.5f,3,4.7f,3,(i==0?-1:1)*2,15,0);
      var knee=box(leg,"knee",JOINT,-1.25f,-.5f,-1.35f,2.5f,3.6f,2.7f,0,3.9f,0);
      box(knee,"greave",PLATE,-1.55f,.15f,-1.65f,3.1f,3.2f,3.3f,0,0,0);
      box(knee,"foot",PLATE,-1.8f,-.2f,-2.6f,3.6f,1.7f,4.5f,0,3.7f,0);
    }
  }
  private static void stag(PartDefinition f) {
    var body=box(f,"body",SKIN,-2.55f,-2.45f,-4.6f,5.1f,4.9f,9.4f,0,11.1f,0);
    box(body,"withers",PLATE,-2.25f,-3.15f,-4.3f,4.5f,1.5f,4.5f,0,0,0);
    var head=box(f,"head",SKIN,-1.8f,-2,-2.55f,3.6f,3.8f,3.5f,0,2.8f,-4.7f);
    tilted(head,"neck",SKIN,-1.6f,-.2f,-1.5f,3.2f,8.5f,3.4f,0,1.4f,1.4f,.16f,0,0);
    box(head,"muzzle",SKIN,-1.3f,-.15f,-4.5f,2.6f,1.8f,2.4f,0,0,0);
    eyes(head,.6f,-.65f,-2.73f,.95f);jaw(head,2.25f,3.7f,1.35f,-.45f,false);
    for(int s:SIGNS) {
      tilted(head,"ear_"+s,SKIN,-1.4f,-.4f,-.45f,2.8f,.9f,1.1f,s*2,-1.1f,.6f,
          0,-s*.3f,-s*.22f);
      var antler=tilted(head,"antler_"+s,PLATE,-.55f,-4.6f,-.5f,1.1f,4.9f,1,
          s*1.5f,-1.5f,.65f,-.15f,0,s*.28f);
      var crown=tilted(antler,"branch",PLATE,-.45f,-3.8f,-.4f,.9f,4.1f,.8f,
          0,-4.25f,0,-.2f,0,s*.35f);
      for(int t=0;t<2;t++) {
        var tine=tilted(t==0?antler:crown,"tine_"+t,TIP,-.35f,-2.4f,-.35f,.7f,2.7f,.7f,
            0,-1.5f-t*.35f,-.05f,.15f,0,t==0?-s*.65f:s*.5f);
        rune(tine,"runes",0,-1.8f,-.43f,.28f,.9f);
      }
    }
    for(int i=0;i<4;i++)paw(f,i,(i%2==0?-1:1)*1.9f,12.4f,i<2?-3.5f:3.5f,
        4.7f,4.9f,i<2?1.2f:1.65f,false,true,false);
    tilted(f,"tail",SKIN,-.7f,-.4f,-.2f,1.4f,2.9f,1.1f,0,10.8f,4.7f,.48f,0,0);
    rune(body,"runes",0,-.3f,-4.75f,.8f,1.6f);
  }
  private static void featherWing(PartDefinition f,int s) {
    var wing=box(f,"wing_"+s,SKIN,s<0?-4.1f:0,-.7f,-.8f,4.1f,1.4f,1.6f,s*2.8f,7.8f,-1.7f);
    box(wing,"wing_coverts",PLATE,s<0?-4.5f:0,-.4f,-.2f,4.5f,.8f,4.4f,0,0,0);
    var elbow=box(wing,"elbow",JOINT,s<0?-4.8f:0,-.45f,-.4f,4.8f,.9f,.8f,s*3.8f,0,0);
    box(elbow,"flight_coverts",PLATE,s<0?-4.8f:0,-.35f,.1f,4.8f,.7f,3.8f,0,0,0);
    for(int n=0;n<4;n++) {
      float xx=s*(.4f+n*1.15f);
      tilted(elbow,"feather_"+n,PLATE,-.85f,-.26f,0,1.7f,.52f,5.4f-n*.45f,
          xx,0,2.2f,0,s*(.08f+n*.09f),0);
    }
  }
  private static void griffin(PartDefinition f) {
    var body=box(f,"body",SKIN,-3,-2.7f,-4.8f,6,5.4f,10.1f,0,11.1f,0);
    box(body,"breast",PLATE,-3.4f,-3.5f,-5.15f,6.8f,6.5f,4.2f,0,0,0);
    var head=box(f,"head",PLATE,-2.2f,-2.5f,-3,4.4f,4.5f,4.0f,0,5.3f,-4.8f);
    box(head,"neck_ruff",PLATE,-2.3f,-.1f,-.3f,4.6f,6.5f,3.2f,0,0,0);
    var beak=box(head,"beak",TIP,-1.5f,-.55f,-5.3f,3,2,2.8f,0,0,0);
    box(beak,"hook",TIP,-1.1f,.8f,-5.3f,2.2f,1.55f,.9f,0,0,0);
    eyes(head,1.52f,-.85f,-3.16f,.6f);jaw(head,2.55f,3.5f,1.3f,-1.35f,false);
    for(int s:SIGNS)tilted(head,"crest_"+s,PLATE,-.65f,-2.5f,-.55f,1.3f,3,1.1f,
        s*1.65f,-2.0f,.8f,-.38f,0,-s*.2f);
    for(int i=0;i<4;i++)paw(f,i,(i%2==0?-1:1)*2.35f,13.2f,i<2?-3.4f:3.8f,
        i<2?3.7f:3.9f,i<2?4.5f:4.3f,i<2?1.5f:2.4f,true,false,i<2);
    featherWing(f,-1);featherWing(f,1);
    var end=tail(f,4,11.9f,4.9f,1.4f,2.4f,.3f);
    box(end,"lion_tuft",PLATE,-1.1f,-1.1f,-.4f,2.2f,2.2f,2.6f,0,0,0);
    rune(body,"runes",0,-.8f,-5.3f,1.1f,1.7f);
  }
  private static void wyvern(PartDefinition f) {
    var body=tilted(f,"body",SKIN,-2.8f,-3.5f,-4.4f,5.6f,7,8.8f,0,10.3f,0,.14f,0,0);
    box(body,"breast",PLATE,-3,-3.1f,-4.8f,6,5.5f,1.6f,0,0,0);
    box(body,"spinal_ridge",PLATE,-.65f,-4.35f,-3.2f,1.3f,1.8f,6.5f,0,0,0);
    var head=box(f,"head",SKIN,-2.2f,-2,-3.5f,4.4f,3.9f,4.5f,0,3.9f,-4.7f);
    tilted(head,"neck",SKIN,-1.7f,-.3f,-1.1f,3.4f,7.5f,3.1f,0,1.3f,1.1f,.25f,0,0);
    box(head,"snout",PLATE,-1.5f,-.3f,-6,3,2.1f,3,0,0,0);
    eyes(head,.7f,-.75f,-3.67f,1.05f);jaw(head,2.8f,5.2f,1.4f,-.65f,true);
    for(int s:SIGNS) {
      tilted(head,"jaw_fin_"+s,PLATE,s<0?-2.2f:0,-1.5f,-.35f,2.2f,3,.7f,
          s*2.1f,-.2f,.65f,0,-s*.38f,-s*.25f);
      sweptHorn(head,s,1.8f,-1.6f,.55f);
    }
    for(int i=0;i<2;i++)paw(f,i,(i==0?-1:1)*2.25f,13.2f,2.5f,4.1f,4.1f,
        2.7f,true,false,false);
    batWing(f,-1,7.5f,-2.1f,true);batWing(f,1,7.5f,-2.1f,true);
    var end=tail(f,5,11.5f,4.2f,2.5f,3.6f,.25f);
    tilted(end,"tail_blade",TIP,-.45f,-2.0f,-.1f,.9f,3.5f,4.1f,0,0,1.5f,-.22f,0,0);
    rune(body,"runes",0,-1.0f,-4.94f,.9f,1.8f);
  }

  private static float finite(float value) { return Float.isFinite(value)?value:0; }
  private static void legs(ModelPart f,int count,float phase,float amount,float attack,
      boolean flying,boolean tall) {
    for(int i=0;i<count;i++) {
      if(!f.hasChild("leg_"+i))continue;
      var leg=f.getChild("leg_"+i);
      float offset=count==4?((i==0||i==3)?0:Mth.PI):i*Mth.PI;
      float cycle=Mth.cos(phase+offset)*amount;
      leg.xRot+=(flying?.48f:cycle*(tall?.43f:.62f))-attack*(i<2?.22f:-.18f);
      var knee=leg.getChild("knee");
      knee.xRot+=(flying?.7f:Math.max(0,-cycle)*(tall?.48f:.82f));
      if(knee.hasChild("foot"))knee.getChild("foot").xRot-=(flying?.35f:cycle*.18f);
    }
  }
  private static void tailMotion(ModelPart p,float age,float amount,float attack,int index) {
    for(String child:new String[]{"tail","segment"})if(p.hasChild(child)) {
      var t=p.getChild(child);
      t.yRot+=Mth.sin(age*.055f-index*.5f)*(.075f+amount*.055f);
      t.xRot+=Mth.cos(age*.041f-index*.4f)*.045f-attack*.045f;
      tailMotion(t,age,amount,attack,index+1);
    }
  }
  private static void wings(ModelPart f,float age,float amount,boolean flying,boolean bird,
      float attack) {
    for(int s:SIGNS)if(f.hasChild("wing_"+s)) {
      var wing=f.getChild("wing_"+s);var wrist=wing.getChild("elbow");
      if(flying) {
        float flap=Mth.sin(age*(bird?.38f:.29f));
        wing.zRot=s*(.12f+flap*(bird?.63f:.51f));
        wing.yRot=-s*.08f;wing.xRot=.08f;
        wrist.zRot=s*(.06f+Mth.sin(age*(bird?.38f:.29f)-.7f)*.17f);
        wrist.yRot=-s*.17f;
      } else {
        // Fold the wrist back against the body. A slow breath moves the whole folded sail.
        wing.yRot=-s*(bird?.55f:.58f);
        wing.zRot=s*(bird?.65f:.42f)+s*Mth.sin(age*.06f)*.022f;
        wing.xRot=-.09f;
        wrist.yRot=-s*(bird?1.98f:2.18f);
        wrist.zRot=s*.17f;
      }
      wing.zRot-=s*attack*.36f;wing.yRot+=s*attack*.28f;
      for(int n=0;n<4;n++)if(wrist.hasChild("feather_"+n))
        wrist.getChild("feather_"+n).yRot+=s*Mth.sin(age*.075f-n*.24f)*.018f;
    }
  }

  /**
   * A state-driven pose: stride, idle respiration, anticipation, impact and recovery are
   * layered on the bind pose. Inputs are bounded, and every call starts from that bind pose.
   */
  public static void animate(ModelPart f,String id,float swing,float stride,float age,float yaw,
      float pitch,float strike,boolean charging,boolean flying) {
    f.getAllParts().forEach(ModelPart::resetPose);
    float amount=Mth.clamp(finite(stride),0,1),time=finite(age)%24000;
    float phase=(finite(swing)%4096)*.68f,hit=Mth.clamp(finite(strike),0,1);
    float impact=Mth.sin(hit*Mth.PI),anticipation=hit>0&&hit<.28f?
        Mth.sin(hit/.28f*Mth.PI):0;
    var body=f.getChild("body");var head=f.getChild("head");
    head.yRot=Mth.clamp(finite(yaw),-58,58)*Mth.DEG_TO_RAD;
    head.xRot=Mth.clamp(finite(pitch),-32,32)*Mth.DEG_TO_RAD;
    float breath=Mth.sin(time*.067f)*.035f;
    body.yScale=1+breath*.14f;
    f.y+=Mth.sin(phase*2)*amount*.18f;
    head.xRot+=breath*.7f+anticipation*.16f-impact*.22f;
    if(head.hasChild("jaw"))head.getChild("jaw").xRot=
        (charging?.64f:impact*.55f)+Math.max(0,breath)*.3f;
    // Emissive pieces are rendered separately by BeastModel; never paint white geometry in base pass.
    f.getAllParts().forEach(p->{if(p.hasChild("eyes"))p.getChild("eyes").visible=false;
      if(p.hasChild("runes"))p.getChild("runes").visible=false;});

    switch(id) {
      case "dragon" -> {
        legs(f,4,phase,amount,impact,flying,false);
        wings(f,time,amount,flying,false,impact);
        body.xRot+=(flying?-.16f:0)+(charging?-.1f:0)-impact*.08f;
        head.xRot+=charging?-.14f:0;head.z-=charging?.8f:impact*.65f;
        tailMotion(f,time,amount,impact,0);
      }
      case "wolf" -> {
        legs(f,4,phase*1.15f,amount,impact,false,false);
        body.xRot+=Mth.cos(phase*2)*amount*.04f-impact*.12f;
        f.y-=impact*.65f;head.z-=impact*.9f;
        for(int s:SIGNS)head.getChild("ear_"+s).xRot-=impact*.38f+(charging?.22f:0);
        tailMotion(f,time,amount,impact,0);
      }
      case "leviathan" -> {
        f.y+=Mth.sin(time*.09f)*.28f;
        body.yRot+=Mth.sin(time*.085f)*(.06f+amount*.07f);
        tailMotion(body,time*1.8f,amount+1,impact,0);
        for(int s:SIGNS) {
          var fin=f.getChild("arm_"+s);
          fin.zRot+=s*Mth.sin(time*.10f+s*.3f)*.18f;
          fin.yRot+=s*Mth.sin(time*.085f)*.12f;
          head.getChild("gill_"+s).zRot+=s*(charging?.28f:impact*.18f);
        }
        head.z-=impact*1.1f;head.xRot+=charging?-.1f:0;
      }
      case "colossus" -> {
        legs(f,2,phase*.75f,amount,impact,false,true);
        body.zRot+=Mth.sin(phase*.75f)*amount*.045f;
        body.xRot+=anticipation*.12f-impact*.10f;
        for(int s:SIGNS) {
          var arm=f.getChild("arm_"+s);
          arm.xRot+=Mth.cos(phase*.75f+(s<0?0:Mth.PI))*amount*.27f;
          arm.xRot-=anticipation*.55f+impact*.35f;
          arm.getChild("elbow").xRot-=impact*.4f;
        }
        f.y+=impact*.35f;
      }
      case "stag" -> {
        legs(f,4,phase*1.05f,amount,impact,false,true);
        body.xRot+=Mth.cos(phase*2)*amount*.025f;
        head.xRot+=(charging?-.3f:0)-impact*.25f;
        for(int s:SIGNS)head.getChild("ear_"+s).yRot+=s*Mth.sin(time*.041f)*.05f;
        f.getChild("tail").yRot+=Mth.sin(time*.075f)*.08f;
      }
      case "griffin" -> {
        legs(f,4,phase,amount,impact,flying,false);
        wings(f,time,amount,flying,true,impact);
        body.xRot+=flying?-.12f:Mth.cos(phase*2)*amount*.035f;
        head.z-=impact*.6f;tailMotion(f,time,amount,impact,0);
      }
      case "wyvern" -> {
        legs(f,2,phase,amount,impact,flying,false);
        wings(f,time,amount,flying,false,impact);
        body.xRot+=flying?-.23f:Mth.cos(phase*2)*amount*.05f;
        head.xRot+=charging?-.19f:0;
        head.z-=impact*.7f;tailMotion(f,time,amount,impact,0);
      }
      default -> throw new IllegalArgumentException("Unknown transformation anatomy: "+id);
    }
  }
}
