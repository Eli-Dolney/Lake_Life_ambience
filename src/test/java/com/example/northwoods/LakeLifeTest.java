package com.example.northwoods;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.northwoods.EnvironmentRules.*;

public final class LakeLifeTest {
    @Test void temperateOceanShoreIsConfigurable() {
        var kind=waterKind(300,true,true,false,false,false,12);
        assertEquals(WaterKind.TEMPERATE_OCEAN_SHORE,kind);
        assertTrue(lakeEligible(kind,true)); assertFalse(lakeEligible(kind,false));
        assertEquals(WaterKind.OPEN_OCEAN,waterKind(500,true,true,false,false,false,60));
        assertFalse(lakeEligible(WaterKind.OPEN_OCEAN,true));
        assertEquals(WaterKind.TROPICAL_OCEAN,waterKind(300,true,true,true,false,false,12));
        assertFalse(lakeEligible(WaterKind.TROPICAL_OCEAN,true));
        assertEquals(WaterKind.FROZEN,waterKind(300,true,true,false,true,false,12));
        assertFalse(lakeEligible(WaterKind.FROZEN,true));
    }
    @Test void riverBiomeDoesNotExcludeBroadWater() {
        assertEquals(WaterKind.INLAND_LAKE,waterKind(200,true,false,false,false,true,10));
        assertEquals(WaterKind.RIVER,waterKind(60,false,false,false,false,true,4));
        assertFalse(lakeEligible(WaterKind.RIVER,true));
        assertEquals(WaterKind.POND,waterKind(8,false,false,false,false,false,4));
        assertFalse(lakeEligible(WaterKind.POND,true));
        assertEquals(WaterKind.NONE,waterKind(0,false,false,false,false,false,100));
    }
    @Test void shorelineWaterIsLocalAndCalm() {
        assertTrue(waves(WaterKind.TEMPERATE_OCEAN_SHORE,true,12,false,true));
        assertFalse(waves(WaterKind.TEMPERATE_OCEAN_SHORE,true,12,false,false));
        assertFalse(waves(WaterKind.INLAND_LAKE,true,32,false,true));
        assertEquals(0,distanceGain(32,32));
        assertFalse(waves(WaterKind.INLAND_LAKE,false,0,false,true));
        assertFalse(waves(WaterKind.INLAND_LAKE,true,10,true,true));
        assertFalse(waves(WaterKind.RIVER,true,10,false,true));
        assertEquals(1,waveWeatherGain(0,0.2f));
        assertTrue(waveWeatherGain(0.8f,0.1f)>1);
        assertTrue(waveWeatherGain(0,0.65f)>1);
        assertFalse(storm(0,1)); assertFalse(storm(1,0)); assertTrue(storm(1,1));
    }
    @Test void windRequiresExposureRatherThanAltitude() {
        assertEquals(0,exposure(false,false,0,8));
        assertEquals(0,exposure(true,true,0,8));
        assertEquals(0,exposure(true,false,4,8));
        assertTrue(exposure(true,false,0,8)>exposure(true,false,0,4));
        assertTrue(ridge(true,1,true,0));
        assertTrue(ridge(true,1,false,15));
        assertFalse(ridge(true,0.2f,true,20));
        assertFalse(ridge(false,1,true,100));
        assertFalse(ridge(true,1,false,0));
        assertEquals(0,windStrength(0,false,false,true,0,0,0));
        assertTrue(windStrength(1,true,false,false,0,0,0)>windStrength(1,false,false,true,0,0,0));
        assertTrue(windStrength(0,false,true,false,0,0,0)>0);
    }
    @Test void insectsFollowCoverHoursAndWeather() {
        assertEquals("none",cricketBlock("none",true,14000,0));
        assertEquals("no_woodland_or_grass",cricketBlock("none",false,14000,0));
        assertEquals("outside_cricket_hours",cricketBlock("none",true,6000,0));
        assertEquals("freezing",cricketBlock("freezing",true,14000,0));
        assertEquals("rain",cricketBlock("none",true,14000,1));
        assertEquals("none",mosquitoBlock("none",true,false,0,0.1f));
        assertEquals("indoors",mosquitoBlock("none",true,true,0,0.1f));
        assertEquals("no_cover_or_shore",mosquitoBlock("none",false,false,0,0.1f));
        assertEquals("heavy_rain",mosquitoBlock("none",true,false,1,0.1f));
        assertEquals("exposed_wind",mosquitoBlock("none",true,false,0,0.65f));
        assertEquals("freezing",mosquitoBlock("freezing",true,false,0,0));
    }
    @Test void birdsFollowDayHabitatAndWeather() {
        assertEquals("none",birdBlock("none",true,true,false,false));
        assertEquals("wrong_habitat",birdBlock("none",false,true,false,false));
        assertEquals("night",birdBlock("none",true,false,false,false));
        assertEquals("rain",birdBlock("none",true,true,true,false));
        assertEquals("cave",birdBlock("cave",true,true,false,false));
    }
    @Test void frequencyRespectsFirstWaitAndClipSpacing() {
        assertEquals(200,delayTicks(10,1,5));
        assertEquals(500,delayTicks(25,1,5));
        assertEquals(900,delayTicks(45,1,20));
        assertEquals(1800,delayTicks(90,1,20));
        assertEquals(500,delayTicks(25,1,20));
        assertEquals(1200,delayTicks(60,1,20));
        assertEquals(6000,delayTicks(300,1,60));
        assertEquals(400,delayTicks(25,3,20));
        assertEquals(1800,delayTicks(45,0.5,20));
    }
    @Test void firstCallsCountEligibleSecondsAndForegroundCallsWait() {
        CallScheduler scheduler=new CallScheduler();
        assertFalse(scheduler.ready("loon",true,20,() -> 200,true));
        for(int i=0;i<9;i++) assertFalse(scheduler.ready("loon",true,20,() -> 200,true));
        assertTrue(scheduler.ready("loon",true,20,() -> 200,true));
        scheduler.played("loon",900,400);
        assertFalse(scheduler.ready("gull",true,20,() -> 20,true));
        assertFalse(scheduler.ready("gull",true,20,() -> 20,true));
        scheduler.tick(400);
        assertTrue(scheduler.ready("gull",true,20,() -> 20,true));
        assertFalse(scheduler.ready("loon",false,20,() -> 200,true));
        assertFalse(scheduler.countdowns().containsKey("loon"));
        assertFalse(scheduler.ready("loon",true,20,() -> 200,true));
        scheduler.reset(); assertTrue(scheduler.countdowns().isEmpty());
        assertFalse(scheduler.ready("loon",true,20,() -> 200,true));
    }
    @Test void mosquitoesStayOccasionalAndDoNotBlockBirds() {
        CallScheduler scheduler=new CallScheduler();
        assertFalse(scheduler.ready("mosquito",true,20,() -> 3000,false));
        for(int i=0;i<149;i++) assertFalse(scheduler.ready("mosquito",true,20,() -> 3000,false));
        assertTrue(scheduler.ready("mosquito",true,20,() -> 3000,false));
        scheduler.played("mosquito",6000,0);
        assertFalse(scheduler.ready("gull",true,20,() -> 20,true));
        assertTrue(scheduler.ready("gull",true,20,() -> 20,true));
        assertFalse(scheduler.ready("mosquito",false,20,() -> 3000,false));
        assertFalse(scheduler.countdowns().containsKey("mosquito"));
    }
}
