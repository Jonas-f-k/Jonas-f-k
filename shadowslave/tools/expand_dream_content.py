"""Author the 0.3 catalogue and its stable visual IDs. Does not edit raster art."""
import json
from pathlib import Path
R=Path(__file__).resolve().parents[1]/'src/main/resources'
p=R/'data/shadowslave/spell/catalogue.json'
d=json.loads(p.read_text());old={m['id']:m for m in d['memories']}
groups={
'armor':['Ashen Shroud','Awakened Mantle','Nightweave','Briar Sovereign','Silver Castellan','Sunkeeper Regalia','Frostbound Sentinel','Blossom Pilgrim','Tidal Warden','Amethyst Oracle','Scarlet Revenant','Mossbound Keeper','Dune Nomad','Aurora Raiment','Clockwork Vow','Ivory Seraph'],
'head':['Tide Crown','Lantern Eye','Bramble Antlers','Moonlit Visor','Sunpetal Circlet','Glacier Helm','Foxglove Hood','Raven Mask','Citrine Diadem','Coral Headdress','Winter Owl Mask','Astral Halo'],
'chest':['Iron Heart','Rosewood Cuirass','Glasswing Mantle','Dawnscale Vest','Frostglass Aegis','Cinder Breastplate','Jade Carapace','Moonwoven Coat','Coral Harness','Golden Hour Tunic','Violet Star Robe','Pilgrim Pauldrons'],
'legs':['Thorn Greaves','Dusksteel Tassets','Lotus Leggings','Glacier Cuisses','Amberleaf Wraps','Rosepetal Skirt','Dune Strider Chaps','Warden Plates','Prism Legguards','Tidewoven Kilt','Ravenfeather Trousers','Moonstone Guards'],
'feet':['Silent Steps','Starwalker','Briar Treads','Sunlit Sandals','Frosthoof Boots','Blossom Slippers','Coral Sabatons','Emberstride','Mosswalker Boots','Cloudstep Shoes','Dune Skippers','Amethyst Heels'],
'weapon':['Soul Edge','Mirror Knife','Hollow Fang','Dawncleaver','Briar Crescent','Glacier Pike','Rose Rapier','Cinder Maul','Moonhook','Tidal Trident','Amber Hatchet','Raven Scythe','Sunpetal Saber','Jade Halberd','Amethyst Glaive','Ivory Lance','Coral Cutlass','Starfall Hammer','Dune Falchion','Winter Fang','Glasswing Dagger','Rootbound Club','Scarlet Cleaver','Aurora Staff','Clockwork Spear','Mossbite Axe','Pearl Stiletto','Violet Greatsword','Honeycomb Mace','Thorn Whipblade','Obsidian Kris','Silver Requiem'],
'charm':['Fading Ember','Divine Spark','Cinder Sigil','Sunseed Locket','Moonlit Ring','Briar Brooch','Glacier Tear','Blossom Bell','Coral Rosary','Amber Hourglass','Raven Medallion','Jade Scarab','Amethyst Heart','Ivory Feather Pin','Dune Compass','Aurora Knot'],
'utility':['Shard Lantern','Drowned Pearl','Feather of Dusk','Dream Atlas','Sunwell Flask','Moonseed Pouch','Briar Key','Frostbound Compass','Blossom Fan','Coral Conch','Amber Music Box','Raven Quill','Jade Chalice','Amethyst Lens','Ivory Scroll','Warm Hearth Candle']}
biomes=['forgotten_shore','chained_isles','death_zone','verdant_gardens','hollow_mountains','sunpetal_meadows','frostglass_taiga','blossom_vale','azure_woods','amethyst_heights','scarlet_wilds','mosslight_ravine','nightmare_desert','aurora_fields','golden_grove','ember_haven']
colors=[0x525966,0xAAAEC1,0x292B43,0x4F9C59,0x899BAE,0xD5A74F,0x80BFCF,0xDA98B4,0x3C8E9E,0x8972B6,0x9E374C,0x597B50,0xBA8F68,0x779EA8,0xAB8854,0xDDD8BB]
trims=[0xD0C5B0,0xECC777,0xAC83C7,0xC4D893,0xE1E3DC,0xFFF0AB,0xD2F7FF,0xFFF0D8,0x87E5CB,0xE3C9F4,0xEEA074,0xC2CA83,0xE8C590,0xD8AEE3,0xB5D6C0,0xD9B965]
styles=['hood','knight','raven','antlers','knight','sun','ice','petal','coral','oracle','horns','leaf','nomad','wings','gear','halo']
effects=['essence','ward','fire','speed','vision','breathing','feather']
effecttext={'essence':'Restores Essence faster at rest.','ward':'Adds 5% Nightmare protection.','fire':'Grants fire resistance.','speed':'Grants movement speed.','vision':'Grants night vision.','breathing':'Grants water breathing.','feather':'Grants slow falling.'}
mem=[]
for typ,names in groups.items():
 for j,name in enumerate(names):
  ident=name.lower().replace(' ','_');i=len(mem);theme=(j if typ=='armor' else (j*3+len(typ)))%16
  rank=j%3
  m=old.get(ident,dict(id=ident,name=name,type=typ,description='',rank=rank,physical=0,magic=0,power=0,effect=''))
  if ident not in old:
   if typ in ['armor','head','chest','legs','feet']:
    m['physical']=round(.22+.10*rank+(j%3)*.02,2);m['magic']=round(.25+.10*rank+((j+1)%3)*.02,2)
    if typ!='armor':m['effect']=effects[(j+len(typ))%len(effects)]
   elif typ=='weapon':m['power']=2+rank*2+(j%2)
   else:m['effect']=effects[j%len(effects)]
   m['description']=('A '+styles[theme]+' design from '+biomes[theme].replace('_',' ')+'. '+(f"Summoned melee weapon; adds {m['power']} damage." if typ=='weapon' else effecttext.get(m['effect'],'Guards body and soul.')))
  m['design']=i+1
  for key,value in dict(biome=biomes[theme],silhouette=styles[theme],color=colors[theme],trim=trims[theme]).items():m.setdefault(key,value)
  mem.append(m)
assert len(mem)==128
assert len({m['id'] for m in mem})==128
d['memories']=mem;p.write_text(json.dumps(d,indent=2)+'\n')
Path('tools/memory_art_order.json').write_text(json.dumps([[m['name'] for m in mem[:64]],[m['name'] for m in mem[64:]]],indent=2))
print('128 Memories; stable visual IDs 1..128; original 18 preserved.')
