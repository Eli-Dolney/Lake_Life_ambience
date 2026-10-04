package com.example.northwoods;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.phys.Vec3;
import java.util.function.Supplier;

/** A fixed world source whose gain follows the listener throughout the recording. */
public final class ResponsiveCallSound extends AbstractTickableSoundInstance {
    private final Supplier<Float> gain;
    private final ResourceLocation clip;
    public ResponsiveCallSound(SoundEvent event, Vec3 at, Supplier<Float> gain, ResourceLocation clip) {
        super(event,SoundSource.AMBIENT,SoundInstance.createUnseededRandom());
        this.gain=gain; this.clip=clip;
        x=at.x; y=at.y; z=at.z; volume=gain.get(); pitch=1;
        attenuation=SoundInstance.Attenuation.NONE;
    }
    @Override public boolean canStartSilent() { return true; }
    @Override public void tick() { volume=CabinRules.approach(volume,Math.max(0,Math.min(1,gain.get()))); }
    @Override public WeighedSoundEvents resolve(SoundManager manager) {
        var events=super.resolve(manager);
        if(clip!=null && events!=null) sound=new Sound(clip,ConstantFloat.of(1),ConstantFloat.of(1),1,Sound.Type.FILE,false,false,16);
        return events;
    }
}
