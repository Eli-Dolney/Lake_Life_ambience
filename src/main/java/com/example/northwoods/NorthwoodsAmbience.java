package com.example.northwoods;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(value = NorthwoodsAmbience.ID, dist = Dist.CLIENT)
public final class NorthwoodsAmbience {
    public static final String ID = "northwoodsambience";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final AmbienceDirector DIRECTOR = new AmbienceDirector();

    public NorthwoodsAmbience(IEventBus modBus, ModContainer container) {
        ModSounds.SOUND_EVENTS.register(modBus);
        container.registerConfig(ModConfig.Type.CLIENT, NorthwoodsConfig.SPEC);

        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(AmbienceCommands::register);
        NeoForge.EVENT_BUS.addListener(DIRECTOR::filterFire);

        LOGGER.info("Northwoods Ambience loaded — Minnesota lake-cabin soundscape ready.");
    }

    private void onClientTick(ClientTickEvent.Post event) {
        DIRECTOR.clientTick(Minecraft.getInstance());
    }
}
