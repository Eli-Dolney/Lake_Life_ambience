package com.example.northwoods;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class NorthwoodsConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue MASTER_VOLUME;
    public static final ModConfigSpec.DoubleValue CABIN_MUFFLE;

    public static final ModConfigSpec.BooleanValue LOON_ENABLED;
    public static final ModConfigSpec.DoubleValue LOON_VOLUME;
    public static final ModConfigSpec.BooleanValue FROG_ENABLED;
    public static final ModConfigSpec.DoubleValue FROG_VOLUME;
    public static final ModConfigSpec.BooleanValue WAVES_ENABLED;
    public static final ModConfigSpec.DoubleValue WAVES_VOLUME;
    public static final ModConfigSpec.BooleanValue STORM_ENABLED;
    public static final ModConfigSpec.DoubleValue STORM_VOLUME;

    public static final ModConfigSpec.DoubleValue ROOF_VOLUME;
    public static final ModConfigSpec.DoubleValue LOON_FREQUENCY;
    public static final ModConfigSpec.DoubleValue FROG_FREQUENCY;

    public static final ModConfigSpec.BooleanValue OCEAN_AS_LAKE;
    public static final Category WIND, GULL, RAPTOR, MOSQUITO, CRICKETS, FIRE;
    public static final ModConfigSpec.BooleanValue CREAKS_ENABLED;
    public static final ModConfigSpec.DoubleValue CREAKS_VOLUME, CREAKS_FREQUENCY;
    public record Category(ModConfigSpec.BooleanValue enabled, ModConfigSpec.DoubleValue volume,
            ModConfigSpec.DoubleValue frequency) {}
    private static Category category(ModConfigSpec.Builder builder, String name, double volume, boolean calls) {
        builder.push(name);
        Category category = new Category(builder.define("enabled",true),builder.defineInRange("volume",volume,0.0,1.0),
            calls ? builder.comment("Call frequency multiplier; 0 disables calls.").defineInRange("frequency",1.0,0.0,3.0) : null);
        builder.pop(); return category;
    }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Northwoods controls lake life, wind and crickets. Dynamic Surroundings supplies forest birds and acoustics.")
            .push("general");
        MASTER_VOLUME = builder
            .comment("Master volume multiplier for all Northwoods-managed sounds (0.0 - 1.0).")
            .defineInRange("masterVolume", 1.0, 0.0, 1.0);
        CABIN_MUFFLE = builder
            .comment("How much outdoor lake sounds are muffled when under a roof (0.0 - 1.0).")
            .defineInRange("cabinMuffle", 0.65, 0.0, 1.0);
        builder.pop();

        builder.push("loon");
        LOON_ENABLED = builder.define("enabled", true);
        LOON_FREQUENCY = builder.comment("Call frequency multiplier; 0 disables calls, 1 is the quiet cabin mix.").defineInRange("frequency", 1.0, 0.0, 3.0);
        LOON_VOLUME = builder.defineInRange("volume", 0.65, 0.0, 1.0);
        builder.pop();

        builder.push("frog");
        FROG_ENABLED = builder.define("enabled", true);
        FROG_FREQUENCY = builder.comment("Call frequency multiplier; 0 disables calls.").defineInRange("frequency", 1.0, 0.0, 3.0);
        FROG_VOLUME = builder.defineInRange("volume", 0.3, 0.0, 1.0);
        builder.pop();

        builder.push("waves");
        WAVES_ENABLED = builder.define("enabled", true);
        WAVES_VOLUME = builder.defineInRange("volume", 0.35, 0.0, 1.0);
        builder.pop();

        builder.push("storm");
        STORM_ENABLED = builder
            .comment("Heavy storm layer outdoors and rain-on-the-roof under cover.")
            .define("enabled", true);
        ROOF_VOLUME = builder.comment("Independent roof-rain volume (still controlled by storm.enabled).").defineInRange("roofVolume", 0.4, 0.0, 1.0);
        STORM_VOLUME = builder.defineInRange("volume", 0.4, 0.0, 1.0);
        builder.pop();

        builder.push("lake");
        OCEAN_AS_LAKE = builder.comment("Interpret temperate ocean shores as Minnesota freshwater lake shores.")
            .define("treatTemperateOceanShoresAsLakes",true);
        builder.pop();
        WIND = category(builder,"wind",0.5,false);
        GULL = category(builder,"gulls",0.5,true);
        RAPTOR = category(builder,"raptors",0.55,true);
        MOSQUITO = category(builder,"mosquito",0.18,true);
        CRICKETS = category(builder,"crickets",0.3,false);
        FIRE = category(builder,"fire",0.28,false);
        builder.push("cabin");
        CREAKS_ENABLED = builder.define("creaksEnabled",true);
        CREAKS_VOLUME = builder.defineInRange("creaksVolume",0.12,0.0,1.0);
        CREAKS_FREQUENCY = builder.comment("Rare wood creaks; 0 disables scheduling.").defineInRange("creaksFrequency",1.0,0.0,3.0);
        builder.pop();
        SPEC = builder.build();
    }

    private NorthwoodsConfig() {}
}
