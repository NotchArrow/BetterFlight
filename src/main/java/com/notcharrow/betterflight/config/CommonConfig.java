package com.notcharrow.betterflight.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import com.notcharrow.betterflight.BetterFlight;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CommonConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("betterflight-common.json");
            
    public static ConfigData CONFIG = new ConfigData();

    // Constants
    public static final double TAKE_OFF_SPEED = 0.170D;
    public static final double TAKE_OFF_THRUST = 1.0D;
    public static final double CLASSIC_FLAP_THRUST = 0.65D;
    public static final double FLARE_DRAG = 0.08D;
    public static final int TAKE_OFF_JUMP_DELAY = 4;

    public static class ConfigData {
        public int maxCharge = 20;
        public int takeOffCost = 3;
        public int flapCost = 2;
        public int rechargeTicksInAir = 80;
        public int rechargeTicksOnGround = 10;
        public int flareTicksPerChargePoint = 40;
        public double exhaustionPerChargePoint = 4.0;
        public int minFood = 6;
        public int cooldownTicks = 10;
        public int softCeiling = 256;
        public int hardCeiling = 400;
        public boolean classicMode = false;
        public String hudLocation = "BAR_CENTER";
        public boolean classicHud = false;
    }

    public static void init() {
        load();
    }

    public static void load() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                CONFIG = gson.fromJson(json, ConfigData.class);
            } catch (IOException e) {
                BetterFlight.LOGGER.error("Failed to load config", e);
                save(); // Save default config if loading fails
            }
        } else {
            save(); // Save default config if file doesn't exist
        }
    }

    public static void save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            String json = gson.toJson(CONFIG);
            Files.writeString(CONFIG_PATH, json);
        } catch (IOException e) {
            BetterFlight.LOGGER.error("Failed to save config", e);
        }
    }
    
}