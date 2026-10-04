package com.example.northwoods;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Bounded building geometry, independent of village generation or player ownership. */
public final class ShelterSampler {
    interface World extends net.minecraft.world.level.BlockGetter {
        boolean hasChunkAt(BlockPos p);
        boolean canSeeSky(BlockPos p);
    }
    private static final class LevelView implements World {
        private final Level level;
        private final Map<BlockPos,BlockState> blocks=new HashMap<>();
        LevelView(Level level) { this.level=level; }
        public boolean hasChunkAt(BlockPos p) { return level.hasChunkAt(p); }
        public boolean canSeeSky(BlockPos p) { return level.canSeeSky(p); }
        public BlockState getBlockState(BlockPos p) { return blocks.computeIfAbsent(p.immutable(),level::getBlockState); }
        public net.minecraft.world.level.material.FluidState getFluidState(BlockPos p) { return level.getFluidState(p); }
        public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos p) { return level.getBlockEntity(p); }
        public int getHeight() { return level.getHeight(); }
        public int getMinBuildHeight() { return level.getMinBuildHeight(); }
    }
    public static Sample sample(Level level, BlockPos feet) { return sample(new LevelView(level),feet); }
    public record Sample(EnvironmentRules.Shelter kind, float enclosure, float opening,
            String openingDirection, BlockPos roof, boolean rainExposed, List<BlockPos> wood) {
        public boolean built() { return kind==EnvironmentRules.Shelter.CABIN || kind==EnvironmentRules.Shelter.OPEN_SHELTER; }
    }
    private record Cover(BlockPos pos, boolean built, boolean log, boolean leaves) {}
    private record Ray(BlockPos hit, double outdoorDistance) {}

    static Sample sample(World level, BlockPos feet) {
        if(!level.hasChunkAt(feet)) return new Sample(EnvironmentRules.Shelter.CAVE,1,0,"none",null,false,List.of());
        Vec3 eye = new Vec3(feet.getX()+0.5, feet.getY()+1.35, feet.getZ()+0.5);
        Set<BlockPos> wood = new LinkedHashSet<>();
        int blocked=0, woodWalls=0;
        float opening=0; Vec3 openingVector=Vec3.ZERO;
        for (int i=0; i<32; i++) {
            double angle=i*Math.PI/16;
            Vec3 dir=new Vec3(Math.cos(angle),0,Math.sin(angle));
            Ray ray=trace(level,eye,dir,12);
            if (ray.hit!=null) {
                blocked++;
                if (wood(level.getBlockState(ray.hit))) { woodWalls++; wood.add(ray.hit); }
            } else if (ray.outdoorDistance<=6) {
                float gain=CabinRules.openingGain(ray.outdoorDistance);
                if (gain>opening) { opening=gain; openingVector=dir; }
            }
        }
        // Aim directly through doors and framed empty windows so narrow openings between rays still count.
        for(int dx=-6;dx<=6;dx++) for(int dz=-6;dz<=6;dz++) {
            if(dx*dx+dz*dz>36 || dx==0&&dz==0) continue;
            BlockPos p=feet.offset(dx,1,dz);
            if(!level.hasChunkAt(p)) continue;
            BlockState state=level.getBlockState(p);
            boolean door=state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.OPEN);
            boolean window=state.isAir() && constructed(level,p.below()) && constructed(level,p.above())
                && (constructed(level,p.north()) || constructed(level,p.south())
                    || constructed(level,p.east()) || constructed(level,p.west()));
            if(!door && !window) continue;
            Vec3 dir=new Vec3(dx,0,dz).normalize();
            Ray ray=trace(level,eye,dir,12);
            double distance=Math.sqrt(dx*dx+dz*dz);
            Vec3 beyond=eye.add(dir.scale(distance+1.0));
            boolean openPorch=lineClear(level,eye,beyond) && openSides(level,beyond)>=2;
            if(ray.hit==null && ray.outdoorDistance<=6 || openPorch) {
                float gain=CabinRules.openingGain(openPorch ? distance : ray.outdoorDistance);
                if(gain>opening) { opening=gain; openingVector=dir; }
            }
        }
        Cover center=cover(level,feet); int logs=0;
        for(int dx=-2;dx<=2;dx+=2) for(int dz=-2;dz<=2;dz+=2) {
            Cover c=cover(level,feet.offset(dx,0,dz));
            if(c.log) logs++;
            if(c.pos!=null && (c.built || c.log) && wood(level.getBlockState(c.pos))) wood.add(c.pos);
        }
        float enclosure=blocked/32f;
        boolean floor=crafted(level.getBlockState(feet.below()));
        boolean logBuilding=CabinRules.logRoof(logs,woodWalls,floor);
        var kind=CabinRules.classify(center.built,center.log,logBuilding,center.leaves,
            !level.canSeeSky(feet)&&!center.built&&!center.log&&!center.leaves,enclosure);
        boolean built=kind==EnvironmentRules.Shelter.CABIN || kind==EnvironmentRules.Shelter.OPEN_SHELTER;
        if(!built) { wood.clear(); opening=0; }
        BlockPos roof=built ? center.pos : null;
        boolean exposed=roof!=null && rainExposed(level,roof);
        String direction=opening==0 ? "none" : Math.abs(openingVector.x)>Math.abs(openingVector.z)
            ? (openingVector.x>0 ? "east":"west") : (openingVector.z>0 ? "south":"north");
        return new Sample(kind,enclosure,opening,direction,roof,exposed,List.copyOf(wood));
    }
    private static Ray trace(World level, Vec3 start, Vec3 dir, int range) {
        Vec3 end=start.add(dir.scale(range));
        double outside=Double.POSITIVE_INFINITY;
        BlockPos last=null;
        for(double d=0.25;d<=range;d+=0.25) {
            BlockPos p=BlockPos.containing(start.add(dir.scale(d)));
            if(p.equals(last)) continue;
            last=p;
            if(!level.hasChunkAt(p)) return new Ray(p,Double.POSITIVE_INFINITY);
            BlockState s=level.getBlockState(p);
            if(!s.is(BlockTags.LEAVES) && s.getCollisionShape(level,p).clip(start,end,p)!=null)
                return new Ray(p,Double.POSITIVE_INFINITY);
            // An opening must lead outside a building, including beneath trees.
            Cover c=cover(level,p.below());
            if(!c.built && !c.log && outside==Double.POSITIVE_INFINITY) outside=d;
            if(c.leaves && outside==Double.POSITIVE_INFINITY) outside=d;
            if(Double.isFinite(outside)) return new Ray(null,outside);
        }
        return new Ray(null,outside);
    }
    private static boolean lineClear(World level,Vec3 start,Vec3 end) {
        Vec3 delta=end.subtract(start); int steps=Math.max(1,(int)Math.ceil(delta.length()*4));
        for(int i=0;i<=steps;i++) {
            BlockPos p=BlockPos.containing(start.add(delta.scale(i/(double)steps)));
            if(!level.hasChunkAt(p)) return false;
            BlockState state=level.getBlockState(p);
            if(!state.is(BlockTags.LEAVES) && state.getCollisionShape(level,p).clip(start,end,p)!=null) return false;
        }
        return true;
    }
    private static int openSides(World level,Vec3 at) {
        int open=0;
        for(Vec3 dir:List.of(new Vec3(1,0,0),new Vec3(-1,0,0),new Vec3(0,0,1),new Vec3(0,0,-1)))
            if(lineClear(level,at,at.add(dir.scale(8)))) open++;
        return open;
    }
    private static Cover cover(World level, BlockPos feet) {
        if(!level.hasChunkAt(feet)) return new Cover(null,false,false,false);
        for(int dy=2;dy<=16 && feet.getY()+dy<level.getMaxBuildHeight();dy++) {
            BlockPos p=feet.above(dy); BlockState s=level.getBlockState(p);
            if(s.is(BlockTags.LEAVES)) return new Cover(p,false,false,true);
            if(s.getCollisionShape(level,p).isEmpty()) continue;
            return new Cover(p,crafted(s),s.is(BlockTags.LOGS),false);
        }
        return new Cover(null,false,false,false);
    }
    private static boolean rainExposed(World level, BlockPos roof) {
        BlockPos top=roof;
        for(int i=0;i<16 && top.getY()+1<level.getMaxBuildHeight();i++) {
            BlockPos above=top.above(); BlockState s=level.getBlockState(above);
            if(s.isAir()) return level.canSeeSky(above);
            if(s.is(BlockTags.LEAVES) || !(crafted(s)||s.is(BlockTags.LOGS))) return false;
            top=above;
        }
        return false;
    }
    private static boolean constructed(World level,BlockPos p) {
        if(!level.hasChunkAt(p)) return false;
        BlockState s=level.getBlockState(p); return crafted(s)||s.is(BlockTags.LOGS);
    }
    public static boolean wood(BlockState s) {
        return s.is(BlockTags.LOGS)||s.is(BlockTags.PLANKS)||s.is(BlockTags.WOODEN_STAIRS)
            ||s.is(BlockTags.WOODEN_SLABS)||s.is(BlockTags.WOODEN_FENCES);
    }
    private static boolean crafted(BlockState s) {
        String id=BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        return s.is(BlockTags.PLANKS)||s.is(BlockTags.WOODEN_STAIRS)||s.is(BlockTags.WOODEN_SLABS)
            ||s.is(BlockTags.WOODEN_TRAPDOORS)||id.contains("glass")||id.contains("brick")
            ||id.contains("cobblestone")||id.contains("copper")||id.contains("terracotta")
            ||id.contains("concrete")||id.contains("polished")||id.contains("tile")
            ||id.contains("iron_block")||id.contains("wool")||id.contains("quartz");
    }
}
