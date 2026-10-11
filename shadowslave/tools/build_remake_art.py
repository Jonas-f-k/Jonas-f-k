"""Register immutable imagegen sources as native sprite-atlas regions and flower models."""
from pathlib import Path
import json,struct
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/shadowslave';D=ROOT/'src/main/resources/data/shadowslave'
manifest=json.loads((A/'art/sprites.json').read_text())
for source,prefix in [('wearable_material_atlas','armor/material_'),('botanical_atlas','flora/flower_')]:
 raw=(A/f'textures/art/{source}.png').read_bytes();w,h=struct.unpack('>II',raw[16:24]);manifest=[m for m in manifest if m['source']!=source]
 for i in range(16):
  col,row=i%4,i//4;x,y=round(col*w/4),round(row*h/4);right,bottom=round((col+1)*w/4),round((row+1)*h/4)
  manifest.append(dict(source=source,sprite=prefix+str(i),x=x,y=y,width=right-x,height=bottom-y,size=16 if source.startswith('wearable') else 32))
source='creature_material_atlas';raw=(A/f'textures/art/{source}.png').read_bytes();w,h=struct.unpack('>II',raw[16:24]);manifest=[m for m in manifest if m['source']!=source]
for i in range(32):
 col,row=i%8,i//8;x,y=round(col*w/8),round(row*h/4);right,bottom=round((col+1)*w/8),round((row+1)*h/4)
 manifest.append(dict(source=source,sprite='entity/species_'+str(i),x=x,y=y,width=right-x,height=bottom-y,size=16))
source='beast_material_atlas';path=A/f'textures/art/{source}.png'
if path.exists():
 raw=path.read_bytes();w,h=struct.unpack('>II',raw[16:24]);manifest=[m for m in manifest if m['source']!=source]
 for i in range(8):
  col,row=i%4,i//4;x,y=round(col*w/4),round(row*h/2);right,bottom=round((col+1)*w/4),round((row+1)*h/2)
  manifest.append(dict(source=source,sprite='entity/beast_'+str(i),x=x,y=y,width=right-x,height=bottom-y,size=32))
(A/'art/sprites.json').write_text(json.dumps(manifest,indent=2)+'\n')
blooms=['rose_bloom','sun_bloom','azure_bloom','jade_bloom','violet_bloom','ember_bloom','moon_bloom','cloud_bloom','crimson_bloom','hibiscus_bloom','crocus_bloom','frost_bloom','lotus_bloom','marigold_bloom','peony_bloom','foxglove_bloom']
for i,name in enumerate(blooms):
 for path,data in [(A/f'models/block/{name}.json',{'parent':'minecraft:block/cross','render_type':'minecraft:cutout','textures':{'cross':f'shadowslave:flora/flower_{i}'}}),(A/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'shadowslave:flora/flower_{i}'}}),(A/f'blockstates/{name}.json',{'variants':{'':{'model':f'shadowslave:block/{name}'}}}),(D/f'loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'shadowslave:'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})]:path.write_text(json.dumps(data,indent=2)+'\n')
for f in (A/'lang').glob('*.json'):
 lang=json.loads(f.read_text());lang.update({'block.shadowslave.'+n:n.replace('_',' ').title() for n in blooms});f.write_text(json.dumps(lang,indent=2,ensure_ascii=False)+'\n')
