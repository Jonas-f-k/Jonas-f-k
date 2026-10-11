"""Validate shipped native assets, licenses, model references and foliage. Optional vanilla JAR."""
from pathlib import Path
import json,hashlib,sys,zipfile
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'src/main/resources';A=R/'assets/shadowslave'
vanilla=zipfile.ZipFile(sys.argv[1]) if len(sys.argv)>1 else None
for p in R.rglob('*.json'):json.loads(p.read_text())
names=json.loads((A/'art/native_sprites.json').read_text());sprites={'shadowslave:'+n for n in names}
assert len(names)==len(set(names))
atlas=json.loads((R/'assets/minecraft/atlases/blocks.json').read_text())
assert {s['resource'] for s in atlas['sources']}==sprites
assert all(s['type']=='minecraft:single' for s in atlas['sources'])
for name in names:
 p=A/'textures'/f'{name}.png';im=Image.open(p);w,h=im.size
 assert w in (16,32,64) and h%w==0,(name,im.size)
 if name.startswith('entity/'):
  assert im.size==(64,128),('Creature material layout mismatch',name,im.size)
  assert not Path(str(p)+'.mcmeta').exists(),('Static creature skin marked animated',name)
  for band in range(4):
   assert len(im.crop((0,band*32,64,(band+1)*32)).getcolors(2048))>=3,('Flat creature material',name,band)
 elif h!=w:assert Path(str(p)+'.mcmeta').exists(),('Missing animation metadata',name)
 if Path(str(p)+'.mcmeta').exists():json.loads(Path(str(p)+'.mcmeta').read_text())
assert not list((A/'textures/art').glob('*.png')),'Retired illustrated atlases must not ship'
icons=[]
cat=json.loads((R/'data/shadowslave/spell/catalogue.json').read_text())['memories']
for m in cat:
 p=A/'textures/item/memories'/f'{m["id"]}.png';im=Image.open(p).convert('RGBA');alpha=im.getchannel('A').getextrema()
 assert alpha==(0,255),('No transparent silhouette',m['id'],alpha)
 icons.append(hashlib.sha256(p.read_bytes()).hexdigest())
assert len(icons)==len(set(icons))==128,'Each Memory must have a distinct image'
credits=json.loads((A/'art/asset_credits.json').read_text())
for c in credits['assets']:
 assert hashlib.sha256((A/'textures'/f'{c["sprite"]}.png').read_bytes()).hexdigest()==c['sha256'],c['sprite']
assert (R/'META-INF/licenses/MalcolmRiley-CC-BY-4.0.txt').exists()
for name in names:
 if name.endswith('_leaves'):
  assert Image.open(A/'textures'/f'{name}.png').convert('RGBA').getchannel('A').getextrema()==(0,255),name
  assert json.loads((A/f'models/{name}.json').read_text())['render_type']=='minecraft:cutout_mipped'
for name in ['ashen_grass','meadow_grass','blossom_grass','lavender_grass','frost_grass','scarlet_litter','autumn_litter','turquoise_moss','sanctuary_moss']:
 model=json.loads((A/f'models/block/{name}.json').read_text())
 assert model['parent']=='minecraft:block/cube_bottom_top' and model['textures']['bottom']=='shadowslave:block/dream_loam'
 assert model['textures']['side']!=model['textures']['top']
 im=Image.open(A/f'textures/block/{name}.png').convert('RGBA')
 assert all(im.getpixel((0,y))==im.getpixel((15,y)) for y in range(16)),name
 assert all(im.getpixel((x,0))==im.getpixel((x,15)) for x in range(16)),name

def exists(kind,identifier):
 ns,name=identifier.split(':',1) if ':' in identifier else ('minecraft',identifier)
 path=f'assets/{ns}/{kind}/{name}'
 return (vanilla is None or path in vanilla.namelist() or name.startswith('builtin/')) if ns=='minecraft' else (R/path).is_file()
models=list((A/'models').rglob('*.json'))
for p in models:
 d=json.loads(p.read_text())
 if 'parent' in d:assert exists('models',d['parent']+'.json'),p
 for tex in d.get('textures',{}).values():assert tex.startswith('#') or tex in sprites or exists('textures',tex+'.png'),(p,tex)
 for e in d.get('elements',[]):
  assert all(-16<=v<=32 for v in e['from']+e['to']),p
  assert all(a<=b for a,b in zip(e['from'],e['to'])),p
for p in (A/'blockstates').glob('*.json'):
 for v in json.loads(p.read_text()).get('variants',{}).values():
  for entry in v if isinstance(v,list) else [v]:assert exists('models',entry['model']+'.json'),p
for item in ['memory','manifested_blade']:
 overrides=json.loads((A/f'models/item/{item}.json').read_text())['overrides'];assert len(overrides)==128
 for m,o in zip(cat,overrides):assert o['predicate']['shadowslave:design']==m['design'] and exists('models',o['model']+'.json')
print(f'PASS: {len(models)} models, {len(names)} native sprites, 128 distinct transparent Memory icons, full-cube grass, cutout leaves, {len(credits["assets"])} credited asset hashes. '+('Vanilla references checked against JAR.' if vanilla else 'Vanilla names not resolved without client JAR.'))
