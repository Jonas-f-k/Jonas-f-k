package com.shadowslave.world;

import java.util.*;

/** Seeded 4–7 island clusters and their connected minimum-spanning chain graphs. Plain math. */
public final class Archipelago {
  public static final int SPACING=384;
  public record Island(int x,int y,int z,int rx,int rz,int thickness,int shape,double angle) {}
  public record Link(Island a,Island b) {}
  private record Key(long seed,int x,int z) {}
  private static final Map<Key,List<Island>> CACHE=Collections.synchronizedMap(new LinkedHashMap<>(256,.75f,true){
    @Override protected boolean removeEldestEntry(Map.Entry<Key,List<Island>> e){return size()>512;}
  });
  private Archipelago() {}
  public static List<Island> cluster(long seed,int cx,int cz) {
    var key=new Key(seed,cx,cz);var cached=CACHE.get(key);if(cached!=null)return cached;
    var r=new SplittableRandom(seed ^ cx*341873128712L ^ cz*132897987541L);
    int count=4+r.nextInt(4),x=cx*SPACING+SPACING/2+r.nextInt(-12,13),z=cz*SPACING+SPACING/2+r.nextInt(-12,13);
    double rotation=r.nextDouble(Math.PI*2);var out=new ArrayList<Island>();
    for(int i=0;i<count;i++){
      double a=rotation+i*Math.PI*2/count+r.nextDouble(-.06,.06),distance=r.nextDouble(108,118);
      out.add(new Island(x+(int)(Math.cos(a)*distance),128+r.nextInt(34),z+(int)(Math.sin(a)*distance),22+r.nextInt(12),22+r.nextInt(17),20+r.nextInt(26),r.nextInt(5),r.nextDouble(Math.PI)));
    }
    var result=List.copyOf(out);CACHE.put(key,result);return result;
  }
  public static List<Link> links(List<Island> islands) {
    var out=new ArrayList<Link>();var used=new HashSet<Island>();used.add(islands.getFirst());
    while(used.size()<islands.size()){
      Island from=null,to=null;double best=Double.MAX_VALUE;
      for(var a:used)for(var b:islands)if(!used.contains(b)){
        double d=Math.hypot(a.x-b.x,a.z-b.z);if(d<best){best=d;from=a;to=b;}
      }
      out.add(new Link(from,to));used.add(to);
    }
    return List.copyOf(out);
  }
  public static double density(Island a,double x,double y,double z) {
    double dx=x-a.x,dz=z-a.z,co=Math.cos(a.angle),si=Math.sin(a.angle);
    double u=(dx*co+dz*si)/a.rx,v=(-dx*si+dz*co)/a.rz;
    double radial=u*u+v*v;
    // Five distinct terrain outlines: dome, shelf, needle, crescent and fractured saddle.
    if(a.shape==3)radial+=.38*Math.max(0,u+.2);
    if(a.shape==4)radial+=.22*Math.abs(Math.sin(u*7+v*3));
    if(radial>=1)return -1;
    double crest=switch(a.shape){case 0->3*(1-radial);case 1->1.5*Math.floor((1-radial)*3);case 2->10*Math.pow(1-radial,2);case 3->3*Math.sin(v*3);default->4*Math.abs(Math.sin(u*3));};
    double top=a.y-3*radial+crest,bottom=a.y-7-a.thickness*Math.pow(1-radial,.65);
    return Math.clamp(Math.min((top-y)/5,(y-bottom)/5),-1,1);
  }
  public static double density(long seed,int x,int y,int z) {
    double result=-1;
    int cx=Math.floorDiv(x,SPACING),cz=Math.floorDiv(z,SPACING);
    for(var a:cluster(seed,cx,cz))result=Math.max(result,density(a,x,y,z));
    return result;
  }
}
