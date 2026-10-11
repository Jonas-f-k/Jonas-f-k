import com.shadowslave.world.Archipelago;
import java.util.*;
public class ArchipelagoCheck {
 public static void main(String[] args){var shapes=new HashSet<Integer>();int islands=0,links=0;
  for(long seed=-128;seed<128;seed++)for(int x=-2;x<=2;x++){
   var group=Archipelago.cluster(seed,x,x-1);if(group.size()<4||group.size()>7)throw new AssertionError("Cluster size");
   if(!group.equals(Archipelago.cluster(seed,x,x-1)))throw new AssertionError("Nondeterministic");
   var edges=Archipelago.links(group);if(edges.size()!=group.size()-1)throw new AssertionError("MST edge count");
   var reached=new HashSet<Archipelago.Island>();reached.add(group.getFirst());
   for(int pass=0;pass<group.size();pass++)for(var e:edges){if(reached.contains(e.a()))reached.add(e.b());if(reached.contains(e.b()))reached.add(e.a());}
   if(reached.size()!=group.size())throw new AssertionError("Disconnected graph");
   if(group.stream().map(a->List.of(a.rx(),a.rz(),a.y(),a.thickness())).distinct().count()<2)throw new AssertionError("Identical islands");
   for(var a:group){
    if(Archipelago.density(a,a.x(),a.y()-4,a.z())<=0)throw new AssertionError("No solid center");
    for(int y=-64;y<64;y+=8)if(Archipelago.density(seed,a.x(),y,a.z())>=0)throw new AssertionError("Solid material below sky belt");
    if(a.x()-Math.max(a.rx(),a.rz())<x*Archipelago.SPACING||a.x()+Math.max(a.rx(),a.rz())>=(x+1)*Archipelago.SPACING)throw new AssertionError("Island crosses cluster cell");
    shapes.add(a.shape());islands++;
   }links+=edges.size();
  }
  if(shapes.size()!=5)throw new AssertionError("Missing shape family");
  if(Archipelago.cluster(1,0,0).equals(Archipelago.cluster(2,0,0)))throw new AssertionError("Seed does not affect islands");
  System.out.println("PASS: "+islands+" seeded islands; "+links+" connected MST links; all five shapes; empty space below floating belt.");
 }
}
