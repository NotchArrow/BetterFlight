package com.notcharrow.betterflight.util;

import com.notcharrow.betterflight.client.gui.ClassicHudOverlay;
import com.notcharrow.betterflight.client.gui.StaminaHudOverlay;
import com.notcharrow.betterflight.client.ClientData;
import com.notcharrow.betterflight.common.FlightActionType;
import com.notcharrow.betterflight.config.CommonConfig;
import com.notcharrow.betterflight.networking.FlightNetworking;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import java.util.stream.Stream;

public class InputHandler {
    private static int rechargeTickCounter = 0;
    private static int flareTickCounter = 0;
    public static int charge = CommonConfig.CONFIG.maxCharge;

    public static boolean classicFlap(PlayerEntity player) {
        // Check cooldown first
        if (ClientData.getCooldown() > 0) {
            return false;
        }

        if (spendCharge(player, CommonConfig.CONFIG.flapCost)) {
            FlightHandler.handleClassicFlap(player);
            // Set cooldown after successful flap
            ClientData.setCooldown(CommonConfig.CONFIG.cooldownTicks);
            return true;
        }
        return false;
    }

    public static boolean modernFlight(PlayerEntity player) {
        // Check cooldown first
        if (ClientData.getCooldown() > 0) {
            return false;
        }

        if (canFlap(player)) {
            if (spendCharge(player, CommonConfig.CONFIG.flapCost)) {
                if (!checkForAir(player.getWorld(), player)) {
                    FlightHandler.handleModernBoost(player);
                } else {
                    FlightHandler.handleModernFlap(player);
                }
                // Set cooldown after successful flap
                ClientData.setCooldown(CommonConfig.CONFIG.cooldownTicks);
                return true;
            }
        }
        return false;
    }

    private static boolean canFlap(PlayerEntity player) {
        return ClientData.isWearingFunctionalWings() && !player.isOnGround() && player.isFallFlying();
    }

    private static boolean spendCharge(PlayerEntity player, int points) {
        if (player.isCreative()) return true;

        if (charge >= points) {
            charge = Math.max(0, charge - points);
            rechargeTickCounter = 0;
            ClassicHudOverlay.setDepletionBorderTimer(5);
            return true;
        }
        return false;
    }

    public static void handleRecharge(PlayerEntity player) {
        if (player.isCreative()) {
            charge = CommonConfig.CONFIG.maxCharge;
            return;
        }

        int chargeThreshold = player.isOnGround() ? 
            CommonConfig.CONFIG.rechargeTicksOnGround : 
            CommonConfig.CONFIG.rechargeTicksInAir;

        if (rechargeTickCounter < chargeThreshold) {
            rechargeTickCounter++;
        }

        if (!ClientData.isFlaring() && rechargeTickCounter >= chargeThreshold && 
            charge < CommonConfig.CONFIG.maxCharge) {
            if (player.getHungerManager().getFoodLevel() > CommonConfig.CONFIG.minFood) {
                charge++;
                rechargeTickCounter = 0;
                ClassicHudOverlay.setRechargeBorderTimer(5);
                StaminaHudOverlay.startRegenAnimation();
                FlightNetworking.sendFlightActionToServer(FlightActionType.RECHARGE);
            }
        }
    }

    public static void tryFlare(PlayerEntity player) {
        if (ClientData.isWearingFunctionalWings() && 
            ClientData.isFlightEnabled() && 
            player.isSneaking() &&
            ((player.isCreative() || charge > 0) || player.isSubmergedInWater() || player.isInLava()) &&
            !player.isOnGround() &&
            player.isFallFlying()) {

            if (player.isSubmergedInWater() || player.isInLava()) {
                FlightHandler.handleFlightStop();
                return;
            }

            FlightHandler.handleFlare(player);
            flareTickCounter++;

            if (flareTickCounter >= CommonConfig.CONFIG.flareTicksPerChargePoint) {
                spendCharge(player, 1);
                flareTickCounter = 0;
            }
        } else {
            if (flareTickCounter > 0) {
                flareTickCounter--;
            }
        }
    }

    public static ElytraData findWings(PlayerEntity player) {
        ItemStack itemStack = findWingsItemStack(player);
        if (itemStack == null)
            return null;
        int durabilityRemaining = itemStack.getMaxDamage() - itemStack.getDamage();
        float durabilityPercent = (float) itemStack.getDamage() / (float) itemStack.getMaxDamage();

        return new ElytraData(itemStack, durabilityRemaining, durabilityPercent);
    }

    private static ItemStack findWingsItemStack(PlayerEntity player) {
        // Check vanilla slot first
        ItemStack elytraStack = player.getEquippedStack(EquipmentSlot.CHEST);
        if (elytraStack.isOf(Items.ELYTRA)) {
            return elytraStack;
        }
        
        // If we can't find it but the player is flying, create a dummy elytra
        if (player.isFallFlying() || ClientData.isWearingFunctionalWings()) {
            ItemStack dummyElytra = new ItemStack(Items.ELYTRA);
            return dummyElytra;
        }
        
        return null;
    }

    public static boolean checkForAir(World world, LivingEntity player) {
        Box boundingBox = player.getBoundingBox()
                .stretch(0, 3.5, 0)
                .expand(1.0, 0, 1.0)
                .offset(0, -1.5, 0);
        Stream<BlockPos> blocks = getBlockPosStream(world, boundingBox);
        return blocks.noneMatch(pos -> 
            world.getBlockState(pos).isFullCube(world, pos) || 
            world.getBlockState(pos).getFluidState().isStill()
        );
    }

       private static Stream<BlockPos> getBlockPosStream(World world, Box box) {
        int minX = MathHelper.floor(box.minX);
        int maxX = MathHelper.floor(box.maxX);
        int minY = MathHelper.floor(box.minY);
        int maxY = MathHelper.floor(box.maxY);
        int minZ = MathHelper.floor(box.minZ);
        int maxZ = MathHelper.floor(box.maxZ);
        return world.isRegionLoaded(minX, minY, minZ, maxX, maxY, maxZ) ? 
            BlockPos.stream(box) : 
            Stream.empty();
    }
}