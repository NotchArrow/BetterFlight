package com.notcharrow.betterflight.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.notcharrow.betterflight.BetterFlight;
import com.notcharrow.betterflight.client.ClientData;
import com.notcharrow.betterflight.config.CommonConfig;
import com.notcharrow.betterflight.util.InputHandler;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import java.util.Random;

public class ClassicHudOverlay {
    // HUD layout constraints
    private static final int OFFSET_FROM_CURSOR = 24;
    private static final int OFFSET_TO_HOTBAR_SIDES = 105;
    private static final int OFFSET_ABOVE_HOTBAR = 44;

    // Texture Data
    private static final int TEXTURE_SIZE = 128;
    private static final int ICON_SIZE = 16;
    private static final int HALF_ICON_SIZE = 8;

    // sprite row offsets
    private static final int SPRITE_DURABILITY_FULL = 0;
    private static final int SPRITE_DURABILITY_HALF = ICON_SIZE;
    private static final int SPRITE_DURABILITY_QUARTER = 2 * ICON_SIZE;
    private static final int SPRITE_DURABILITY_LOW = 3 * ICON_SIZE;

    // sprite column offsets
    private static final int SPRITE_BORDER_BLACK = 0;
    private static final int SPRITE_BORDER_RECHARGE = ICON_SIZE;
    private static final int SPRITE_BORDER_DEPLETION = 2 * ICON_SIZE;
    private static final int SPRITE_BORDER_FLARE = 3 * ICON_SIZE;
    private static final int SPRITE_METER_EMPTY = 4 * ICON_SIZE;
    private static final int SPRITE_METER_FULL = 5 * ICON_SIZE;
    private static final int SPRITE_ALARM = 6 * ICON_SIZE;

    private static final Random random = new Random(System.currentTimeMillis());
    private static final Identifier ELYTRA_ICONS = new Identifier(BetterFlight.MODID, "textures/elytraicons.png");
    private static int rechargeBorderTimer = 0;
    private static int depletionBorderTimer = 0;

    public static void init() {
        HudRenderCallback.EVENT.register((DrawContext context, float tickDelta) -> {
            if (!ClientData.isFlightEnabled() || !ClientData.isWearingFunctionalWings()) return;
            if (!CommonConfig.CONFIG.classicHud) return;

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.player == null) return;
            if (mc.player.hasVehicle()) return;

            renderOverlay(context, mc);
        });
    }

    private static void renderOverlay(DrawContext context, MinecraftClient mc) {
        int shakeX = 0;
        int shakeY = 0;
        int scaleWidth = mc.getWindow().getScaledWidth();
        int scaleHeight = mc.getWindow().getScaledHeight();
        int widgetPosX = 0;
        int widgetPosY = 0;

        // Calculate widget position
        switch (CommonConfig.CONFIG.hudLocation) {
            case "CURSOR_BELOW" -> {
                widgetPosX = scaleWidth / 2 - HALF_ICON_SIZE;
                widgetPosY = scaleHeight / 2 + OFFSET_FROM_CURSOR - HALF_ICON_SIZE;
            }
            case "CURSOR_LEFT" -> {
                widgetPosX = scaleWidth / 2 - OFFSET_FROM_CURSOR - HALF_ICON_SIZE;
                widgetPosY = scaleHeight / 2 - HALF_ICON_SIZE;
            }
            case "CURSOR_RIGHT" -> {
                widgetPosX = scaleWidth / 2 + OFFSET_FROM_CURSOR - HALF_ICON_SIZE;
                widgetPosY = scaleHeight / 2 - HALF_ICON_SIZE;
            }
            case "CURSOR_ABOVE" -> {
                widgetPosX = scaleWidth / 2 - HALF_ICON_SIZE;
                widgetPosY = scaleHeight / 2 - OFFSET_FROM_CURSOR - HALF_ICON_SIZE;
            }
            case "BAR_LEFT" -> {
                widgetPosX = scaleWidth / 2 - OFFSET_TO_HOTBAR_SIDES - HALF_ICON_SIZE;
                widgetPosY = scaleHeight - 12 - HALF_ICON_SIZE;
            }
            case "BAR_RIGHT" -> {
                widgetPosX = scaleWidth / 2 + OFFSET_TO_HOTBAR_SIDES - HALF_ICON_SIZE;
                widgetPosY = scaleHeight - 12 - HALF_ICON_SIZE;
            }
            default -> { // BAR_CENTER
                widgetPosX = scaleWidth / 2 - HALF_ICON_SIZE;
                widgetPosY = scaleHeight - OFFSET_ABOVE_HOTBAR - HALF_ICON_SIZE;
            }
        }

        // Handle durability and effects
        float durability = ClientData.getElytraDurability();
        int durabilityOffset = SPRITE_DURABILITY_FULL;
        if (durability > 0.50f) durabilityOffset = SPRITE_DURABILITY_HALF;
        if (durability > 0.75f) durabilityOffset = SPRITE_DURABILITY_QUARTER;
        if (durability > 0.90f) durabilityOffset = SPRITE_DURABILITY_LOW;

        // Handle border effects
        int borderOffset = SPRITE_BORDER_BLACK;
        if (durability > 0.95) {
            if (mc.world != null) {
                long thisTick = mc.world.getTime();
                borderOffset = (int)(((thisTick / 5) % 2) * ICON_SIZE) + SPRITE_ALARM;

                if (((thisTick / 3) % 2) > 0) {
                    shakeX = random.nextInt(3) - 1;
                } else {
                    shakeY = random.nextInt(3) - 1;
                }
            }
        } else if (ClientData.isFlaring()) {
            borderOffset = SPRITE_BORDER_FLARE;
        } else if (depletionBorderTimer > 0) {
            borderOffset = SPRITE_BORDER_DEPLETION;
        } else if (rechargeBorderTimer > 0) {
            borderOffset = SPRITE_BORDER_RECHARGE;
        }

        // Calculate meter drain
        int drainedPixels = (int) Math.floor(
                (1.0f - (float) InputHandler.charge / (float) CommonConfig.CONFIG.maxCharge) * ICON_SIZE);

        // Render HUD elements
        RenderSystem.setShaderTexture(0, ELYTRA_ICONS);

        // Draw meter background
        context.drawTexture(ELYTRA_ICONS,
            widgetPosX + shakeX, widgetPosY + shakeY,
            SPRITE_METER_FULL, durabilityOffset,
            ICON_SIZE, ICON_SIZE,
            TEXTURE_SIZE, TEXTURE_SIZE);

        // Draw drain level
        context.drawTexture(ELYTRA_ICONS,
            widgetPosX + shakeX, widgetPosY + shakeY,
            SPRITE_METER_EMPTY, durabilityOffset,
            ICON_SIZE, drainedPixels,
            TEXTURE_SIZE, TEXTURE_SIZE);

        // Draw border
        context.drawTexture(ELYTRA_ICONS,
            widgetPosX + shakeX, widgetPosY + shakeY,
            borderOffset, durabilityOffset,
            ICON_SIZE, ICON_SIZE,
            TEXTURE_SIZE, TEXTURE_SIZE);
    }

    public static void borderTick() {
        if (depletionBorderTimer > 0) depletionBorderTimer--;
        if (rechargeBorderTimer > 0) rechargeBorderTimer--;
    }

    public static void setDepletionBorderTimer(int ticks) {
        depletionBorderTimer = ticks;
    }

    public static void setRechargeBorderTimer(int ticks) {
        rechargeBorderTimer = ticks;
    }

    public static void cycleWidgetLocation() {
        String currentLocation = CommonConfig.CONFIG.hudLocation;
        CommonConfig.CONFIG.hudLocation = switch (currentLocation) {
            case "BAR_CENTER" -> "BAR_LEFT";
            case "BAR_LEFT" -> "BAR_RIGHT";
            case "BAR_RIGHT" -> "CURSOR_ABOVE";
            case "CURSOR_ABOVE" -> "CURSOR_RIGHT";
            case "CURSOR_RIGHT" -> "CURSOR_BELOW";
            case "CURSOR_BELOW" -> "CURSOR_LEFT";
            case "CURSOR_LEFT" -> "BAR_CENTER";
            default -> "BAR_CENTER";
        };
        CommonConfig.save();
    }
}