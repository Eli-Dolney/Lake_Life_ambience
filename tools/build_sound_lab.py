#!/usr/bin/env python3
"""Generate the standalone 1.21.1 Sound Lab datapack and isolated server workspace.
Run Minecraft there to bake the fixtures into an importable save; never touches user saves.
"""
import json, shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
WORK=ROOT/'build/sound-lab-server'
WORLD=WORK/'Northwoods Sound Lab'
PACK=WORLD/'datapacks/northwoods_sound_lab'
FUN=PACK/'data/northwoods_lab/function'
AREAS=[('forest','forest'),('taiga','taiga'),('plains','plains'),('swamp','swamp'),('lake','ocean'),('river','river'),('winter','snowy_taiga'),('ridge','windswept_hills')]
checks=[]

def function(name,lines):
 p=FUN/(name+'.mcfunction');p.parent.mkdir(parents=True,exist_ok=True);p.write_text('\n'.join(lines)+'\n')
def block(x,y,z,b):return f'setblock {x} {y} {z} minecraft:{b}'
def fill(lines,x1,y1,z1,x2,y2,z2,b):
 # Vanilla's normal 32768 block limit, including its inclusive endpoints.
 spans=[x2-x1+1,y2-y1+1,z2-z1+1]
 if spans[0]*spans[1]*spans[2]>32768:
  axis=max(range(3),key=lambda a:spans[a]); lo=[x1,y1,z1];hi=[x2,y2,z2];mid=(lo[axis]+hi[axis])//2
  a=hi.copy();a[axis]=mid;c=lo.copy();c[axis]=mid+1
  fill(lines,*lo,*a,b);fill(lines,*c,*hi,b);return
 lines.append(f'fill {x1} {y1} {z1} {x2} {y2} {z2} minecraft:{b}')
def expect(x,y,z,b,label):checks.append({'position':[x,y,z],'block':b,'label':label})
def sign(lines,x,y,z,rows):
 texts=[json.dumps({'text':t}) for t in (rows+['']*4)[:4]]
 messages=','.join("'"+t.replace("'","\\'")+"'" for t in texts)
 nbt='{front_text:{messages:['+messages+']},back_text:{messages:['+messages+']},is_waxed:1b}'
 lines.append(block(x,y,z,'oak_sign[rotation=8]'+nbt))
def tree(lines,x,z,spruce=False):
 wood='spruce' if spruce else 'oak';h=9 if spruce else 6
 fill(lines,x,64,z,x,64+h,z,wood+'_log[axis=y]')
 for y,r in ([(68,3),(70,2),(72,1),(74,1)] if spruce else [(68,2),(69,2),(70,1)]):
  fill(lines,x-r,y,z-r,x+r,y,z+r,wood+'_leaves[persistent=true]')
def cabin(lines,x,z,wall='oak_planks',roof='oak_planks',porch=True,glass=True):
 fill(lines,x,64,z,x+12,64,z+14,'oak_planks' if wall!='stone_bricks' else 'stone_bricks')
 fill(lines,x,65,z,x+12,68,z+14,wall)
 fill(lines,x+1,65,z+1,x+11,68,z+13,'air')
 fill(lines,x-1,69,z-1,x+13,69,z+15,roof)
 for half,y in [('lower',65),('upper',66)]:lines.append(block(x+6,y,z,f'oak_door[facing=north,half={half},hinge=left,open=false]'))
 fill(lines,x,66,z+5,x,67,z+7,'glass' if glass else 'air')
 # Framed open window on opposite wall.
 fill(lines,x+12,66,z+5,x+12,66,z+7,'air')
 lines.append(block(x+3,65,z+10,'campfire[lit=true,signal_fire=false]'))
 lines.append(block(x+9,65,z+10,'furnace[lit=false,facing=north]'))
 lines.append(block(x+9,65,z+12,'furnace[lit=true,facing=north]'))
 # Actual fuel/cook inventory makes the lit stove persist under ticking.
 lines.append(f'data merge block {x+9} 65 {z+12} {{BurnTime:32767s,CookTime:0s,CookTimeTotal:200s,Items:[{{Slot:0b,id:"minecraft:raw_iron",count:64}},{{Slot:1b,id:"minecraft:coal_block",count:64}}]}}')
 if porch:
  fill(lines,x-1,64,z-6,x+13,64,z-1,'oak_planks')
  fill(lines,x-1,69,z-6,x+13,69,z-2,roof)
  for xx in [x-1,x+13]:fill(lines,xx,65,z-6,xx,68,z-6,'oak_log')
 expect(x+6,65,z,'minecraft:oak_door','Cabin door')
 expect(x+3,65,z+10,'minecraft:campfire','Lit indoor campfire')
 sign(lines,x+4,65,z-5,['CABIN / PORCH','Open the door','Listen inside','then outside'])
 return [x+6,65,z+4]
def lake(lines,x,z,width=88,depth=84,frozen=False):
 fill(lines,x,57,z,x+width-1,63,z+depth-1,'air')
 fill(lines,x,56,z,x+width-1,56,z+depth-1,'clay')
 fill(lines,x,57,z,x+width-1,61,z+depth-1,'water')
 if frozen:fill(lines,x,61,z,x+width-1,61,z+depth-1,'ice')
 expect(x+width//2,61,z+depth//2,'minecraft:ice' if frozen else 'minecraft:water','Lake surface')
def button(lines,x,y,z,label,command):
 lines.append(block(x,y,z,'command_block[facing=up]{Command:'+json.dumps(command)+',TrackOutput:0b}'))
 lines.append(block(x,y+1,z,'stone_button[face=floor,facing=north]'))
 sign(lines,x,y+1,z-1,label)

def main():
 WORK.mkdir(parents=True,exist_ok=True)
 if (WORLD/'level.dat').exists():raise SystemExit('Existing Sound Lab save found; use a fresh build workspace rather than overwriting edits.')
 FUN.mkdir(parents=True,exist_ok=True)
 (PACK/'pack.mcmeta').write_text(json.dumps({'pack':{'pack_format':48,'description':'Northwoods Sound Lab — staged setup and listening controls (1.21.1)'}},indent=2))
 tags=PACK/'data/minecraft/tags/function';tags.mkdir(parents=True,exist_ok=True)
 (tags/'load.json').write_text(json.dumps({'values':['northwoods_lab:load']}))
 function('load',['execute unless data storage northwoods_lab:state initialized run function northwoods_lab:init'])
 init=['data modify storage northwoods_lab:state initialized set value 1b','data modify storage northwoods_lab:state complete set value 0b',
       'gamerule doMobSpawning false','gamerule doDaylightCycle false','gamerule doWeatherCycle false','gamerule doFireTick false',
       'gamerule randomTickSpeed 0','gamerule spawnRadius 0','gamerule commandBlockOutput false','gamerule sendCommandFeedback true',
       'difficulty peaceful','time set 6000','weather clear','setworldspawn 240 64 -16','forceload add 224 -32 255 -1',
       'schedule function northwoods_lab:stage/0_start 1s replace']
 function('init',init)
 teleports={}
 for i,(name,biome) in enumerate(AREAS):
  x=(i%4)*128;z=(i//4)*128
  start=[f'forceload add {x} {z} {x+127} {z+127}',f'schedule function northwoods_lab:stage/{i}_wait 2s replace']
  function(f'stage/{i}_start',start)
  # Poll all 64 chunks, not only corners, before issuing fill commands.
  wait=[f'data modify storage northwoods_lab:state ready set value 1b']
  for xx in range(x,x+128,16):
   for zz in range(z,z+128,16):wait.append(f'execute unless loaded {xx} 64 {zz} run data modify storage northwoods_lab:state ready set value 0b')
  wait += [f'execute if data storage northwoods_lab:state {{ready:1b}} run function northwoods_lab:stage/{i}_build',f'execute if data storage northwoods_lab:state {{ready:0b}} run schedule function northwoods_lab:stage/{i}_wait 2s replace']
  function(f'stage/{i}_wait',wait)
  lines=[]
  for xx in range(x,x+128,32):
   for zz in range(z,z+128,32):
    for yy in range(-64,320,32):lines.append(f'fillbiome {xx} {yy} {zz} {xx+31} {yy+31} {zz+31} minecraft:{biome}')
  # Border and entrance make crossings audible and visible without walls.
  fill(lines,x,63,z,x+127,63,z,'smooth_stone');fill(lines,x,63,z,x,63,z+127,'smooth_stone')
  sign(lines,x+3,64,z+3,[name.upper(),biome,'128 x 128','Sound Lab'])
  destination=[x+64,64,z+24]
  if name=='forest':
   destination=cabin(lines,x+48,z+48)
   for dx,dz in [(25,28),(31,45),(24,66),(36,88),(80,34),(88,62),(92,91),(50,90)]:tree(lines,x+dx,z+dz)
   lake(lines,x+10,z+94,12,12)
   # Natural stone overhang/cave, deliberately no crafted roof.
   fill(lines,x+90,64,z+10,x+115,75,z+30,'stone');fill(lines,x+94,64,z+10,x+110,69,z+25,'air')
   # A covered roof and a roof below leaves.
   cabin(lines,x+8,z+48,porch=False);fill(lines,x+7,73,z+47,x+21,73,z+63,'stone')
   cabin(lines,x+90,z+48,porch=False);fill(lines,x+89,72,z+47,x+103,72,z+63,'oak_leaves[persistent=true]')
  elif name=='taiga':
   destination=cabin(lines,x+45,z+48,'spruce_log[axis=y]','spruce_log[axis=x]')
   cabin(lines,x+82,z+75,'cobblestone','spruce_planks')
   for dx,dz in [(20,25),(30,35),(20,60),(28,83),(65,27),(88,28),(103,48),(63,98),(105,106)]:tree(lines,x+dx,z+dz,True)
  elif name=='plains':
   destination=cabin(lines,x+25,z+40)
   cabin(lines,x+78,z+40,'stone_bricks','stone_bricks')
   for xx in range(x+35,x+76,3):
    for zz in range(z+82,z+105,3):
     lines.append(block(xx,64,zz,'tall_grass[half=lower]'));lines.append(block(xx,65,zz,'tall_grass[half=upper]'))
   tree(lines,x+92,z+96)
   # Grouped, unlit, and open fireplace sources.
   for xx,lit in [(x+48,'true'),(x+50,'true'),(x+58,'false')]:lines.append(block(xx,64,z+24,f'campfire[lit={lit}]'))
   fill(lines,x+64,63,z+23,x+66,63,z+25,'netherrack');lines.append(block(x+65,64,z+24,'fire'))
  elif name=='swamp':
   lake(lines,x+24,z+36,72,64)
   for xx in range(x+18,x+105,4):
    lines.append(block(xx,64,z+32,'short_grass'));lines.append(block(xx,62,z+36,'sugar_cane'))
   tree(lines,x+16,z+65);tree(lines,x+106,z+78)
   destination=[x+64,64,z+32]
  elif name=='lake':
   lake(lines,x+30,z+16,88,100)
   cabin(lines,x+8,z+45)
   fill(lines,x+22,63,z+52,x+45,64,z+55,'air')
   fill(lines,x+22,62,z+52,x+45,62,z+55,'spruce_planks')
   destination=[x+24,63,z+53]
   sign(lines,x+23,63,z+52,['LAKE / DOCK','Temperate ocean','Freshwater mode','Listen for loons'])
  elif name=='river':
   lake(lines,x+10,z+10,56,56)
   lake(lines,x+102,z+8,5,110)
   lake(lines,x+16,z+110,8,8)
   destination=[x+70,64,z+38]
   sign(lines,x+70,64,z+36,['RIVER BIOME','Broad lake west','Narrow: SE end','Small pool: SW'])
   sign(lines,x+110,64,z+116,['NARROW CHANNEL','Isolated listening','No lake nearby','Compare loons'])
   sign(lines,x+25,64,z+114,['SMALL POOL','Isolated listening','No lake loons','Pond water only'])
  elif name=='winter':
   lake(lines,x+32,z+18,82,92,True)
   destination=cabin(lines,x+8,z+45,'spruce_planks','spruce_planks')
   for dx,dz in [(15,20),(19,94),(109,116)]:tree(lines,x+dx,z+dz,True)
   fill(lines,x+1,64,z+1,x+28,64,z+25,'snow')
  elif name=='ridge':
   for y in range(64,97):
    inset=y-64
    fill(lines,x+12+inset,y,z+28+inset,x+109-inset,y,z+99-inset,'stone' if y<94 else 'grass_block')
   destination=[x+64,97,z+64]
   fill(lines,x+93,64,z+105,x+115,73,z+123,'stone');fill(lines,x+95,64,z+105,x+113,68,z+119,'air')
   sign(lines,x+63,97,z+63,['EXPOSED RIDGE','Wind above','Hollow southeast','Compare shelter'])
  teleports[name]=destination
  button(lines,x+4,63,z+8,['RETURN','TO KIOSK'], 'tp @p 240 64 -16')
  lines += [f'forceload remove {x} {z} {x+127} {z+127}',f'data modify storage northwoods_lab:state stage set value {i+1}',
            f'schedule function northwoods_lab:stage/{i+1}_start 1s replace' if i<7 else 'schedule function northwoods_lab:finish 1s replace']
  function(f'stage/{i}_build',lines)
 finish=[]
 fill(finish,224,63,-32,255,63,-1,'smooth_stone')
 for i,(name,pos) in enumerate(teleports.items()):
  command=f'tp @p {pos[0]} {pos[1]} {pos[2]}'
  function('visit/'+name,[command]);button(finish,226+i*4,63,-26,[name.upper(),'TELEPORT'],command)
 for i,(name,command) in enumerate([('DAY','time set 6000'),('DUSK','time set 12500'),('NIGHT','time set 18000'),('DAWN','time set 23000'),('CLEAR','weather clear'),('RAIN','weather rain'),('THUNDER','weather thunder')]):
  button(finish,226+i*4,63,-7,[name,'GLOBAL'],command);function('controls/'+name.lower(),[command])
 sign(finish,237,64,-20,['NORTHWOODS','SOUND LAB 0.5','Biomes north row','and south row'])
 sign(finish,243,64,-20,['/northwoods','status','Compare doors','fire and rain'])
 finish += ['data modify storage northwoods_lab:state complete set value 1b','forceload remove 224 -32 255 -1','say Northwoods Sound Lab construction complete.']
 function('finish',finish)
 # Controls contain no destructive rebuild/reset command; init is load-once.
 function('visit/kiosk',['tp @p 240 64 -16'])
 generator={'biome':'minecraft:plains','lakes':False,'features':False,'structure_overrides':[],
 'layers':[{'height':1,'block':'minecraft:bedrock'},{'height':124,'block':'minecraft:stone'},{'height':2,'block':'minecraft:dirt'},{'height':1,'block':'minecraft:grass_block'}]}
 properties={'level-name':'Northwoods Sound Lab','level-type':'minecraft:flat','generator-settings':json.dumps(generator,separators=(',',':')),
 'gamemode':'creative','difficulty':'peaceful','generate-structures':'false','spawn-protection':'0','enable-command-block':'true',
 'server-ip':'127.0.0.1','server-port':'25585','online-mode':'true','view-distance':'3','simulation-distance':'3','sync-chunk-writes':'true','max-tick-time':'180000','enable-rcon':'false'}
 (WORK/'server.properties').write_text('\n'.join(k+'='+v for k,v in properties.items())+'\n')
 (WORK/'eula.txt').write_text('eula=true\n')
 manifest={'minecraft':'1.21.1','spawn':[240,64,-16],'areas':[{'name':name,'biome':'minecraft:'+biome,'bounds':[i%4*128,i//4*128,i%4*128+127,i//4*128+127],'teleport':teleports[name]} for i,(name,biome) in enumerate(AREAS)],'fixtures':checks}
 (ROOT/'build/sound-lab-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
 print(f'Generated {len(list(FUN.rglob("*.mcfunction")))} functions; 8 biome areas, {len(checks)} fixture checks; isolated workspace: {WORK}')
if __name__=='__main__':main()
