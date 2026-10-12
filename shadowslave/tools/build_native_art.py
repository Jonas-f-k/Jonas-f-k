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
def shade(color,delta):
 rgb=[max(0,min(255,int(color[i:i+2],16)+delta)) for i in (1,3,5)]
 return '#'+''.join(f'{c:02x}' for c in rgb)
def natural_palette(token):
 presets={
  'ash_stone':('#3f454e','#555d67','#68727b'),
  'fractured_stone':('#393f49','#515963','#646e78'),
  'sanctuary_limestone':('#a5ae9d','#c5ccb9','#dde2d1'),
  'pale_marble':('#a7aca7','#c7cec4','#e0e4d9'),
  'dream_loam':('#49352b','#604737','#775946'),
  'ashen_soil':('#41453f','#535a4f','#656c5d'),
  'blue_shale':('#3e4c5d','#536477','#6b7e90'),
  'drowned_stone':('#3b5057','#536c70','#6c8588'),
  'crystal_rock':('#535068','#6c667f','#888198'),
  'rose_clay':('#865f62','#a27878','#ba9090'),
  'warm_sand':('#a89470','#c4b38d','#dacba5'),
  'golden_sandstone':('#9d835c','#bca17b','#d4bb92'),
  'red_sandstone':('#845a4b','#aa7660','#c89175'),
  'snow_soil':('#aebcc0','#d0dedc','#e6efea'),
  'meadow_grass':('#46663d','#60864b','#769957'),
  'sanctuary_moss':('#45624b','#62845b','#7b9b71'),
  'turquoise_moss':('#355e5b','#4c8374','#63a18d'),
  'ashen_grass':('#525c48','#6d7960','#889477'),
  'blossom_grass':('#667956','#829567','#9dab7f'),
  'lavender_grass':('#635f77','#807c92','#9996a8'),
  'frost_grass':('#829997','#a7bcb0','#c1d2c2'),
  'scarlet_litter':('#74413f','#95554e','#b8755e'),
  'autumn_litter':('#7d6844','#9b8959','#bba36b'),
 }
 return presets.get(token)
def natural_tile(token,seed):
 p=natural_palette(token)
 if p:
  dark,mid,light=p;parts=[rect(0,0,16,16,mid)]
  grass=any(k in token for k in ['grass','moss','litter'])
  for i in range(28):
   bits=(seed*1664525+i*1013904223)&0xffffffff;x=1+bits%13;y=1+(bits>>8)%13
   color=shade(mid,(-7,-4,4,7)[i%4])
   parts.append(rect(x,y,1+i%2,1+(i//4)%2,color))
   if grass and i%5==0:parts += [rect(x,y,1,2,dark),rect(x+1,y,1,1,light)]
  if 'stone' in token or 'shale' in token or 'rock' in token or 'marble' in token:
   for i in range(4):
    x=2+(seed>>(i*4))%9;y=2+(seed>>(i*5+1))%10
    parts += [rect(x,y,3,1,shade(mid,-10)),rect(x+2,y+1,2,1,shade(mid,-6))]
  parts += [rect(0,0,16,1,mid),rect(0,15,16,1,mid),rect(0,0,1,16,mid),rect(15,0,1,16,mid)]
  return parts
 if token.endswith('_leaves'):
  base={'golden':'#b09b56','pink':'#b9809a','crimson':'#91505e','emerald':'#5c855c','teal':'#4d897d','cyan':'#669f9d','lavender':'#8b84a3','frost':'#9dbab2'}.get(token.split('_')[0],'#65825d')
  parts=[]
  for x,y in [(0,0),(5,1),(11,0),(2,5),(8,5),(13,6),(0,11),(5,12),(11,11)]:
   parts += [rect(x,y,4,4,shade(base,-12)),rect(x+1,y,3,3,base),rect(x+1,y,2,1,shade(base,14)),rect(x+2,y+3,1,1,shade(base,-3))]
  return parts
 if token in ['dream_oak','dream_birch','blue_bark','rose_bark','redwood_bark','runic_timber']:
  base={'dream_oak':'#70573d','dream_birch':'#c4c9b9','blue_bark':'#657d80','rose_bark':'#9b767c','redwood_bark':'#7d4d3f','runic_timber':'#68634a'}[token]
  parts=[rect(0,0,16,16,base)]
  for i,x in enumerate([1,4,7,11,14]):
   parts += [rect(x,0,1,16,shade(base,-13-i%2*6)),rect(x+1,0,1,16,shade(base,9))]
   for j in range(3):parts.append(rect(x-1,2+j*5+(i%2),2,2,shade(base,-5)))
  if token=='dream_birch':
   for x,y,w in [(2,3,4),(10,6,5),(4,11,3),(12,14,3)]:parts.append(rect(x,y,w,1,'#656a60'))
  return parts
 return None
def export(name,size,parts):
 w,h=size if isinstance(size,tuple) else (size,size)
 svg=f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}" shape-rendering="crispEdges">'+''.join(parts)+'</svg>'
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
  ident=species_ids[n] if token.startswith('species_') else (['dragon','moonveil_wraith','leviathan','colossus','stag','griffin','wyvern'][n%7] if token.startswith('nightmare_') else ['dragon','wolf','leviathan','colossus','stag','griffin','wyvern','trim'][n%8])
  dark,mid,light,accent=palettes[family]
  if ident=='forsaken_knight':dark,mid,light,accent=['#1b2029','#414c60','#9eafc3','#c49770']
  elif ident=='stone_saint':dark,mid,light,accent=['#222a32','#56626d','#a4b4bf','#a6ba91']
  elif ident=='carapace_demon':dark,mid,light,accent=['#323e47','#9eaeb9','#d6e2e4','#faf4dc']
  elif ident=='carapace_centurion':dark,mid,light,accent=['#211f27','#46323b','#98666b','#dfcfab']
  elif ident=='carapace_scavenger':dark,mid,light,accent=['#35272a','#805b52','#c79978','#d7b896']
  elif ident=='soul_devouring_tree':dark,mid,light,accent=['#40312a','#715045','#a44e4d','#e0ae74']
  elif ident=='lord_of_dead':dark,mid,light,accent=['#555047','#a49b83','#d2c6a6','#e8debf']
  elif ident=='mordant_mimic':dark,mid,light,accent=['#31272c','#725b43','#927c5d','#d9c7a9']
  elif ident in ['dragon','cinder_hound']:dark,mid,light,accent=['#30272b','#6d4542','#b66d51','#e8bc79']
  parts=[]
  furry=any(k in ident for k in ['wolf','hound','hart','ravager','stalker'])
  woody=any(k in ident for k in ['tree','stag','briar','flower'])
  metallic=any(k in ident for k in ['iron','knight','saint','colossus','stone','carapace','mimic'])
  # Match CreatureRig's explicit material bands, including small joints and tips.
  # Details cover each region; no anatomical part relies on a tiny shared corner.
  plate=light if ident=='carapace_demon' else '#a44e4d' if ident=='soul_devouring_tree' else shade(mid,22 if metallic else 13)
  for band,base in enumerate([mid,dark,plate,shade(light,-10) if metallic else accent]):
   offset=band*32;parts.append(rect(0,offset,64,32,base))
   for yy in range(0,32,4):
    for x in range(0,64,6):
     y=offset+yy;xx=x+(2 if yy%8 else 0)
     if band==1:parts += [rect(x,y,3,1,shade(base,8)),rect(x+3,y+2,2,1,shade(base,-5)),rect(x+4,y+3,1,1,shade(base,13))]
     elif band==3:parts += [rect(x,y+2,3,1,shade(base,-12)),rect(x+1,y,1,2,shade(base,10))]
     elif furry:parts += [poly(f'{xx},{y} {xx+3},{y} {xx+2},{y+3} {xx+1},{y+4}',shade(base,-10)),rect(xx+1,y,1,1,shade(base,8))]
     elif woody:parts += [rect(xx,y,1,4,shade(base,-13)),rect(xx+2,y+1,1,2,shade(base,9))]
     elif metallic:parts += [rect(x+2,y+1,2,1,shade(base,-9)),rect(x+1,y+2,1,1,shade(base,7))]
     else:parts += [poly(f'{xx},{y} {xx+3},{y} {xx+2},{y+2} {xx+1},{y+3}',shade(base,-9)),rect(xx+1,y,1,1,shade(base,7))]
   if band==2:
    for i in range(5):parts += [rect((seed//(i+1))%58+2,offset+3+i*5,1,1,shade(base,14))]
  export(name,(64,128),parts);continue
 if name.startswith('armor/'):
  index=int(token.split('_')[-1]);m=cat[index];c=f'#{m["color"]:06x}';a=f'#{m["trim"]:06x}'
  parts=[rect(0,0,64,64,c)]
  for y in range(0,64,16):parts += [rect(0,y,64,2,a),rect(0,y+14,64,2,'#25303d'),rect(4,y+4,8,7,a),rect(5,y+5,6,5,c)]
  export(name,64,parts);continue
 if name.startswith('flora/flower_'):
  index=int(token.split('_')[-1]);color=['#cc7292','#d9bb64','#78b6c4','#85b37a','#b095cc','#d89c67','#c4dbe1','#e1dfd0','#b66872','#d285a1','#bfb0d6','#a7cdd5','#d1b9cc','#d6ad59','#d5a3b8','#a08bbb'][index%16]
  parts=[rect(7,6,1,10,'#4d704d'),rect(8,7,1,9,'#63815a'),rect(4,11,3,1,'#63815a'),rect(5,12,2,1,'#4d704d'),rect(9,9,3,1,'#7b9868')]
  if index==15:
   for y in [2,5,8]:parts += [rect(5,y,4,2,color),rect(6,y+1,2,1,shade(color,-24)),rect(5,y,2,1,shade(color,17))]
  elif index%4==0:parts += [rect(5,3,6,4,color),rect(6,2,4,6,color),rect(5,3,2,2,shade(color,18)),rect(7,4,2,2,'#d6bf8a')]
  elif index%4==1:parts += [rect(4,3,3,3,color),rect(9,3,3,3,color),rect(6,1,4,3,color),rect(6,6,4,2,color),rect(6,3,4,3,'#a58453'),rect(7,3,2,1,'#d6c68e')]
  elif index%4==2:parts += [rect(6,2,4,5,color),rect(5,4,6,2,color),rect(6,2,2,2,shade(color,20)),rect(7,6,2,1,shade(color,-23))]
  else:parts += [rect(5,4,6,2,color),rect(6,2,4,4,color),rect(7,1,2,1,shade(color,15)),rect(6,4,2,1,shade(color,22))]
  export(name,16,parts);continue
 # Blocks / foliage / plants: compact hand-designed pixel clusters, no image-atlas sampling.
 natural=natural_tile(token,seed)
 if name.startswith('block/') and natural is not None:
  export(name,16,natural);continue
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
 name='block/'+token+'_side';p=natural_palette(token);parts=natural_tile('dream_loam',int(hashlib.sha256(token.encode()).hexdigest()[:8],16))
 parts += [rect(0,0,16,3,p[1])]
 for x in range(16):
  depth=1+(x*7+len(token))%3
  parts += [rect(x,3,1,depth,p[1]),rect(x,0,1,1,shade(p[1],7)),rect(x,3+depth,1,1,shade(p[1],-10))]
 export(name,16,parts)
 m.write_text(json.dumps({'parent':'minecraft:block/cube_bottom_top','textures':{'top':'shadowslave:block/'+token,'side':'shadowslave:'+name,'bottom':'shadowslave:block/dream_loam'}},indent=2)+'\n')
# Ensure generated trees use cutout foliage on both graphics presets.
for token in ['dream_oak','dream_birch','blue_bark','rose_bark','redwood_bark','runic_timber']:
 name='block/'+token+'_top';base={'dream_oak':'#9c825b','dream_birch':'#bdbaa0','blue_bark':'#8fa99e','rose_bark':'#bba397','redwood_bark':'#ae805b','runic_timber':'#969574'}[token]
 parts=[rect(0,0,16,16,shade(base,-25))]
 for inset in [1,3,5,7]:parts += [rect(inset,inset,16-inset*2,16-inset*2,shade(base,-12+inset*2)),rect(inset,inset,16-inset*2,1,shade(base,9))]
 export(name,16,parts)
 (A/f'models/block/{token}.json').write_text(json.dumps({'parent':'minecraft:block/cube_column','textures':{'side':'shadowslave:block/'+token,'end':'shadowslave:'+name}},indent=2)+'\n')
for m in (A/'models/block').glob('*_leaves.json'):
 data=json.loads(m.read_text());data['render_type']='minecraft:cutout_mipped';m.write_text(json.dumps(data,indent=2)+'\n')
for m in (A/'models/block').glob('*_bloom.json'):
 data=json.loads(m.read_text());data['render_type']='minecraft:cutout';m.write_text(json.dumps(data,indent=2)+'\n')
(A/'art/native_sprites.json').write_text(json.dumps(textures,indent=2)+'\n')
(R/'assets/minecraft/atlases/blocks.json').write_text(json.dumps({'sources':[{'type':'minecraft:single','resource':'shadowslave:'+n} for n in textures]},indent=2)+'\n')
(A/'art/asset_credits.json').write_text(json.dumps({'author':'Malcolm Riley','project':'https://github.com/malcolmriley/unused-textures','revision':'9218cb8997bf1dc83617bd7748acf44a16c19c42','license':'CC BY 4.0','assets':credits},indent=2)+'\n')
license_dest=R/'META-INF/licenses';license_dest.mkdir(parents=True,exist_ok=True)
if (up/'LICENSE').exists():shutil.copyfile(up/'LICENSE',license_dest/'MalcolmRiley-CC-BY-4.0.txt')
(ROOT/'ASSET_CREDITS.md').write_text('# Asset credits\n\n'+f'{len(credits)} unchanged sprites by **Malcolm Riley**, from [Unused Textures](https://github.com/malcolmriley/unused-textures), revision `9218cb8997bf1dc83617bd7748acf44a16c19c42`. Licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Original pixels and available animation metadata are retained. Exact file mapping and SHA-256 values are in `src/main/resources/assets/shadowslave/art/asset_credits.json`; license text ships in `META-INF/licenses`.\n\nAll other new textures and all rigs are original code/vector artwork for this rework. Their editable SVGs are under `art_source/native`. No assets or code were copied from the other reference mods.\n')
print(f'Generated {len(textures)} native sprites; {len(credits)} unchanged attributed upstream assets.')
