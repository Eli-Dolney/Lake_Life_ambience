#!/usr/bin/env python3
"""Fetch explicitly credited recordings; source pages retained as license evidence."""
import html,json,re,urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
SOURCES=[
 ('fire_cozy','aerror_campfire.mp3','https://freesound.org/people/aerror/sounds/350757/','aerror','CC0'),
 ('wood_creak','rudmer_creak_v2.mp3','https://freesound.org/people/Rudmer_Rotteveel/sounds/502504/','Rudmer_Rotteveel','CC0'),
 ('wood_creak','rudmer_creak_v3.mp3','https://freesound.org/people/Rudmer_Rotteveel/sounds/502511/','Rudmer_Rotteveel','CC0'),
 ('wood_creak','rudmer_creak_v5.mp3','https://freesound.org/people/Rudmer_Rotteveel/sounds/506660/','Rudmer_Rotteveel','CC0'),
 ('shore_waves','trp_gentle_lapping.mp3','https://freesound.org/people/TRP/sounds/573163/','TRP','CC0'),
 ('shore_waves','flyfishing_gentle_waves.mp3','https://freesound.org/people/TheFlyFishingFilmmaker/sounds/614299/','TheFlyFishingFilmmaker','CC0'),
 ('wind_breeze','fthgurdy_gentle_wind.mp3','https://freesound.org/people/fthgurdy/sounds/528944/','fthgurdy','CC0'),
 ('wind_foliage','bone666138_wind_trees.mp3','https://freesound.org/people/bone666138/sounds/520183/','bone666138','CC0'),
 ('mosquito','ianfsa_mosquito.mp3','https://freesound.org/people/IanFSA/sounds/662969/','IanFSA','CC0'),
 ('rain_storm','zidane_rain.ogg','https://commons.wikimedia.org/wiki/File:Rain.ogg','ジダネ','Public domain'),
 ('owl_horned','bushman_owl.mp3','https://commons.wikimedia.org/wiki/File:Bubo_virginianus_-_Great_Horned_Owl_XC156403.mp3','Bushman','CC BY-SA 3.0'),
 ('frog_peeper','mdf_peepers.ogg','https://commons.wikimedia.org/wiki/File:Springpeepers.ogg','Mdf; edited by Borisblue','CC BY-SA 3.0'),
 ('rain_roof','ogrebane_roof.mp3','https://opengameart.org/content/rain-gutter-loop','Ogrebane','CC0'),
 ('frog_croak','splicesound_frog.mp3','https://freesound.org/people/SpliceSound/sounds/188195/','SpliceSound','CC0'),
]
def get(url):
    return urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'NorthwoodsAmbience/0.5 (audio attribution research)'}),timeout=45).read()
def main():
    evidence=ROOT/'tools/source_evidence'; evidence.mkdir(exist_ok=True)
    records=[]
    import sys
    selected=set(sys.argv[1:])
    manifest=evidence/'sources.json'
    previous=json.loads(manifest.read_text()) if manifest.exists() else []
    records=previous
    for event,name,page,author,license in SOURCES:
        if selected and event not in selected: continue
        target=ROOT/'raw_sounds'/event/name
        saved=evidence/(Path(name).stem+'.html')
        text=saved.read_text() if saved.exists() else get(page).decode()
        saved.write_text(text)
        if license == "CC0" and "freesound.org" in page:
            assert "creativecommons.org/publicdomain/zero/1.0" in text, "CC0 license missing: " + page
        if 'commons.wikimedia' in page:
            section=re.search(r'class="fullMedia".*?</div>',text,re.S).group()
            url=html.unescape(re.search(r'href="([^"]+)"',section).group(1))
        elif 'opengameart' in page:
            url=html.unescape(re.search(r'href="([^"]+rain-gutter-loop[^" ]*\.mp3)"',text).group(1))
        else:
            urls=re.findall(r'https[^\s"<>]+\.mp3',html.unescape(text))
            url=next(u for u in urls if 'previews' in u and 'hq' in u)
        url=url.split('?')[0]
        target.parent.mkdir(parents=True,exist_ok=True)
        if not target.exists(): target.write_bytes(get(url))
        records=[r for r in records if r['file']!=name]
        records.append(dict(event=event,file=name,page=page,author=author,license=license,download=url))
        print(event,target.stat().st_size,flush=True)
        (evidence/'sources.json').write_text(json.dumps(records,indent=2,ensure_ascii=False)+'\n')
if __name__=='__main__': main()
