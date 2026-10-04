#!/usr/bin/env python3
"""Validate audio registrations and measure decoded signal integrity (not a listening test)."""
import array,json,math,re,shutil,subprocess,tempfile,zipfile,os
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/northwoodsambience'
FF=shutil.which('ffmpeg') or '/opt/homebrew/bin/ffmpeg'
FP=shutil.which('ffprobe') or '/opt/homebrew/bin/ffprobe'
def main():
    data=json.loads((A/'sounds.json').read_text())
    manifest=json.loads((A/'audio_manifest.json').read_text())
    expected=set(re.findall(r'register\("([a-z_]+)"\)',(ROOT/'src/main/java/com/example/northwoods/ModSounds.java').read_text()))
    assert set(data)==expected, 'Registered sound events and assets differ'
    lang=json.loads((A/'lang/en_us.json').read_text())
    report=[]
    referenced=set()
    dependency_path=os.environ.get('DSURROUND_JAR')
    if not dependency_path:
        candidates=list((Path.home()/'.gradle/caches/modules-2/files-2.1/maven.modrinth/dynamicsurroundingsfabric/IzWXE0ve').glob('*/*.jar'))
        assert candidates, 'Set DSURROUND_JAR to the Dynamic Surroundings 0.4.5 NeoForge JAR'
        dependency_path=str(candidates[0])
    dependency=zipfile.ZipFile(dependency_path)
    scratch=tempfile.TemporaryDirectory()
    dependency_sounds=json.loads(dependency.read('assets/dsurround/sounds.json'))
    for factory in json.loads((A/'dsconfigs/sound_factories.json').read_text()):
        ns,event=factory['soundEvent'].split(':')
        assert event in (data if ns=='northwoodsambience' else dependency_sounds), factory
    biomes=json.loads((A/'dsconfigs/biomes.json').read_text())
    controlled={'northwoodsambience:crickets','dsurround:raptor','dsurround:seagulls','dsurround:biome.wind','dsurround:biome.river','northwoodsambience:forest_day','northwoodsambience:meadow_day','northwoodsambience:bog_day'}
    for rule in biomes:
        assert 'player.getY() >' not in json.dumps(rule), 'Altitude-only wind rule remains'
        for acoustic in rule.get('acoustics',[]): assert acoustic['factory'] not in controlled, 'Duplicate managed layer'
    for event,definition in data.items():
        assert definition['subtitle'] in lang
        assert definition['sounds'], event
        for entry in definition['sounds']:
            ns,name=entry['name'].split(':')
            relative=name+'.ogg'
            if ns=='northwoodsambience':
                path=A/'sounds'/relative; referenced.add(path)
                assert path.exists() and relative in manifest, path
                assert 'synth' not in manifest[relative]
            else:
                assert ns=='dsurround' and entry['name']+'.ogg' in manifest
                path=Path(scratch.name)/(name.replace('/','_')+'.ogg')
                path.write_bytes(dependency.read('assets/dsurround/sounds/'+relative))
                relative=ns+':'+relative
            info=json.loads(subprocess.check_output([FP,'-v','error','-show_streams','-of','json',str(path)]))['streams'][0]
            assert info['codec_name']=='vorbis' and info['channels']==1 and info['sample_rate']=='44100', relative
            pcm=array.array('f',subprocess.check_output([FF,'-v','error','-i',str(path),'-f','f32le','-']))
            assert pcm and all(math.isfinite(x) for x in pcm)
            peak=max(abs(x) for x in pcm); rms=math.sqrt(sum(x*x for x in pcm)/len(pcm))
            reference_gain=entry.get('volume',1)
            assert 0.0001<rms and peak*reference_gain<1, (relative,peak,reference_gain,rms)
            row=dict(file=relative,reference_gain=reference_gain,effective_peak_dbfs=round(20*math.log10(peak*reference_gain),2),seconds=round(len(pcm)/44100,3),peak_dbfs=round(20*math.log10(peak),2),rms_dbfs=round(20*math.log10(rms),2))
            if entry.get('stream'):
                jumps=sorted(abs(pcm[i]-pcm[i-1]) for i in range(1,len(pcm)))
                seam=abs(pcm[0]-pcm[-1]); p99=jumps[int(len(jumps)*.99)]
                row['seam_step']=round(seam,6); row['adjacent_step_p99']=round(p99,6)
                assert seam < max(.01,p99*4), ('Loop seam outlier',relative,seam,p99)
            report.append(row)
    assert referenced==set((A/'sounds').glob('*/*.ogg')), 'Unreferenced Ogg assets'
    dest=ROOT/'build/reports/audio-validation.json';dest.parent.mkdir(parents=True,exist_ok=True)
    dest.write_text(json.dumps(report,indent=2)+'\n')
    dependency.close(); scratch.cleanup()
    print(f'PASS: {len(data)} events, {len(report)} clips; mono Vorbis, subtitles, attribution mapping, reference peaks below full scale, no loop-seam outliers.')
    print('Signal checks do not certify species identity, background noise, or perceived loop quality.')
if __name__=='__main__':main()
