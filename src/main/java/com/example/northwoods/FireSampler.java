package com.example.northwoods;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.*;

/** Amortized 33x17x33 scan, with active sources revalidated every tick. */
public final class FireSampler {
    private BlockPos origin;
    private int cursor;
    private final List<BlockPos> pending=new ArrayList<>();
    private List<BlockPos> found=List.of();
    public void reset() { origin=null; cursor=0; pending.clear(); found=List.of(); }
    public static boolean lit(Level level,BlockPos p) {
        if(!level.hasChunkAt(p)) return false;
        return isLit(level.getBlockState(p));
    }
    static boolean isLit(net.minecraft.world.level.block.state.BlockState s) {
        return s.is(BlockTags.FIRE) || (s.getBlock() instanceof CampfireBlock || s.is(Blocks.FURNACE)
            ||s.is(Blocks.SMOKER)||s.is(Blocks.BLAST_FURNACE)) && s.hasProperty(BlockStateProperties.LIT)
            && s.getValue(BlockStateProperties.LIT);
    }
    public List<List<BlockPos>> tick(Level level, BlockPos listener) {
        if(origin==null || origin.distSqr(listener)>=64) { origin=listener.immutable(); cursor=0; pending.clear(); }
        int total=33*17*33;
        for(int n=0;n<512 && cursor<total;n++,cursor++) {
            int x=cursor%33-16,z=cursor/33%33-16,y=cursor/(33*33)-8;
            BlockPos p=origin.offset(x,y,z);
            if(x*x+y*y+z*z<=256 && lit(level,p)) pending.add(p);
        }
        if(cursor==total) { found=List.copyOf(pending); pending.clear(); cursor=0; origin=listener.immutable(); }
        found=found.stream().filter(p -> p.distSqr(listener)<256 && lit(level,p)).toList();
        var groups=FireGroups.nearest(found.stream().map(p->new FireGroups.Point(p.getX(),p.getY(),p.getZ())).toList(),
            new FireGroups.Point(listener.getX(),listener.getY(),listener.getZ()));
        return groups.stream().map(g->g.stream().map(p->new BlockPos(p.x(),p.y(),p.z())).toList()).toList();
    }
}
