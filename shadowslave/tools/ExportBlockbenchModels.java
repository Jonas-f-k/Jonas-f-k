import com.google.gson.*;
import com.shadowslave.client.*;
import com.shadowslave.entity.CreatureEcology;
import net.minecraft.client.model.geom.ModelPart;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Editable cuboids, bone hierarchy, native UV offsets and sampled animation, not a flattened mesh. */
public class ExportBlockbenchModels {
  private static final Gson JSON=new GsonBuilder().disableHtmlEscaping().create();
  private static final float DEGREES=180/(float)Math.PI;
  private record Bone(String path,ModelPart part,String uuid,float x,float y,float z,float rx,float ry,float rz){}
  private static Object field(Object instance,String name)throws Exception{
    Field f=instance.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(instance);
  }
  private static String uuid(String value){return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString();}
  private static JsonArray array(float...values){var a=new JsonArray();for(float v:values){if(!Float.isFinite(v))throw new AssertionError("Nonfinite exported pose");a.add(Math.round(v*10000)/10000f);}return a;}
  private static JsonObject vector(float x,float y,float z){var v=new JsonObject();v.addProperty("x",String.valueOf(x));v.addProperty("y",String.valueOf(y));v.addProperty("z",String.valueOf(z));return v;}
  @SuppressWarnings("unchecked")
  private static JsonObject bone(String id,String path,ModelPart p,float px,float py,float pz,JsonArray elements,JsonArray groups,List<Bone> bones)throws Exception{
    float x=px+p.x,y=py+p.y,z=pz+p.z;String uid=uuid(id+":"+path);
    bones.add(new Bone(path,p,uid,p.x,p.y,p.z,p.xRot,p.yRot,p.zRot));
    var group=new JsonObject();group.addProperty("name",path.replace('/','_').replace('-','_'));
    group.addProperty("uuid",uid);group.add("origin",array(x,24-y,z));
    group.add("rotation",array(-p.xRot*DEGREES,p.yRot*DEGREES,-p.zRot*DEGREES));
    group.addProperty("color",0);group.addProperty("export",true);group.addProperty("visibility",true);
    groups.add(group.deepCopy());var children=new JsonArray();
    List<ModelPart.Cube> cubes=(List<ModelPart.Cube>)field(p,"cubes");
    for(int i=0;i<cubes.size();i++){
      ModelPart.Cube c=cubes.get(i);var cube=new JsonObject();String cid=uuid(id+":"+path+":"+i);
      cube.addProperty("name",path.replace('/','_')+"_"+i);cube.addProperty("uuid",cid);cube.addProperty("type","cube");
      cube.add("from",array(x+c.minX,24-y-c.maxY,z+c.minZ));cube.add("to",array(x+c.maxX,24-y-c.minY,z+c.maxZ));
      cube.add("origin",array(x,24-y,z));cube.addProperty("box_uv",true);cube.addProperty("autouv",0);cube.addProperty("color",0);
      float u=1,v=1;
      for(Object polygon:(Object[])field(c,"polygons"))for(Object vertex:(Object[])field(polygon,"vertices")){
        u=Math.min(u,(float)field(vertex,"u"));v=Math.min(v,(float)field(vertex,"v"));
      }
      cube.add("uv_offset",array(u*64,v*128));var faces=new JsonObject();
      for(String face:List.of("north","south","east","west","up","down")){
        var f=new JsonObject();f.add("uv",array(0,0,0,0));f.addProperty("texture",0);faces.add(face,f);
      }
      cube.add("faces",faces);cube.addProperty("visibility",!path.endsWith("/eyes")&&!path.endsWith("/runes"));
      elements.add(cube);children.add(cid);
    }
    Map<String,ModelPart> nested=(Map<String,ModelPart>)field(p,"children");
    for(var child:nested.entrySet())children.add(bone(id,path+"/"+child.getKey(),child.getValue(),x,y,z,elements,groups,bones));
    group.add("children",children);return group;
  }
  private interface Animate {void sample(int mode,float time);}
  private static JsonArray animations(String id,List<Bone> bones,Animate animator){
    var result=new JsonArray();String[] names={"idle","walk","windup","strike","flight"};
    for(int mode=0;mode<names.length;mode++){
      var animation=new JsonObject();animation.addProperty("name",names[mode]);animation.addProperty("uuid",uuid(id+":animation:"+mode));
      animation.addProperty("loop",mode==2||mode==3?"once":"loop");animation.addProperty("length",2);animation.addProperty("snapping",12);
      var tracks=new JsonObject();float[][][] poses=new float[bones.size()][25][6];
      for(int frame=0;frame<=24;frame++){
        animator.sample(mode,frame/12f);
        for(int b=0;b<bones.size();b++){
          var bone=bones.get(b);var part=bone.part();
          poses[b][frame]=new float[]{part.x-bone.x(),-(part.y-bone.y()),part.z-bone.z(),-(part.xRot-bone.rx())*DEGREES,(part.yRot-bone.ry())*DEGREES,-(part.zRot-bone.rz())*DEGREES};
        }
      }
      for(int b=0;b<bones.size();b++){
        var keys=new JsonArray();
        for(int channel=0;channel<2;channel++){
          boolean changes=false;
          for(var pose:poses[b])for(int i=0;i<3;i++)if(Math.abs(pose[channel*3+i])>.0001f)changes=true;
          if(!changes)continue;
          for(int frame=0;frame<=24;frame++){
            float[] values=poses[b][frame];int offset=channel*3;var key=new JsonObject();
            key.addProperty("channel",channel==0?"position":"rotation");key.addProperty("time",frame/12f);key.addProperty("interpolation","linear");
            var data=new JsonArray();data.add(vector(values[offset],values[offset+1],values[offset+2]));key.add("data_points",data);keys.add(key);
          }
        }
        if(keys.isEmpty())continue;var track=new JsonObject();track.addProperty("name",bones.get(b).path().replace('/','_').replace('-','_'));track.addProperty("type","bone");track.add("keyframes",keys);tracks.add(bones.get(b).uuid(),track);
      }
      animation.add("animators",tracks);result.add(animation);
    }
    return result;
  }
  private static void export(Path directory,String id,ModelPart rig,String skin,Animate animator)throws Exception{
    rig.getAllParts().forEach(ModelPart::resetPose);var model=new JsonObject();var meta=new JsonObject();
    meta.addProperty("format_version","5.0");meta.addProperty("model_format","modded_entity");meta.addProperty("box_uv",true);model.add("meta",meta);
    model.addProperty("name",id);model.addProperty("model_identifier",id);model.add("resolution",JSON.toJsonTree(Map.of("width",64,"height",128)));
    var elements=new JsonArray();var groups=new JsonArray();var outliner=new JsonArray();var bones=new ArrayList<Bone>();
    outliner.add(bone(id,"root",rig,0,0,0,elements,groups,bones));
    model.add("elements",elements);model.add("groups",groups);model.add("outliner",outliner);
    Path png=Path.of("src/main/resources/assets/shadowslave/textures",skin+".png");var texture=new JsonObject();
    texture.addProperty("name",id+".png");texture.addProperty("id","0");texture.addProperty("uuid",uuid(id+":texture"));
    texture.addProperty("source","data:image/png;base64,"+Base64.getEncoder().encodeToString(Files.readAllBytes(png)));
    texture.addProperty("mode","bitmap");texture.addProperty("width",64);texture.addProperty("height",128);texture.addProperty("uv_width",64);texture.addProperty("uv_height",128);
    var textures=new JsonArray();textures.add(texture);model.add("textures",textures);model.add("animations",animations(id,bones,animator));
    Files.writeString(directory.resolve(id+".bbmodel"),JSON.toJson(model));
  }
  public static void main(String[] args)throws Exception{
    net.minecraft.SharedConstants.tryDetectVersion();Path out=Path.of(args.length>0?args[0]:"build/reports/blockbench-models");Files.createDirectories(out);
    var nightmares=new NightmareModel(NightmareModel.createLayer().bakeRoot());
    for(int i=0;i<CreatureEcology.ALL.size();i++){
      final int index=i;var s=CreatureEcology.ALL.get(i);nightmares.animate(i,s.family(),0,0,0,0,0,0,false);
      export(out,s.id(),nightmares.selectedRig(),"entity/species_"+i,(mode,time)->nightmares.animate(index,s.family(),time*6,mode==1?.75f:0,time*20,0,0,mode==3?Math.max(.001f,time/2):0,mode==2));
    }
    var beasts=new BeastModel(BeastModel.createLayer().bakeRoot());
    for(int i=0;i<7;i++){
      final int family=i;beasts.animate(i,0,0,0,0,0,0,false,false);
      export(out,"form_"+BeastRig.IDS[i],beasts.selectedRig(),"entity/beast_"+i,(mode,time)->beasts.animate(family,time*6,mode==1?.75f:0,time*20,0,0,mode==3?Math.max(.001f,time/2):0,mode==4,mode==2));
    }
    System.out.println("PASS: exported 39 editable Blockbench 5 models with embedded skins, cuboids, bone hierarchy and five sampled animation tracks.");
  }
}
