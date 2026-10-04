# Audio sources

Processed, credited audio is included in `src/main/resources`, so building the
mod does not require downloading original recordings. Large original recordings
and conversion scratch files are excluded from Git.

Add reusable recordings to the event's subfolder and update `CREDITS.md` with
author, source, license and modifications. Run `python3 tools/convert_sounds.py`,
`python3 tools/verify_audio.py`, then `./gradlew build` (ffmpeg/ffprobe required).

The converter never uses `synth*` files. `shore_waves` explicitly selects the two
credited gentle-lapping recordings, excluding retired rough surf. It normalizes
mono 44.1 kHz Vorbis, filters rumble, splits calls, and crossfades loops. Mosquitoes
use two strongest filtered 2.5-second buzz windows. Originals remain untouched.

Loops: crickets, shore_waves, wind_breeze, wind_foliage, rain_storm, rain_roof.
Calls: loon_wail, loon_tremolo, loon_yodel, owl_barred, owl_horned, frog_peeper,
frog_croak, mosquito. Gull/eagle/hawk events reference Dynamic Surroundings files
without copying them. Source mappings are in `audio_manifest.json`.

`tools/fetch_recordings.py [event ...]` retrieves credited sources and saves their
source-page evidence/download URLs under `tools/source_evidence/`. New sources use
Freesound's publicly available high-quality MP3 previews. Redistribution licenses
are in CREDITS.md. Listening checks for unrelated noise, call identity and perceived
repetition still matter; signal validation does not replace them.
