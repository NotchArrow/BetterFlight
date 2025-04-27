package com.notcharrow.betterflight.networking;

import com.notcharrow.betterflight.sound.ModSounds;
import com.notcharrow.betterflight.util.InputHandler;
import com.notcharrow.betterflight.BetterFlight;
import com.notcharrow.betterflight.common.FlightActionType;
import com.notcharrow.betterflight.config.CommonConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class FlightNetworking {
    // Packet identifiers
    public static final Identifier FLIGHT_EFFECTS = new Identifier(BetterFlight.MODID, "flight_effects");
    public static final Identifier ELYTRA_CHARGE = new Identifier(BetterFlight.MODID, "elytra_charge");

    public static void init() {
        registerServerReceivers();
        registerClientReceivers();
    }

    public static void registerServerReceivers() {
        // Handle flight effects (client -> server)
        ServerPlayNetworking.registerGlobalReceiver(FLIGHT_EFFECTS, (server, player, handler, buf, responseSender) -> {
            FlightActionType action = buf.readEnumConstant(FlightActionType.class);
            
            server.execute(() -> {
                switch (action) {
                    case FLAP -> {
                        playSound(player, ModSounds.FLAP, 0.5F, 2.0F);
                    }
                    case BOOST -> {
                        playSound(player, ModSounds.BOOST, 2.0F, 1.0F);
                    }
                    case TAKEOFF -> {
                        player.startFallFlying();
                        playSound(player, ModSounds.FLAP, 1.0F, 2.0F);
                    }
                    case STOP -> {
                        player.stopFallFlying();
                    }
                    case RECHARGE -> {
                        handleFlightStaminaExhaustion(player);
                    }
                }
            });
        });
    }

    public static void registerClientReceivers() {
        // Handle elytra charge updates (server -> client)
        ClientPlayNetworking.registerGlobalReceiver(ELYTRA_CHARGE, (client, handler, buf, responseSender) -> {
            int charge = buf.readInt();
            
            client.execute(() -> {
                InputHandler.charge = charge;
            });
        });
    }

    // Utility methods for sending packets
    public static void sendFlightActionToServer(FlightActionType action) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumConstant(action);
        ClientPlayNetworking.send(FLIGHT_EFFECTS, buf);
    }

    public static void sendChargeToClient(ServerPlayerEntity player, int charge) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(charge);
        ServerPlayNetworking.send(player, ELYTRA_CHARGE, buf);
    }

    // Helper methods
    private static void playSound(ServerPlayerEntity player, SoundEvent soundEvent, float volume, float pitch) {
        if (soundEvent != null) {
            Vec3d pos = player.getPos();
            player.getWorld().playSound(
                null, 
                BlockPos.ofFloored(pos),
                soundEvent,
                SoundCategory.PLAYERS,
                volume,
                pitch
            );
        }
    }

    private static void handleFlightStaminaExhaustion(ServerPlayerEntity player) {
        player.addExhaustion((float)CommonConfig.CONFIG.exhaustionPerChargePoint);
    }
}