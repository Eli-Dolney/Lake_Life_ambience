package com.example.northwoods;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntSupplier;

/** Counts eligible time, resets on habitat loss, and spaces foreground wildlife calls. */
public final class CallScheduler {
    private final Map<String,Integer> remaining = new HashMap<>();
    private int foregroundGap;
    public void tick(int ticks) { foregroundGap = Math.max(0,foregroundGap-ticks); }
    public boolean ready(String key, boolean eligible, int elapsedTicks, IntSupplier firstDelay, boolean foreground) {
        if (!eligible) { remaining.remove(key); return false; }
        if (!remaining.containsKey(key)) {
            remaining.put(key,Math.max(1,firstDelay.getAsInt())); return false;
        }
        int next = Math.max(0,remaining.get(key)-elapsedTicks);
        remaining.put(key,next);
        return next==0 && (!foreground || foregroundGap==0);
    }
    public void played(String key, int nextDelay, int spacing) {
        remaining.put(key,Math.max(1,nextDelay)); foregroundGap=Math.max(foregroundGap,spacing);
    }
    public int foregroundRemaining() { return foregroundGap; }
    public void reset() { remaining.clear(); foregroundGap=0; }
    public Map<String,Integer> countdowns() { return Map.copyOf(remaining); }
}
