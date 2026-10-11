import com.shadowslave.client.SpellGeometry;
public class VisualMathCheck {
 public static void main(String[] args){int[] count={0};
  for(int family=0;family<7;family++)for(int kind=0;kind<7;kind++)for(double radius:new double[]{.25,4,32})for(double time:new double[]{0,.1,.3,.6,.9,1}){
   int before=count[0];
   SpellGeometry.emit(family,kind,radius,time,(ax,ay,az,bx,by,bz,cx,cy,cz,dx,dy,dz,a)->{
    for(double v:new double[]{ax,ay,az,bx,by,bz,cx,cy,cz,dx,dy,dz})if(!Double.isFinite(v)||Math.abs(v)>radius*2.5+.2)throw new AssertionError("Effect outside bounds");
    if(!Float.isFinite(a)||a<0||a>1)throw new AssertionError("Invalid alpha");count[0]++;
   });
   if(count[0]==before||count[0]-before>256)throw new AssertionError("Missing or excessive geometry");
  }
  int before=count[0];for(double r:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1,33})SpellGeometry.emit(0,0,r,.5,(ax,ay,az,bx,by,bz,cx,cy,cz,dx,dy,dz,a)->count[0]++);
  if(before!=count[0])throw new AssertionError("Invalid input accepted");
  System.out.println("PASS: all 49 family/effect combinations; "+count[0]+" finite, bounded quads; invalid radii rejected.");
 }
}
