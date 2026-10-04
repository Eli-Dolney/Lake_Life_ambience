package com.example.northwoods;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CabinLifeTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        // Unit runtime has registries but no server datapack reload: bind fixture block tags explicitly.
        var tags=new HashMap<net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block>,List<net.minecraft.core.Holder<net.minecraft.world.level.block.Block>>>();
        tags.put(net.minecraft.tags.BlockTags.PLANKS,List.of(Blocks.OAK_PLANKS.builtInRegistryHolder(),Blocks.SPRUCE_PLANKS.builtInRegistryHolder()));
        tags.put(net.minecraft.tags.BlockTags.LOGS,List.of(Blocks.OAK_LOG.builtInRegistryHolder(),Blocks.SPRUCE_LOG.builtInRegistryHolder()));
        tags.put(net.minecraft.tags.BlockTags.LEAVES,List.of(Blocks.OAK_LEAVES.builtInRegistryHolder(),Blocks.SPRUCE_LEAVES.builtInRegistryHolder()));
        tags.put(net.minecraft.tags.BlockTags.FIRE,List.of(Blocks.FIRE.builtInRegistryHolder(),Blocks.SOUL_FIRE.builtInRegistryHolder()));
        net.minecraft.core.registries.BuiltInRegistries.BLOCK.bindTags(tags);
    }
    static class Geometry implements ShelterSampler.World {
        final Map<BlockPos,BlockState> blocks=new HashMap<>();
        boolean loaded=true;
        void put(int x,int y,int z,BlockState state) { blocks.put(new BlockPos(x,y,z),state); }
        void box(int x1,int y1,int z1,int x2,int y2,int z2,BlockState state) {
            for(int x=x1;x<=x2;x++) for(int y=y1;y<=y2;y++) for(int z=z1;z<=z2;z++) put(x,y,z,state);
        }
        public boolean hasChunkAt(BlockPos p) { return loaded; }
        public boolean canSeeSky(BlockPos p) {
            for(int y=p.getY();y<96;y++) if(!getBlockState(new BlockPos(p.getX(),y,p.getZ())).isAir()) return false;
            return true;
        }
        public BlockState getBlockState(BlockPos p) { if(!loaded) throw new AssertionError("Unloaded chunk read"); return blocks.getOrDefault(p,Blocks.AIR.defaultBlockState()); }
        public net.minecraft.world.level.material.FluidState getFluidState(BlockPos p) { return getBlockState(p).getFluidState(); }
        public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos p) { return null; }
        public int getHeight() { return 384; }
        public int getMinBuildHeight() { return -64; }
    }
    static Geometry cabin(BlockState wall,BlockState roof) {
        Geometry g=new Geometry();
        g.box(-5,63,-5,5,63,5,wall); g.box(-5,64,-5,5,67,5,wall);
        g.box(-4,64,-4,4,67,4,Blocks.AIR.defaultBlockState());g.box(-6,68,-6,6,68,6,roof);
        return g;
    }
    static void door(Geometry g,boolean open) {
        BlockState d=Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.OPEN,open).setValue(DoorBlock.FACING,Direction.NORTH);
        g.put(0,64,-5,d.setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER));
        g.put(0,65,-5,d.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));
    }
    @Test void doorwayChangesExposureAndClosedGlassDoesNot() {
        Geometry g=cabin(Blocks.OAK_PLANKS.defaultBlockState(),Blocks.OAK_PLANKS.defaultBlockState());
        BlockPos listener=new BlockPos(0,64,-3);
        door(g,false);var closed=ShelterSampler.sample(g,listener);
        door(g,true);var open=ShelterSampler.sample(g,listener);
        assertEquals(EnvironmentRules.Shelter.CABIN,closed.kind()); assertTrue(open.opening()>closed.opening());
        assertEquals("north",open.openingDirection());
        assertTrue(CabinRules.outdoorGain(true,open.enclosure(),open.opening(),.65f)>CabinRules.outdoorGain(true,closed.enclosure(),closed.opening(),.65f));
        g.box(0,64,-5,0,65,-5,Blocks.GLASS.defaultBlockState());assertEquals(0,ShelterSampler.sample(g,listener).opening());
        g.box(0,65,-5,0,65,-5,Blocks.AIR.defaultBlockState());assertTrue(ShelterSampler.sample(g,listener).opening()>0);
    }
    @Test void openingOntoCoveredPorchStillLetsForestIn() {
        Geometry g=cabin(Blocks.OAK_PLANKS.defaultBlockState(),Blocks.OAK_PLANKS.defaultBlockState());
        g.box(-6,68,-11,6,68,-6,Blocks.OAK_PLANKS.defaultBlockState());
        BlockPos listener=new BlockPos(0,64,-3);
        door(g,false);float closed=ShelterSampler.sample(g,listener).opening();
        door(g,true);assertTrue(ShelterSampler.sample(g,listener).opening()>closed);
    }
    @Test void labCabinDoorOpensOntoWidePorch() {
        Geometry g=new Geometry();
        g.box(48,64,48,60,68,62,Blocks.OAK_PLANKS.defaultBlockState());
        g.box(49,65,49,59,68,61,Blocks.AIR.defaultBlockState());
        g.box(47,69,42,61,69,63,Blocks.OAK_PLANKS.defaultBlockState());
        g.box(47,64,42,61,64,47,Blocks.OAK_PLANKS.defaultBlockState());
        BlockState d=Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.OPEN,true).setValue(DoorBlock.FACING,Direction.NORTH);
        g.put(54,65,48,d.setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER));g.put(54,66,48,d.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));
        assertTrue(ShelterSampler.sample(g,new BlockPos(54,65,50)).opening()>0);
    }
    @Test void logsAndMixedHousesAreBuildingsButTreesAreNot() {
        Geometry logs=cabin(Blocks.SPRUCE_LOG.defaultBlockState(),Blocks.SPRUCE_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X));
        var sample=ShelterSampler.sample(logs,new BlockPos(0,64,0));
        assertEquals(EnvironmentRules.Shelter.CABIN,sample.kind());assertFalse(sample.wood().isEmpty());assertTrue(sample.rainExposed());
        Geometry mixed=cabin(Blocks.COBBLESTONE.defaultBlockState(),Blocks.SPRUCE_PLANKS.defaultBlockState());
        assertEquals(EnvironmentRules.Shelter.CABIN,ShelterSampler.sample(mixed,new BlockPos(0,64,0)).kind());
        Geometry tree=new Geometry();tree.box(-3,68,-3,3,69,3,Blocks.OAK_LEAVES.defaultBlockState());tree.box(3,64,0,3,69,0,Blocks.OAK_LOG.defaultBlockState());
        var canopy=ShelterSampler.sample(tree,new BlockPos(0,64,0));
        assertEquals(EnvironmentRules.Shelter.CANOPY,canopy.kind());assertFalse(canopy.rainExposed());assertTrue(canopy.wood().isEmpty());
    }
    @Test void porchesCavesAndCoveredRoofsStayDistinct() {
        Geometry porch=new Geometry();porch.box(-5,68,-5,5,68,5,Blocks.OAK_PLANKS.defaultBlockState());
        assertEquals(EnvironmentRules.Shelter.OPEN_SHELTER,ShelterSampler.sample(porch,new BlockPos(0,64,0)).kind());
        Geometry cave=cabin(Blocks.STONE.defaultBlockState(),Blocks.STONE.defaultBlockState());
        assertEquals(EnvironmentRules.Shelter.CAVE,ShelterSampler.sample(cave,new BlockPos(0,64,0)).kind());
        Geometry g=cabin(Blocks.OAK_PLANKS.defaultBlockState(),Blocks.OAK_PLANKS.defaultBlockState());
        g.box(-6,72,-6,6,72,6,Blocks.OAK_LEAVES.defaultBlockState());assertFalse(ShelterSampler.sample(g,new BlockPos(0,64,0)).rainExposed());
        g.box(-6,72,-6,6,72,6,Blocks.STONE.defaultBlockState());assertFalse(ShelterSampler.sample(g,new BlockPos(0,64,0)).rainExposed());
        Geometry stone=cabin(Blocks.STONE_BRICKS.defaultBlockState(),Blocks.STONE_BRICKS.defaultBlockState());
        assertTrue(ShelterSampler.sample(stone,new BlockPos(0,64,0)).wood().isEmpty());
        g.loaded=false;assertDoesNotThrow(()->ShelterSampler.sample(g,new BlockPos(0,64,0)));
    }
    @Test void attenuationConvergesSmoothlyAndCallPositionsRemainIndependent() {
        float v=1,target=CabinRules.outdoorGain(true,1,0,.65f);
        for(int tick=0;tick<40;tick++) { float next=CabinRules.approach(v,target);assertTrue(next<=v&&next>=target);v=next; }
        assertEquals(target,v,.025);
        assertTrue(EnvironmentRules.distanceGain(30,130)>EnvironmentRules.distanceGain(90,130));
        assertEquals(1,CabinRules.outdoorGain(false,1,0,.65f));
        assertFalse(CabinRules.creaks(true,0,false,false));assertFalse(CabinRules.creaks(true,4,true,false));
    }
    @Test void fireClustersAreStableAndExtinguishedMembersDisappear() {
        var a=new FireGroups.Point(0,0,0);var b=new FireGroups.Point(2,0,0);var c=new FireGroups.Point(9,0,0);var d=new FireGroups.Point(15,0,0);
        var groups=FireGroups.nearest(List.of(d,b,c,a),a);assertEquals(2,groups.size());assertEquals(List.of(a,b),groups.getFirst());
        assertEquals(List.of(a),FireGroups.nearest(List.of(a,c,d),a).getFirst());
        assertEquals(List.of(),FireGroups.nearest(List.of(),a));
        assertTrue(FireSampler.isLit(Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT,true)));
        assertFalse(FireSampler.isLit(Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT,false)));
        assertFalse(FireSampler.isLit(Blocks.FURNACE.defaultBlockState()));
        assertTrue(FireSampler.isLit(Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.LIT,true)));
    }
    @Test void terrainUsesAHeightmapActuallySentToClients() {
        assertTrue(EnvironmentSampler.TERRAIN_HEIGHTMAP.sendToClient());
        assertFalse(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES.sendToClient());
    }
    @Test void dryForestNeverSchedulesALoonFromTheNoneDiagnostic() {
        assertEquals("no_large_lake",EnvironmentRules.loonBlock("none",false,false,false,"none"));
        assertEquals("no_large_lake",EnvironmentRules.loonBlock("none",false,true,false,"none"));
        assertEquals("no_water_emitter",EnvironmentRules.loonBlock("none",true,false,false,"eligible"));
        var scheduler=new CallScheduler();
        boolean eligible=EnvironmentRules.loonBlock("none",false,false,false,"none").equals("none");
        for(int i=0;i<120;i++) assertFalse(scheduler.ready("loon",eligible,20,()->200,true));
        assertTrue(scheduler.countdowns().isEmpty());
    }
    @Test void creaksRespectWildlifeGapAndResetOnWorldChange() {
        var scheduler=new CallScheduler();
        assertFalse(scheduler.ready("creak",true,20,()->2400,true));
        scheduler.played("loon",1000,400);
        assertFalse(scheduler.ready("creak",true,2400,()->2400,true));
        scheduler.tick(400);assertTrue(scheduler.ready("creak",true,20,()->2400,true));
        scheduler.reset();assertFalse(scheduler.ready("creak",true,20,()->2400,true));
    }
}
