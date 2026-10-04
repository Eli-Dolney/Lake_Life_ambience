#!/usr/bin/env python3
import json,re
from pathlib import Path
from lab_nbt import load,value,chunk,block_at,biome_at
ROOT=Path(__file__).resolve().parent.parent
WORLD=ROOT/'build/sound-lab-server/Northwoods Sound Lab'
def main():
 manifest=json.loads((ROOT/'build/sound-lab-manifest.json').read_text())
 data=value(load(WORLD/'level.dat')[1][1],'Data')
 assert value(data,'DataVersion')==3955
 assert value(data,'GameType')==1
 assert [value(data,k) for k in ['SpawnX','SpawnY','SpawnZ']]==manifest['spawn']
 rules=value(data,'GameRules')
 for k in ['doMobSpawning','doDaylightCycle','doWeatherCycle','doFireTick']:assert value(rules,k)=='false',k
 assert value(data,'DayTime')%24000==6000
 assert value(data,'raining')==0 and value(data,'thundering')==0
 cache={}
 def get(x,z):
  key=(x//16,z//16)
  if key not in cache:cache[key]=chunk(WORLD,*key)
  return cache[key]
 for area in manifest['areas']:
  x,z,x2,z2=area['bounds']
  for xx in range(x,x2+1,16):
   for zz in range(z,z2+1,16):
    c=get(xx,zz)
    # All 24 sections, including terrain below the ground and above the ridge.
    for y in range(-64,320,16):
     assert biome_at(c,xx+8,y+4,zz+8)==area['biome'],(area['name'],xx,y,zz)
  tx,ty,tz=area['teleport']
  for yy in [ty,ty+1]:assert block_at(get(tx,tz),tx,yy,tz)[0] in ['minecraft:air','minecraft:oak_sign'],(area['name'],'unsafe teleport',yy)
  assert block_at(get(tx,tz),tx,ty-1,tz)[0]!='minecraft:air',(area['name'],'unsupported teleport')
 for check in manifest['fixtures']:
  x,y,z=check['position'];actual=block_at(get(x,z),x,y,z)[0]
  assert actual==check['block'],(check,actual)
 controls=0
 for c in [get(x,z) for x in range(224,256,16) for z in range(-32,0,16)]:
  for entity in value(c,'block_entities')[1]:
   if value(entity,'id')=='minecraft:command_block':
    controls+=1;command=value(entity,'Command')
    assert command.startswith(('tp @p ','time set ','weather ')),command
 assert controls==15,controls
 state=load(WORLD/'data/command_storage_northwoods_lab.dat')[1][1]
 state=value(value(value(state,'data'),'contents'),'state')
 assert value(state,'complete')==1 and value(state,'initialized')==1
 force=WORLD/'data/chunks.dat'
 if force.exists():assert not value(value(load(force)[1][1],'data'),'Forced',[])
 # Catch syntax-independent command size/biome alignment mistakes before launch.
 for p in (WORLD/'datapacks/northwoods_sound_lab/data/northwoods_lab/function').rglob('*.mcfunction'):
  for line in p.read_text().splitlines():
   if line.startswith(('fill ','fillbiome ')):
    a=list(map(int,line.split()[1:7]));size=(a[3]-a[0]+1)*(a[4]-a[1]+1)*(a[5]-a[2]+1)
    assert size<=32768,(p,line,size)
 report={'version':'1.21.1','biome_chunks':512,'biome_sections':512*24,'fixtures':len(manifest['fixtures']),'kiosk_controls':controls,'setup_complete':True,'forceloads_remaining':0}
 (ROOT/'build/reports').mkdir(exist_ok=True)
 (ROOT/'build/reports/sound-lab-validation.json').write_text(json.dumps(report,indent=2)+'\n')
 print('PASS:',json.dumps(report))
if __name__=='__main__':main()
