package com.notcharrow.betterflight;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.notcharrow.betterflight.config.CommonConfig;
import com.notcharrow.betterflight.networking.FlightNetworking;
import com.notcharrow.betterflight.sound.ModSounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetterFlight implements ModInitializer {
    public static final String MODID = "betterflight";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    
    @Override
    public void onInitialize() {
        LOGGER.info("BetterFlight mod is initializing!"); 
 
        // Initialize configs
        CommonConfig.init();
        
        // Initialize networking
        FlightNetworking.init();
        
        // Initialize sounds
        ModSounds.register();
        
        // Register server events
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            server.getPlayerManager().getPlayerList().forEach(player -> {
                if (player.isFallFlying()) {
                    // Server-side flight handling
                }
            });
        });
    }
}