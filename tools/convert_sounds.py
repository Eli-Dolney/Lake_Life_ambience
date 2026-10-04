#!/usr/bin/env python3
"""Convert raw_sounds/<event>/ recordings into Minecraft-ready mono Ogg Vorbis and regenerate sounds.json.

Each folder under raw_sounds/ is one sound event. Files whose names start with "synth" are
placeholders and are never included in release conversion.
"""
import array
import json
import math
import os
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RAW = ROOT / "raw_sounds"
WORK = RAW / "_work"
ASSETS = ROOT / "src/main/resources/assets/northwoodsambience"
OUT = ASSETS / "sounds"
FFMPEG = os.environ.get("FFMPEG", shutil.which("ffmpeg") or "/opt/homebrew/bin/ffmpeg")
FFPROBE = os.environ.get("FFPROBE", shutil.which("ffprobe") or "/opt/homebrew/bin/ffprobe")
MOD_ID = "northwoodsambience"
AUDIO_EXT = {".wav", ".mp3", ".ogg", ".oga", ".flac", ".m4a", ".aiff", ".aif"}

# event -> (kind, subtitle key suffix). Loops are crossfaded; one-shots are split into calls.
EVENTS = {
    "fire_cozy": ("loop", "fire"),
    "wood_creak": ("oneshot", "creak"),
    "loon_wail": ("oneshot", "loon"),
    "loon_tremolo": ("oneshot", "loon"),
    "loon_yodel": ("oneshot", "loon"),
    "crickets": ("loop", "crickets"),
    "owl_barred": ("oneshot", "owl"),
    "owl_horned": ("oneshot", "owl"),
    "frog_peeper": ("oneshot", "frog"),
    "frog_croak": ("oneshot", "frog"),
    "shore_waves": ("loop", "waves"),
    "rain_storm": ("loop", "rain"),
    "rain_roof": ("loop", "rain"),
    "wind_breeze": ("loop", "wind"),
    "wind_foliage": ("loop", "wind"),
    "mosquito": ("oneshot", "mosquito"),
}

# Refer to the dependency's files in place; do not duplicate or relicence its recordings.
DEPENDENCY_SOUNDS = {
    "gull": ("gull", ["ambient/seagulls/seagulls1", "ambient/seagulls/seagulls2"]),
    "eagle": ("eagle", ["ambient/birds/bald_eagle_chirp"]),
    "hawk": ("hawk", ["ambient/birds/red_tailed_hawk"]),
}

ONESHOT_SPLIT_OVER = 20.0
MAX_CALLS_PER_FILE = 6
LOOP_MAX = 60.0
LOOP_CROSSFADE = 1.5


def run(args):
    subprocess.run(args, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)


def duration(path):
    out = subprocess.run(
        [FFPROBE, "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(path)],
        check=True, capture_output=True, text=True,
    ).stdout.strip()
    return float(out)


def to_mono_wav(src, dest):
    event = src.parent.name
    filtering = "highpass=f=60"
    if event in {"shore_waves", "wind_breeze", "wind_foliage"}: filtering = "highpass=f=180,lowpass=f=6500"
    if event == "fire_cozy": filtering = "highpass=f=100,lowpass=f=5500,acompressor=threshold=0.12:ratio=3:attack=5:release=120"
    if event == "wood_creak": filtering = "highpass=f=100,lowpass=f=5000"
    if event == "mosquito": filtering = "highpass=f=350,lowpass=f=2200"
    run([FFMPEG, "-y", "-hide_banner", "-loglevel", "error", "-i", str(src),
         "-af", "aformat=channel_layouts=mono,"+filtering+",loudnorm=I=-22:TP=-3:LRA=11",
         "-ar", "44100", str(dest)])


def find_calls(wav):
    """Return (start, end) spans that stand out above the recording's own background noise."""
    rate, window = 8000, 0.1
    pcm = subprocess.run(
        [FFMPEG, "-v", "error", "-i", str(wav), "-af", "highpass=f=250", "-ac", "1", "-ar", str(rate),
         "-f", "s16le", "-"],
        check=True, capture_output=True,
    ).stdout
    samples = array.array("h", pcm)
    step = int(rate * window)
    levels = []
    for i in range(0, len(samples) - step, step):
        chunk = samples[i:i + step]
        rms = math.sqrt(sum(s * s for s in chunk) / step) or 1.0
        levels.append(20 * math.log10(rms))
    if not levels:
        return []
    floor = sorted(levels)[len(levels) // 5]
    active = [lv > floor + 10 for lv in levels]

    spans, start, gap = [], None, 0
    for idx, on in enumerate(active + [False] * 13):
        if on:
            if start is None:
                start = idx
            gap = 0
        elif start is not None:
            gap += 1
            if gap > 12:
                spans.append((start, idx - gap + 1))
                start, gap = None, 0

    total = len(levels) * window
    calls = []
    for a, b in spans:
        begin, end = a * window, b * window
        if end - begin < 1.0:
            continue
        end = min(end, begin + 15)
        loudness = max(levels[a:b])
        calls.append((loudness, max(0.0, begin - 0.2), min(total, end + 0.4)))
    loudest = sorted(calls, reverse=True)[:MAX_CALLS_PER_FILE]
    return sorted((s, e) for _, s, e in loudest)


def encode_oneshot(wav, dest, start=None, end=None):
    trim = [] if start is None else ["-ss", f"{start:.3f}", "-to", f"{end:.3f}"]
    length = (end - start) if start is not None else duration(wav)
    fade_out = max(0.0, length - 0.12)
    run([FFMPEG, "-y", "-hide_banner", "-loglevel", "error", *trim, "-i", str(wav),
         "-af", f"afade=t=in:st=0:d=0.04,afade=t=out:st={fade_out:.3f}:d=0.12,"
                "loudnorm=I=-20:TP=-3:LRA=11,aformat=channel_layouts=mono",
         "-ar", "44100", "-c:a", "libvorbis", "-q:a", "5", str(dest)])


def encode_loop(wav, dest):
    length = min(duration(wav), LOOP_MAX)
    xf = min(LOOP_CROSSFADE, length / 4)
    # Body [xf, length] crossfades into head [0, xf]; restarting at the body start is then seamless.
    run([FFMPEG, "-y", "-hide_banner", "-loglevel", "error", "-i", str(wav),
         "-filter_complex",
         f"[0]atrim=0:{length:.3f},asplit[a][b];"
         f"[a]atrim={xf:.3f}:{length:.3f},asetpts=PTS-STARTPTS[body];"
         f"[b]atrim=0:{xf:.3f},asetpts=PTS-STARTPTS[head];"
         f"[body][head]acrossfade=d={xf:.3f}:c1=qsin:c2=qsin,aformat=channel_layouts=mono[out]",
         "-map", "[out]", "-ar", "44100", "-c:a", "libvorbis", "-q:a", "5", str(dest)])


def sources_for(event):
    folder = RAW / event
    if not folder.is_dir():
        return [], False
    files = sorted(p for p in folder.iterdir() if p.suffix.lower() in AUDIO_EXT)
    real = [p for p in files if not p.name.lower().startswith("synth")]
    if event == "shore_waves":
        real = [p for p in real if p.name in {"trp_gentle_lapping.mp3", "flyfishing_gentle_waves.mp3"}]
    return real, False


def main():
    selected = set(sys.argv[1:]) or set(EVENTS)
    assert selected <= set(EVENTS), "Unknown event"
    missing_sources = [e for e in selected if not sources_for(e)[0]]
    if missing_sources:
        raise SystemExit("Real recordings required: " + ", ".join(missing_sources))
    WORK.mkdir(parents=True, exist_ok=True)
    sounds_json = json.loads((ASSETS / "sounds.json").read_text()) if selected != set(EVENTS) else {}
    manifest = json.loads((ASSETS / "audio_manifest.json").read_text()) if selected != set(EVENTS) else {}
    report = []
    for event, (kind, subtitle) in EVENTS.items():
        if event not in selected: continue
        manifest = {k:v for k,v in manifest.items() if not k.startswith(event+"/")}
        files, placeholder = sources_for(event)
        out_dir = OUT / event
        if out_dir.exists():
            shutil.rmtree(out_dir)
        out_dir.mkdir(parents=True)
        entries, n = [], 0
        for src in files:
            first = n + 1
            wav = WORK / f"{event}__{src.stem}.wav"
            to_mono_wav(src, wav)
            if kind == "loop":
                n += 1
                encode_loop(wav, out_dir / f"{n}.ogg")
            elif event == "mosquito":
                # Select two strongest short buzz phrases in the filtered recording.
                pcm = array.array("f", subprocess.check_output([FFMPEG,"-v","error","-i",str(wav),"-ar","8000","-f","f32le","-"]))
                window = 20000
                candidates = [(sum(v*v for v in pcm[i:i+window]),i/8000) for i in range(0,len(pcm)-window,window)]
                for _, start in sorted(candidates,reverse=True)[:2]:
                    n += 1
                    encode_oneshot(wav,out_dir / f"{n}.ogg",start,start+2.5)
            elif event == "frog_peeper":
                # A steady chorus has no isolated energy peaks: preserve short natural phrases.
                for start, end in [(2, 10), (12, 20), (21, 29)]:
                    n += 1
                    encode_oneshot(wav, out_dir / f"{n}.ogg", start, min(end, duration(wav)))
            elif duration(wav) > ONESHOT_SPLIT_OVER:
                for start, end in find_calls(wav):
                    n += 1
                    encode_oneshot(wav, out_dir / f"{n}.ogg", start, end)
            else:
                n += 1
                encode_oneshot(wav, out_dir / f"{n}.ogg")
            for index in range(first, n + 1):
                manifest[f"{event}/{index}.ogg"] = str(src.relative_to(ROOT))
        for i in range(1, n + 1):
            entries.append({"name": f"{MOD_ID}:{event}/{i}", "stream": kind == "loop"})
        if entries:
            sounds_json[event] = {"sounds": entries, "subtitle": f"subtitles.{MOD_ID}.{subtitle}"}
        report.append(f"{event:14} {kind:8} {n:2} clip(s) from {len(files)} file(s)"
                      + ("  [PLACEHOLDER]" if placeholder else ""))

    for event,(subtitle,paths) in DEPENDENCY_SOUNDS.items():
        sounds_json[event] = {"subtitle": f"subtitles.{MOD_ID}.{subtitle}",
            "sounds": [{"name": "dsurround:"+p, "stream": False, "volume": {"gull": 0.65, "eagle": 0.85, "hawk": 0.5}[event]} for p in paths]}
        for path in paths: manifest["dsurround:"+path+".ogg"] = "Dynamic Surroundings resource reference; see CREDITS.md"
    (ASSETS / "sounds.json").write_text(json.dumps(sounds_json, indent=2) + "\n")
    (ASSETS / "audio_manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    print("\n".join(report))
    missing = [e for e in EVENTS if e not in sounds_json]
    if missing:
        raise SystemExit("No audio extracted for " + ", ".join(missing))


if __name__ == "__main__":
    main()
