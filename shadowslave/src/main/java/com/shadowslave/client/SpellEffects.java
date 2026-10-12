package com.shadowslave.client;

import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.network.SpellNetwork;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.util.ArrayDeque;

/** Server-authoritative, depth-tested local magic with bundled GLSL and vanilla fallback. */
@EventBusSubscriber(modid=ShadowSlaveMod.ID,value=Dist.CLIENT)
public final class SpellEffects {
  private record Active(SpellNetwork.Visual v,long start){}
  private static final ArrayDeque<Active> active=new ArrayDeque<>();
  private static net.minecraft.client.multiplayer.ClientLevel lastLevel;
  @SubscribeEvent public static void render(RenderLevelStageEvent e){
    if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_WEATHER)return;
    var mc=Minecraft.getInstance();
    if(mc.level!=lastLevel){active.clear();SpellNetwork.visuals.clear();lastLevel=mc.level;}
    if(mc.level==null)return;
    long now=mc.level.getGameTime();
    for(SpellNetwork.Visual v;(v=SpellNetwork.visuals.poll())!=null;){
      if(!v.dimension().equals(mc.level.dimension().location()))continue;
      while(active.size()>=32)active.removeFirst();active.addLast(new Active(v,now));
    }
    active.removeIf(a->now-a.start>=a.v.duration());
    if(active.isEmpty())return;
    var camera=e.getCamera().getPosition();var pose=e.getPoseStack();
    pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
    var render=MagicShaders.renderType();boolean shader=MagicShaders.isLoaded();
    var shadowRender=MagicShaders.renderTypeForMaterial(SpellGeometry.WISP);
    var buffers=mc.renderBuffers().bufferSource();
    double partial=e.getPartialTick().getGameTimeDeltaPartialTick(false);
    MagicShaders.time((float)((now%24000+partial)/20.0));MagicShaders.viewRange(96);
    int[] budget={0};
    try {
      for(int pass=0;pass<(shader?2:1);pass++){
        boolean wisps=pass==1;var passRender=wisps?shadowRender:render;
        var vertex=buffers.getBuffer(passRender);
        for(var a:active){
        var v=a.v;if(camera.distanceToSqr(v.x(),v.y(),v.z())>9216)continue;
        int color=v.kind()==5?0xFFF5A272:SpellTheme.accent(v.family());
        int red=(color>>16)&255,green=(color>>8)&255,blue=color&255;
        double progress=(now-a.start+partial)/v.duration();
        SpellGeometry.emit(v.family(),v.kind(),v.radius(),progress,new SpellGeometry.Sink(){
          @Override public void quad(double ax,double ay,double az,double bx,double by,double bz,double cx,double cy,double cz,double dx,double dy,double dz,float alpha){
            styledQuad(SpellGeometry.RIBBON,ax,ay,az,bx,by,bz,cx,cy,cz,dx,dy,dz,alpha);
          }
          @Override public void styledQuad(int style,double ax,double ay,double az,double bx,double by,double bz,double cx,double cy,double cz,double dx,double dy,double dz,float alpha){
            if(budget[0]>=12000||alpha<.002f||(!shader&&style==SpellGeometry.RUNE))return;
            if(shader&&((style==SpellGeometry.WISP)!=wisps))return;
            budget[0]+=4;int opacity=(int)(Math.clamp(alpha,0,1)*190);
            int lift=style==SpellGeometry.MOTE?48:style==SpellGeometry.GLYPH?20:0;
            int rr=Math.min(255,red+lift),gg=Math.min(255,green+lift),bb=Math.min(255,blue+lift);
            point(ax,ay,az,0,0,style,rr,gg,bb,opacity);point(bx,by,bz,1,0,style,rr,gg,bb,opacity);
            point(cx,cy,cz,1,1,style,rr,gg,bb,opacity);point(dx,dy,dz,0,1,style,rr,gg,bb,opacity);
          }
          private void point(double x,double y,double z,float u,float vv,int style,int r,int g,int b,int opacity){
            VertexConsumer point=vertex.addVertex(pose.last().pose(),(float)(v.x()+x),(float)(v.y()+y),(float)(v.z()+z)).setColor(r,g,b,opacity);
            if(shader)point.setUv(style*2+u,vv);
          }
        });
      }
        buffers.endBatch(passRender);
      }
    }finally{pose.popPose();}
  }
}
