# Northwoods Ambience 0.5.0 — Cabin, Porch, and Firelight

A client-only northern Minnesota lake-life soundscape for Minecraft **1.21.1 /
NeoForge 21.1.235+**, with **Dynamic Surroundings 0.4.5+** and **Architectury API
13.0.8+**. The development runtime is verified with Dynamic Surroundings 0.4.5.

## Install or upgrade

Download the [0.5.0 mod JAR](https://github.com/Eli-Dolney/Lake_Life_ambience/releases/download/v0.5.0/northwoodsambience-0.5.0.jar)
and optional [Northwoods Sound Lab world ZIP](https://github.com/Eli-Dolney/Lake_Life_ambience/releases/download/v0.5.0/Northwoods-Sound-Lab-0.5.0.zip)
from the [release page](https://github.com/Eli-Dolney/Lake_Life_ambience/releases/tag/v0.5.0).

Close Minecraft, remove the old Northwoods JAR, and put
`northwoodsambience-0.5.0.jar` in your instance's `mods` folder alongside the
NeoForge builds of Dynamic Surroundings and Architectury. Servers do not need
Northwoods. No world migration is needed.

Existing config values remain intact; restart adds the new controls. Crickets now
use Northwoods' controls rather than its former Dynamic Surroundings factory.
The Northwoods master volume now covers wind, birds, crickets and insects too.
Dynamic Surroundings supplies the forest/meadow/bog recordings, remaining forest
wildlife, thunder, flowing-water effects, and acoustics. Existing saved settings
remain in effect; new fire/creak keys are added on startup. The Northwoods master
volume now also covers the three managed daytime beds.

Version 0.4.1 lowers vanilla fish swimming and flop sounds to 30% of their
previous volume, so groups of fish do not dominate the lake. The change applies
to existing installations automatically and keeps the original clips and pitches.

## Cabin, porch, and firelight (0.5.0)

- Roof and wall geometry recognizes plank, log, mixed-material, and village-style
  houses. Closed doors and glass block openings; open doors and unglazed windows
  within six blocks bring the outdoors closer, including through a covered porch.
  Shelter is resampled twice per second and its volume blends over about two seconds.
- Wildlife calls keep their world positions and update volume during playback as
  you walk away or enter a cabin. Forest/meadow/bog recordings still come from
  Dynamic Surroundings; Northwoods now schedules those three beds so they share
  the cabin fade without duplicate layers. Their dependency sound identifiers and
  Dynamic Surroundings sound-volume scaling remain in use.
- Rain comes from the detected roof surface only when precipitation reaches it.
  Trees, covered roofs, and freezing conditions do not trigger roof rain.
- Nearby lit campfires, fire blocks, and fueled furnaces use a soft recorded fire.
  Adjacent sources form groups with at most two active layers within 16 blocks.
  Matching vanilla crackles are suppressed only while their replacement is active.
  Fire uses Minecraft's **Blocks** slider; it is not an outdoor ambience layer.
  Discovery takes about two seconds; extinguished sources fade promptly. Disabling
  fire replacement restores the ordinary Minecraft fire sounds.
- Wooden buildings have occasional quiet creaks, normally every 120–240 eligible
  seconds. Wind/rain changes can shorten the next interval slightly. Creaks share
  wildlife spacing and never select the same bundled clip twice consecutively.

The optional **Northwoods Sound Lab** is a separate superflat world with eight
actual biome areas, houses, lakes, a ridge, and weather/time controls. See
[SOUND_LAB.md](SOUND_LAB.md) for installation and a short listening route. It is
not installed into or generated inside your existing worlds.

## The lake-life mix

- Temperate ocean shores sound like large freshwater lake shores by default.
  Broad inland water can host loons even when its biome is named river. Small
  ponds, narrow rivers, tropical oceans, frozen water and open ocean far from
  shore do not receive loon calls.
- Loons start after **10–25 eligible seconds**, then wait **45–90 seconds by day**
  or **25–60 seconds around dawn/dusk/night** at frequency 1. Habitat loss resets
  the initial wait. Calls stop being scheduled during rain/freezing conditions.
- Gentle lake lapping has two recordings, fades out at **32 blocks**, and is
  quieter at ponds. Narrow rivers retain Dynamic Surroundings' actual flowing-water
  effects instead of a constant biome water bed. Calm water is not boosted;
  rain or exposed wind increases its level modestly.
- Breeze follows local terrain exposure and grass/shore habitat; forest wind uses
  a foliage recording. Ridges get stronger wind; a high Y coordinate alone does
  not trigger it. Cabins mute Northwoods wind, and open shelters reduce it.
- Daytime gulls call at large lake shores; actual bald eagle chirps come from
  large-water habitat and red-tailed hawk calls from exposed grassland/mountains.
  Default intervals are 60–120 seconds for gulls and 90–180 for raptors.
- Crickets need woodland or nearby grass/ferns/flowers, warm weather, and dusk/night.
  A short directional mosquito flyby occurs every 150–300 eligible seconds around
  vegetation or shorelines. Roofs, freezing, heavier rain, and exposed wind suppress it.
- Northwoods birds/frogs share a **20-second minimum foreground gap**. Mosquitoes
  are brief and independent. Loop variants reselect with fades every two minutes.

The old rough surf, altitude-only wind rule, constant river bed, and duplicate
Northwoods cricket/raptor/gull biome layers are removed. Dynamic Surroundings'
virtual cloud wind is suppressed so it cannot double ridge wind. Daytime biome
beds are kept at lower levels. Vanilla rain remains; Northwoods' extra storm layer
requires both local rain and thunder, and roof rain requires exposed built cover.

## Controls

Edit `config/northwoodsambience-client.toml` while Minecraft is closed.
Existing values are preserved, including volume choices from 0.3.0.

| Setting | Default | Meaning |
| --- | --- | --- |
| `general.masterVolume` | 1.0 | All Northwoods-managed sounds |
| `general.cabinMuffle` | 0.65 | Maximum enclosure attenuation; openings and porches reduce it |
| `lake.treatTemperateOceanShoresAsLakes` | true | Freshwater lake life at temperate ocean shores |
| `loon.volume` / `loon.frequency` | 0.65 / 1.0 | Loon level and frequency |
| `frog.volume` / `frog.frequency` | 0.30 / 1.0 | Frog level and frequency |
| `waves.volume` | 0.35 | Lapping volume |
| `storm.volume` / `storm.roofVolume` | 0.40 / 0.40 | Outdoor storm and roof rain levels |
| `wind.volume` | 0.50 | Breeze, foliage and ridge wind |
| `gulls.volume` / `gulls.frequency` | 0.50 / 1.0 | Gulls |
| `raptors.volume` / `raptors.frequency` | 0.55 / 1.0 | Eagle and hawk calls |
| `mosquito.volume` / `mosquito.frequency` | 0.18 / 1.0 | Brief flybys |
| `crickets.volume` | 0.30 | Woodland/grass-cover crickets |
| `fire.enabled` / `fire.volume` | true / 0.28 | Real fire replacement, also uses Minecraft Blocks volume |
| `cabin.creaksEnabled` / `cabin.creaksVolume` / `cabin.creaksFrequency` | true / 0.12 / 1.0 | Rare wooden-building sounds |

Each sound category also has `enabled`; creaks use `cabin.creaksEnabled`. `storm.enabled` controls both rain layers.
Call frequency 0 disables scheduling; 0.5 approximately doubles waits; maximum 3.
Minimum waits and the shared foreground gap prevent a dense overlapping chorus.
Minecraft's Ambient/Environment slider applies to ambience and creaks; the Blocks slider applies to fire. Dynamic Surroundings' own
controls still govern its remaining beds and birds; its acoustics can further
attenuate Northwoods sounds.

## Diagnostics and limitations

`/northwoods status` shows water classification, nearest-shore distance, sample
age, shelter, exposure/ridge/grass state, weather, active loops, suppression reasons,
and call countdowns. A ready call can wait for the shared foreground gap.
`/northwoods play <sound>` auditions a registered event at your position.

Water sampling refreshes every 40 ticks or eight blocks of movement and reads only
loaded chunks. Connected water at matching surface heights is used for shape
classification. The temperate ocean setting excludes warm/lukewarm oceans and
requires shore within 48 blocks; gulls require lake shore within 40 blocks.
Biome/shape rules are bounded heuristics and do not model water salinity.

Shelters use a bounded 16-block roof search, 32 horizontal wall rays up to 12
blocks, and a six-block door/window search. Log roofs require supporting building
geometry. This is a local heuristic, not a full room solver: unusually large,
underground, or heavily modded buildings may need additional tuning. Checks never
load chunks. `/northwoods status` includes enclosure, opening direction/strength,
smoothed outdoor gain, roof position, grouped fires, and the creak countdown.
Fire discovery is amortized to at most 512 block probes per tick, with active
sources revalidated separately, and covers a
16-block horizontal radius with eight blocks above/below the listener.

## Build and verify

Use JDK 21:

```sh
./gradlew build
python3 tools/verify_audio.py
./gradlew runClient
```

The JAR is in `build/libs/`. Audio checks require ffmpeg/ffprobe and the pinned
Dynamic Surroundings dependency JAR (found in Gradle's cache, or set
`DSURROUND_JAR` to its path). Tests cover environment rules and eligible-time
scheduling. See [VERIFICATION.md](VERIFICATION.md) for results and the listening
checklist, [CREDITS.md](CREDITS.md) for licenses, and
[raw_sounds/README.md](raw_sounds/README.md) for the conversion workflow.

To open the copied development Sound Lab directly, use `./gradlew runClient -PsoundLab`
with the world installed in `run/saves/Northwoods Sound Lab`.
