"""Deterministic biome, surface and block model authoring for the Dream Realm."""
import json
from pathlib import Path
R=Path(__file__).resolve().parents[1]/'src/main/resources';A=R/'assets/shadowslave';D=R/'data/shadowslave'
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2)+'\n')
old=['ash_stone','ash_bricks','fractured_stone','ash_tiles','sanctuary_limestone','aged_bronze','amber_glass','silver_rune','ashen_soil','ashen_grass','sanctuary_moss','petrified_bark','black_iron','fossil_bone','ember_obsidian','drowned_stone']
new=['meadow_grass','blossom_grass','lavender_grass','autumn_litter','turquoise_moss','frost_grass','scarlet_litter','warm_sand','dream_loam','rose_clay','snow_soil','golden_sandstone','blue_shale','crystal_rock','red_sandstone','pale_marble','emerald_leaves','pink_leaves','lavender_leaves','golden_leaves','cyan_leaves','teal_leaves','frost_leaves','crimson_leaves','dream_oak','dream_birch','blue_bark','rose_bark','redwood_bark','runic_timber','honey_planks','plum_planks','emerald_crystal','amethyst_crystal','cyan_crystal','rose_crystal','peach_bricks','mossy_ruin_stone','moon_brass','navy_cloth','silver_plate','charcoal_plate','leaf_plate','plum_cloth','gold_plate','ice_plate','scarlet_plate','pearl_plate']
write(Path('tools/realm_materials.json'),old+new)
# name, ground, tree foliage, trunk, temperature, humidity, sky, fog, water, flower
biomes=[
('frostglass_taiga','frost_grass','frost_leaves','blue_bark',-.8,-.6,0xACD7EA,0xD2E6EB,0x559DBB,'frost_bloom'),
('hollow_mountains','ash_stone','','petrified_bark',-.8,0,0x7E8FAD,0x969EA8,0x416D8A,'moon_bloom'),
('aurora_fields','lavender_grass','lavender_leaves','dream_birch',-.8,.6,0x96C5D6,0xC0DCD9,0x68B5C6,'violet_bloom'),
('death_zone','ashen_soil','','petrified_bark',-.4,-.6,0x646772,0x8D9097,0x3B505A,'moon_bloom'),
('azure_woods','turquoise_moss','cyan_leaves','blue_bark',-.4,0,0x78C4E8,0xB2DAEC,0x42B6D0,'azure_bloom'),
('moonlit_grove','turquoise_moss','teal_leaves','blue_bark',-.4,.6,0x718EAA,0x799C9F,0x398FA9,'azure_bloom'),
('forgotten_shore','ashen_grass','','petrified_bark',0,-.6,0x9BA5B6,0xB6B8BC,0x557E92,'moon_bloom'),
('blossom_vale','blossom_grass','pink_leaves','rose_bark',0,0,0x9AD1F1,0xE8D8E6,0x69BFDB,'rose_bloom'),
('mosslight_ravine','sanctuary_moss','emerald_leaves','dream_oak',0,.6,0x91BDCC,0xB4CDB5,0x509F97,'jade_bloom'),
('amethyst_heights','lavender_grass','lavender_leaves','rose_bark',.4,-.6,0xB3B3E9,0xD4CBE4,0x887BC8,'violet_bloom'),
('golden_grove','autumn_litter','golden_leaves','dream_birch',.4,0,0xB3D9E6,0xF1D9A7,0x71B4C7,'sun_bloom'),
('verdant_gardens','meadow_grass','emerald_leaves','runic_timber',.4,.6,0x8ECFEE,0xD4EBC4,0x5ABABD,'jade_bloom'),
('nightmare_desert','warm_sand','','redwood_bark',.8,-.6,0xBFAD91,0xD9BD97,0x527E8B,'ember_bloom'),
('sunpetal_meadows','meadow_grass','golden_leaves','dream_oak',.8,0,0x8CCDED,0xEFE6C0,0x63BDD0,'sun_bloom'),
('scarlet_wilds','scarlet_litter','crimson_leaves','redwood_bark',.8,.6,0xB58299,0xC89F9F,0xA56781,'ember_bloom'),
('ember_haven','meadow_grass','golden_leaves','dream_oak',.2,.3,0xABD4E3,0xE9D7BA,0x65B5C0,'sun_bloom'),
('chained_isles','meadow_grass','cyan_leaves','dream_oak',0,0,0xA6CFEC,0xCEE2E9,0x5798B8,'cloud_bloom')]
write(Path('tools/realm_biomes.json'),[dict(zip(['id','ground','leaves','trunk','temperature','humidity','sky','fog','water','flower'],b)) for b in biomes])
entries=[]
for i,b in enumerate(biomes):
 name,ground,leaves,trunk,t,humid,sky,fog,water,flower=b
 effects={'sky_color':sky,'fog_color':fog,'water_color':water,'water_fog_color':water,'grass_color':0x81B94C,'foliage_color':0x6EA84C}
 if name in ['moonlit_grove','azure_woods','amethyst_heights']:effects['particle']={'probability':.006,'options':{'type':'minecraft:end_rod'}}
 elif name in ['blossom_vale','verdant_gardens','aurora_fields']:effects['particle']={'probability':.008,'options':{'type':'minecraft:cherry_leaves'}}
 elif name=='scarlet_wilds':effects['particle']={'probability':.006,'options':{'type':'minecraft:crimson_spore'}}
 features=[[],[],[],[],[],['shadowslave:island_chains','shadowslave:dream_ruin'],['minecraft:ore_coal_upper','minecraft:ore_iron_upper','minecraft:ore_iron_middle','minecraft:ore_diamond','minecraft:ore_gold'],[],[],['shadowslave:realm_decoration'],[]]
 write(D/f'worldgen/biome/{name}.json',{'has_precipitation':name=='frostglass_taiga','temperature':-.5 if name=='frostglass_taiga' else .8,'downfall':.5 if name=='frostglass_taiga' else 0,'effects':effects,'carvers':{'air':[] if name=='chained_isles' else ['minecraft:cave']},'features':features,'spawners':{},'spawn_costs':{}})
 params={k:[-1,1] for k in ['temperature','humidity','continentalness','erosion','depth','weirdness']};params['offset']=0
 if name=='chained_isles':params['weirdness']=[.35,1]
 else:params.update(temperature=t,humidity=humid,weirdness=[-1,.35])
 entries.append({'biome':'shadowslave:'+name,'parameters':params})
dim=json.loads((D/'dimension/dream_realm.json').read_text());dim['type']='shadowslave:dream_realm';dim['generator']['biome_source']['biomes']=entries;write(D/'dimension/dream_realm.json',dim)
type_=json.loads((D/'dimension_type/dream.json').read_text());type_.pop('fixed_time',None);type_['ambient_light']=.08;write(D/'dimension_type/dream_realm.json',type_)
n=json.loads((D/'worldgen/noise_settings/dream.json').read_text())
for k in ['final_density','initial_density_without_jaggedness']:n['noise_router'][k]['when_in_range']={'type':'shadowslave:islands'}
# Real topsoil, soil underlayer and contrasting cliffs; not a single painted cube for all terrain.
def block(name):return {'type':'minecraft:block','result_state':{'Name':('minecraft:' if name=='bedrock' else 'shadowslave:')+name}}
def cond(test,result):return {'type':'minecraft:condition','if_true':test,'then_run':result}
def depth(offset):return {'type':'minecraft:stone_depth','add_surface_depth':False,'offset':offset,'secondary_depth_range':0,'surface_type':'floor'}
seq=[cond({'type':'minecraft:vertical_gradient','random_name':'minecraft:bedrock_floor','true_at_and_below':{'above_bottom':0},'false_at_and_above':{'above_bottom':5}},block('bedrock'))]
for b in biomes:
 name,ground,*_=b
 cliff='blue_shale' if name in ['azure_woods','frostglass_taiga','chained_isles','aurora_fields'] else 'crystal_rock' if name=='amethyst_heights' else 'golden_sandstone' if name=='nightmare_desert' else 'red_sandstone' if name=='scarlet_wilds' else 'ash_stone' if name in ['death_zone','hollow_mountains','forgotten_shore'] else 'sanctuary_limestone'
 soil='snow_soil' if name=='frostglass_taiga' else 'warm_sand' if name=='nightmare_desert' else 'dream_loam'
 steps=[cond({'type':'minecraft:steep'},block(cliff)),cond(depth(0),block(ground)),cond(depth(3),block(soil)),cond(depth(9),block(cliff))]
 seq.append(cond({'type':'minecraft:biome','biome_is':['shadowslave:'+name]},{'type':'minecraft:sequence','sequence':steps}))
n['surface_rule']={'type':'minecraft:sequence','sequence':seq};write(D/'worldgen/noise_settings/dream.json',n)
write(D/'worldgen/configured_feature/island_chains.json',{'type':'shadowslave:island_chains','config':{}})
write(D/'worldgen/placed_feature/island_chains.json',{'feature':'shadowslave:island_chains','placement':[]})
write(D/'worldgen/placed_feature/realm_decoration.json',{'feature':'shadowslave:realm_decoration','placement':[{'type':'minecraft:count','count':9},{'type':'minecraft:in_square'},{'type':'minecraft:heightmap','heightmap':'WORLD_SURFACE_WG'},{'type':'minecraft:biome'}]})
preset=Path('src/gametest/resources/data/minecraft/worldgen/world_preset/flat.json');p=json.loads(preset.read_text());p['dimensions']['shadowslave:dream_realm']=dim;write(preset,p)
# Registry declarations generated from the explicit material catalogue.
p=Path('src/main/java/com/shadowslave/world/RealmBlocks.java');s=p.read_text();start=s.find('    // EXPANSION START');end=s.find('    // EXPANSION END')
if start>=0:s=s[:start]+s[end+len('    // EXPANSION END\n'):]
reg='    // EXPANSION START\n    static {\n'
for name in new:
 source='Blocks.OAK_LEAVES' if 'leaves' in name else 'Blocks.OAK_PLANKS' if any(w in name for w in ['bark','oak','birch','timber','planks']) else 'Blocks.DIRT' if any(w in name for w in ['grass','litter','loam','soil','moss']) else 'Blocks.STONE'
 reg+=f'        material("{name}",{source},{5 if "crystal" in name or name=="teal_leaves" else 0});\n'
for name in ['rose_bloom','sun_bloom','azure_bloom','jade_bloom','violet_bloom','ember_bloom','moon_bloom','cloud_bloom']:
 reg+=f'        DeferredBlock<Block> {name}=ShadowSlaveMod.BLOCKS.register("{name}",()->new CarvedBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY).noCollission().lightLevel(s->3),Block.box(2,0,2,14,12,14)));MATERIALS.put("{name}",{name});ShadowSlaveMod.ITEMS.registerSimpleBlockItem({name});\n'
for name in ['moon_lantern','runic_basin','amethyst_cluster','potion_shelf','star_books','bloom_pot','lantern_pedestal']:
 reg+=f'        DeferredBlock<Block> {name}=ShadowSlaveMod.BLOCKS.register("{name}",()->new CarvedBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).noOcclusion().lightLevel(s->{15 if name=="moon_lantern" else 4}),Block.box(1,0,1,15,14,15)));MATERIALS.put("{name}",{name});ShadowSlaveMod.ITEMS.registerSimpleBlockItem({name});\n'
reg+='    }\n    public static BlockState state(String name){var state=MATERIALS.get(name).get().defaultBlockState();return state.getBlock() instanceof LeavesBlock?state.setValue(LeavesBlock.PERSISTENT,true):state;}\n    // EXPANSION END\n'
s=s.replace('    private static DeferredBlock<Block> material(',reg+'    private static DeferredBlock<Block> material(');p.write_text(s)
lang=json.loads((A/'lang/en_us.json').read_text())
for b in biomes:lang['biome.shadowslave.'+b[0]]=b[0].replace('_',' ').title()
write(A/'lang/en_us.json',lang)
print(f'{len(biomes)} biomes, 64 materials, 8 blooms, discrete island terrain and chain feature.')
