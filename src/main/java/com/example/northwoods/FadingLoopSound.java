package com.example.northwoods;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Looping ambient bed that fades toward a target volume and tracks a moving source.
 */
public final class FadingLoopSound extends AbstractTickableSoundInstance {
    private final Supplier<Float> targetVolume;
    private final Supplier<Vec3> positionSupplier;
    private final float fadeSpeed;
    private boolean stopping;
    private float currentVolume;

    public FadingLoopSound(
        SoundEvent event,
        SoundSource source,
        Supplier<Float> targetVolume,
        Supplier<Vec3> positionSupplier,
        float fadeSpeed
    ) {
        super(event, source, SoundInstance.createUnseededRandom());
        this.targetVolume = targetVolume;
        this.positionSupplier = positionSupplier;
        this.fadeSpeed = fadeSpeed;
        this.looping = true;
        this.delay = 0;
        this.relative = false;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.volume = 0.001f;
        this.currentVolume = 0.001f;
        this.pitch = 1.0f;
        Vec3 pos = positionSupplier.get();
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
    }

    public float audibleVolume() { return currentVolume; }

    public void requestStop() {
        this.stopping = true;
    }

    public void resume() { stopping = false; }

    public boolean isStopping() {
        return stopping;
    }

    @Override
    public void tick() {
        Vec3 pos = positionSupplier.get();
        this.x += (pos.x - this.x) * 0.2;
        this.y += (pos.y - this.y) * 0.2;
        this.z += (pos.z - this.z) * 0.2;

        float target = stopping ? 0.0f : Math.max(0.0f, targetVolume.get());
        if (currentVolume < target) {
            currentVolume = Math.min(target, currentVolume + fadeSpeed);
        } else if (currentVolume > target) {
            currentVolume = Math.max(target, currentVolume - fadeSpeed);
        }
        this.volume = currentVolume;

        if (stopping && currentVolume <= 0.001f) {
            this.stop();
        }
    }
}
