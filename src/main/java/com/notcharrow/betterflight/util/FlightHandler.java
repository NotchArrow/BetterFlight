package com.notcharrow.betterflight.util;

import com.notcharrow.betterflight.config.CommonConfig;
import com.notcharrow.betterflight.common.FlightActionType;
import com.notcharrow.betterflight.networking.FlightNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class FlightHandler {
    public static void handleClassicTakeoff(PlayerEntity player) {
        // Take offs need no forward component due to player already sprinting.
        // They need additional vertical thrust to reliably get enough time to flap
        Vec3d upwards = new Vec3d(0.0D, CommonConfig.TAKE_OFF_THRUST, 0.0D)
            .multiply(getCeilingFactor(player));
        player.startFallFlying();
        player.addVelocity(upwards.x, upwards.y, upwards.z);

        // This plays the sound to everyone EXCEPT the player it is invoked on.
        // The player's copy of the sound is handled on the client side.
        FlightNetworking.sendFlightActionToServer(FlightActionType.TAKEOFF);
    }

    public static void handleClassicFlap(PlayerEntity player) {
        double ceilingFactor = getCeilingFactor(player);
        // Increased thrust values for more noticeable effect
        Vec3d upwards = new Vec3d(0.0D, CommonConfig.CLASSIC_FLAP_THRUST * 1.5, 0.0D)
            .multiply(ceilingFactor);
        Vec3d forwards = player.getVelocity().normalize()
            .multiply(CommonConfig.CLASSIC_FLAP_THRUST * 0.5)
            .multiply(ceilingFactor);
        Vec3d impulse = forwards.add(upwards);
        
        System.out.println("Applying flap impulse: " + impulse); // Debug print
        player.addVelocity(impulse.x, impulse.y, impulse.z);
        FlightNetworking.sendFlightActionToServer(FlightActionType.FLAP);
    }

    public static void handleFlare(PlayerEntity player) {
        Vec3d velocity = player.getVelocity();
        Vec3d dragDirection = velocity.normalize().negate();
        double velocitySquared = velocity.lengthSquared();
        
        // Calculate horizontal drag
        Vec3d horizontalVelocity = new Vec3d(velocity.x, 0, velocity.z);
        Vec3d horizontalDrag = horizontalVelocity.normalize().negate()
            .multiply(velocitySquared * CommonConfig.FLARE_DRAG);

        // Calculate vertical drag (more aggressive for falling)
        double verticalDrag = 0;
        if (velocity.y < 0) {
            // When falling, provide stronger upward force
            verticalDrag = -velocity.y * Math.max(0.5, Math.abs(velocity.y) * 0.15);
        } else {
            // When rising, provide normal drag
            verticalDrag = -velocity.y * CommonConfig.FLARE_DRAG;
        }

        // Apply the combined forces
        player.addVelocity(
            horizontalDrag.x,
            verticalDrag,
            horizontalDrag.z
        );
    }

    public static void handleModernFlap(PlayerEntity player) {
        double d0 = 0.1; // delta coefficient. Influenced by difference between d0 and current delta
        double d1 = 0.55; // boost coefficient
        Vec3d looking = player.getRotationVector();
        Vec3d delta = player.getVelocity();

        Vec3d impulse = delta.add(
            looking.x * d1 + (looking.x * d0 - delta.x) * 1.5,
            looking.y * d1 + (looking.y * d0 - delta.y) * 1.5,
            looking.z * d1 + (looking.z * d0 - delta.z) * 1.5
        )
        .multiply(getCeilingFactor(player))
        .add(getUpVector(player).multiply(0.25));

        player.addVelocity(impulse.x, impulse.y, impulse.z);
        FlightNetworking.sendFlightActionToServer(FlightActionType.FLAP);
    }

    public static void handleModernBoost(PlayerEntity player) {
        double d0 = 0.1; // delta coefficient
        double d1 = 1.0; // boost coefficient
        Vec3d looking = player.getRotationVector();
        Vec3d delta = player.getVelocity();

        Vec3d impulse = delta.add(
            looking.x * d1 + (looking.x * d0 - delta.x) * 1.5,
            looking.y * d1 + (looking.y * d0 - delta.y) * 1.5,
            looking.z * d1 + (looking.z * d0 - delta.z) * 1.5
        )
        .multiply(getCeilingFactor(player))
        .add(getUpVector(player).multiply(0.25));

        player.addVelocity(impulse.x, impulse.y, impulse.z);
        FlightNetworking.sendFlightActionToServer(FlightActionType.BOOST);
    }

    private static double getCeilingFactor(PlayerEntity player) {
        double altitude = player.getY();
        // flying low, full power
        if (altitude < CommonConfig.CONFIG.softCeiling) {
            return 1.0D;
        }
        // flying too high, no power
        if (altitude > CommonConfig.CONFIG.hardCeiling) {
            return 0.0D;
        }
        // flying in between, scale power accordingly
        return 1.0D - (altitude - CommonConfig.CONFIG.softCeiling) / 
            (CommonConfig.CONFIG.hardCeiling - CommonConfig.CONFIG.softCeiling);
    }

    private static Vec3d getUpVector(PlayerEntity player) {
        float yaw = player.getYaw() % 360;
        double rads = yaw * (Math.PI / 180);
        Vec3d left = new Vec3d(Math.cos(rads), 0, Math.sin(rads));
        return player.getRotationVector().crossProduct(left);
    }

    public static void handleFlightStop() {
        FlightNetworking.sendFlightActionToServer(FlightActionType.STOP);
    }
}