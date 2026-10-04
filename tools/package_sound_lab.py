#!/usr/bin/env python3
"""Set single-player metadata, validate, and zip the generated world without server artifacts."""
import shutil,zipfile
from pathlib import Path
from lab_nbt import load,save,value
from verify_sound_lab import main as verify
ROOT=Path(__file__).resolve().parent.parent
WORLD=ROOT/'build/sound-lab-server/Northwoods Sound Lab'
def main():
 name,node=load(WORLD/'level.dat');data=value(node[1],'Data')
 data['allowCommands']=(1,1);data['GameType']=(3,1);data['LevelName']=(8,'Northwoods Sound Lab')
 data['Difficulty']=(1,0);data['DifficultyLocked']=(1,0)
 save(WORLD/'level.dat',name,node)
 # Keep the fallback metadata identical rather than reverting cheats on recovery.
 shutil.copyfile(WORLD/'level.dat',WORLD/'level.dat_old')
 verify()
 out=ROOT/'build/distributions';out.mkdir(parents=True,exist_ok=True)
 dest=out/'Northwoods-Sound-Lab-0.5.0.zip'
 with zipfile.ZipFile(dest,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
  for p in sorted(WORLD.rglob('*')):
   if p.is_file() and p.name!='session.lock':z.write(p,str(p.relative_to(WORLD.parent)))
  z.write(ROOT/'SOUND_LAB.md','INSTALL-AND-LISTEN.md')
 print(f'World packaged: {dest} ({dest.stat().st_size:,} bytes)')
if __name__=='__main__':main()
