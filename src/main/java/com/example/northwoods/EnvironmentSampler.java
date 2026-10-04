package com.example.northwoods;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public final class EnvironmentSampler {
    private final LakeCacheClock cacheClock = new LakeCacheClock();
    private LakeSample lake;

    public void reset() { cacheClock.reset(); lake = null; }

    public EnvironmentSnapshot sample(Minecraft mc) {
        if (mc.player == null || mc.level == null) return null;
        Level level = mc.level;
        BlockPos feet = mc.player.blockPosition();
        if(!level.hasChunkAt(feet)) return null;
        long now = level.getGameTime();
        if (cacheClock.update(level, now, feet.getX(), feet.getY(), feet.getZ())) {
            lake = sampleLake(level, feet);
        }
        var holder = level.getBiome(feet);
        Biome biome = holder.value();
        boolean sky = level.canSeeSky(feet);
        ShelterSampler.Sample cover = ShelterSampler.sample(level,feet);
        boolean cave = cover.kind()==EnvironmentRules.Shelter.CAVE;
        boolean built = cover.built();
        int blocked = Math.round(cover.enclosure()*4);
        var shelter = cover.kind();
        boolean cold = biome.coldEnoughToSnow(feet);
        boolean rainHere = biome.getPrecipitationAt(feet) == Biome.Precipitation.RAIN;
        boolean swamp = holder.unwrapKey().map(k -> k.location().getPath().contains("swamp")).orElse(false);
        Terrain terrain = terrain(level,feet,built,sky,blocked,holder.is(BiomeTags.IS_MOUNTAIN));
        EnvironmentRules.WaterKind kind = EnvironmentRules.waterKind(lake.columns,lake.large,lake.ocean,
            lake.tropical,cold || lake.frozen,holder.is(BiomeTags.IS_RIVER),
            lake.lakeShore == null ? Double.POSITIVE_INFINITY : Vec3.atCenterOf(lake.lakeShore).distanceTo(mc.player.position()));
        boolean large = EnvironmentRules.lakeEligible(kind,NorthwoodsConfig.OCEAN_AS_LAKE.get());
        return new EnvironmentSnapshot(level.getDayTime() % 24000, level.dimension() == Level.OVERWORLD,
            shelter == EnvironmentRules.Shelter.OUTDOORS, built, sky, mc.player.isUnderWater(), cave,
            holder.is(BiomeTags.IS_FOREST), holder.is(BiomeTags.IS_TAIGA), swamp, cold,
            lake.nearestShore != null && lake.nearestShore.distSqr(feet) <= 24 * 24,
            biome.getBaseTemperature(), rainHere ? level.getRainLevel(1) : 0, level.getThunderLevel(1),
            (float)mc.player.getY(), lake.columns, large, lake.nearestShore, lake.farWater, lake.shoreline,
            mc.player.position(), shelter, cover.rainExposed() && rainHere, cacheClock.age(now),
            large ? "eligible" : kind.name().toLowerCase(java.util.Locale.ROOT),kind,lake.lakeShore,
            terrain.grassy,terrain.vegetation,holder.is(BiomeTags.IS_MOUNTAIN),terrain.exposure,terrain.ridge,now,cover);
    }

    private static BlockPos waterAt(Level level, int x, int z) {
        BlockPos probe = new BlockPos(x, level.getSeaLevel(), z);
        if (!level.hasChunkAt(probe)) return null;
        BlockPos p = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z)-1,z);
        return level.getFluidState(p).is(FluidTags.WATER) ? p : null;
    }

    private static BlockPos cachedWater(Level level, java.util.Map<Long, java.util.Optional<BlockPos>> cache, int x, int z) {
        long key = ((long)x << 32) ^ (z & 0xffffffffL);
        return cache.computeIfAbsent(key, k -> java.util.Optional.ofNullable(waterAt(level,x,z))).orElse(null);
    }

    private static LakeSample sampleLake(Level level, BlockPos origin) {
        int n = 25;
        BlockPos[][] positions = new BlockPos[n][n];
        boolean[][] inland = new boolean[n][n];
        boolean[][] oceans = new boolean[n][n], tropical = new boolean[n][n];
        java.util.Map<Integer,List<BlockPos>> cellShores = new java.util.HashMap<>();
        List<BlockPos> shores = new ArrayList<>();
        int columns = 0;
        java.util.Map<Long, java.util.Optional<BlockPos>> waterCache = new java.util.HashMap<>();
        for (int x=0; x<n; x++) for (int z=0; z<n; z++) {
            BlockPos p = cachedWater(level, waterCache, origin.getX()+(x-12)*4, origin.getZ()+(z-12)*4);
            if (p == null) continue;
            positions[x][z] = p; columns++;
            var b = level.getBiome(p);
            oceans[x][z] = b.is(BiomeTags.IS_OCEAN);
            String biomeName = b.unwrapKey().map(k -> k.location().getPath()).orElse("");
            tropical[x][z] = oceans[x][z] && (biomeName.contains("warm_ocean") || biomeName.contains("lukewarm_ocean"));
            // The shape, not the river tag, determines broad lake-shaped inland water.
            inland[x][z] = !b.value().coldEnoughToSnow(p);
            // Only refine near a coarse water/land boundary (maximum ~9k cached columns).
            boolean edge = false;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                int ex = p.getX() + dir.getStepX()*4, ez = p.getZ() + dir.getStepZ()*4;
                if (level.hasChunkAt(new BlockPos(ex,p.getY(),ez)) && cachedWater(level,waterCache,ex,ez)==null) edge=true;
            }
            if (!edge) continue;
            // Refine within the sampled cell so a narrow shoreline is not skipped by the grid.
            for (int dx=-2; dx<=2; dx++) for (int dz=-2; dz<=2; dz++) {
                BlockPos w = cachedWater(level,waterCache,p.getX()+dx,p.getZ()+dz);
                if (w == null) continue;
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos adjacent = w.relative(dir);
                    if (level.hasChunkAt(adjacent) && cachedWater(level,waterCache,adjacent.getX(),adjacent.getZ()) == null) {
                        shores.add(w); cellShores.computeIfAbsent(x*n+z,k -> new ArrayList<>()).add(w); break;
                    }
                }
            }
        }
        boolean[][] east = new boolean[n][n], south = new boolean[n][n];
        for (int x=0; x<n; x++) for (int z=0; z<n; z++) if (inland[x][z]) {
            if (x+1<n && inland[x+1][z]) east[x][z] = connectedWater(level,waterCache,positions[x][z],positions[x+1][z]);
            if (z+1<n && inland[x][z+1]) south[x][z] = connectedWater(level,waterCache,positions[x][z],positions[x][z+1]);
        }
        boolean[][] body = EnvironmentRules.largestWaterBody(inland,east,south);
        BlockPos far = null, shore = null, lakeShore = null;
        boolean ocean = false, warmOcean = false;
        double bestLakeShore = Double.MAX_VALUE;
        double best = Double.MAX_VALUE, farthest = 0;
        for (BlockPos p : shores) if (p.distSqr(origin)<best) { shore=p; best=p.distSqr(origin); }
        for (int x=0;x<n;x++) for (int z=0;z<n;z++) if (body[x][z]) {
            BlockPos p=positions[x][z]; double d=p.distSqr(origin);
            // Prefer a stable emitter across nearby water rather than the farthest audible cell.
            double emitterScore = Math.abs(Math.sqrt(d)-36);
            if (far == null || emitterScore < farthest) { far=p; farthest=emitterScore; }
            ocean |= oceans[x][z]; warmOcean |= tropical[x][z];
            for (BlockPos candidate : cellShores.getOrDefault(x*n+z,List.of())) {
                double sd=candidate.distSqr(origin);
                if (sd<bestLakeShore) { bestLakeShore=sd; lakeShore=candidate; }
            }
        }
        BlockPos surface = new BlockPos(origin.getX(),level.getHeight(Heightmap.Types.WORLD_SURFACE,origin.getX(),origin.getZ())-1,origin.getZ());
        boolean frozen = columns==0 && (level.getBlockState(surface).is(Blocks.ICE) || level.getBlockState(surface).is(Blocks.FROSTED_ICE));
        return new LakeSample(columns,EnvironmentRules.largeLake(body),shore,far,List.copyOf(shores),lakeShore,ocean,warmOcean,frozen);
    }
    private static boolean connectedWater(Level level, java.util.Map<Long, java.util.Optional<BlockPos>> cache,
            BlockPos a, BlockPos b) {
        if (a.getY()!=b.getY()) return false;
        int dx=Integer.signum(b.getX()-a.getX()), dz=Integer.signum(b.getZ()-a.getZ());
        for (int i=1; i<4; i++) {
            BlockPos mid=cachedWater(level,cache,a.getX()+dx*i,a.getZ()+dz*i);
            if (mid==null || mid.getY()!=a.getY()) return false;
        }
        return true;
    }

    private record LakeSample(int columns, boolean large, BlockPos nearestShore, BlockPos farWater,
            List<BlockPos> shoreline,BlockPos lakeShore,boolean ocean,boolean tropical,boolean frozen) {}

    // Only CLIENT heightmaps arrive in chunk packets. NO_LEAVES is server-only in 1.21.1.
    static final Heightmap.Types TERRAIN_HEIGHTMAP=Heightmap.Types.MOTION_BLOCKING;
    private static int terrainHeight(Level level,BlockPos column) {
        int height=level.getHeight(TERRAIN_HEIGHTMAP,column.getX(),column.getZ());
        for(int i=0;i<16 && height>level.getMinBuildHeight();i++) {
            BlockPos p=new BlockPos(column.getX(),height-1,column.getZ());
            var state=level.getBlockState(p);
            if(!state.isAir() && !state.is(BlockTags.LEAVES)) break;
            height--;
        }
        return height;
    }
    private record Terrain(float exposure, boolean ridge, boolean grassy, BlockPos vegetation) {}
    private static Terrain terrain(Level level, BlockPos feet, boolean builtRoof, boolean sky, int blocked, boolean mountain) {
        int open=0, samples=0, totalY=0, grass=0;
        BlockPos vegetation=null;
        for (int dx=-1; dx<=1; dx++) for (int dz=-1; dz<=1; dz++) {
            if (dx==0 && dz==0) continue;
            BlockPos p=feet.offset(dx*24,0,dz*24);
            if (!level.hasChunkAt(p)) continue;
            int height=terrainHeight(level,p);
            totalY+=height; samples++;
            if (height <= feet.getY()+3) open++;
        }
        float exposure=EnvironmentRules.exposure(sky || builtRoof,false,blocked,open);
        int ground=terrainHeight(level,feet);
        boolean ridge=EnvironmentRules.ridge(Math.abs(feet.getY()-ground)<=5,exposure,mountain,
            samples==0 ? 0 : feet.getY()-(double)totalY/samples);
        for (int dx=-8;dx<=8;dx+=4) for (int dz=-8;dz<=8;dz+=4) {
            BlockPos probe=feet.offset(dx,0,dz);
            if (!level.hasChunkAt(probe)) continue;
            int h=level.getHeight(Heightmap.Types.WORLD_SURFACE,probe.getX(),probe.getZ())-1;
            if (Math.abs(h-feet.getY())>8) continue;
            BlockPos p=new BlockPos(probe.getX(),h,probe.getZ());
            var state=level.getBlockState(p);
            if (state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN) || state.is(BlockTags.FLOWERS)) {
                grass++; if (vegetation==null || p.distSqr(feet)<vegetation.distSqr(feet)) vegetation=p;
            }
        }
        return new Terrain(exposure,ridge,grass>=2,vegetation);
    }
}
