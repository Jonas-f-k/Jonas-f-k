package com.shadowslave.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.shadowslave.combat.BeastForms;
import com.shadowslave.entity.CreatureEcology;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/** The actual animated Minecraft models, available from the Bestiary without spawning an enemy. */
public final class CreatureGalleryScreen extends Screen {
  private static final String[] MODES={"Idle","Walk","Windup","Strike","Flight"};
  private final Screen parent;
  private NightmareModel creatures;
  private BeastModel beasts;
  private int group,index,mode,ticks;
  private float yaw=-35;
  private boolean overview,rendered;
  private Bounds[] creatureBounds,beastBounds;

  public CreatureGalleryScreen(Screen parent,int group,int index){
    super(Component.literal("Dream Bestiary"));this.parent=parent;this.group=Math.clamp(group,0,2);this.index=Math.max(0,index);
  }
  public void setOverview(boolean value){overview=value;if(minecraft!=null)rebuild();}
  public void setAnimationMode(int value){mode=Math.clamp(value,0,4);}
  public boolean hasRendered(){return rendered;}
  private int count(){return group==0?CreatureEcology.ALL.size():7;}
  @Override protected void init(){
    if(creatures==null){
      creatures=new NightmareModel(minecraft.getEntityModels().bakeLayer(NightmareModel.LAYER));
      beasts=new BeastModel(minecraft.getEntityModels().bakeLayer(BeastModel.LAYER));
      creatureBounds=new Bounds[CreatureEcology.ALL.size()];beastBounds=new Bounds[7];
      for(int i=0;i<creatureBounds.length;i++){creatures.animate(i,CreatureEcology.ALL.get(i).family(),0,0,0,0,0,0,false);creatureBounds[i]=Bounds.measure(creatures.selectedRig());}
      for(int i=0;i<7;i++){beasts.animate(i,0,0,0,0,0,0,false,false);beastBounds[i]=Bounds.measure(beasts.selectedRig());}
    }
    rebuild();
  }
  private void rebuild(){
    clearWidgets();index=Math.floorMod(index,count());
    int buttonWidth=Math.min(112,(width-40)/5),x=12;
    String[] labels={"Creatures","Transformations","Magic"};
    for(int i=0;i<3;i++){int chosen=i;addRenderableWidget(SpellTheme.button(labels[i],x,34,buttonWidth,()->{group=chosen;index=0;rebuild();},group==i));x+=buttonWidth+4;}
    addRenderableWidget(SpellTheme.button(overview?"Single model":"Overview",x,34,buttonWidth,()->{overview=!overview;rebuild();},overview));
    addRenderableWidget(SpellTheme.button("Back",width-68,8,56,this::onClose,false));
    int y=height-28;
    addRenderableWidget(SpellTheme.button("‹",12,y,24,()->{index=Math.floorMod(index-1,count());},false));
    addRenderableWidget(SpellTheme.button("›",40,y,24,()->{index=(index+1)%count();},false));
    if(group<2){
      x=76;int mw=Math.min(70,Math.max(36,(width-88)/5));
      for(int i=0;i<MODES.length;i++){int m=i;addRenderableWidget(SpellTheme.button(MODES[i],x,y,mw,()->{mode=m;rebuild();},mode==i));x+=mw+3;}
    }else addRenderableWidget(SpellTheme.button("Cast / Impact",76,y,115,()->{mode=(mode+1)%2;},false));
  }
  @Override public void tick(){ticks++;}
  @Override public boolean isPauseScreen(){return false;}
  @Override public void onClose(){minecraft.setScreen(parent);}
  @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){
    if(button==0&&my>60&&my<height-35){yaw+=(float)dx*.7f;return true;}return super.mouseDragged(mx,my,button,dx,dy);
  }
  @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta){
    g.fillGradient(0,0,width,height,0xFF111B24,0xFF080D14);
    for(int i=0;i<34;i++){int x=(i*173+29)%Math.max(1,width),y=(i*97+47)%Math.max(1,height);g.fill(x,y,x+1,y+1,0xFF293D49);}
  }
  @Override public void render(GuiGraphics g,int mx,int my,float delta){
    super.render(g,mx,my,delta);
    g.drawString(font,"THE DREAM BESTIARY",12,12,SpellTheme.GOLD,false);
    if(overview)renderOverview(g,delta);else renderSingle(g,delta);
    rendered=true;
  }
  private String name(int i){return group==0?CreatureEcology.ALL.get(i).name():group==1?BeastForms.ALL.get(i).name():new String[]{"Flame","Shadow","Tide","Steel","Thorn","Mirror","Gravity"}[i];}
  private void renderOverview(GuiGraphics g,float delta){
    int columns=Math.max(1,Math.min(group==0?8:4,(width-24)/(group==0?140:220)));
    int shown=Math.min(count(),columns*Math.max(1,(height-96)/140));
    int start=index/shown*shown,rows=(Math.min(shown,count()-start)+columns-1)/columns;
    int cellW=(width-24)/columns,cellH=(height-96)/rows;
    for(int slot=0;slot<shown&&start+slot<count();slot++){
      int x=12+slot%columns*cellW,y=66+slot/columns*cellH;
      SpellTheme.panel(g,x+2,y+2,cellW-4,cellH-4,SpellTheme.EDGE);
    }
    g.flush();
    for(int slot=0;slot<shown&&start+slot<count();slot++){
      int i=start+slot,x=12+slot%columns*cellW,y=66+slot/columns*cellH;
      draw(g,i,x+cellW*.5f,y+(cellH-21)*.5f,cellW-18,cellH-28,delta);
      g.drawCenteredString(font,font.plainSubstrByWidth(name(i),cellW-12),x+cellW/2,y+cellH-17,SpellTheme.TEXT);
    }
  }
  private void renderSingle(GuiGraphics g,float delta){
    int x=12,y=66,cardW=width-24,cardH=height-108;
    SpellTheme.panel(g,x,y,cardW,cardH,SpellTheme.accent(group==0?CreatureEcology.ALL.get(index).family():index));
    g.drawString(font,name(index),x+14,y+13,SpellTheme.GOLD,false);
    if(group==0){var s=CreatureEcology.ALL.get(index);
      g.drawString(font,CreatureEcology.RANKS[s.rank()]+" "+CreatureEcology.CLASSES[s.cores()],x+14,y+28,SpellTheme.TEXT,false);
      g.drawString(font,font.plainSubstrByWidth(s.regions().replace('_',' ').replace(","," / "),cardW-28),x+14,y+42,0x9DB3BF,false);
    }
    g.flush();draw(g,index,width*.5f,y+cardH*.57f,cardW-32,cardH-78,delta);
    g.drawString(font,"Drag to rotate",x+14,y+cardH-15,0x8197A5,false);
  }
  private void draw(GuiGraphics g,int i,float cx,float cy,float availableW,float availableH,float delta){
    if(group==2){drawMagic(g,i,cx,cy,availableW,availableH,delta);return;}
    float age=ticks+delta,stride=mode==1?.75f:0,strike=mode==3?Math.max(.001f,((ticks%34)+delta)/34f):0;
    Bounds b=group==0?creatureBounds[i]:beastBounds[i];
    float scale=Math.min(availableW/Math.max(.4f,(b.maxX-b.minX)*.85f+(b.maxZ-b.minZ)*.65f),availableH/Math.max(.4f,b.maxY-b.minY))* .78f;
    var pose=g.pose();pose.pushPose();pose.translate(cx,cy,120);pose.scale(scale,scale,-scale);
    pose.mulPose(Axis.XP.rotationDegrees(-12));pose.mulPose(Axis.YP.rotationDegrees(yaw));
    pose.translate(-(b.minX+b.maxX)*.5f,-(b.minY+b.maxY)*.5f,-(b.minZ+b.maxZ)*.5f);
    Lighting.setupForEntityInInventory();RenderSystem.enableDepthTest();RenderSystem.setShaderColor(1,1,1,1);
    var atlas=InventoryMenu.BLOCK_ATLAS;var buffers=g.bufferSource();var skin=buffers.getBuffer(RenderType.entityCutoutNoCull(atlas));
    var white=Minecraft.getInstance().getTextureAtlas(atlas).apply(ResourceLocation.withDefaultNamespace("block/white_concrete"));
    int family=group==0?CreatureEcology.ALL.get(i).family():i;
    if(group==0){
      creatures.animate(i,family,age*.8f,stride,age,0,0,strike,mode==2);
      creatures.renderToBuffer(pose,skin,0xF000F0,OverlayTexture.NO_OVERLAY,0xFFFFFFFF);
      creatures.renderGlow(pose,white.wrap(buffers.getBuffer(RenderType.eyes(atlas))),NightmareRenderer.eyeTint(CreatureEcology.ALL.get(i).id(),family,false),false);
    }else{
      beasts.animate(i,age*.8f,stride,age,0,0,strike,mode==4,mode==2);
      beasts.renderToBuffer(pose,skin,0xF000F0,OverlayTexture.NO_OVERLAY,0xFFFFFFFF);
      beasts.glow(pose,white.wrap(buffers.getBuffer(RenderType.eyes(atlas))),SpellTheme.accent(family));
    }
    buffers.endBatch(RenderType.entityCutoutNoCull(atlas));buffers.endBatch(RenderType.eyes(atlas));
    Lighting.setupFor3DItems();pose.popPose();
  }
  private void drawMagic(GuiGraphics g,int family,float cx,float cy,float w,float h,float delta){
    if(!MagicShaders.isLoaded())return;
    g.flush();var pose=g.pose();pose.pushPose();pose.translate(cx,cy+Math.min(20,h*.1f),110);
    float scale=Math.min(w/5f,h/4f);pose.scale(scale,-scale,scale);pose.mulPose(Axis.XP.rotationDegrees(22));
    MagicShaders.time((ticks+delta)/20);MagicShaders.viewRange(100000);
    int color=SpellTheme.accent(family),r=(color>>16)&255,green=(color>>8)&255,blue=color&255;
    float progress=((ticks%64)+delta)/64f;
    for(int pass=0;pass<2;pass++){
      boolean wisps=pass==1;
      var type=wisps?MagicShaders.renderTypeForMaterial(SpellGeometry.WISP):MagicShaders.renderType();
      var vertex=g.bufferSource().getBuffer(type);
      SpellGeometry.emit(family,mode%2==0?1:2,2,progress,new SpellGeometry.Sink(){
      @Override public void quad(double ax,double ay,double az,double bx,double by,double bz,double ccx,double ccy,double ccz,double dx,double dy,double dz,float alpha){styledQuad(0,ax,ay,az,bx,by,bz,ccx,ccy,ccz,dx,dy,dz,alpha);}
      @Override public void styledQuad(int style,double ax,double ay,double az,double bx,double by,double bz,double ccx,double ccy,double ccz,double dx,double dy,double dz,float alpha){
        if((style==SpellGeometry.WISP)!=wisps)return;
        int a=(int)(Math.clamp(alpha,0,1)*220);float u=style*2;
        vertex.addVertex(pose.last().pose(),(float)ax,(float)ay,(float)az).setColor(r,green,blue,a).setUv(u,0);
        vertex.addVertex(pose.last().pose(),(float)bx,(float)by,(float)bz).setColor(r,green,blue,a).setUv(u+1,0);
        vertex.addVertex(pose.last().pose(),(float)ccx,(float)ccy,(float)ccz).setColor(r,green,blue,a).setUv(u+1,1);
        vertex.addVertex(pose.last().pose(),(float)dx,(float)dy,(float)dz).setColor(r,green,blue,a).setUv(u,1);
      }
      });
      g.bufferSource().endBatch(type);
    }
    pose.popPose();
  }
  private static final class Bounds implements VertexConsumer {
    float minX=Float.MAX_VALUE,minY=Float.MAX_VALUE,minZ=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,maxY=-Float.MAX_VALUE,maxZ=-Float.MAX_VALUE;
    static Bounds measure(ModelPart rig){var b=new Bounds();rig.visit(new PoseStack(),(pose,path,i,cube)->cube.compile(pose,b,0,0,0xFFFFFFFF));return b;}
    @Override public VertexConsumer addVertex(float x,float y,float z){minX=Math.min(minX,x);minY=Math.min(minY,y);minZ=Math.min(minZ,z);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);maxZ=Math.max(maxZ,z);return this;}
    @Override public VertexConsumer setColor(int r,int g,int b,int a){return this;}
    @Override public VertexConsumer setUv(float u,float v){return this;}
    @Override public VertexConsumer setUv1(int u,int v){return this;}
    @Override public VertexConsumer setUv2(int u,int v){return this;}
    @Override public VertexConsumer setNormal(float x,float y,float z){return this;}
  }
}
