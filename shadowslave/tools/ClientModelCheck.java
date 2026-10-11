import com.shadowslave.client.*;
import com.shadowslave.entity.CreatureEcology;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import com.mojang.blaze3d.vertex.*;

/** Real Minecraft model construction/UV check; requires Gradle's generated client classpath. */
public class ClientModelCheck {
 private static int parts,vertices;
 private static void check(ModelPart root){root.getAllParts().forEach(p->{for(float v:new float[]{p.x,p.y,p.z,p.xRot,p.yRot,p.zRot,p.xScale,p.yScale,p.zScale})if(!Float.isFinite(v))throw new AssertionError("Nonfinite pose");parts++;});}
 private static class UV implements VertexConsumer {
  public VertexConsumer addVertex(float x,float y,float z){if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(z))throw new AssertionError("Nonfinite vertex");vertices++;return this;}
  public VertexConsumer setColor(int r,int g,int b,int a){return this;}
  public VertexConsumer setUv(float u,float v){if(!Float.isFinite(u)||!Float.isFinite(v)||u<0||u>1||v<0||v>1)throw new AssertionError("UV outside sprite: "+u+","+v);return this;}
  public VertexConsumer setUv1(int u,int v){return this;}
  public VertexConsumer setUv2(int u,int v){return this;}
  public VertexConsumer setNormal(float x,float y,float z){return this;}
 }
 private static void geometry(ModelPart root){var out=new UV();root.visit(new PoseStack(),(pose,path,index,cube)->cube.compile(pose,out,0,0,0xFFFFFFFF));}
 public static void main(String[] args)throws Exception{
  net.minecraft.SharedConstants.tryDetectVersion();
  net.minecraft.server.Bootstrap.bootStrap();
  var nightmare=NightmareModel.createLayer().bakeRoot();var nm=new NightmareModel(nightmare);geometry(nightmare);
  for(int species=-1;species<CreatureEcology.ALL.size();species++)for(int family=0;family<(species<0?7:1);family++)for(boolean charge:new boolean[]{false,true})for(float hit:new float[]{0,.5f,1}){
   nm.animate(species,family,4,.8f,100,25,-18,hit,charge);check(nightmare);
   for(int i=0;i<CreatureEcology.ALL.size();i++)if(nightmare.getChild("species_"+i).visible!=(species==i))throw new AssertionError("Species visibility mismatch");
  }
  var beasts=BeastModel.createLayer().bakeRoot();var bm=new BeastModel(beasts);geometry(beasts);
  for(int family=0;family<7;family++)for(boolean flying:new boolean[]{false,true})for(boolean breathing:new boolean[]{false,true}){
   bm.animate(family,3.4f,.8f,100,23,-14,.6f,flying,breathing);check(beasts);
   for(int i=0;i<7;i++)if(beasts.getChild("beast_"+i).visible!=(i==family))throw new AssertionError("Form visibility mismatch");
   bm.claw(-1);bm.claw(1);
  }
  var mesh=MemoryArmorLayer.class.getDeclaredMethod("mesh",String.class,boolean.class);mesh.setAccessible(true);
  for(String style:new String[]{"hood","knight","raven","antlers","sun","ice","petal","coral","oracle","horns","leaf","nomad","wings","gear","halo"})for(boolean trim:new boolean[]{false,true}){
   var root=((LayerDefinition)mesh.invoke(null,style,trim)).bakeRoot();new HumanoidModel<>(root);check(root);geometry(root);
  }
  System.out.println("PASS: all 32 species, 7 legacy families, 7 beast forms and 30 armor layers; "+parts+" finite part poses; "+vertices+" texture vertices inside their sprite UVs.");
 }
}
