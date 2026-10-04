# Northwoods Sound Lab — Minecraft 1.21.1

Close Minecraft. Extract `Northwoods-Sound-Lab-0.5.0.zip` and put the **Northwoods
Sound Lab** folder (the one containing `level.dat`) into your Minecraft instance's
`saves` folder. For CurseForge, use the profile's **Open Folder** command to find
that instance. Install Northwoods 0.5.0 with NeoForge, Dynamic Surroundings and
Architectury in the same instance. Open **Northwoods Sound Lab** from Singleplayer.

The world starts in Creative with cheats, at the central kiosk (240, 64, -16).
It uses actual biome assignments on a superflat foundation. Buttons teleport to
each area and set day/dusk/night/dawn or clear/rain/thunder. Weather is global.
Time and weather stay fixed until changed. Natural mob spawning, fire spreading,
and random block ticks are disabled to make comparisons repeatable. Village-style
houses have no villagers, so their voices do not mask the ambience.

| Area | Biome | What to compare |
| --- | --- | --- |
| Forest | Forest | Plank cabin, porch, doorway, glass/open windows, trees, pond, cave, sheltered roofs |
| Taiga | Taiga | Log cabin, mixed cobblestone/wood house, spruce canopy |
| Plains | Plains | Wooden and stone village houses, tall grass, clustered/unlit campfires, open fire |
| Swamp | Swamp | Pond edges, frogs and vegetation |
| Lake | Ocean | Freshwater interpretation, loons, dock, shoreline cabin |
| River | River | Large lake west of the landing, narrow channel east, small pool south |
| Winter | Snowy taiga | Frozen lake and snowy cabin; no warm-weather calls |
| Ridge | Windswept hills | Wind on the constructed ridge versus the hollow southeast |

Each area is 128 × 128 blocks, with eight neighboring areas in two rows. A button
near each area's northwest corner returns to the kiosk. `/function northwoods_lab:visit/kiosk` also returns you. Teleport functions for the areas use
`forest`, `taiga`, `plains`, `swamp`, `lake`, `river`, `winter`, and `ridge` after
`northwoods_lab:visit/`. Teleport landing points have been checked for safe footing.

## Suggested listening route

1. Select **DAY / CLEAR**, visit Forest, and use `/northwoods status`. Walk from
   the cabin to the porch and trees. Close and open the front door while standing
   near it. Compare the glass window with the empty window on the opposite wall.
2. Select **RAIN**, then **THUNDER**, at the kiosk. Compare the normal roof with
   the covered-roof cabin west of it and the roof-under-leaves cabin east of it.
   Stand under trees and inside the natural stone overhang to check suppression.
3. Compare the log cabin in Taiga and the wooden/stone houses in Plains. Wooden
   creaks are deliberately rare: allow 2–4 eligible minutes, or audition the clip
   using `/northwoods play wood_creak` without testing its scheduling.
4. Listen beside a lit campfire and the fueled furnace. Extinguish a campfire with
   a shovel and relight it using flint and steel. Place nearby campfires to check
   grouping. Fire discovery can take about two seconds; extinguished sources fade
   promptly. Empty furnaces remain quiet. Fueled furnaces eventually run out.
5. Select **DUSK / CLEAR**, visit Lake, and wait for a loon. Move indoors during a
   call. Compare River's broad lake with the south end of its narrow channel
   (237, 64, 244) and the small pool (150, 64, 247). Neighboring areas remain
   acoustically connected; these last two spots are checked for no lake loons.
6. Visit Winter and Ridge; then pause/resume, reload resources with F3+T, and
   return to the same listening spots. macOS may require Fn with function keys.

Use `/northwoods status` for eligibility, opening direction, outdoor gain, active
fire sources, roof position, and call timers. `/northwoods play fire_cozy` auditions
the recording; actual fire replacement requires a lit block.

The world builds once. Reopening it preserves edits. There is no automatic reset
button; extract a fresh copy under a different folder name for a clean test.
The embedded datapack contains the setup and control functions and is separate
from the client-only mod. Source generator: `tools/build_sound_lab.py`.
