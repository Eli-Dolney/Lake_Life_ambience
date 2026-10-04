package com.example.northwoods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

/** Context-aware lake life; Dynamic Surroundings supplies the remaining biome acoustics. */
public final class AmbienceDirector {
    private static SoundEvent dependency(String name) { return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("dsurround",name)); }
    private static final SoundEvent FOREST_DAY=dependency("biome.forest.pine"), MEADOW_DAY=dependency("biome.plains"), BOG_DAY=dependency("biome.bog");
    private final FireSampler fireSampler=new FireSampler();
    private List<List<BlockPos>> fireGroups=List.of();
    private final Map<String,List<BlockPos>> activeFires=new HashMap<>();
    private int lastCreak=-1, rainShiftTicks;
    private float previousRain;
    private boolean verificationReloaded;
    private final EnvironmentSampler sampler = new EnvironmentSampler();
    private final CallScheduler schedule = new CallScheduler();
    private final Map<String,FadingLoopSound> beds = new HashMap<>();
    private final Map<String,AtomicReference<Float>> bedVolumes = new HashMap<>();
    private final Map<String,AtomicReference<Vec3>> bedPositions = new HashMap<>();
    private final Map<String,Integer> bedStarted = new HashMap<>();
    private final List<FadingLoopSound> retiringBeds = new ArrayList<>();
    private final List<SoundInstance> oneShots = new ArrayList<>();
    private net.minecraft.client.multiplayer.ClientLevel activeWorld;
    private int tickCounter, lastLoon=-1, lastRaptor=-1;
    private float outdoorGain=1;
    private EnvironmentSnapshot snapshot;

    public void clientTick(Minecraft mc) {
        if (mc.level!=activeWorld) {
            beds.values().forEach(mc.getSoundManager()::stop);
            retiringBeds.forEach(mc.getSoundManager()::stop);
            oneShots.forEach(mc.getSoundManager()::stop);
            beds.clear(); bedVolumes.clear(); bedPositions.clear(); bedStarted.clear();
            retiringBeds.clear(); oneShots.clear(); schedule.reset(); sampler.reset(); fireSampler.reset(); fireGroups=List.of(); activeFires.clear(); lastCreak=-1; rainShiftTicks=0; previousRain=0;
            snapshot=null; tickCounter=0; outdoorGain=1; lastLoon=-1; lastRaptor=-1; activeWorld=mc.level;
        }
        if (mc.level==null || mc.player==null || mc.isPaused()) return;
        if (snapshot!=null) {
            var cover=snapshot.cover();
            float target=CabinRules.outdoorGain(cover.built(),cover.enclosure(),cover.opening(),NorthwoodsConfig.CABIN_MUFFLE.get().floatValue());
            outdoorGain=CabinRules.approach(outdoorGain,target);
        }
        if(rainShiftTicks>0) rainShiftTicks--;
        fireGroups=enabled(NorthwoodsConfig.FIRE) ? fireSampler.tick(mc.level,mc.player.blockPosition()) : List.of();
        updateFires(mc);
        oneShots.removeIf(s -> !mc.getSoundManager().isActive(s));
        retiringBeds.removeIf(s -> s.isStopped() || !mc.getSoundManager().isActive(s));
        if (++tickCounter%10!=0) return;
        snapshot=sampler.sample(mc);
        if (snapshot==null) {
            for(String key:beds.keySet().toArray(new String[0])) stopBed(key);
            oneShots.forEach(mc.getSoundManager()::stop); oneShots.clear(); return;
        }
        if (!snapshot.overworld() || snapshot.underwater() || snapshot.inCave()) {
            for (String key : beds.keySet().toArray(new String[0])) if(!key.startsWith("fire:")) stopBed(key);
            oneShots.forEach(mc.getSoundManager()::stop); oneShots.clear(); schedule.reset(); return;
        }
        if(Math.abs(snapshot.rainLevel()-previousRain)>0.02f) rainShiftTicks=200;
        previousRain=snapshot.rainLevel();
        updateBeds(mc);
        if(tickCounter%20==0) {
            schedule.tick(20); tryOneShots(mc);
            if(Boolean.getBoolean("northwoods.verifyLab")) {
                NorthwoodsAmbience.LOGGER.info("LAB_SAMPLE pos={} shelter={} enclosure={} opening={} gain={} roofRain={} fires={} layers={} calls={} water={} lake={} ridge={} wind={}",
                    mc.player.blockPosition().toShortString(),snapshot.shelter(),snapshot.cover().enclosure(),snapshot.cover().opening(),outdoorGain,
                    snapshot.roofExposed()&&snapshot.raining(),activeFires.size(),beds.keySet(),countdowns(),snapshot.waterKind(),snapshot.largeLake(),snapshot.ridge(),snapshot.windStrength());
                if(!verificationReloaded && tickCounter>=1350) {
                    verificationReloaded=true;
                    mc.reloadResourcePacks().thenRun(()->NorthwoodsAmbience.LOGGER.info("LAB_RESOURCE_RELOAD_COMPLETE"));
                }
            }
        }
    }

    public EnvironmentSnapshot snapshot() { return snapshot; }
    public Map<String,FadingLoopSound> beds() { return beds; }
    public String countdowns() {
        return schedule.countdowns().entrySet().stream().sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey()+"="+(e.getValue()+19)/20+"s").collect(java.util.stream.Collectors.joining(", "));
    }
    public String suppression() {
        return "loon="+control(NorthwoodsConfig.LOON_ENABLED.get(),NorthwoodsConfig.LOON_VOLUME.get(),NorthwoodsConfig.LOON_FREQUENCY.get(),snapshot.loonBlock())
            +" frog="+control(NorthwoodsConfig.FROG_ENABLED.get(),NorthwoodsConfig.FROG_VOLUME.get(),NorthwoodsConfig.FROG_FREQUENCY.get(),snapshot.frogBlock())
            +" gull="+control(NorthwoodsConfig.GULL,snapshot.gullBlock())
            +" eagle="+control(NorthwoodsConfig.RAPTOR,snapshot.eagleBlock())+" hawk="+control(NorthwoodsConfig.RAPTOR,snapshot.hawkBlock())
            +" crickets="+control(NorthwoodsConfig.CRICKETS,snapshot.cricketBlock())
            +" mosquito="+control(NorthwoodsConfig.MOSQUITO,snapshot.mosquitoBlock())
            +" creak="+control(NorthwoodsConfig.CREAKS_ENABLED.get(),NorthwoodsConfig.CREAKS_VOLUME.get(),NorthwoodsConfig.CREAKS_FREQUENCY.get(),creakEligible() ? "none":"no_wooden_building")
            +" foregroundWait="+(schedule.foregroundRemaining()+19)/20+"s";
    }
    private String control(NorthwoodsConfig.Category c,String habitat) {
        return control(c.enabled().get(),c.volume().get(),c.frequency()==null ? 1 : c.frequency().get(),habitat);
    }
    private String control(boolean enabled,double volume,double frequency,String habitat) {
        if (!enabled) return "disabled";
        if (volume<=0 || master()<=0) return "muted";
        if (frequency<=0) return "frequency_zero";
        return habitat;
    }
    public void playDebug(Minecraft mc,SoundEvent event) {
        if (mc.player!=null) playAt(mc,event,mc.player.position(),1);
    }
    private float master() { return NorthwoodsConfig.MASTER_VOLUME.get().floatValue(); }
    private boolean enabled(NorthwoodsConfig.Category c) { return c.enabled().get() && c.volume().get()>0 && master()>0; }
    private float gain(NorthwoodsConfig.Category c) { return master()*outdoorGain*c.volume().get().floatValue(); }

    private void updateBeds(Minecraft mc) {
        Vec3 player=snapshot.playerPos();
        boolean waves=NorthwoodsConfig.WAVES_ENABLED.get() && EnvironmentRules.waves(snapshot.waterKind(),
            snapshot.nearestShore()!=null,snapshot.shoreDistance(),snapshot.snowy(),NorthwoodsConfig.OCEAN_AS_LAKE.get());
        Vec3 shore=snapshot.nearestShore()==null ? player : Vec3.atCenterOf(snapshot.nearestShore());
        float waveGain=master()*outdoorGain*NorthwoodsConfig.WAVES_VOLUME.get().floatValue()
            *EnvironmentRules.distanceGain(snapshot.shoreDistance(),32)
            *EnvironmentRules.waveWeatherGain(snapshot.rainLevel(),snapshot.windStrength())
            *(snapshot.waterKind()==EnvironmentRules.WaterKind.POND ? 0.5f : 1f);
        maintainBed(mc,"waves",ModSounds.SHORE_WAVES.get(),waves,waveGain,shore,0.006f,true);
        boolean storm=NorthwoodsConfig.STORM_ENABLED.get() && EnvironmentRules.storm(snapshot.rainLevel(),snapshot.thunderLevel())
            && !snapshot.underRoof();
        maintainBed(mc,"storm",ModSounds.RAIN_STORM.get(),storm,
            master()*NorthwoodsConfig.STORM_VOLUME.get().floatValue()*0.35f*snapshot.rainLevel(),player.add(0,4,0),0.008f,false);
        boolean roof=NorthwoodsConfig.STORM_ENABLED.get() && snapshot.raining() && snapshot.underRoof() && snapshot.roofExposed();
        maintainBed(mc,"roof",ModSounds.RAIN_ROOF.get(),roof,
            master()*NorthwoodsConfig.ROOF_VOLUME.get().floatValue()*snapshot.rainLevel()
                *(snapshot.shelter()==EnvironmentRules.Shelter.OPEN_SHELTER ? 0.7f : 1),
            snapshot.cover().roof()==null ? player.add(0,3,0) : Vec3.atCenterOf(snapshot.cover().roof()),0.008f,false);
        boolean dayBed=!snapshot.raining() && !snapshot.snowy() && !snapshot.night() && foundationBiome(mc);
        boolean forest=dayBed && snapshot.wooded() && !snapshot.swamp() && !snapshot.mountain();
        boolean bog=dayBed && snapshot.swamp();
        boolean meadow=dayBed && !forest && !bog && !snapshot.mountain()
            && mc.level.getBiome(mc.player.blockPosition()).is(net.minecraft.tags.BiomeTags.IS_OVERWORLD)
            && mc.level.getBiome(mc.player.blockPosition()).unwrapKey().map(k->k.location().getPath().contains("plains")||k.location().getPath().equals("meadow")).orElse(false);
        maintainBed(mc,"forest_day",FOREST_DAY,forest,master()*outdoorGain*0.18f,player,0.005f,false);
        maintainBed(mc,"meadow_day",MEADOW_DAY,meadow,master()*outdoorGain*0.15f,player,0.005f,false);
        maintainBed(mc,"bog_day",BOG_DAY,bog,master()*outdoorGain*0.18f,player,0.005f,false);
        float wind=snapshot.windStrength();
        boolean foliage=snapshot.wooded() && !snapshot.ridge();
        maintainBed(mc,"breeze",ModSounds.WIND_BREEZE.get(),enabled(NorthwoodsConfig.WIND) && !foliage && wind>0,
            gain(NorthwoodsConfig.WIND)*wind,player.add(5,2,2),0.003f,true);
        maintainBed(mc,"foliage",ModSounds.WIND_FOLIAGE.get(),enabled(NorthwoodsConfig.WIND) && foliage && wind>0,
            gain(NorthwoodsConfig.WIND)*wind,player.add(4,4,-3),0.003f,true);
        Vec3 cover=snapshot.vegetationSource()==null ? player.add(6,0,-4) : Vec3.atCenterOf(snapshot.vegetationSource());
        maintainBed(mc,"crickets",ModSounds.CRICKETS.get(),enabled(NorthwoodsConfig.CRICKETS) && snapshot.cricketBlock().equals("none"),
            gain(NorthwoodsConfig.CRICKETS)*(snapshot.night() ? 1 : 0.65f),cover,0.005f,true);
    }

    private void tryOneShots(Minecraft mc) {
        ThreadLocalRandom rng=ThreadLocalRandom.current();
        boolean loon=master()>0 && NorthwoodsConfig.LOON_ENABLED.get() && NorthwoodsConfig.LOON_VOLUME.get()>0
            && NorthwoodsConfig.LOON_FREQUENCY.get()>0 && snapshot.farWater()!=null && snapshot.loonBlock().equals("none");
        if (schedule.ready("loon",loon,20,() -> delay(10,25,NorthwoodsConfig.LOON_FREQUENCY.get(),5),true)) {
            Vec3 at=Vec3.atCenterOf(snapshot.farWater()).add(0,0.5,0);
            playResponsive(mc,pickLoon(rng),at,NorthwoodsConfig.LOON_VOLUME::get,NorthwoodsConfig.LOON_ENABLED::get,130,true,null);
            schedule.played("loon",snapshot.loonPreferred() ? delay(25,60,NorthwoodsConfig.LOON_FREQUENCY.get(),20)
                : delay(45,90,NorthwoodsConfig.LOON_FREQUENCY.get(),20),400);
        }
        boolean frog=master()>0 && NorthwoodsConfig.FROG_ENABLED.get() && NorthwoodsConfig.FROG_VOLUME.get()>0
            && NorthwoodsConfig.FROG_FREQUENCY.get()>0 && snapshot.frogBlock().equals("none");
        if (schedule.ready("frog",frog,20,() -> delay(15,30,NorthwoodsConfig.FROG_FREQUENCY.get(),16),true)) {
            Vec3 at=Vec3.atCenterOf(snapshot.nearestShore());
            playResponsive(mc,rng.nextBoolean() ? ModSounds.FROG_PEEPER.get() : ModSounds.FROG_CROAK.get(),at,NorthwoodsConfig.FROG_VOLUME::get,NorthwoodsConfig.FROG_ENABLED::get,40,true,null);
            schedule.played("frog",delay(20,50,NorthwoodsConfig.FROG_FREQUENCY.get(),16),400);
        }
        var gull=NorthwoodsConfig.GULL;
        boolean gullEligible=enabled(gull) && gull.frequency().get()>0 && snapshot.gullBlock().equals("none");
        if (schedule.ready("gull",gullEligible,20,() -> delay(20,45,gull.frequency().get(),12),true)) {
            Vec3 at=Vec3.atCenterOf(snapshot.lakeShore()).add(0,8,0);
            playResponsive(mc,ModSounds.GULL.get(),at,gull.volume()::get,gull.enabled()::get,100,true,null);
            schedule.played("gull",delay(60,120,gull.frequency().get(),20),400);
        }
        var raptor=NorthwoodsConfig.RAPTOR;
        boolean eagle=snapshot.eagleBlock().equals("none"), hawk=snapshot.hawkBlock().equals("none");
        boolean raptorEligible=enabled(raptor) && raptor.frequency().get()>0 && (eagle || hawk);
        if (schedule.ready("raptor",raptorEligible,20,() -> delay(30,75,raptor.frequency().get(),20),true)) {
            boolean chooseEagle=eagle && (!hawk || lastRaptor!=0);
            lastRaptor=chooseEagle ? 0 : 1;
            Vec3 at=chooseEagle && snapshot.farWater()!=null ? Vec3.atCenterOf(snapshot.farWater()).add(0,14,0)
                : snapshot.playerPos().add(18,18,-10);
            playResponsive(mc,chooseEagle ? ModSounds.EAGLE.get() : ModSounds.HAWK.get(),at,raptor.volume()::get,raptor.enabled()::get,130,true,null);
            schedule.played("raptor",delay(90,180,raptor.frequency().get(),20),400);
        }
        boolean creak=master()>0 && NorthwoodsConfig.CREAKS_ENABLED.get() && NorthwoodsConfig.CREAKS_VOLUME.get()>0
            && NorthwoodsConfig.CREAKS_FREQUENCY.get()>0 && creakEligible();
        double creakFrequency=NorthwoodsConfig.CREAKS_FREQUENCY.get()*(1+0.25*snapshot.windStrength()+(rainShiftTicks>0 ? 0.15:0));
        if(schedule.ready("creak",creak,20,()->delay(120,240,creakFrequency,90),true)) {
            int choice=rng.nextInt(2); if(choice>=lastCreak && lastCreak>=0) choice++;
            lastCreak=choice;
            List<BlockPos> sources=snapshot.cover().wood();
            Vec3 at=Vec3.atCenterOf(sources.get(rng.nextInt(sources.size())));
            playResponsive(mc,ModSounds.WOOD_CREAK.get(),at,NorthwoodsConfig.CREAKS_VOLUME::get,NorthwoodsConfig.CREAKS_ENABLED::get,16,false,
                ResourceLocation.fromNamespaceAndPath(NorthwoodsAmbience.ID,"wood_creak/"+(choice+1)));
            schedule.played("creak",delay(120,240,creakFrequency,90),400);
        }
        var mosquito=NorthwoodsConfig.MOSQUITO;
        boolean mosquitoEligible=enabled(mosquito) && mosquito.frequency().get()>0 && snapshot.mosquitoBlock().equals("none");
        if (schedule.ready("mosquito",mosquitoEligible,20,() -> delay(150,300,mosquito.frequency().get(),60),false)) {
            SoundInstance flyby=new MosquitoFlybySound(ModSounds.MOSQUITO.get(),gain(mosquito),rng.nextDouble(Math.PI*2),
                () -> mc.player==null ? snapshot.playerPos() : mc.player.getEyePosition());
            oneShots.add(flyby); mc.getSoundManager().play(flyby);
            schedule.played("mosquito",delay(150,300,mosquito.frequency().get(),60),0);
        }
        if (!mosquitoEligible) {
            for (SoundInstance sound : oneShots) if (sound instanceof MosquitoFlybySound) mc.getSoundManager().stop(sound);
        }
    }
    private boolean creakEligible() {
        return snapshot!=null && CabinRules.creaks(snapshot.cover().built(),snapshot.cover().wood().size(),snapshot.underwater(),snapshot.inCave());
    }
    private static boolean foundationBiome(Minecraft mc) {
        var b=mc.level.getBiome(mc.player.blockPosition());
        String id=b.unwrapKey().map(k->k.location().getPath()).orElse("");
        return !b.is(net.minecraft.tags.BiomeTags.IS_JUNGLE) && !id.contains("savanna") && !id.contains("dark_forest")
            && b.value().getBaseTemperature()<1.5f;
    }
    private void playResponsive(Minecraft mc,SoundEvent event,Vec3 at,java.util.function.Supplier<Double> volume,
            java.util.function.Supplier<Boolean> enabled,double range,boolean outdoors,ResourceLocation clip) {
        SoundInstance sound=new ResponsiveCallSound(event,at,()->mc.player==null || !enabled.get() ? 0f
            : master()*volume.get().floatValue()*(outdoors ? outdoorGain:1)*EnvironmentRules.distanceGain(mc.player.position().distanceTo(at),range),clip);
        oneShots.add(sound); mc.getSoundManager().play(sound);
    }
    private void updateFires(Minecraft mc) {
        Set<String> wanted=new HashSet<>(); activeFires.clear();
        if(enabled(NorthwoodsConfig.FIRE) && !mc.player.isUnderWater()) for(var group:fireGroups) {
            BlockPos anchor=group.getFirst(); String key="fire:"+anchor.asLong(); wanted.add(key);
            Vec3 at=Vec3.atCenterOf(anchor);
            double distance=group.stream().mapToDouble(p->mc.player.position().distanceTo(Vec3.atCenterOf(p))).min().orElse(16);
            float volume=master()*NorthwoodsConfig.FIRE.volume().get().floatValue()*EnvironmentRules.distanceGain(distance,16);
            // Fire is local to a real block, so do not apply the outdoor-cabin gain to it.
            maintainBed(mc,key,ModSounds.FIRE_COZY.get(),volume>0,volume,at,0.015f,false);
            FadingLoopSound bed=beds.get(key);
            if(bed!=null && !bed.isStopping() && mc.getSoundManager().isActive(bed) && bed.audibleVolume()>0.001f)
                activeFires.put(key,group);
        }
        for(String key:beds.keySet().toArray(new String[0])) if(key.startsWith("fire:")&&!wanted.contains(key)) stopBed(key);
    }
    public String fireStatus() { return activeFires.values().stream().map(g->g.getFirst().toShortString()+" ("+g.size()+")").collect(java.util.stream.Collectors.joining("; ")); }
    public float outdoorGain() { return outdoorGain; }
    public void filterFire(PlaySoundEvent event) {
        SoundInstance sound=event.getSound();
        if(sound==null || !sound.getLocation().getNamespace().equals("minecraft")) return;
        String id=sound.getLocation().getPath();
        if(!Set.of("block.campfire.crackle","block.fire.ambient","block.furnace.fire_crackle").contains(id)) return;
        Vec3 at=new Vec3(sound.getX(),sound.getY(),sound.getZ());
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null || !enabled(NorthwoodsConfig.FIRE)) return;
        boolean replaced=activeFires.values().stream().flatMap(List::stream)
            .anyMatch(p->at.distanceToSqr(Vec3.atCenterOf(p))<=2.25 && FireSampler.lit(mc.level,p));
        if(replaced) event.setSound(null);
    }
    private static int delay(int min,int max,double frequency,int floor) {
        return EnvironmentRules.delayTicks(ThreadLocalRandom.current().nextInt(min,max+1),frequency,floor);
    }
    private SoundEvent pickLoon(ThreadLocalRandom rng) {
        float r=rng.nextFloat(); int choice=r<0.78f ? 0 : r<0.94f ? 1 : 2;
        if (choice==lastLoon && choice!=0) choice=0;
        lastLoon=choice;
        return choice==0 ? ModSounds.LOON_WAIL.get() : choice==1 ? ModSounds.LOON_TREMOLO.get() : ModSounds.LOON_YODEL.get();
    }
    private void playAt(Minecraft mc,SoundEvent event,Vec3 pos,float volume) {
        if (volume<=0) return;
        SoundInstance sound=new SimpleSoundInstance(event.getLocation(),SoundSource.AMBIENT,Math.min(1,volume),1,
            SoundInstance.createUnseededRandom(),false,0,SoundInstance.Attenuation.NONE,pos.x,pos.y,pos.z,false);
        oneShots.add(sound); mc.getSoundManager().play(sound);
    }
    private void maintainBed(Minecraft mc,String key,SoundEvent event,boolean wanted,float volume,Vec3 position,float fadeSpeed,boolean rotate) {
        if (!wanted || volume<=0.001f) { stopBed(key); return; }
        var vol=bedVolumes.computeIfAbsent(key,k -> new AtomicReference<>(volume));
        var pos=bedPositions.computeIfAbsent(key,k -> new AtomicReference<>(position));
        vol.set(Math.min(1,volume)); pos.set(position);
        FadingLoopSound existing=beds.get(key);
        boolean replace=existing==null || existing.isStopped() || !mc.getSoundManager().isActive(existing);
        if (!replace && rotate && tickCounter-bedStarted.getOrDefault(key,tickCounter)>=2400) {
            existing.requestStop(); retiringBeds.add(existing); replace=true;
        }
        if (replace) {
            FadingLoopSound sound=new FadingLoopSound(event,key.startsWith("fire:") ? SoundSource.BLOCKS : SoundSource.AMBIENT,vol::get,pos::get,fadeSpeed);
            beds.put(key,sound); bedStarted.put(key,tickCounter); mc.getSoundManager().play(sound);
        } else existing.resume();
    }
    private void stopBed(String key) {
        FadingLoopSound sound=beds.get(key);
        if (sound==null) return;
        sound.requestStop();
        if (sound.isStopped()) { beds.remove(key); bedVolumes.remove(key); bedPositions.remove(key); bedStarted.remove(key); }
    }
}
