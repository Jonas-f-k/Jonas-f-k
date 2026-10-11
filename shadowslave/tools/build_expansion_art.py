"""Build native models and the client's texture import manifest; source PNGs are unchanged."""
import json,runpy
from pathlib import Path
runpy.run_path(str(Path(__file__).with_name('build_art_resources.py')))
R=Path(__file__).resolve().parents[1]/'src/main/resources';A=R/'assets/shadowslave';D=R/'data/shadowslave'
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2)+'\n')
names=json.loads(Path('tools/realm_materials.json').read_text());mem=json.loads((D/'spell/catalogue.json').read_text())['memories'];sprites=[]
for i,name in enumerate(names):
 x=round((i%8)*1254/8);y=round((i//8)*1254/8);w=round((i%8+1)*1254/8)-x;h=round((i//8+1)*1254/8)-y
 sprites.append(dict(source='world_material_atlas',sprite='block/'+name,x=x,y=y,width=w,height=h,size=16))
 write(A/f'models/block/{name}.json',{'parent':'minecraft:block/cube_all','textures':{'all':'shadowslave:block/'+name}})
# Layered soil sides: the ground colour appears on top and as a shallow grassy lip, not down the entire cliff.
def cuboid(lo,hi,tex):return {'from':lo,'to':hi,'faces':{f:{'texture':'#'+tex,'uv':[0,0,16,16]} for f in ['up','down','north','south','east','west']}}
for name in ['ashen_grass','sanctuary_moss','meadow_grass','blossom_grass','lavender_grass','autumn_litter','turquoise_moss','frost_grass','scarlet_litter']:
 write(A/f'models/block/{name}.json',{'parent':'minecraft:block/block','textures':{'particle':'shadowslave:block/'+name,'ground':'shadowslave:block/'+name,'soil':'shadowslave:block/'+('snow_soil' if name=='frost_grass' else 'dream_loam')},'elements':[cuboid([0,0,0],[16,13,16],'soil'),cuboid([0,13,0],[16,16,16],'ground')]})
# Each bloom has a different petal arrangement, width and stem height.
blooms=['rose_bloom','sun_bloom','azure_bloom','jade_bloom','violet_bloom','ember_bloom','moon_bloom','cloud_bloom']
petals=['pink_leaves','gold_plate','cyan_crystal','emerald_leaves','lavender_leaves','scarlet_plate','pearl_plate','ice_plate']
for i,name in enumerate(blooms):
 h=6+i%4;r=2+i%3;parts=[cuboid([7,0,7],[9,h,9],'stem'),cuboid([4,3,7],[8,4,9],'stem')]
 for x,z in [(-r,0),(r,0),(0,-r),(0,r)]:parts.append(cuboid([6+x,h-1,6+z],[10+x,h+1+(i%2),10+z],'petal'))
 parts.append(cuboid([7,h,7],[9,h+2,9],'center'))
 write(A/f'models/block/{name}.json',{'parent':'minecraft:block/block','textures':{'particle':'shadowslave:block/'+petals[i],'petal':'shadowslave:block/'+petals[i],'stem':'shadowslave:block/emerald_leaves','center':'shadowslave:block/gold_plate'},'elements':parts})
for name in names+blooms:
 write(A/f'blockstates/{name}.json',{'variants':{'':{'model':'shadowslave:block/'+name}}});write(A/f'models/item/{name}.json',{'parent':'shadowslave:block/'+name})
 write(D/f'loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'shadowslave:'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
# Authored row boundaries account for the tall full-suit illustrations in the armor source.
rows=[0,200,399,546,679,827,974,1108,1254]
for i,m in enumerate(mem):
 j=i%64;row=j//8;col=j%8;x=round(col*1254/8);right=round((col+1)*1254/8)
 y=rows[row] if i<64 else round(row*1254/8);bottom=rows[row+1] if i<64 else round((row+1)*1254/8)
 sprites.append(dict(source='memory_armor_atlas' if i<64 else 'memory_relic_atlas',sprite='item/memories/'+m['id'],x=x,y=y,width=right-x,height=bottom-y,size=32))
 write(A/f'models/item/memories/{m["id"]}.json',{'parent':'minecraft:item/handheld' if m['type']=='weapon' else 'minecraft:item/generated','textures':{'layer0':'shadowslave:item/memories/'+m['id']}})
for item in ['memory','manifested_blade']:
 write(A/f'models/item/{item}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'shadowslave:item/memories/'+('ashen_shroud' if item=='memory' else 'soul_edge')},'overrides':[{'predicate':{'shadowslave:design':m['design']},'model':'shadowslave:item/memories/'+m['id']} for m in mem]})
write(A/'art/sprites.json',sprites);write(R/'assets/minecraft/atlases/blocks.json',{'sources':[{'type':'shadowslave:dream_sprites'}]})
lang=json.loads((A/'lang/en_us.json').read_text())
for name in names+blooms:lang['block.shadowslave.'+name]=name.replace('_',' ').title()
write(A/'lang/en_us.json',lang)
# Useful harvest categories; leaves remain deliberately persistent decorative foliage.
for tag,values in [('axe',[n for n in names if any(w in n for w in ['bark','oak','birch','planks','timber'])]),('hoe',[n for n in names if 'leaves' in n or 'moss' in n]),('shovel',[n for n in names if any(w in n for w in ['grass','soil','loam','litter','sand']) and 'sandstone' not in n])]:
 write(R/f'data/minecraft/tags/block/mineable/{tag}.json',{'replace':False,'values':['shadowslave:'+n for n in values]})
print('Imported 64 block sprites at 16px, 128 unique Memory sprites at 32px; 8 original geometric flowers.')
# Small original furniture and crystal models for landmarks and creative builders.
decor={
'moon_lantern':([([4,0,4],[12,2,12],'brass'),([6,2,6],[10,12,10],'light'),([4,12,4],[12,14,12],'brass'),([7,14,7],[9,16,9],'brass')]),
'runic_basin':([([4,0,4],[12,3,12],'stone'),([6,3,6],[10,7,10],'wood'),([1,7,1],[15,9,15],'stone'),([1,9,1],[3,13,15],'wood'),([13,9,1],[15,13,15],'wood'),([3,9,1],[13,13,3],'wood'),([3,9,13],[13,13,15],'wood'),([3,9,3],[13,10,13],'crystal')]),
'amethyst_cluster':([([2,0,2],[14,2,14],'stone'),([5,2,5],[11,13,11],'purple'),([6,13,6],[10,16,10],'purple'),([1,1,7],[5,8,11],'purple'),([11,1,2],[15,10,6],'purple')]),
'potion_shelf':([([0,0,0],[16,2,16],'wood'),([0,14,0],[16,16,16],'wood'),([0,2,14],[16,14,16],'wood'),([2,2,3],[6,9,7],'crystal'),([3,9,4],[5,12,6],'brass'),([9,2,4],[14,7,9],'purple'),([10,7,5],[13,10,8],'brass')]),
'star_books':([([2,0,3],[14,3,12],'cloth'),([3,3,2],[13,6,13],'purple'),([1,6,4],[15,9,12],'wood'),([3,9,3],[13,12,13],'cloth'),([7,0,2],[9,12,3],'brass')]),
'bloom_pot':([([4,0,4],[12,6,12],'pot'),([3,5,3],[13,7,13],'pot'),([7,6,7],[9,12,9],'wood'),([3,11,5],[13,13,11],'flower'),([5,10,3],[11,12,13],'flower'),([7,13,7],[9,15,9],'light')]),
'lantern_pedestal':([([2,0,2],[14,3,14],'wood'),([5,3,5],[11,12,11],'wood'),([2,12,2],[14,15,14],'stone'),([6,14,6],[10,16,10],'crystal')])}
textures={'wood':'shadowslave:block/runic_timber','brass':'shadowslave:block/moon_brass','light':'shadowslave:block/amber_glass','stone':'shadowslave:block/pale_marble','crystal':'shadowslave:block/emerald_crystal','purple':'shadowslave:block/amethyst_crystal','cloth':'shadowslave:block/navy_cloth','pot':'shadowslave:block/peach_bricks','flower':'shadowslave:block/pink_leaves'}
for name,parts in decor.items():
 write(A/f'models/block/{name}.json',{'parent':'minecraft:block/block','textures':{'particle':textures['wood'],**textures},'elements':[cuboid(lo,hi,tex) for lo,hi,tex in parts]})
 write(A/f'blockstates/{name}.json',{'variants':{'':{'model':'shadowslave:block/'+name}}});write(A/f'models/item/{name}.json',{'parent':'shadowslave:block/'+name})
 write(D/f'loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'shadowslave:'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
 lang['block.shadowslave.'+name]=name.replace('_',' ').title()
write(A/'lang/en_us.json',lang)
for i in range(7):
 x=round((i%4)*1254/4);y=round((i//4)*1254/4)
 sprites.append(dict(source='nightmare_material_atlas',sprite=f'entity/nightmare_{i}',x=x,y=y,width=round((i%4+1)*1254/4)-x,height=round((i//4+1)*1254/4)-y,size=32))
write(A/'art/sprites.json',sprites)
