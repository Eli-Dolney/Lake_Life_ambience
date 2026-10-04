# Sound Credits — Northwoods Ambience

Recordings are trimmed, split into individual calls, loudness-normalized and converted to mono Ogg Vorbis.

Forest beds, thunder, woodpeckers, mourning doves, crows, gulls, bald eagles, red-tailed hawks, coyotes, wolves and the extra
owl calls are played from Dynamic Surroundings' own library; see its
[CREDITS.md](https://github.com/OreCruncher/DynamicSurroundingsFabric/blob/main/CREDITS.md).

## Recordings bundled in this mod (Wikimedia Commons)

| Sound | Source file | Author | License |
| --- | --- | --- | --- |
| `loon_wail` | [Loon call.ogg](https://commons.wikimedia.org/wiki/File:Loon_call.ogg) | stephan (pdsounds.org) | Public domain |
| `loon_wail` | [Loons.ogg](https://commons.wikimedia.org/wiki/File:Loons.ogg) | unknown | Public domain |
| `loon_wail` | [Yellowstone sound library - Common Loon - 001.mp3](https://commons.wikimedia.org/wiki/File:Yellowstone_sound_library_-_Common_Loon_-_001.mp3) | NPS & MSU Acoustic Atlas / Jennifer Jerrett | Public domain |
| `loon_tremolo` | [Common loon tremolo.ogg](https://commons.wikimedia.org/wiki/File:Common_loon_tremolo.ogg) | Darwin Long | CC BY-SA 2.5 |
| `loon_yodel` | [Common loon yodels.ogg](https://commons.wikimedia.org/wiki/File:Common_loon_yodels.ogg) | Darwin Long, Peter Otto | CC BY-SA 2.5 |
| `loon_yodel` | [Loon yodel.ogg](https://commons.wikimedia.org/wiki/File:Loon_yodel.ogg) | D. Gordon E. Robertson | CC BY-SA 3.0 |
| `owl_barred` | [Barred Owl, Yellowstone National Park.oga](https://commons.wikimedia.org/wiki/File:Barred_Owl,_Yellowstone_National_Park.oga) | National Park Service | Public domain |
| `owl_barred` | [Barred Owl (Strix varia) (W1CDR0000351 BD27).ogg](https://commons.wikimedia.org/wiki/File:Barred_Owl_(Strix_varia)_(W1CDR0000351_BD27).ogg) | British Library | CC BY 4.0 |
| `crickets` | [Field cricket Gryllus pennsylvanicus.ogg](https://commons.wikimedia.org/wiki/File:Field_cricket_Gryllus_pennsylvanicus.ogg) | Thatcher | CC BY-SA 3.0 |
| `crickets` | [Field cricket unedited.ogg](https://commons.wikimedia.org/wiki/File:Field_cricket_unedited.ogg) | Thatcher | CC BY-SA 3.0 |

## Recordings added in 0.3.0

Source pages and license labels checked October 2, 2026; local evidence is in
`tools/source_evidence/`. These are field recordings representing the intended soundscape,
not a claim that every recording was made in Minnesota.

| Sound | Source | Author | License |
| --- | --- | --- | --- |
| Retired 0.3.0 `shore_waves` (not shipped in 0.4.0) | [Waves.ogg — Lake Ontario](https://commons.wikimedia.org/wiki/File:Waves.ogg) | Dsw4 | Public domain |
| `rain_storm` | [Rain.ogg](https://commons.wikimedia.org/wiki/File:Rain.ogg) | ジダネ | Public domain |
| `rain_roof` | [Rain in the Gutter Loop](https://opengameart.org/content/rain-gutter-loop) | Ogrebane | [CC0](https://creativecommons.org/publicdomain/zero/1.0/) |
| `owl_horned` | [Great Horned Owl XC156403](https://commons.wikimedia.org/wiki/File:Bubo_virginianus_-_Great_Horned_Owl_XC156403.mp3) | Bushman | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/) |
| `frog_peeper` | [Springpeepers.ogg](https://commons.wikimedia.org/wiki/File:Springpeepers.ogg) | Mdf; edited by Borisblue | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/) |
| `frog_croak` | [Green frog, croaking.wav](https://freesound.org/people/SpliceSound/sounds/188195/) | SpliceSound | [CC0](https://creativecommons.org/publicdomain/zero/1.0/) |

The frog recording uses the source site's publicly available high-quality MP3 preview.
The peeper recording also contains wood frogs; roof rain includes gutter water.

## Recordings added in 0.4.0

Source descriptions and CC0 labels checked October 2, 2026. The high-quality MP3
previews are downloaded from the recordists' Freesound pages. These recordings
represent the intended soundscape; their recording locations vary.

| Sound | Source | Author | License |
| --- | --- | --- | --- |
| `shore_waves` | [Gentle lapping lake water waves cottage](https://freesound.org/people/TRP/sounds/573163/) | TRP | [CC0](https://creativecommons.org/publicdomain/zero/1.0/) |
| `shore_waves` | [Gentle waves on a lake](https://freesound.org/people/TheFlyFishingFilmmaker/sounds/614299/) | TheFlyFishingFilmmaker | [CC0](https://creativecommons.org/publicdomain/zero/1.0/) |
| `wind_breeze` | [Gentle wind](https://freesound.org/people/fthgurdy/sounds/528944/) | fthgurdy | [CC0](https://creativecommons.org/publicdomain/zero/1.0/) |
| `wind_foliage` | [Wind in Trees — Maine](https://freesound.org/people/bone666138/sounds/520183/) | bone666138 | [CC0](https://creativecommons.org/publicdomain/zero/1.0/) |
| `mosquito` | [Mosquito 2 Edit.wav](https://freesound.org/people/IanFSA/sounds/662969/) | IanFSA | [CC0](https://creativecommons.org/publicdomain/zero/1.0/) |

`gull`, `eagle`, and `hawk` reference Dynamic Surroundings' mono files directly:
`ambient/seagulls/seagulls1`, `ambient/seagulls/seagulls2`,
`ambient/birds/bald_eagle_chirp`, and `ambient/birds/red_tailed_hawk`.
Those recordings remain in the required dependency; no copies are bundled here.
Their authors/licenses remain documented by Dynamic Surroundings; see its credits
linked above. The eagle uses the actual eagle chirp recording, separately from the
hawk call. References have gain adjustments only (gulls 0.65, eagle 0.85, hawk 0.5).

### Modifications and redistribution

Bundled clips are mono 44.1 kHz Ogg Vorbis, loudness normalized and excerpted as
needed. Most clips have a 60 Hz high-pass filter; lapping/breeze/foliage have
180 Hz high-pass and 6.5 kHz low-pass filters to reduce rumble; mosquitoes have
350 Hz high-pass and 2.2 kHz low-pass filters. Calls have short fades; loop tails
are crossfaded into their heads. Peepers use three eight-second phrases, and
mosquitoes use two 2.5-second buzz excerpts. `audio_manifest.json` maps output clips
and dependency references to their sources. Loop variants are reselected with
crossfades every two minutes during sustained listening.

Adapted CC BY-SA audio remains under its source's CC BY-SA version; it is **not**
covered by the code's MIT license. Preserve attribution, source and license links,
and this modification notice when redistributing. Existing license links:
[CC BY-SA 2.5](https://creativecommons.org/licenses/by-sa/2.5/),
[CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/),
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).
No endorsement by any recordist is implied. Public-domain recordings remain public domain.

## Recordings added in 0.5.0

| Event | Source | Author | License |
| --- | --- | --- | --- |
| `fire_cozy` | [campfire.wav](https://freesound.org/people/aerror/sounds/350757/) | aerror | [CC0](https://creativecommons.org/publicdomain/zero/1.0/) |
| `wood_creak` | [Wood Creak Single V2](https://freesound.org/people/Rudmer_Rotteveel/sounds/502504/), [V3](https://freesound.org/people/Rudmer_Rotteveel/sounds/502511/), [V5](https://freesound.org/people/Rudmer_Rotteveel/sounds/506660/) | Rudmer_Rotteveel | [CC0](https://creativecommons.org/publicdomain/zero/1.0/) |

Source pages explicitly identify CC0 and are retained under `tools/source_evidence`.
The public high-quality previews were converted to mono 44.1 kHz Vorbis. Fire uses
100 Hz high-pass, 5.5 kHz low-pass, light peak compression, loudness normalization,
and a 1.5-second loop crossfade. Creaks use 100 Hz high-pass, 5 kHz low-pass,
normalization and short fades. They are recordings of flexing wooden floorboards,
used as quiet structural wood accents. No new category required an unverified source.

The forest/meadow/bog beds keep referring to Dynamic Surroundings' existing sound
events (`biome.forest.pine`, `biome.plains`, `biome.bog`). Northwoods now controls
when they play and their shelter gain; their recordings are not copied.

## Vanilla fish sound references (0.4.1)

Fish swimming and cod/salmon/pufferfish/tropical fish flop events reference the
original Minecraft assets at 30% gain. No Minecraft recordings are copied or
redistributed. Original variants, pitches and subtitles are retained.

## Code

MIT, see `LICENSE`.
