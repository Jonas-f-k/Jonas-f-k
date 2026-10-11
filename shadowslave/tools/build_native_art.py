"""Generate original pixel SVGs/PNGs and import credited upstream sprites unchanged.
Usage: python tools/build_native_art.py /path/to/malcolmriley/unused-textures
Requires cairosvg. No illustrated source atlas is sampled or edited.
"""
from pathlib import Path
import json, hashlib, shutil, sys
import cairosvg
ROOT=Path(__file__).resolve().parents[1]; R=ROOT/'src/main/resources'; A=R/'assets/shadowslave'
SRC=ROOT/'art_source/native'; SRC.mkdir(parents=True,exist_ok=True)
manifest=json.loads((ROOT/'tools/legacy_sprite_layout.json').read_text())
cat=json.loads((R/'data/shadowslave/spell/catalogue.json').read_text())['memories']
mem={m['id']:m for m in cat}
up=Path(sys.argv[1]) if len(sys.argv)>1 else ROOT/'art_source/upstream'
credits=[]; textures=[]
# Cohesive regional palettes, dark base / midtone / edge / material accent.
palettes=[['#282c36','#464653','#727482','#c69264'],['#252535','#414453','#747989','#bcbad9'],['#183941','#28646a','#52959a','#afd2c5'],['#30353a','#58636a','#8b9799','#b8a26b'],['#293d2c','#536c49','#8ca370','#d7c6a0'],['#48435a','#81768e','#c8bdc9','#e5d6ae'],['#28293e','#4c526c','#858ca9','#c5d2e4']]
regions={'frostglass_taiga':3,'aurora_fields':6,'amethyst_heights':5,'hollow_mountains':3,'death_zone':0,'nightmare_desert':0,'scarlet_wilds':0,'forgotten_shore':3,'chained_isles':1,'azure_woods':2,'moonlit_grove':5,'golden_grove':4,'verdant_gardens':4,'mosslight_ravine':4}
def rect(x,y,w,h,c):return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{c}"/>'
def poly(points,c):return f'<polygon points="{points}" fill="{c}"/>'
def export(name,size,parts):
 svg=f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 {size} {size}" shape-rendering="crispEdges">'+''.join(parts)+'</svg>'
 source=SRC/(name.replace('/','_')+'.svg');source.write_text(svg)
 dest=A/'textures'/f'{name}.png';dest.parent.mkdir(parents=True,exist_ok=True)
 cairosvg.svg2png(bytestring=svg.encode(),write_to=str(dest))
 textures.append(name)
def copied(name,path):
 dest=A/'textures'/f'{name}.png';dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(path,dest)
 metas=[Path(str(path)+'.mcmeta'),path.with_suffix('.mcmeta')]
 for meta in metas:
  if meta.exists():shutil.copyfile(meta,Path(str(dest)+'.mcmeta'));break
 credits.append({'sprite':name,'source':str(path.relative_to(up)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest()});textures.append(name)
# Several hand-selected existing materials, retaining their exact original pixels.
imports={'block/ash_bricks':'blocks/sturdy_bricks.png','block/ash_tiles':'blocks/sturdy_brick_tiles.png','block/fossil_bone':'blocks/ore_stone_bones.png','block/petrified_bark':'blocks/log_cronetree_sides.png','block/mossy_ruin_stone':'blocks/stone_tiles_small_mossy.png','block/silver_rune':'blocks/overlay_rune_4.png','block/honey_planks':'blocks/plank_plywood.png'}
# Opaque tile is required for the silver-rune block, so use the complete runed metal tile.
imports['block/silver_rune']='blocks/block_metal_aurichalcum_rune_4.png'
items={'weapon':sorted((up/'items').glob('tool_greatsword_*.png')),'head':sorted((up/'items').glob('armor_helmet_*.png')),'chest':sorted((up/'items').glob('armor_chest_*.png')),'legs':sorted((up/'items').glob('armor_legs_*.png')),'feet':sorted((up/'items').glob('armor_boots_*.png')),'charm':sorted((up/'items').glob('artifact_*ring*.png')),'utility':sorted((up/'items').glob('artifact_dripping_orb_*.png'))}
used=set();species_text=(ROOT/'src/main/java/com/shadowslave/entity/CreatureEcology.java').read_text()
import re
species_ids=re.findall(r's\(\s*"([a-z_]+)",',species_text)
families=[int(x) for x in re.findall(r's\(\s*"[a-z_]+",\s*"[^"]+",\s*\d+,\s*\d+,\s*(\d+),',species_text)]
for entry in manifest:
 name=entry['sprite'];token=name.split('/')[-1];seed=int(hashlib.sha256(name.encode()).hexdigest()[:8],16)
 if name in imports and (up/imports[name]).exists():copied(name,up/imports[name]);continue
 if name.startswith('item/memories/'):
  m=mem[token];pool=items.get(m['type'],[]);candidate=next((p for p in pool if p not in used),None)
  if candidate:copied(name,candidate);used.add(candidate);continue
  cols=palettes[regions.get(m['biome'],m['rank']%7)]; dark,mid,light,accent=cols
  parts=[];k=m['design'];t=m['type'];c=f'#{m["color"]:06x}';a=f'#{m["trim"]:06x}'
  if t=='weapon':
   parts=[poly(f'14,3 18,3 20,{10+k%8} 18,24 14,24 12,{10+k%8}',dark),rect(14,4,3,19,light),rect(17,5,2,16,c),rect(10-k%3,22,12+2*(k%3),3,a),rect(15,25,3,5,dark),rect(14,29,5,2,a)]
  elif t in ['armor','chest']:
   parts=[poly('7,7 12,5 14,9 18,9 20,5 25,7 28,15 24,17 24,28 8,28 8,17 4,15',dark),rect(9,10,14,16,c),rect(11,11,3,12,light),rect(9,23,14,3,a),rect(5,8,5,6,a),rect(22,8,5,6,a)]
  elif t=='head':parts=[poly('7,25 7,11 11,6 21,6 25,11 25,25 21,25 21,20 11,20 11,25',dark),rect(9,11,14,9,c),rect(10,9,12,3,light),rect(11,17,10,2,a)]
  elif t=='legs':parts=[rect(8,5,16,5,a),rect(9,10,6,19,c),rect(18,10,6,19,c),rect(10,12,2,13,light),rect(19,12,2,13,light),rect(9,26,6,3,dark),rect(18,26,6,3,dark)]
  elif t=='feet':parts=[rect(6,7,7,16,c),rect(19,7,7,16,c),rect(3,22,10,6,dark),rect(16,22,10,6,dark),rect(6,8,7,3,a),rect(19,8,7,3,a),rect(7,12,2,10,light),rect(20,12,2,10,light)]
  elif t=='charm':parts=[poly('16,4 26,16 16,29 6,16',dark),poly('16,8 22,16 16,24 10,16',a),rect(14,12,4,8,c),rect(14,12,2,4,light)]
  else:parts=[rect(10,7,12,3,a),rect(11,10,10,5,dark),poly('11,15 7,24 10,29 22,29 25,24 21,15',c),rect(11,17,3,9,light),rect(10,28,12,2,a)]
  # A tiny carved rune gives every generated Memory its own silhouette/detail signature.
  for bit in range(7):
   if k&(1<<bit):parts.append(rect(8+bit*2,3+(k%3),1,2,a))
  export(name,32,parts);continue
 if name.startswith('entity/'):
  n=int(token.split('_')[-1]);family=families[n] if token.startswith('species_') else n%7
  dark,mid,light,accent=palettes[family];parts=[rect(0,0,64,64,dark)]
  ident=species_ids[n] if token.startswith('species_') else ['dragon','wolf','leviathan','colossus','stag','griffin','wyvern','trim'][n%8]
  furry=any(k in ident for k in ['wolf','hound','hart','ravager','stalker'])
  woody=any(k in ident for k in ['tree','stag','briar','flower'])
  metallic=any(k in ident for k in ['iron','knight','saint','colossus','stone','carapace','mimic'])
  for y in range(0,64,8):
   for x in range(-4,64,12):
    xx=x+(4 if y%16 else 0)
    if furry:parts += [poly(f'{xx},{y} {xx+9},{y} {xx+7},{y+5} {xx+3},{y+8}',mid),rect(xx+2,y+1,5,1,light)]
    elif woody:parts += [rect(xx,y,3,8,mid),rect(xx+3,y,1,6,light),rect(xx+7,y+3,3,5,mid)]
    elif metallic:parts += [rect(xx,y+1,10,6,mid),rect(xx+1,y+1,8,1,light),rect(xx+1,y+5,8,1,dark)]
    else:parts += [poly(f'{xx},{y+1} {xx+10},{y+1} {xx+8},{y+6} {xx+2},{y+6}',mid),rect(xx+2,y+1,6,1,light)]
  for i in range(4):parts.append(rect((seed//(i+1))%28+4,8+i*12,4,2,accent))
  export(name,64,parts);continue
 if name.startswith('armor/'):
  index=int(token.split('_')[-1]);m=cat[index];c=f'#{m["color"]:06x}';a=f'#{m["trim"]:06x}'
  parts=[rect(0,0,64,64,c)]
  for y in range(0,64,16):parts += [rect(0,y,64,2,a),rect(0,y+14,64,2,'#25303d'),rect(4,y+4,8,7,a),rect(5,y+5,6,5,c)]
  export(name,64,parts);continue
 # Blocks / foliage / plants: compact hand-designed pixel clusters, no image-atlas sampling.
 lower=token.lower();family=4 if any(s in lower for s in ['moss','meadow','emerald','leaf','runic']) else 2 if any(s in lower for s in ['azure','cyan','blue','teal','turquoise','drowned']) else 5 if any(s in lower for s in ['pink','rose','lavender','amethyst','plum','pearl']) else 0 if any(s in lower for s in ['ash','scarlet','crimson','red','ember']) else 3
 dark,mid,light,accent=palettes[family]
 if any(s in lower for s in ['gold','sun','honey','warm','autumn','bronze']):dark,mid,light,accent=['#6b5535','#aa8950','#d4b779','#f1d594']
 if any(s in lower for s in ['snow','frost','ice']):dark,mid,light,accent=['#88a1b6','#b9ccd1','#e4eeeb','#eff7f2']
 if lower.endswith('_leaves'):
  parts=[]
  for x,y in [(0,1),(6,0),(11,2),(2,7),(9,6),(0,12),(6,12),(12,11)]:parts+=[rect(x,y,4,3,dark),rect(x+1,y,3,2,mid),rect(x+1,y,2,1,light)]
 elif lower.endswith('_bloom'):
  parts=[rect(7,6,2,10,dark),rect(3,11,4,2,mid),rect(9,9,4,2,mid),rect(4,3,8,4,accent),rect(6,1,4,8,accent),rect(7,3,2,3,light)]
 else:
  parts=[rect(0,0,16,16,mid)]
  if any(s in lower for s in ['bark','oak','birch','timber','plank']):
   parts += [rect(3,0,2,16,dark),rect(10,0,2,16,dark),rect(5,0,1,16,light),rect(12,0,1,16,light)]
  elif any(s in lower for s in ['plate','iron','brass','bronze','glass','crystal','rune']):
   parts += [rect(0,0,16,1,light),rect(0,0,1,16,light),rect(0,15,16,1,dark),rect(15,0,1,16,dark),rect(4,4,8,1,accent),rect(7,5,2,6,accent)]
  else:
   for i in range(6):
    x=2+(seed>>(i*3))%9;y=2+(seed>>(i*2))%9
    parts.append(rect(x,y,3+i%2,2,light if i%2 else dark))
  # Preserve uniform edge colors on natural top tiles to prevent visible grid seams.
  parts += [rect(0,0,16,1,mid),rect(0,15,16,1,mid),rect(0,0,1,16,mid),rect(15,0,1,16,mid)]
 export(name,16,parts)
# Real full cubes with matching top, soil underside and a separately authored turf side.
for token in ['ashen_grass','meadow_grass','blossom_grass','lavender_grass','frost_grass','scarlet_litter','autumn_litter','turquoise_moss','sanctuary_moss']:
 m=A/f'models/block/{token}.json'
 if not m.exists():continue
 name='block/'+token+'_side';p=palettes[4 if token in ['meadow_grass','sanctuary_moss'] else 5 if 'blossom' in token or 'lavender' in token else 3]
 export(name,16,[rect(0,0,16,16,'#524337'),rect(0,0,16,3,p[1]),rect(2,3,3,2,p[1]),rect(9,3,4,1,p[1]),rect(3,8,4,2,'#6b5744'),rect(10,12,3,2,'#6b5744')])
 m.write_text(json.dumps({'parent':'minecraft:block/cube_bottom_top','textures':{'top':'shadowslave:block/'+token,'side':'shadowslave:'+name,'bottom':'shadowslave:block/dream_loam'}},indent=2)+'\n')
# Ensure generated trees use cutout foliage on both graphics presets.
for m in (A/'models/block').glob('*_leaves.json'):
 data=json.loads(m.read_text());data['render_type']='minecraft:cutout_mipped';m.write_text(json.dumps(data,indent=2)+'\n')
(A/'art/native_sprites.json').write_text(json.dumps(textures,indent=2)+'\n')
(R/'assets/minecraft/atlases/blocks.json').write_text(json.dumps({'sources':[{'type':'minecraft:single','resource':'shadowslave:'+n} for n in textures]},indent=2)+'\n')
(A/'art/asset_credits.json').write_text(json.dumps({'author':'Malcolm Riley','project':'https://github.com/malcolmriley/unused-textures','revision':'9218cb8997bf1dc83617bd7748acf44a16c19c42','license':'CC BY 4.0','assets':credits},indent=2)+'\n')
license_dest=R/'META-INF/licenses';license_dest.mkdir(parents=True,exist_ok=True)
if (up/'LICENSE').exists():shutil.copyfile(up/'LICENSE',license_dest/'MalcolmRiley-CC-BY-4.0.txt')
(ROOT/'ASSET_CREDITS.md').write_text('# Asset credits\n\n'+f'{len(credits)} unchanged sprites by **Malcolm Riley**, from [Unused Textures](https://github.com/malcolmriley/unused-textures), revision `9218cb8997bf1dc83617bd7748acf44a16c19c42`. Licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Original pixels and available animation metadata are retained. Exact file mapping and SHA-256 values are in `src/main/resources/assets/shadowslave/art/asset_credits.json`; license text ships in `META-INF/licenses`.\n\nAll other new textures and all rigs are original code/vector artwork for this rework. Their editable SVGs are under `art_source/native`. No assets or code were copied from the other reference mods.\n')
print(f'Generated {len(textures)} native sprites; {len(credits)} unchanged attributed upstream assets.')
