package com.shadowslave.client;

/** Deterministic, bounded spell choreography; independent of Minecraft for validation. */
public final class SpellGeometry {
  public static final int RIBBON=0,MOTE=1,FLAME=2,PRISM=3,GLYPH=4,WISP=5,WAVE=6,THORN=7,RUNE=8;
  /** The original functional API remains usable by geometry validators. */
  public interface Sink {
    void quad(double ax,double ay,double az,double bx,double by,double bz,double cx,double cy,double cz,double dx,double dy,double dz,float alpha);
    default void styledQuad(int style,double ax,double ay,double az,double bx,double by,double bz,double cx,double cy,double cz,double dx,double dy,double dz,float alpha){
      quad(ax,ay,az,bx,by,bz,cx,cy,cz,dx,dy,dz,alpha);
    }
  }
  private static final double TAU=Math.PI*2;
  private SpellGeometry(){}
  public static void emit(int family,int type,double radius,double progress,Sink out){
    if(family<0||family>6||type<0||type>6||!Double.isFinite(radius)||radius<.25||radius>32||!Double.isFinite(progress))return;
    double t=Math.clamp(progress,0,1);
    if(type==5){telegraph(out,radius,t);return;}
    boolean impact=type==6;
    double anticipation=impact?1:ease(Math.min(1,t/.18));
    double burst=impact?1-Math.pow(1-t,3):ease(Math.clamp((t-.14)/.34,0,1));
    double release=1-ease(Math.clamp((t-.57)/.43,0,1));
    float alpha=(float)Math.clamp(impact?Math.pow(1-t,1.45):anticipation*(.54+.38*burst)*release,0,1);
    double r=radius*(impact?.18+.82*burst:.27+.73*burst),spin=t*(family==6?2.4:family==1?-1.3:.85);
    seal(out,r,.035,alpha*.28f);
    ring(out,r,.055,Math.max(.012,r*.017),24,spin,RIBBON,alpha*.66f);
    for(int i=0;i<8;i++)glyph(out,i*TAU/8+spin,r*.84,.062,r*.09,r*.026,alpha*.72f,i%3);
    if(type==2||type==3){
      seal(out,r*.56,.085,alpha*.36f);
      for(int i=0;i<12;i++){
        double a=i*TAU/12-spin,h=r*(.12+.88*burst)*(type==3?1.5:1.05);
        ribbon(out,Math.cos(a)*r*.72,.08,Math.sin(a)*r*.72,Math.cos(a+.5)*r*.46,h,Math.sin(a+.5)*r*.46,r*.018,GLYPH,alpha*.40f);
      }
    }
    switch(family){
      case 0 -> flame(out,r,t,burst,alpha);
      case 1 -> shadow(out,r,t,burst,alpha);
      case 2 -> tide(out,r,t,burst,alpha);
      case 3 -> steel(out,r,t,burst,alpha);
      case 4 -> thorn(out,r,t,burst,alpha);
      case 5 -> mirror(out,r,t,burst,alpha);
      case 6 -> gravity(out,r,t,burst,alpha);
    }
    if(type==0)for(int i=0;i<4;i++){
      double angle=i*TAU/4+t,rr=r*.28;
      sprite(out,Math.cos(angle)*rr,.12*r,Math.sin(angle)*rr,r*.018,r*.065,MOTE,alpha*.64f);
    }
    if(type==1){
      ring(out,r*(.35+.55*t),.095,Math.max(.013,r*.022),24,-spin,WAVE,alpha*.52f);
      ring(out,r*(.26+.47*t),.16,Math.max(.012,r*.012),24,spin,RIBBON,alpha*.34f);
    }
    if(type==4)for(int i=0;i<12;i++){
      double angle=i*TAU/12+spin;
      ribbon(out,Math.cos(angle)*r*.18,.08,Math.sin(angle)*r*.18,Math.cos(angle)*r*.91,r*(.08+.26*burst),Math.sin(angle)*r*.91,r*.024,GLYPH,alpha*.58f);
    }
    if(type==6)for(int i=0;i<3;i++)sprite(out,(i-1)*r*.12,r*.14,0,r*.022,r*.09,MOTE,alpha*.85f);
    if(type==3)for(int i=0;i<8;i++)ring(out,r*(.5-.025*i),r*(i/8.0)*(1.1+.25*burst),Math.max(.012,r*.013),10,spin+i*.23,RIBBON,alpha*(.32f-i*.02f));
  }
  private static void telegraph(Sink out,double r,double t){
    float a=(float)(.40+.38*t);
    seal(out,r,.035,a*.38f);
    ring(out,r,.065,Math.max(.035,r*.035),32,0,RIBBON,a);
    ring(out,r*(1-.77*ease(t)),.075,Math.max(.018,r*.018),24,-t*.4,RIBBON,a*.64f);
    // Clockwise wedges fill before the attack; actual damage remains server-authoritative.
    for(int i=0;i<16;i++)glyph(out,i*TAU/16,r*.91,.08,r*.075,r*.028,a*(i/16.0<t?1:.28f),i%2);
  }
  private static void flame(Sink out,double r,double t,double burst,float a){
    for(int i=0;i<16;i++){
      double phase=fract(i*.618+t*1.6),angle=i*2.399+t*1.3,rr=r*(.26+.64*fract(i*.37))*(1-.24*phase),y=r*phase*(.8+.5*burst);
      sprite(out,Math.cos(angle)*rr,y,Math.sin(angle)*rr,r*(.045+.026*(1-phase)),r*(.18+.1*burst)*(1-.4*phase),FLAME,a*(float)(1-phase*.8));
    }
    for(int i=0;i<16;i++){
      double p=fract(i*.371+t*1.5),angle=i*2.399+.3*t;
      sprite(out,Math.cos(angle)*r*(.2+p*.7),r*(.08+p*1.35),Math.sin(angle)*r*(.2+p*.7),r*.012,r*.038,MOTE,a*(float)(1-p));
    }
  }
  private static void shadow(Sink out,double r,double t,double burst,float a){
    for(int i=0;i<10;i++){
      double angle=i*TAU/10-t*1.8,rr=r*(.62+.12*Math.sin(i+t*5));
      for(int j=0;j<3;j++){
        double p=j/3.0,h=r*(.1+.62*burst+p*.28),next=angle-.2;
        ribbon(out,Math.cos(angle)*rr*(1-p*.2),h,Math.sin(angle)*rr*(1-p*.2),Math.cos(next)*rr*(.88-p*.2),h+r*.2,Math.sin(next)*rr*(.88-p*.2),r*(.045-p*.014),WISP,a*(.62f-j*.15f));
      }
    }
    for(int i=0;i<14;i++){
      double p=fract(i*.381+t*.65),angle=i*2.399-t;
      sprite(out,Math.cos(angle)*r*(1-p*.55),r*(.12+p*.9),Math.sin(angle)*r*(1-p*.55),r*.035,r*.11,WISP,a*(float)(.7*(1-p)));
    }
  }
  private static void tide(Sink out,double r,double t,double burst,float a){
    for(int band=0;band<3;band++){
      double rr=r*(.36+band*.26),y=r*(.06+band*.12)*(1-.4*t);
      for(int i=0;i<28;i++){
        double aa=i*TAU/28+t*(band%2==0?1:-1),bb=(i+1)*TAU/28+t*(band%2==0?1:-1),ya=y+Math.sin(aa*3+t*7)*r*.04,yb=y+Math.sin(bb*3+t*7)*r*.04,w=r*.045;
        out.styledQuad(WAVE,Math.cos(aa)*(rr-w),ya,Math.sin(aa)*(rr-w),Math.cos(bb)*(rr-w),yb,Math.sin(bb)*(rr-w),Math.cos(bb)*(rr+w),yb,Math.sin(bb)*(rr+w),Math.cos(aa)*(rr+w),ya,Math.sin(aa)*(rr+w),a*(.78f-band*.13f));
      }
    }
    for(int i=0;i<12;i++){
      double p=fract(i*.317+t),angle=i*2.399;
      sprite(out,Math.cos(angle)*r*(.35+.65*p),r*(.1+Math.sin(p*Math.PI)*.65*burst),Math.sin(angle)*r*(.35+.65*p),r*.018,r*.068,MOTE,a*(float)Math.sin(Math.PI*p));
    }
  }
  private static void steel(Sink out,double r,double t,double burst,float a){
    for(int i=0;i<12;i++){
      double angle=i*TAU/12+t*.6,rr=r*(.48+.24*burst),y=r*(.16+(i%3)*.18);
      prism(out,Math.cos(angle)*rr,y,Math.sin(angle)*rr,r*.045,r*(.14+.1*burst),angle,PRISM,a*.82f);
      radial(out,angle,r*.25,r*.78,.07,r*.018,GLYPH,a*.42f);
    }
  }
  private static void thorn(Sink out,double r,double t,double burst,float a){
    for(int i=0;i<12;i++){
      double angle=i*TAU/12+.12*Math.sin(i),h=r*(.25+(i%3)*.12)*burst,x=Math.cos(angle)*r*.78,z=Math.sin(angle)*r*.78;
      ribbon(out,x,.07,z,x*.72,h+.07,z*.72,r*.035,THORN,a*.85f);
      ribbon(out,x*.82,h*.64,z*.82,x*.7+r*.09*Math.sin(angle),h*.85,z*.7-r*.09*Math.cos(angle),r*.027,THORN,a*.6f);
      sprite(out,x*.7,h*.75+.04,z*.7,r*.045,r*.08,THORN,a*.46f);
    }
    ring(out,r*.52,.075,Math.max(.013,r*.013),24,t*.25,GLYPH,a*.42f);
  }
  private static void mirror(Sink out,double r,double t,double burst,float a){
    for(int i=0;i<10;i++){
      double angle=i*TAU/10+t*.7,rr=r*(.68+.1*Math.sin(t*5+i)),y=r*(.16+.22*Math.sin(angle*2+t)*Math.sin(angle*2+t)+.18*burst);
      prism(out,Math.cos(angle)*rr,y,Math.sin(angle)*rr,r*.095,r*(.19+.08*burst),angle+t,PRISM,a*.7f);
      sprite(out,Math.cos(angle-.18)*rr,y+.08*r,Math.sin(angle-.18)*rr,r*.012,r*.055,MOTE,a*.72f);
    }
  }
  private static void gravity(Sink out,double r,double t,double burst,float a){
    for(int orbit=0;orbit<3;orbit++){
      double rr=r*(.50+orbit*.14),tilt=(orbit-1)*.58,spin=t*(1.5+orbit*.35)+orbit*1.047;
      for(int i=0;i<28;i++){
        double aa=i*TAU/28+spin,bb=(i+1)*TAU/28+spin,x=Math.cos(aa)*rr,z=Math.sin(aa)*rr,xx=Math.cos(bb)*rr,zz=Math.sin(bb)*rr;
        ribbon(out,x,r*.48+z*tilt,z,xx,r*.48+zz*tilt,zz,r*.013,RIBBON,a*(.68f-orbit*.1f));
      }
      double aa=spin*2.5;
      sprite(out,Math.cos(aa)*rr,r*.48+Math.sin(aa)*rr*tilt,Math.sin(aa)*rr,r*.032,r*.075,MOTE,a*.92f);
    }
    for(int i=0;i<8;i++){
      double p=fract(i*.381+t*.6),angle=i*2.399-t*1.1;
      sprite(out,Math.cos(angle)*r*(1-p*.65),r*(.08+p*.8*burst),Math.sin(angle)*r*(1-p*.65),r*.013,r*.04,MOTE,a*(float)(1-p));
    }
  }
  private static void seal(Sink out,double r,double y,float a){out.styledQuad(RUNE,-r,y,-r,r,y,-r,r,y,r,-r,y,r,a);}
  private static void ring(Sink out,double r,double y,double w,int steps,double spin,int style,float a){
    for(int i=0;i<steps;i++){
      double aa=i*TAU/steps+spin,bb=(i+1)*TAU/steps+spin;
      out.styledQuad(style,Math.cos(aa)*r,y,Math.sin(aa)*r,Math.cos(bb)*r,y,Math.sin(bb)*r,Math.cos(bb)*(r-w),y,Math.sin(bb)*(r-w),Math.cos(aa)*(r-w),y,Math.sin(aa)*(r-w),a);
    }
  }
  private static void sprite(Sink out,double x,double y,double z,double w,double h,int style,float a){
    out.styledQuad(style,x-w,y,z,x+w,y,z,x+w,y+h,z,x-w,y+h,z,a);
    out.styledQuad(style,x,y,z-w,x,y,z+w,x,y+h,z+w,x,y+h,z-w,a*.78f);
  }
  private static void prism(Sink out,double x,double y,double z,double w,double h,double angle,int style,float a){
    for(int i=0;i<3;i++){
      double aa=angle+i*TAU/3,bb=angle+(i+1)*TAU/3;
      out.styledQuad(style,x+Math.cos(aa)*w,y,z+Math.sin(aa)*w,x,y+h,z,x+Math.cos(bb)*w,y,z+Math.sin(bb)*w,x,y-h*.32,z,a*(1-i*.16f));
    }
  }
  private static void glyph(Sink out,double angle,double r,double y,double length,double width,float a,int variant){
    radial(out,angle,r-length*.5,r+length*.5,y,width,GLYPH,a);
    double tangent=angle+Math.PI/2,x=Math.cos(angle)*r,z=Math.sin(angle)*r;
    ribbon(out,x-Math.cos(tangent)*length*.32,y+.001,z-Math.sin(tangent)*length*.32,x+Math.cos(tangent)*length*.32,y+.001,z+Math.sin(tangent)*length*.32,width*.7,GLYPH,a*.8f);
    if(variant==2)radial(out,angle+.026,r-length*.34,r+length*.18,y+.002,width*.5,GLYPH,a*.55f);
  }
  private static void radial(Sink out,double angle,double inner,double outer,double y,double w,int style,float a){
    double x=Math.cos(angle),z=Math.sin(angle),dx=-z*w,dz=x*w;
    out.styledQuad(style,x*inner+dx,y,z*inner+dz,x*outer+dx,y,z*outer+dz,x*outer-dx,y,z*outer-dz,x*inner-dx,y,z*inner-dz,a);
  }
  private static void ribbon(Sink out,double ax,double ay,double az,double bx,double by,double bz,double w,int style,float a){
    double dx=bx-ax,dz=bz-az,len=Math.hypot(dx,dz),nx=len<1e-9?w:-dz/len*w,nz=len<1e-9?0:dx/len*w;
    out.styledQuad(style,ax+nx,ay,az+nz,bx+nx,by,bz+nz,bx-nx,by,bz-nz,ax-nx,ay,az-nz,a);
  }
  private static double ease(double x){return x*x*(3-2*x);}
  private static double fract(double x){return x-Math.floor(x);}
}
