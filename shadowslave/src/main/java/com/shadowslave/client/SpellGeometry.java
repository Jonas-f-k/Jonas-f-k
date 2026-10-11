package com.shadowslave.client;

/** Pure bounded geometry, also exercised outside Minecraft by the validation tool. */
public final class SpellGeometry {
  public interface Sink {void quad(double ax,double ay,double az,double bx,double by,double bz,double cx,double cy,double cz,double dx,double dy,double dz,float alpha);}
  private SpellGeometry() {}
  public static void emit(int family,int type,double radius,double progress,Sink out) {
    if(family<0||family>6||type<0||type>6||!Double.isFinite(radius)||radius<.25||radius>32||!Double.isFinite(progress))return;
    double t=Math.clamp(progress,0,1),r=radius*(type==5?1:.35+.65*t);
    float alpha=(float)((type==5?.6:.78)*Math.sin(Math.PI*Math.clamp(t,.03,.97)));
    ring(out,r,.06,Math.max(.04,r*.022),alpha);
    if(type==5){ring(out,r*.92,.065,.045,alpha);return;}
    double power=type==2||type==3?1.35:1;
    switch(family) {
      case 0 -> { // Flame: rising helix tongues.
        for(int i=0;i<28;i++){double a=i*Math.PI/7+t*5, rr=r*(1-i/38.0);double x=Math.cos(a)*rr,z=Math.sin(a)*rr,y=i*r*.06;billboard(out,x,y,z,r*.075,r*.18*power,alpha);}
      }
      case 1 -> { // Shadow: swept lances, kept separate from the ground warning.
        for(int i=0;i<7;i++){double a=i*Math.PI*2/7+t;radial(out,a,r*.25,r,r*.9*power,.045*r,alpha);}
      }
      case 2 -> { // Tide: layered shells.
        for(int i=0;i<4;i++)ring(out,r*(1-i*.17),.12+i*r*.12,r*.035,alpha*(1-i*.15f));
      }
      case 3 -> { // Steel: squared sigil bars.
        for(int i=0;i<8;i++)radial(out,i*Math.PI/4,r*.45,r,.1,r*.075,alpha);
        ring(out,r*.55,.15,r*.04,alpha);
      }
      case 4 -> { // Thorn: alternating rooted spears.
        for(int i=0;i<12;i++)radial(out,i*Math.PI/6,r*.75,r,.5*r*(i%2==0?1:.6)*power,r*.035,alpha);
      }
      case 5 -> { // Mirror: floating prism facets.
        for(int i=0;i<8;i++){double a=i*Math.PI/4+t*2;billboard(out,Math.cos(a)*r*.7,(.4+.25*Math.sin(a*2))*r,Math.sin(a)*r*.7,r*.12,r*.4,alpha);}
      }
      case 6 -> { // Gravity: two inclined orbital rings.
        for(int i=0;i<40;i++){double a=i*Math.PI/20+t*3,b=(i+1)*Math.PI/20+t*3;
          double x=Math.cos(a)*r,z=Math.sin(a)*r,xx=Math.cos(b)*r,zz=Math.sin(b)*r;
          out.quad(x, .45*r+z*.3,z,xx,.45*r+zz*.3,zz,xx,.45*r+zz*.3+.04*r,zz,x,.45*r+z*.3+.04*r,z,alpha);
          out.quad(x,.6*r-z*.3,z,xx,.6*r-zz*.3,zz,xx,.6*r-zz*.3+.04*r,zz,x,.6*r-z*.3+.04*r,z,alpha);
        }
      }
    }
    if(type==3)ring(out,r*.65,r*.3,r*.055,alpha);
  }
  private static void ring(Sink out,double r,double y,double thick,float a){for(int i=0;i<40;i++){double x=i*Math.PI/20,z=(i+1)*Math.PI/20;out.quad(Math.cos(x)*r,y,Math.sin(x)*r,Math.cos(z)*r,y,Math.sin(z)*r,Math.cos(z)*(r-thick),y,Math.sin(z)*(r-thick),Math.cos(x)*(r-thick),y,Math.sin(x)*(r-thick),a);}}
  private static void billboard(Sink out,double x,double y,double z,double w,double h,float a){out.quad(x-w,y,z,x+w,y,z,x+w,y+h,z,x-w,y+h,z,a);out.quad(x,y,z-w,x,y,z+w,x,y+h,z+w,x,y+h,z-w,a);}
  private static void radial(Sink out,double angle,double inner,double outer,double h,double w,float a){double x=Math.cos(angle),z=Math.sin(angle),dx=-z*w,dz=x*w;out.quad(x*inner+dx,0,z*inner+dz,x*outer+dx,h,z*outer+dz,x*outer-dx,h,z*outer-dz,x*inner-dx,0,z*inner-dz,a);}
}
