package com.example.northwoods;

public final class EnvironmentRulesTest {
    private static int checks;
    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
    @org.junit.jupiter.api.Test
    public void environmentRegression() {
        check(EnvironmentRules.refreshLake(0,-1,0),"first sample / reset");
        check(!EnvironmentRules.refreshLake(139,100,0),"cache valid before 40 ticks");
        check(EnvironmentRules.refreshLake(140,100,0),"refresh at two seconds");
        check(EnvironmentRules.refreshLake(101,100,64),"eight-block movement invalidates");
        check(EnvironmentRules.refreshLake(0,100,0),"clock rollback invalidates");
        LakeCacheClock cache = new LakeCacheClock();
        Object worldA=new Object(), worldB=new Object();
        check(cache.update(worldA,100,0,64,0), "initial world");
        check(!cache.update(worldA,120,0,64,0), "same world retains cache");
        check(cache.age(120)==20, "diagnostic age");
        check(cache.update(worldB,120,0,64,0), "world change resets even at same time and position");
        check(cache.update(worldB,121,8,64,0), "teleport invalidates cache");
        cache.reset();
        check(cache.update(worldB,121,8,64,0), "disconnect reset");
        check(EnvironmentRules.shelter(false,false,true,4)==EnvironmentRules.Shelter.CANOPY,"trees never become roof");
        check(EnvironmentRules.shelter(false,true,false,1)==EnvironmentRules.Shelter.OPEN_SHELTER,"porch");
        check(EnvironmentRules.shelter(false,true,false,3)==EnvironmentRules.Shelter.CABIN,"cabin");
        check(EnvironmentRules.shelter(true,false,false,4)==EnvironmentRules.Shelter.CAVE,"cave");
        check(EnvironmentRules.shelter(false,false,false,0)==EnvironmentRules.Shelter.OUTDOORS,"open sky");
        boolean[][] grid=new boolean[25][25];
        check(!EnvironmentRules.largeLake(EnvironmentRules.largestWaterBody(grid)),"ocean/river-filtered grid");
        for(int x=2;x<14;x++) for(int z=2;z<14;z++) grid[x][z]=true;
        check(EnvironmentRules.largeLake(EnvironmentRules.largestWaterBody(grid)),"broad connected lake");
        check(!EnvironmentRules.largeLake(EnvironmentRules.largestWaterBody(grid,new boolean[25][25],new boolean[25][25])),
            "narrow land strips between coarse sample points prevent pool merging");
        grid=new boolean[25][25];
        for(int x=0;x<25;x++) for(int z=10;z<13;z++) grid[x][z]=true;
        check(!EnvironmentRules.largeLake(EnvironmentRules.largestWaterBody(grid)),"narrow river");
        grid=new boolean[25][25];
        for(int x=0;x<25;x++) for(int z=0;z<25;z++) grid[x][z]=x%5<3&&z%5<3;
        check(!EnvironmentRules.largeLake(EnvironmentRules.largestWaterBody(grid)),"scattered pools do not add up");
        check(EnvironmentRules.distanceGain(48,48)==0,"waves inaudible at fade boundary");
        check(EnvironmentRules.distanceGain(80,48)==0,"no minimum-volume floor");
        check(EnvironmentRules.distanceGain(0,48)==1,"source volume");
        check(EnvironmentRules.distanceGain(10,48)>EnvironmentRules.distanceGain(20,48),"distance monotonic");
        check(EnvironmentRules.wildlifeBlock(true,false,false,true).equals("freezing"),"cold suppresses calls");
        check(EnvironmentRules.wildlifeBlock(false,false,false,false).equals("outside_overworld"),"dimension");
        check(EnvironmentRules.wildlifeBlock(true,true,false,false).equals("underwater"),"underwater");
        check(EnvironmentRules.wildlifeBlock(true,false,true,false).equals("cave"),"cave silence");
        check(EnvironmentRules.wildlifeBlock(true,false,false,false).equals("none"),"warm outdoor eligibility");
        System.out.println("Passed "+checks+" environment policy checks");
    }
}
