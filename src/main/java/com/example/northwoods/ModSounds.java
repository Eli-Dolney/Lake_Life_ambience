package com.example.northwoods;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, NorthwoodsAmbience.ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> LOON_WAIL = register("loon_wail");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOON_TREMOLO = register("loon_tremolo");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOON_YODEL = register("loon_yodel");

    // Played by Dynamic Surroundings through dsconfigs
    public static final DeferredHolder<SoundEvent, SoundEvent> CRICKETS = register("crickets");
    public static final DeferredHolder<SoundEvent, SoundEvent> OWL_BARRED = register("owl_barred");
    public static final DeferredHolder<SoundEvent, SoundEvent> OWL_HORNED = register("owl_horned");

    public static final DeferredHolder<SoundEvent, SoundEvent> FROG_PEEPER = register("frog_peeper");
    public static final DeferredHolder<SoundEvent, SoundEvent> FROG_CROAK = register("frog_croak");

    public static final DeferredHolder<SoundEvent, SoundEvent> SHORE_WAVES = register("shore_waves");

    public static final DeferredHolder<SoundEvent, SoundEvent> RAIN_STORM = register("rain_storm");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAIN_ROOF = register("rain_roof");

    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_BREEZE = register("wind_breeze");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_FOLIAGE = register("wind_foliage");
    public static final DeferredHolder<SoundEvent, SoundEvent> GULL = register("gull");
    public static final DeferredHolder<SoundEvent, SoundEvent> EAGLE = register("eagle");
    public static final DeferredHolder<SoundEvent, SoundEvent> HAWK = register("hawk");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSQUITO = register("mosquito");

    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_COZY = register("fire_cozy");
    public static final DeferredHolder<SoundEvent, SoundEvent> WOOD_CREAK = register("wood_creak");

    private ModSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, SoundEvent::createVariableRangeEvent);
    }
}
