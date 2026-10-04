"""Small typed NBT reader/writer for validating and packaging the generated lab save."""
import gzip,io,struct,zlib
from pathlib import Path

def read(data):
 f=io.BytesIO(data)
 def number(fmt):return struct.unpack('>'+fmt,f.read(struct.calcsize(fmt)))[0]
 def string():return f.read(number('H')).decode('utf-8')
 def payload(t):
  if t in (1,2,3,4,5,6):return number({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t])
  if t==7:return f.read(number('i'))
  if t==8:return string()
  if t==9:
   typ=number('B');return (typ,[payload(typ) for _ in range(number('i'))])
  if t==10:
   result={}
   while (typ:=number('B')):
    name=string();result[name]=(typ,payload(typ))
   return result
  if t in (11,12):return [number('i' if t==11 else 'q') for _ in range(number('i'))]
  raise ValueError(t)
 typ=number('B');name=string();return name,(typ,payload(typ))
def write(name,node):
 f=io.BytesIO()
 def number(fmt,value):f.write(struct.pack('>'+fmt,value))
 def string(s):b=s.encode('utf-8');number('H',len(b));f.write(b)
 def payload(t,v):
  if t in (1,2,3,4,5,6):number({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t],v)
  elif t==7:number('i',len(v));f.write(v)
  elif t==8:string(v)
  elif t==9:
   typ,values=v;number('B',typ);number('i',len(values))
   for x in values:payload(typ,x)
  elif t==10:
   for k,(typ,value) in v.items():number('B',typ);string(k);payload(typ,value)
   number('B',0)
  elif t in (11,12):
   number('i',len(v))
   for x in v:number('i' if t==11 else 'q',x)
  else:raise ValueError(t)
 number('B',node[0]);string(name);payload(*node);return f.getvalue()
def load(path):return read(gzip.decompress(Path(path).read_bytes()))
def save(path,name,node):Path(path).write_bytes(gzip.compress(write(name,node),mtime=0))
def value(compound,key,default=None):return compound.get(key,(None,default))[1]
def chunk(world,cx,cz):
 p=Path(world)/'region'/f'r.{cx//32}.{cz//32}.mca'
 with p.open('rb') as f:
  f.seek(4*((cx%32)+(cz%32)*32));loc=int.from_bytes(f.read(4),'big');assert loc,('Missing chunk',cx,cz)
  f.seek((loc>>8)*4096);length=int.from_bytes(f.read(4),'big');compression=f.read(1)[0];data=f.read(length-1)
  assert compression==2,compression
  return read(zlib.decompress(data))[1][1]
def palette_entry(container,index,minimum_bits):
 palette=value(container,'palette')[1]
 if len(palette)==1:return palette[0]
 bits=max(minimum_bits,(len(palette)-1).bit_length());per_long=64//bits
 raw=value(container,'data');v=(raw[index//per_long]&((1<<64)-1))>>((index%per_long)*bits)
 return palette[v&((1<<bits)-1)]
def block_at(c,x,y,z):
 sections=value(c,'sections')[1]
 section=next(s for s in sections if value(s,'Y')==y//16)
 entry=palette_entry(value(section,'block_states'),(y%16)*256+(z%16)*16+x%16,4)
 return value(entry,'Name'),value(entry,'Properties',{})
def biome_at(c,x,y,z):
 sections=value(c,'sections')[1];section=next(s for s in sections if value(s,'Y')==y//16)
 return palette_entry(value(section,'biomes'),((y%16)//4)*16+((z%16)//4)*4+(x%16)//4,1)
