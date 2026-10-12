"""Export editable Blockbench 5 files from the actual compiled Minecraft geometry."""
from pathlib import Path
import json,os,shutil,subprocess

root=Path(__file__).resolve().parents[1]
dependencies=(root/'build/moddev/clientLegacyClasspath.txt').read_text().splitlines()
dependencies.extend(str(p) for p in (root/'build/moddev/artifacts').glob('*.jar') if not p.name.endswith('-sources.jar'))
classpath=os.pathsep.join([str(root/'build/classes/java/main'),str(root/'build/resources/main'),*dependencies])
java=os.environ.get('SHADOWSLAVE_JAVA') or shutil.which('java')
if not java:raise SystemExit('Set SHADOWSLAVE_JAVA to JDK 21 java.')
output=root/'build/reports/blockbench-models'
subprocess.run([java,'-Djava.awt.headless=true','--class-path',classpath,str(root/'tools/ExportBlockbenchModels.java'),str(output)],cwd=root,check=True)
files=list(output.glob('*.bbmodel'))
assert len(files)==39,len(files)
for path in files:
 model=json.loads(path.read_text());assert model['meta']['format_version']=='5.0'
 assert model['textures'][0]['source'].startswith('data:image/png;base64,')
 ids=[cube['uuid'] for cube in model['elements']]+[bone['uuid'] for bone in model['groups']]
 assert len(ids)==len(set(ids)),path
 assert len(model['animations'])==5 and model['outliner'],path
 for cube in model['elements']:
  assert all(lo<=hi for lo,hi in zip(cube['from'],cube['to'])),path
  assert all(0<=uv<limit for uv,limit in zip(cube['uv_offset'],(64,128))),path
plugin=root/'tools/blockbench-plugins/animation_sliders'
shutil.copytree(plugin,output/'animation_sliders',dirs_exist_ok=True)
shutil.copyfile(root/'docs/EDITABLE_MODELS.md',output/'README.md')
print('PASS: 39 model JSONs, unique bone/cube IDs, positive cuboids, bounded UVs, five animations, embedded PNGs and licensed editor plugin.')
