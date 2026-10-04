package com.example.northwoods;

import java.util.ArrayDeque;

/** Pure policies shared by the sampler, director, and deterministic regression tests. */
public final class EnvironmentRules {
    private EnvironmentRules() {}
    public enum Shelter { OUTDOORS, CANOPY, OPEN_SHELTER, CABIN, CAVE }

    public static boolean refreshLake(long now, long sampledAt, double movedSquared) {
        return sampledAt < 0 || now < sampledAt || now - sampledAt >= 40 || movedSquared >= 64;
    }

    public static Shelter shelter(boolean cave, boolean builtRoof, boolean canopy, int blockedSides) {
        if (cave) return Shelter.CAVE;
        if (builtRoof) return blockedSides >= 3 ? Shelter.CABIN : Shelter.OPEN_SHELTER;
        return canopy ? Shelter.CANOPY : Shelter.OUTDOORS;
    }

    /** Grid cells are four blocks apart; keep only the largest connected inland water body. */
    public static boolean[][] largestWaterBody(boolean[][] water) {
        return largestWaterBody(water, null, null);
    }

    public static boolean[][] largestWaterBody(boolean[][] water, boolean[][] east, boolean[][] south) {
        int n = water.length;
        boolean[][] seen = new boolean[n][n], best = new boolean[n][n];
        int bestSize = 0;
        for (int x = 0; x < n; x++) for (int z = 0; z < n; z++) {
            if (!water[x][z] || seen[x][z]) continue;
            var cells = new ArrayDeque<Integer>();
            var queue = new ArrayDeque<Integer>();
            queue.add(x * n + z); seen[x][z] = true;
            while (!queue.isEmpty()) {
                int cell = queue.remove(), cx = cell / n, cz = cell % n;
                cells.add(cell);
                for (int[] d : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                    int nx = cx + d[0], nz = cz + d[1];
                    if (nx >= 0 && nz >= 0 && nx < n && nz < n && water[nx][nz] && !seen[nx][nz]) {
                        boolean connected = east == null || (d[0] == 1 ? east[cx][cz]
                            : d[0] == -1 ? east[nx][nz] : d[1] == 1 ? south[cx][cz] : south[nx][nz]);
                        if (connected) { seen[nx][nz] = true; queue.add(nx * n + nz); }
                    }
                }
            }
            if (cells.size() > bestSize) {
                bestSize = cells.size(); best = new boolean[n][n];
                for (int c : cells) best[c / n][c % n] = true;
            }
        }
        return best;
    }

    public static boolean largeLake(boolean[][] body) {
        int count = 0, broad = 0, n = body.length;
        for (int x = 0; x < n; x++) for (int z = 0; z < n; z++) {
            if (!body[x][z]) continue;
            count++;
            if (x > 0 && z > 0 && x < n-1 && z < n-1) {
                boolean interior = true;
                for (int dx=-1; dx<=1; dx++) for (int dz=-1; dz<=1; dz++) interior &= body[x+dx][z+dz];
                if (interior) broad++;
            }
        }
        return count >= 100 && broad >= 36;
    }

    public static float distanceGain(double distance, double range) {
        float t = (float)Math.max(0, Math.min(1, 1 - distance / range));
        return t * t * (3 - 2 * t);
    }

    public enum WaterKind { NONE, POND, RIVER, INLAND_LAKE, TEMPERATE_OCEAN_SHORE, OPEN_OCEAN, TROPICAL_OCEAN, FROZEN }

    public static WaterKind waterKind(int columns, boolean broad, boolean ocean, boolean tropical,
            boolean frozen, boolean river, double shoreDistance) {
        if (columns == 0) return frozen ? WaterKind.FROZEN : WaterKind.NONE;
        if (frozen) return WaterKind.FROZEN;
        if (tropical) return WaterKind.TROPICAL_OCEAN;
        if (ocean) return broad && shoreDistance <= 48 ? WaterKind.TEMPERATE_OCEAN_SHORE : WaterKind.OPEN_OCEAN;
        if (broad) return WaterKind.INLAND_LAKE;
        return river ? WaterKind.RIVER : WaterKind.POND;
    }

    public static boolean lakeEligible(WaterKind kind, boolean oceanAsLake) {
        return kind == WaterKind.INLAND_LAKE || oceanAsLake && kind == WaterKind.TEMPERATE_OCEAN_SHORE;
    }

    public static float exposure(boolean sky, boolean builtRoof, int blockedSides, int openDirections) {
        if (builtRoof || !sky) return 0;
        return Math.max(0,Math.min(1,(openDirections/8f)*(1-blockedSides/4f)));
    }

    public static boolean ridge(boolean onSurface, float exposure, boolean mountain, double relief) {
        return onSurface && exposure >= 0.5f && (mountain || relief >= 12);
    }

    public static float windStrength(float exposure, boolean ridge, boolean foliage, boolean shoreOrGrass,
            float rain, float thunder, double phase) {
        if (!ridge && !foliage && !shoreOrGrass) return 0;
        float base = ridge ? 0.65f : foliage ? 0.14f : 0.2f;
        float gust = (float)(0.8 + 0.2*Math.sin(phase));
        return Math.min(1,(base + Math.max(rain*0.1f,thunder*0.2f))*gust*(foliage ? Math.max(0.35f,exposure) : exposure));
    }

    public static boolean storm(float rain, float thunder) { return rain > 0.05f && thunder > 0.2f; }
    public static boolean waves(WaterKind kind, boolean shore, double distance, boolean cold, boolean oceanAsLake) {
        return shore && !cold && distance < 32 && (kind == WaterKind.POND || lakeEligible(kind,oceanAsLake));
    }
    public static float waveWeatherGain(float rain, float wind) {
        return 1 + Math.max(rain > 0.05f ? rain*0.25f : 0,wind >= 0.45f ? wind*0.35f : 0);
    }
    public static String cricketBlock(String common, boolean cover, long time, float rain) {
        if (!common.equals("none")) return common;
        if (!cover) return "no_woodland_or_grass";
        if (time < 12000 || time >= 23500) return "outside_cricket_hours";
        return rain > 0.05f ? "rain" : "none";
    }
    public static String birdBlock(String common, boolean habitat, boolean day, boolean raining, boolean cabin) {
        if (!common.equals("none")) return common;
        if (!habitat) return "wrong_habitat";
        if (!day) return "night";
        if (raining) return "rain";
        return cabin ? "indoors" : "none";
    }
    public static String mosquitoBlock(String common, boolean habitat, boolean roof, float rain, float wind) {
        if (!common.equals("none")) return common;
        if (roof) return "indoors";
        if (!habitat) return "no_cover_or_shore";
        if (rain > 0.2f) return "heavy_rain";
        return wind >= 0.45f ? "exposed_wind" : "none";
    }
    public static int delayTicks(int seconds, double frequency, int minimumSeconds) {
        return (int)Math.max(minimumSeconds*20,seconds*20/Math.max(0.001,frequency));
    }

    public static String loonBlock(String common, boolean lake, boolean emitter, boolean raining, String lakeReason) {
        if (!common.equals("none")) return common;
        if (!lake) return lakeReason.equals("none") || lakeReason.equals("eligible") ? "no_large_lake" : lakeReason;
        if (!emitter) return "no_water_emitter";
        return raining ? "rain" : "none";
    }

    public static String frogBlock(String common, boolean nearWater, long time, boolean thunder) {
        if (!common.equals("none")) return common;
        if (!nearWater) return "no_nearby_shore";
        if (time < 12000 || time >= 22000) return "outside_frog_hours";
        return thunder ? "thunderstorm" : "none";
    }

    public static String wildlifeBlock(boolean overworld, boolean underwater, boolean cave, boolean cold) {
        if (!overworld) return "outside_overworld";
        if (underwater) return "underwater";
        if (cave) return "cave";
        if (cold) return "freezing";
        return "none";
    }
}
