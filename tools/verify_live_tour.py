#!/usr/bin/env python3
"""Check observed live-client diagnostics. These assertions are not a listening test."""
import json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent

def samples(name):
 text=(ROOT/name).read_text();case='';result={}
 assert '/ERROR]' not in text, 'Client error in '+name
 for line in text.splitlines():
  if 'Server thread/' in line and 'LAB_CASE ' in line:case=line.rsplit('LAB_CASE ',1)[1]
  if 'LAB_SAMPLE ' in line:
   record=line.split('LAB_SAMPLE ',1)[1]
   result.setdefault(case,[]).append(record)
 return text,result

def field(sample,key):return re.search(r'\b'+key+r'=([^ ]+)',sample).group(1)
def main():
 text,a=samples('run_client_050_tour.log')
 assert 'NORTHWOODS_TOUR_COMPLETE' in text
 assert 'LAB_DOOR_OPEN_CONFIRMED' in text and 'LAB_DOOR_CLOSED_CONFIRMED' in text
 assert 'LAB_RESOURCE_RELOAD_COMPLETE' in text
 closed=float(field(a['closed_cabin'][-1],'gain'));opened=float(field(a['open_door'][-1],'gain'))
 assert opened>closed+.2
 assert abs(float(field(a['closed_again'][-1],'gain'))-closed)<.02
 assert field(a['porch'][-1],'shelter')=='OPEN_SHELTER'
 assert field(a['roof_rain'][-1],'roofRain')=='true'
 for key in ['leaves_above_roof','covered_roof','tree_canopy']:
  assert field(a[key][-1],'roofRain')=='false',key
 assert field(a['tree_canopy'][-1],'shelter')=='CANOPY'
 assert field(a['log_cabin'][-1],'shelter')=='CABIN'
 assert 'creak=' not in a['stone_house'][-1]
 assert int(field(a['extinguish'][-1],'fires'))<int(field(a['fireplace'][-1],'fires'))
 assert int(field(a['relight'][-1],'fires'))==int(field(a['fireplace'][-1],'fires'))
 assert all(int(field(s,'fires'))<=2 for rows in a.values() for s in rows)
 assert 'loon=' not in a['winter'][-1] and 'frog=' not in a['winter'][-1]
 text,b=samples('run_client_050_water_tour.log')
 assert 'NORTHWOODS_WATER_TOUR_COMPLETE' in text,'Water tour is not finished'
 assert field(b['broad_river_lake'][-1],'water')=='INLAND_LAKE'
 assert field(b['broad_river_lake'][-1],'lake')=='true'
 assert field(b['narrow_river'][-1],'water')=='RIVER'
 for key in ['narrow_river','small_pool']:
  assert field(b[key][-1],'lake')=='false' and 'loon=' not in b[key][-1]
 assert field(b['ridge'][-1],'ridge')=='true'
 assert float(field(b['ridge'][-1],'wind'))>.3 and 'breeze' in b['ridge'][-1]
 assert field(b['lake_shore'][-1],'water')=='TEMPERATE_OCEAN_SHORE'
 assert field(b['lake_shore'][-1],'lake')=='true'
 report={'closed_door_gain':closed,'open_door_gain':opened,'porch_and_roof_rain':True,'covered_roofs_and_trees_suppress_roof_rain':True,
 'log_cabin_recognized':True,'stone_house_no_creaks':True,'fire_extinguish_and_relight':True,'max_active_fire_groups':2,
 'resource_reload_recovered':True,'frozen_wildlife_suppressed':True,'broad_river_lake_eligible':True,'narrow_river_and_pool_ineligible':True,
 'ridge_wind':float(field(b['ridge'][-1],'wind')),'temperate_ocean_shore_eligible':True,'subjective_listening':'unverified'}
 (ROOT/'build/reports/live-tour-validation.json').write_text(json.dumps(report,indent=2)+'\n')
 print('PASS:',json.dumps(report))
if __name__=='__main__':main()
