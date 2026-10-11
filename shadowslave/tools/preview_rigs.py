"""Render a review sheet from verify_rigs_offline.py's geometry JSON, not an in-game capture."""
import json,math,sys
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw,ImageFont
rigs=json.loads(Path(sys.argv[1]).read_text());out=Path(sys.argv[2]);cols=5;cw,ch=240,240
im=Image.new('RGB',(cols*cw,80+math.ceil(len(rigs)/cols)*ch),'#0d1420');draw=ImageDraw.Draw(im)
fontpath='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
font=ImageFont.truetype(fontpath,13);title=ImageFont.truetype(fontpath,24);small=ImageFont.truetype(fontpath,11)
palettes=[(136,102,84),(91,94,126),(62,141,146),(136,151,157),(118,150,90),(177,160,194),(116,136,185)]
draw.text((24,16),'SHADOW SLAVE · CREATURE RIG REWORK',font=title,fill='#e0ba76')
draw.text((24,50),'32 species + 7 transformations · shaded geometry preview · generated from rig source, not a Minecraft screenshot',font=small,fill='#b8c8d0')
def rotation(x,y,z):
 c,s=math.cos,math.sin
 rx=np.array([[1,0,0],[0,c(x),-s(x)],[0,s(x),c(x)]])
 ry=np.array([[c(y),0,s(y)],[0,1,0],[-s(y),0,c(y)]])
 rz=np.array([[c(z),-s(z),0],[s(z),c(z),0],[0,0,1]])
 return rz@ry@rx
camera=rotation(.25,-.60,0)
faces=[(0,1,3,2),(4,6,7,5),(0,4,5,1),(2,3,7,6),(0,2,6,4),(1,5,7,3)]
light=np.array([-.5,-.8,-.3]);light/=np.linalg.norm(light)
def collect(p,parent,path=''):
 pose=p['pose'];matrix=np.eye(4);matrix[:3,:3]=rotation(*pose[3:]);matrix[:3,3]=pose[:3];matrix=parent@matrix;out=[]
 for box in p['cubes']:
  x,y,z,w,h,d=box
  vertices=np.array([[x+i*w,y+j*h,z+k*d,1] for k in range(2) for j in range(2) for i in range(2)])
  world=(matrix@vertices.T).T[:,:3];view=(camera@world.T).T
  for face in faces:
   points=world[list(reversed(face))];normal=np.cross(points[1]-points[0],points[2]-points[0]);length=np.linalg.norm(normal)
   if length==0:continue
   normal/=length;shade=.52+.45*max(0,float(np.dot(normal,light)))
   out.append((view[list(reversed(face))],shade,path))
 for name,child in p['children'].items():
  if name in ['eyes','runes']:continue
  out+=collect(child,matrix,path+'/'+name)
 return out
for n,entry in enumerate(rigs):
 x=(n%cols)*cw;y=80+(n//cols)*ch
 draw.rounded_rectangle((x+5,y+5,x+cw-5,y+ch-5),radius=8,fill='#172231',outline='#394859')
 mesh=collect(entry['rig'],np.eye(4));allpoints=np.concatenate([f[0] for f in mesh]);lo=allpoints[:,:2].min(0);hi=allpoints[:,:2].max(0)
 scale=min(198/(hi[0]-lo[0]),179/(hi[1]-lo[1]));center=(hi+lo)/2
 color=palettes[entry['family']]
 for points,shade,path in sorted(mesh,key=lambda p:p[0][:,2].mean(),reverse=True):
  tint=(221,203,166) if any(k in path for k in ['fang','tusk','horn','crown','claw','beak']) else color
  rgb=tuple(int(c*shade) for c in tint);xy=[(x+cw/2+(p[0]-center[0])*scale,y+103+(p[1]-center[1])*scale) for p in points]
  draw.polygon(xy,fill=rgb)
 draw.text((x+12,y+202),entry['name'],font=font,fill='#e0ba76' if n>=32 else '#dce4e5')
 draw.text((x+12,y+221),'ASPECT TRANSFORMATION' if n>=32 else entry['id'].replace('_',' ').upper(),font=small,fill='#849bad')
out.parent.mkdir(parents=True,exist_ok=True);im.save(out)
print(out)
