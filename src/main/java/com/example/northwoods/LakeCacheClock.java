package com.example.northwoods;

/** Cache lifetime is tied to a world instance, game ticks, and the last sampling position. */
public final class LakeCacheClock {
    private Object world;
    private long sampledAt = -1;
    private int x, y, z;
    public void reset() { world = null; sampledAt = -1; }
    public boolean update(Object currentWorld, long tick, int px, int py, int pz) {
        double dx=(double)px-x, dy=(double)py-y, dz=(double)pz-z;
        if (world != currentWorld || EnvironmentRules.refreshLake(tick,sampledAt,dx*dx+dy*dy+dz*dz)) {
            world=currentWorld; sampledAt=tick; x=px; y=py; z=pz; return true;
        }
        return false;
    }
    public long age(long tick) { return Math.max(0,tick-sampledAt); }
}
