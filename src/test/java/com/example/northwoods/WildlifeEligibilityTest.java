package com.example.northwoods;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public final class WildlifeEligibilityTest {
    @Test void loonEligibility() {
        assertEquals("none",EnvironmentRules.loonBlock("none",true,true,false,"eligible"));
        assertEquals("freezing",EnvironmentRules.loonBlock("freezing",true,true,false,"eligible"));
        assertEquals("rain",EnvironmentRules.loonBlock("none",true,true,true,"eligible"));
        assertEquals("no_lake",EnvironmentRules.loonBlock("none",false,true,false,"no_lake"));
        assertEquals("no_water_emitter",EnvironmentRules.loonBlock("none",true,false,false,"eligible"));
    }
    @Test void frogEligibility() {
        assertEquals("none",EnvironmentRules.frogBlock("none",true,14000,false));
        assertEquals("outside_frog_hours",EnvironmentRules.frogBlock("none",true,6000,false));
        assertEquals("no_nearby_shore",EnvironmentRules.frogBlock("none",false,14000,false));
        assertEquals("freezing",EnvironmentRules.frogBlock("freezing",true,14000,false));
        assertEquals("thunderstorm",EnvironmentRules.frogBlock("none",true,14000,true));
    }
    @Test void frogTimeBoundaries() {
        assertEquals("outside_frog_hours",EnvironmentRules.frogBlock("none",true,11999,false));
        assertEquals("none",EnvironmentRules.frogBlock("none",true,12000,false));
        assertEquals("none",EnvironmentRules.frogBlock("none",true,21999,false));
        assertEquals("outside_frog_hours",EnvironmentRules.frogBlock("none",true,22000,false));
    }
}
