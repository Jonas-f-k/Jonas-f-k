package com.shadowslave.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Shared palette and native controls for the Spell screen and HUD. */
public final class SpellTheme {
  public static final int INK=0xFF0E1420, PANEL=0xF51B2432, EDGE=0xFF394859, TEXT=0xFFDCE4E5, GOLD=0xFFE0BA76;
  private static final int[] FAMILY={0xFFF2A266,0xFFB0A4E5,0xFF71D5D3,0xFFC8CDD4,0xFFA5CE82,0xFFD9BCE7,0xFFA8B9F1};
  private SpellTheme() {}
  public static int accent(int family){return FAMILY[Math.clamp(family,0,6)];}
  public static void panel(GuiGraphics g,int x,int y,int w,int h,int accent){
    g.fill(x,y,x+w,y+h,INK);g.fillGradient(x+1,y+1,x+w-1,y+h-1,PANEL,0xF5101825);
    g.fill(x,y,x+w,y+1,EDGE);g.fill(x,y+h-1,x+w,y+h,EDGE);
    g.fill(x,y,x+1,y+h,EDGE);g.fill(x+w-1,y,x+w,y+h,EDGE);
    for(int s:new int[]{0,1}){int xx=s==0?x:x+w-7;g.fill(xx,y,xx+7,y+2,accent);g.fill(xx,y+h-2,xx+7,y+h,accent);}
  }
  public static void core(GuiGraphics g,int x,int y,int size,double fill,int accent){
    int mid=size/2;
    for(int i=0;i<size;i++){int half=Math.min(i,size-1-i);g.fill(x+mid-half,y+i,x+mid+half+1,y+i+1,EDGE);}
    int start=(int)((size-2)*(1-Math.clamp(fill,0,1)));
    for(int i=1;i<size-1;i++){int half=Math.max(0,Math.min(i,size-1-i)-1);g.fill(x+mid-half,y+i,x+mid+half+1,y+i+1,i-1>=start?accent:INK);}
  }
  public static Button button(String text,int x,int y,int width,Runnable action,boolean selected){return new SpellButton(text,x,y,width,action,selected);}
  private static final class SpellButton extends Button {
    private final boolean selected;
    SpellButton(String label,int x,int y,int width,Runnable action,boolean selected){super(x,y,width,20,Component.literal(label),b->action.run(),DEFAULT_NARRATION);this.selected=selected;}
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
      int x=getX(),y=getY(),w=getWidth();boolean hover=isHoveredOrFocused();
      g.fill(x,y,x+w,y+20,selected?0xFF354255:hover?0xFF2B3749:0xFF1D2938);
      g.fill(x,y+19,x+w,y+20,selected?GOLD:hover?0xFF8293A6:EDGE);
      if(selected)g.fill(x,y,x+2,y+20,GOLD);
      var font=Minecraft.getInstance().font;
      g.drawCenteredString(font,font.plainSubstrByWidth(getMessage().getString(),w-8),x+w/2,y+6,active?(selected?GOLD:TEXT):0xFF657184);
    }
  }
}
