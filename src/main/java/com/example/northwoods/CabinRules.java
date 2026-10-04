package com.example.northwoods;

/** Geometry and mix policies kept independent of the game for regression tests. */
public final class CabinRules {
    private CabinRules() {}
    public static boolean logRoof(int logColumns, int woodenWalls, boolean craftedFloor) {
        return logColumns >= 3 && (woodenWalls >= 20 || craftedFloor);
    }
    public static EnvironmentRules.Shelter classify(boolean centralBuilt, boolean centralLog,
            boolean logConstruction, boolean canopy, boolean naturalCover, float enclosure) {
        if (centralBuilt || centralLog && logConstruction)
            return enclosure >= 0.65f ? EnvironmentRules.Shelter.CABIN : EnvironmentRules.Shelter.OPEN_SHELTER;
        if (canopy || centralLog) return EnvironmentRules.Shelter.CANOPY;
        return naturalCover ? EnvironmentRules.Shelter.CAVE : EnvironmentRules.Shelter.OUTDOORS;
    }
    public static float openingGain(double distance) {
        return distance > 6 ? 0 : (float)(1 - distance / 7);
    }
    public static float outdoorGain(boolean builtCover, float enclosure, float opening, float muffle) {
        if (!builtCover) return 1;
        float effective = Math.max(0, Math.min(1, enclosure)) * (1 - 0.75f * opening);
        return 1 - muffle * effective;
    }
    public static float approach(float current, float target) {
        return Math.abs(current-target)<0.0001f ? target : current+(target-current)*0.08f;
    }
    public static boolean creaks(boolean builtCover, int woodSources, boolean underwater, boolean cave) {
        return builtCover && woodSources > 0 && !underwater && !cave;
    }
}
