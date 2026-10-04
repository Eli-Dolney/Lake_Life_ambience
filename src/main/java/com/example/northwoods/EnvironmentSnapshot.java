package com.example.northwoods;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public record EnvironmentSnapshot(
    long dayTime,
    boolean overworld,
    boolean outdoors,
    boolean underRoof,
    boolean canSeeSky,
    boolean underwater,
    boolean inCave,
    boolean forest,
    boolean taiga,
    boolean swamp,
    boolean snowy,
    boolean nearWater,
    float temperature,
    float rainLevel,
    float thunderLevel,
    float altitude,
    int waterColumns,
    boolean largeLake,
    BlockPos nearestShore,
    BlockPos farWater,
    List<BlockPos> shoreline,
    Vec3 playerPos,
    EnvironmentRules.Shelter shelter,
    boolean roofExposed,
    long lakeSampleAgeTicks,
    String lakeReason,
    EnvironmentRules.WaterKind waterKind,
    BlockPos lakeShore,
    boolean grassyCover,
    BlockPos vegetationSource,
    boolean mountain,
    float exposure,
    boolean ridge,
    long gameTime,
    ShelterSampler.Sample cover
) {
    public String wildlifeBlock() {
        return EnvironmentRules.wildlifeBlock(overworld, underwater, inCave, snowy);
    }

    public String loonBlock() {
        return EnvironmentRules.loonBlock(wildlifeBlock(), EnvironmentRules.lakeEligible(waterKind,NorthwoodsConfig.OCEAN_AS_LAKE.get()), farWater != null, raining(), lakeReason);
    }

    public String frogBlock() {
        return EnvironmentRules.frogBlock(wildlifeBlock(), nearWater && nearestShore != null, dayTime, thundering());
    }

    public double shoreDistance() { return nearestShore == null ? Double.POSITIVE_INFINITY : Vec3.atCenterOf(nearestShore).distanceTo(playerPos); }
    public double lakeShoreDistance() { return lakeShore == null ? Double.POSITIVE_INFINITY : Vec3.atCenterOf(lakeShore).distanceTo(playerPos); }
    public float windStrength() {
        if (shelter == EnvironmentRules.Shelter.CABIN || inCave || underwater) return 0;
        return EnvironmentRules.windStrength(exposure,ridge,wooded(),shoreDistance()<32 || grassyCover,
            rainLevel,thunderLevel,gameTime/200d) * (underRoof ? 0.3f : 1f);
    }
    public String cricketBlock() {
        return EnvironmentRules.cricketBlock(wildlifeBlock(),wooded() || grassyCover,dayTime,rainLevel);
    }
    public String gullBlock() {
        return EnvironmentRules.birdBlock(EnvironmentRules.wildlifeBlock(overworld,underwater,inCave,false),EnvironmentRules.lakeEligible(waterKind,NorthwoodsConfig.OCEAN_AS_LAKE.get())
            && lakeShoreDistance()<=40,!night(),raining() || thundering(),false);
    }
    public String eagleBlock() {
        return EnvironmentRules.birdBlock(EnvironmentRules.wildlifeBlock(overworld,underwater,inCave,false),EnvironmentRules.lakeEligible(waterKind,NorthwoodsConfig.OCEAN_AS_LAKE.get()),
            !night(),raining() || thundering(),false);
    }
    public String hawkBlock() {
        return EnvironmentRules.birdBlock(EnvironmentRules.wildlifeBlock(overworld,underwater,inCave,false),ridge || mountain || (exposure>=0.5f && grassyCover),
            !night(),raining() || thundering(),false);
    }
    public String mosquitoBlock() {
        return EnvironmentRules.mosquitoBlock(wildlifeBlock(),wooded() || grassyCover || nearWater,
            underRoof,rainLevel,windStrength());
    }

    public boolean dusk() {
        return dayTime >= 12000 && dayTime < 14000;
    }

    public boolean night() {
        return dayTime >= 13000 && dayTime < 23000;
    }

    public boolean dawn() {
        return dayTime >= 22500 || dayTime < 1000;
    }

    public boolean cricketHours() {
        return dayTime >= 12500 && dayTime < 23500;
    }

    public boolean owlHours() {
        return dayTime >= 13500 && dayTime < 22500;
    }

    public boolean frogHours() {
        return dayTime >= 12000 && dayTime < 22000;
    }

    public boolean loonPreferred() {
        return dusk() || night() || dawn();
    }

    public boolean raining() {
        return rainLevel > 0.05f;
    }

    public boolean thundering() {
        return thunderLevel > 0.2f;
    }

    public boolean wooded() {
        return forest || taiga;
    }
}
