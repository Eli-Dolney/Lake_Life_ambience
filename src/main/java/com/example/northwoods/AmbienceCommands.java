package com.example.northwoods;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class AmbienceCommands {
    private AmbienceCommands() {}

    public static void register(RegisterClientCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("northwoods")
            .then(Commands.literal("status").executes(ctx -> {
                showStatus(ctx.getSource());
                return 1;
            }))
            .then(Commands.literal("play")
                .then(Commands.argument("sound", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        for (String key : soundMap().keySet()) {
                            builder.suggest(key);
                        }
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "sound");
                        SoundEvent eventSound = soundMap().get(name);
                        if (eventSound == null) {
                            ctx.getSource().sendFailure(Component.literal(
                                "Unknown sound. Try: " + String.join(", ", soundMap().keySet())));
                            return 0;
                        }
                        Minecraft mc = Minecraft.getInstance();
                        NorthwoodsAmbience.DIRECTOR.playDebug(mc, eventSound);
                        ctx.getSource().sendSuccess(() -> Component.literal("Playing " + name), false);
                        return 1;
                    })));

        event.getDispatcher().register(root);
    }

    private static void showStatus(CommandSourceStack source) {
        EnvironmentSnapshot snap = NorthwoodsAmbience.DIRECTOR.snapshot();
        if (snap == null) {
            source.sendSuccess(() -> Component.literal("No environment snapshot yet (join a world)."), false);
            return;
        }
        String beds = NorthwoodsAmbience.DIRECTOR.beds().entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isStopped())
            .map(Map.Entry::getKey)
            .sorted()
            .collect(Collectors.joining(", "));
        if (beds.isEmpty()) {
            beds = "(none)";
        }
        String msg = "water="+snap.waterKind()+" lakeReason="+snap.lakeReason()
            +" oceanAsLake="+NorthwoodsConfig.OCEAN_AS_LAKE.get()+" shoreDistance="+(Double.isFinite(snap.shoreDistance()) ? String.format("%.1f",snap.shoreDistance()) : "none")
            +" sampleAgeTicks="+snap.lakeSampleAgeTicks()+" shelter="+snap.shelter()+" roofExposed="+snap.roofExposed()
            +" mountain="+snap.mountain()+" ridge="+snap.ridge()+" exposure="+String.format("%.2f",snap.exposure())
            +" wind="+String.format("%.2f",snap.windStrength())+" grass="+snap.grassyCover()
            +" rain="+snap.rainLevel()+" thunder="+snap.thunderLevel()+" layers="+beds;
        source.sendSuccess(() -> Component.literal(msg),false);
        var cover=snap.cover();
        source.sendSuccess(() -> Component.literal("enclosure="+String.format("%.2f",cover.enclosure())+" opening="+cover.openingDirection()
            +" strength="+String.format("%.2f",cover.opening())+" outdoorGain="+String.format("%.2f",NorthwoodsAmbience.DIRECTOR.outdoorGain())
            +" roof="+(cover.roof()==null ? "none":cover.roof().toShortString())+" fires="+NorthwoodsAmbience.DIRECTOR.fireStatus()),false);
        source.sendSuccess(() -> Component.literal("blocked: "+NorthwoodsAmbience.DIRECTOR.suppression()),false);
        source.sendSuccess(() -> Component.literal("call timers: "+NorthwoodsAmbience.DIRECTOR.countdowns()
            +"; loon enabled="+NorthwoodsConfig.LOON_ENABLED.get()+" frequency="+NorthwoodsConfig.LOON_FREQUENCY.get()
            +" master="+NorthwoodsConfig.MASTER_VOLUME.get()),false);
    }

    private static Map<String, SoundEvent> soundMap() {
        Map<String, SoundEvent> map = new LinkedHashMap<>();
        map.put("loon_wail", ModSounds.LOON_WAIL.get());
        map.put("loon_tremolo", ModSounds.LOON_TREMOLO.get());
        map.put("loon_yodel", ModSounds.LOON_YODEL.get());
        map.put("crickets", ModSounds.CRICKETS.get());
        map.put("owl_barred", ModSounds.OWL_BARRED.get());
        map.put("owl_horned", ModSounds.OWL_HORNED.get());
        map.put("frog_peeper", ModSounds.FROG_PEEPER.get());
        map.put("frog_croak", ModSounds.FROG_CROAK.get());
        map.put("fire_cozy", ModSounds.FIRE_COZY.get());
        map.put("wood_creak", ModSounds.WOOD_CREAK.get());
        map.put("shore_waves", ModSounds.SHORE_WAVES.get());
        map.put("rain_storm", ModSounds.RAIN_STORM.get());
        map.put("rain_roof", ModSounds.RAIN_ROOF.get());
        map.put("wind_breeze", ModSounds.WIND_BREEZE.get());
        map.put("wind_foliage", ModSounds.WIND_FOLIAGE.get());
        map.put("gull", ModSounds.GULL.get());
        map.put("eagle", ModSounds.EAGLE.get());
        map.put("hawk", ModSounds.HAWK.get());
        map.put("mosquito", ModSounds.MOSQUITO.get());
        return map;
    }
}
