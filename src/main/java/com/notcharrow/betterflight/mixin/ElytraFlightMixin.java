package com.notcharrow.betterflight.mixin;

import com.notcharrow.betterflight.client.ClientData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class ElytraFlightMixin {
    private static final double FLAP_BOOST = 0.8D;
    private static final double FLARE_MULTIPLIER = 0.6D;
    private static final double MIN_SPEED = 0.1D;

    @Inject(method = "travel", at = @At("HEAD"))
    private void onTravel(Vec3d movementInput, CallbackInfo info) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        if (entity.isFallFlying() && entity instanceof PlayerEntity player) {
            if (ClientData.isFlaring()) {
                // Handle flare (slowdown)
                Vec3d velocity = player.getVelocity();
                double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                
                if (speed > MIN_SPEED) {
                    player.setVelocity(
                        velocity.x * FLARE_MULTIPLIER,
                        velocity.y,
                        velocity.z * FLARE_MULTIPLIER
                    );
                }
            }
            
            // Handle flap for upward boost if it was triggered
            if (ClientData.isWearingFunctionalWings() && ClientData.shouldBoostThisTick()) {
                Vec3d lookVec = player.getRotationVector();
                Vec3d currentVel = player.getVelocity();
                double upBoost = FLAP_BOOST + (currentVel.y < 0 ? -currentVel.y * 0.5 : 0);
                
                player.setVelocity(
                    currentVel.x + lookVec.x * 0.4,
                    currentVel.y + upBoost,
                    currentVel.z + lookVec.z * 0.4
                );
                
                // Reset the boost flag
                ClientData.setBoostThisTick(false);
            }
        }
    }
}