package com.example.northwoods;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import java.util.function.Supplier;

/** A short mono recording crossing the listener; no permanent insect loop. */
public final class MosquitoFlybySound extends AbstractTickableSoundInstance {
    private final Supplier<Vec3> listener;
    private final float gain;
    private final double angle;
    private int age;
    public MosquitoFlybySound(SoundEvent event, float gain, double angle, Supplier<Vec3> listener) {
        super(event,SoundSource.AMBIENT,SoundInstance.createUnseededRandom());
        this.listener=listener; this.gain=gain; this.angle=angle;
        attenuation=SoundInstance.Attenuation.NONE; volume=0.001f; pitch=1;
        Vec3 p=listener.get(); x=p.x; y=p.y+0.2; z=p.z;
    }
    @Override public void tick() {
        if (++age>=50) { stop(); return; }
        double t=age/50d, travel=-3+6*t;
        Vec3 p=listener.get();
        x=p.x+Math.cos(angle)*travel-Math.sin(angle)*0.8;
        z=p.z+Math.sin(angle)*travel+Math.cos(angle)*0.8;
        y=p.y+0.2+0.15*Math.sin(t*Math.PI);
        volume=gain*(float)Math.sin(t*Math.PI);
    }
}
