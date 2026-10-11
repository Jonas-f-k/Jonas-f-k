package com.shadowslave.client;

import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.network.SpellNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.util.ArrayDeque;

/** Built-in depth-tested translucent render path; no replacement shader or framebuffer. */
@EventBusSubscriber(modid=ShadowSlaveMod.ID,value=Dist.CLIENT)
public final class SpellEffects {
  private record Active(SpellNetwork.Visual v,long start) {}
  private static final ArrayDeque<Active> active=new ArrayDeque<>();
  private static net.minecraft.client.multiplayer.ClientLevel lastLevel;
  @SubscribeEvent public static void render(RenderLevelStageEvent e){
    if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_WEATHER)return;
    var mc=Minecraft.getInstance();
    if(mc.level!=lastLevel){active.clear();SpellNetwork.visuals.clear();lastLevel=mc.level;}
    if(mc.level==null)return;
    long now=mc.level.getGameTime();
    for(SpellNetwork.Visual v;(v=SpellNetwork.visuals.poll())!=null;){if(!v.dimension().equals(mc.level.dimension().location()))continue;while(active.size()>=32)active.removeFirst();active.addLast(new Active(v,now));}
    active.removeIf(a->now-a.start>=a.v.duration());
    var camera=e.getCamera().getPosition();var pose=e.getPoseStack();pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
    var render=RenderType.lightning();var buffers=mc.renderBuffers().bufferSource();var vertex=buffers.getBuffer(render);
    int[] budget={0};
    for(var a:active){var v=a.v;if(camera.distanceToSqr(v.x(),v.y(),v.z())>9216)continue;
      int color=v.kind()==5?0xFFF59B71:SpellTheme.accent(v.family());int r=(color>>16)&255,g=(color>>8)&255,b=color&255;
      double progress=(now-a.start+e.getPartialTick().getGameTimeDeltaPartialTick(false))/v.duration();
      SpellGeometry.emit(v.family(),v.kind(),v.radius(),progress,(ax,ay,az,bx,by,bz,cx,cy,cz,dx,dy,dz,alpha)->{
        if(budget[0]>=12000)return;budget[0]+=4;int opacity=(int)(alpha*200);
        vertex.addVertex(pose.last().pose(),(float)(v.x()+ax),(float)(v.y()+ay),(float)(v.z()+az)).setColor(r,g,b,opacity);
        vertex.addVertex(pose.last().pose(),(float)(v.x()+bx),(float)(v.y()+by),(float)(v.z()+bz)).setColor(r,g,b,opacity);
        vertex.addVertex(pose.last().pose(),(float)(v.x()+cx),(float)(v.y()+cy),(float)(v.z()+cz)).setColor(r,g,b,opacity);
        vertex.addVertex(pose.last().pose(),(float)(v.x()+dx),(float)(v.y()+dy),(float)(v.z()+dz)).setColor(r,g,b,opacity);
      });
    }
    buffers.endBatch(render);pose.popPose();
  }
}
