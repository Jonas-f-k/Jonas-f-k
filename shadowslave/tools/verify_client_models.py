"""Run CPU model construction after ./gradlew build has created the client classpath."""
from pathlib import Path
import os
import shutil
import subprocess

root=Path(__file__).resolve().parents[1]
dependencies=(root/'build/moddev/clientLegacyClasspath.txt').read_text().splitlines()
dependencies.extend(str(p) for p in (root/'build/moddev/artifacts').glob('*.jar') if not p.name.endswith('-sources.jar'))
classpath=os.pathsep.join([str(root/'build/classes/java/main'),str(root/'build/resources/main'),*dependencies])
java=os.environ.get('SHADOWSLAVE_JAVA') or shutil.which('java')
if not java:raise SystemExit('Set SHADOWSLAVE_JAVA to a JDK 21 java executable.')
raise SystemExit(subprocess.run([java,'-Djava.awt.headless=true','--class-path',classpath,str(root/'tools/ClientModelCheck.java')],cwd=root).returncode)
