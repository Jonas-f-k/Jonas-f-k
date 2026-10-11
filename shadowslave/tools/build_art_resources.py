"""Build code-native models and sprite mappings; never modifies the source paintings."""
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
R = ROOT / 'src/main/resources'
A = R / 'assets/shadowslave'
D = R / 'data/shadowslave'
NAMES = ['ash_stone','ash_bricks','fractured_stone','ash_tiles','sanctuary_limestone','aged_bronze','amber_glass','silver_rune','ashen_soil','ashen_grass','sanctuary_moss','petrified_bark','black_iron','fossil_bone','ember_obsidian','drowned_stone']

def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2)+'\n')

def element(lo, hi, tex, rotation=None):
    data={'from':lo,'to':hi,'faces':{face:{'uv':[0,0,16,16],'texture':'#'+tex} for face in ['north','south','east','west','up','down']}}
    if rotation: data['rotation']=rotation
    return data

def model(name, elements, textures, parent='minecraft:block/block'):
    write(A/f'models/block/{name}.json',{'parent':parent,'textures':{'particle':next(iter(textures.values())),**textures},'elements':elements})

# Minecraft's native unstitcher chooses a 256px interior from each of the sixteen
# painted panels. Power-of-two sprites retain the user's normal mipmap setting.
regions=[]
for i,name in enumerate(NAMES):
    regions.append({'sprite':f'shadowslave:block/{name}','x':round((i%4)*313.5+28.75),'y':round((i//4)*313.5+28.75),'width':256,'height':256})
    write(A/f'models/block/{name}.json',{'parent':'minecraft:block/cube_all','textures':{'all':f'shadowslave:block/{name}'}})
write(R/'assets/minecraft/atlases/blocks.json',{'sources':[{'type':'minecraft:unstitch','resource':'shadowslave:art/world_material_atlas','divisor_x':1254,'divisor_y':1254,'regions':regions}]})

tex={'stone':'shadowslave:block/ash_tiles','metal':'shadowslave:block/aged_bronze','light':'shadowslave:block/amber_glass','rune':'shadowslave:block/silver_rune'}
lantern=[element([3,0,3],[13,2,13],'metal'),element([5,2,5],[11,13,11],'light'),element([3,13,3],[13,15,13],'metal'),element([7,15,7],[9,16,9],'metal')]
for x in [4,11]:
    for z in [4,11]:lantern.append(element([x,2,z],[x+1,13,z+1],'metal'))
model('amber_lantern',lantern,tex)
model('trial_altar',[element([0,0,0],[16,4,16],'stone'),element([3,4,3],[13,11,13],'metal'),element([1,11,1],[15,13,15],'stone'),element([3,13,3],[13,14,13],'rune')],tex)
for name,core in [('gateway','light'),('trial_exit','rune')]:
    parts=[element([0,0,0],[16,3,16],'stone'),element([5,3,5],[11,13,11],core),element([2,13,2],[14,16,14],'metal')]
    for x in [3,11]:
        for z in [3,11]:parts.append(element([x,3,z],[x+2,13,z+2],'metal'))
    model(name,parts,tex)

all_blocks=NAMES+['amber_lantern','trial_altar','gateway','trial_exit']
for name in all_blocks:
    write(A/f'blockstates/{name}.json',{'variants':{'':{'model':f'shadowslave:block/{name}'}}})
    write(A/f'models/item/{name}.json',{'parent':f'shadowslave:block/{name}'})
    if name not in ['trial_altar','gateway','trial_exit']:
        write(D/f'loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'shadowslave:{name}'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})

for name,parent in [('ash_slab','slab'),('ash_slab_top','slab_top'),('ash_stairs','stairs'),('ash_stairs_inner','inner_stairs'),('ash_stairs_outer','outer_stairs')]:
    write(A/f'models/block/{name}.json',{'parent':f'minecraft:block/{parent}','textures':{key:'shadowslave:block/ash_tiles' for key in ['bottom','top','side']}})
write(A/'blockstates/ash_slab.json',{'variants':{'type=bottom':{'model':'shadowslave:block/ash_slab'},'type=top':{'model':'shadowslave:block/ash_slab_top'},'type=double':{'model':'shadowslave:block/ash_tiles'}}})
# Vanilla stair geometry conventions, authored blockstate rotations.
variants={}
for facing,rot in [('east',0),('south',90),('west',180),('north',270)]:
    for half in ['bottom','top']:
        for shape in ['straight','inner_left','inner_right','outer_left','outer_right']:
            y=rot+(270 if shape.endswith('left') else 0)+(90 if half=='top' and shape!='straight' else 0)
            modelname='ash_stairs'+('_inner' if shape.startswith('inner') else '_outer' if shape.startswith('outer') else '')
            entry={'model':f'shadowslave:block/{modelname}','y':y%360,'uvlock':True}
            if half=='top':entry['x']=180
            variants[f'facing={facing},half={half},shape={shape}']=entry
write(A/'blockstates/ash_stairs.json',{'variants':variants})
for name in ['ash_stairs','ash_slab']:
    write(A/f'models/item/{name}.json',{'parent':f'shadowslave:block/{name}'})
    entry={'type':'minecraft:item','name':f'shadowslave:{name}'}
    if name=='ash_slab':entry['functions']=[{'function':'minecraft:set_count','count':2,'conditions':[{'condition':'minecraft:block_state_property','block':'shadowslave:ash_slab','properties':{'type':'double'}}]}]
    write(D/f'loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[entry],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})

# Original small, three-dimensional relics use the same painted palette.
display={'gui':{'rotation':[15,-30,0],'translation':[0,0,0],'scale':[.85,.85,.85]},'ground':{'translation':[0,3,0],'scale':[.35,.35,.35]},'fixed':{'rotation':[0,180,0],'scale':[.65,.65,.65]},'thirdperson_righthand':{'rotation':[0,-90,45],'translation':[0,2,1],'scale':[.75,.75,.75]},'firstperson_righthand':{'rotation':[0,-90,25],'translation':[1,3,1],'scale':[.85,.85,.85]}}
def item(name,parts):
    write(A/f'models/item/{name}.json',{'textures':{'particle':tex['rune'],**tex,'bone':'shadowslave:block/fossil_bone','iron':'shadowslave:block/black_iron'},'elements':parts,'display':display,'gui_light':'front'})
diamond={'origin':[8,8,8],'axis':'z','angle':45}
item('soul_shard',[element([4,4,7],[12,12,9],'rune',diamond),element([6,6,6],[10,10,10],'light',diamond)])
item('memory',[element([2,2,7],[14,14,9],'metal',diamond),element([4,4,6],[12,12,10],'stone',diamond),element([6,6,5],[10,10,11],'light',diamond)])
item('manifested_blade',[element([7,0,7],[9,5,9],'metal'),element([3,4,7],[13,6,9],'metal'),element([6,6,7],[10,19,9],'iron'),element([6,6,6.8],[7,19,9.2],'bone'),element([9,6,6.8],[10,19,9.2],'bone'),element([7,6,6.5],[9,17,7],'rune'),element([7,19,7],[9,22,9],'bone'),element([7,3,6.5],[9,6,7],'light')])

lang=json.loads((A/'lang/en_us.json').read_text())
for name in NAMES+['amber_lantern','ash_stairs','ash_slab']:lang[f'block.shadowslave.{name}']=name.replace('_',' ').title()
lang['biome.shadowslave.ember_haven']='Ember Haven'
write(A/'lang/en_us.json',lang)
pickaxes=[f'shadowslave:{n}' for n in NAMES if n not in ['ashen_soil','ashen_grass','sanctuary_moss']]+['shadowslave:amber_lantern','shadowslave:ash_slab','shadowslave:ash_stairs']
write(R/'data/minecraft/tags/block/mineable/pickaxe.json',{'replace':False,'values':pickaxes})
write(R/'data/minecraft/tags/block/mineable/shovel.json',{'replace':False,'values':['shadowslave:ashen_soil']})
for tag,values in [('slabs',['ash_slab']),('stairs',['ash_stairs'])]:write(R/f'data/minecraft/tags/block/{tag}.json',{'replace':False,'values':['shadowslave:'+v for v in values]})
for output,pattern,ingredient,count in [('ash_bricks',['##','##'],'ash_stone',4),('ash_tiles',['##','##'],'ash_bricks',4),('ash_slab',['###'],'ash_tiles',6),('ash_stairs',['#  ','## ','###'],'ash_tiles',4)]:
    write(D/f'recipe/{output}.json',{'type':'minecraft:crafting_shaped','category':'building','pattern':pattern,'key':{'#':{'item':'shadowslave:'+ingredient}},'result':{'id':'shadowslave:'+output,'count':count}})
print('Generated 16 material mappings, 22 blocks, relic models, loot and recipes.')
