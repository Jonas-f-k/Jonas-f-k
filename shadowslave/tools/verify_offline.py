"""Run the checks that do not require downloading Minecraft/NeoForge. JDK 21 + Pillow."""
from pathlib import Path
import subprocess,tempfile,sys
root=Path(__file__).resolve().parents[1]
for script in ['verify_art_resources.py','validate_worldgen.py','verify_rigs_offline.py']:
 subprocess.run([sys.executable,str(root/'tools'/script)],cwd=root,check=True)
with tempfile.TemporaryDirectory() as temp:
 sources=['src/main/java/com/shadowslave/world/Archipelago.java','src/main/java/com/shadowslave/client/SpellGeometry.java','tools/java/ArchipelagoCheck.java','tools/java/VisualMathCheck.java']
 subprocess.run(['javac','-d',temp,*sources],cwd=root,check=True)
 for check in ['ArchipelagoCheck','VisualMathCheck']:subprocess.run(['java','-cp',temp,check],cwd=root,check=True)
