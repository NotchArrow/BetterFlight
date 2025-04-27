package com.notcharrow.betterflight.client.gui;

import com.notcharrow.betterflight.BetterFlight;
import com.notcharrow.betterflight.client.ClientData;
import com.notcharrow.betterflight.config.CommonConfig;
import com.notcharrow.betterflight.util.InputHandler;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import java.util.Random;

public class StaminaHudOverlay {
    private static final int SPRITE_WIDTH = 9;
    private static final int SPRITE_HEIGHT = 9;

    // columns
    private static final int NONE = 0;
    private static final int FILL_FULL = 9;
    private static final int FILL_HALF = 18;
    private static final int WHITE_OUTLINE = 27;
    private static final int FLARE_OUTLINE = 36;
    private static final int RED_OUTLINE = 45;

    // Row durability states
    private static final int FULL_DURABILITY = 0;
    private static final int HALF_DURABILITY = 9;
    private static final int QUARTER_DURABILITY = 18;
    private static final int LOW_DURABILITY = 27;

    private static final Random random = new Random(System.currentTimeMillis());
    private static final Identifier STAMINA_ICONS = new Identifier(BetterFlight.MODID, "textures/elytraspritesheet.png");
    private static int shakeEffectTimer = 0;
    private static int regenEffectTimer = 0;
    private static int durability;

    public static void init() {
        HudRenderCallback.EVENT.register((DrawContext context, float tickDelta) -> {
            if (!ClientData.isFlightEnabled() || !ClientData.isWearingFunctionalWings()) return;
            if (CommonConfig.CONFIG.classicHud) return;

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.player == null || mc.options.hudHidden) return;
            if (mc.player.hasVehicle()) return;

            renderStaminaBar(context, mc);
        });
    }

    private static void renderStaminaBar(DrawContext context, MinecraftClient mc) {
        int x = mc.getWindow().getScaledWidth() / 2;
        int y = mc.getWindow().getScaledHeight();
        int shakeX = 0;
        int shakeY = 0;
        int rightOffset = 49;  // Adjusted to move above hunger bar

        durability = getDurabilityState(ClientData.getElytraDurability());

        if (durability == LOW_DURABILITY) {
            if (mc.world != null && !mc.isPaused()) {
                long thisTick = mc.world.getTime();
                if (shakeEffectTimer > 0) {
                    if (thisTick % 3 == 0) {
                        shakeX = random.nextInt(2) - 1;
                    } else if (thisTick % 3 == 1) {
                        shakeY = random.nextInt(2) - 1;
                    }
                    shakeEffectTimer--;
                } else if (thisTick % 20 == 0) {
                    shakeEffectTimer = 20;
                }
            }
        } else {
            shakeEffectTimer = 0;
        }

        // Render empty resources
        for (int i = 0; i < 10; i++) {
            context.drawTexture(STAMINA_ICONS,
                getXPos(x, i) + shakeX, y - rightOffset + shakeY,
                NONE, durability,
                SPRITE_WIDTH, SPRITE_HEIGHT,
                256, 256);
        }

        // Render filled resources
        for (int i = 0; i < 10; i++) {
            if ((i + 1) <= Math.ceil((double) InputHandler.charge / 2.0d)
                    && InputHandler.charge > 0) {
                int type = ((i + 1 == Math.ceil((double) InputHandler.charge / 2.0d)
                        && (InputHandler.charge % 2 != 0)) ? FILL_HALF : FILL_FULL);
                context.drawTexture(STAMINA_ICONS,
                    getXPos(x, i) + shakeX, y - rightOffset + shakeY,
                    type, durability,
                    SPRITE_WIDTH, SPRITE_HEIGHT,
                    256, 256);
            }
        }

        // Render special effects
        if (ClientData.isFlaring()) {
            renderOutline(context, x, y, rightOffset, shakeX, shakeY, FLARE_OUTLINE);
        }
        
        if (shakeEffectTimer > 0) {
            renderOutline(context, x, y, rightOffset, shakeX, shakeY, RED_OUTLINE);
        }
        
        if (regenEffectTimer > 0) {
            renderOutline(context, x, y, rightOffset, shakeX, shakeY, WHITE_OUTLINE);
            regenEffectTimer--;
        }
    }

    private static void renderOutline(DrawContext context, int x, int y, int rightOffset, int shakeX, int shakeY, int outlineType) {
        for (int i = 0; i < 10; i++) {
            context.drawTexture(STAMINA_ICONS,
                getXPos(x, i) + shakeX, y - rightOffset + shakeY,
                outlineType, durability,
                SPRITE_WIDTH, SPRITE_HEIGHT,
                256, 256);
        }
    }

    private static int getDurabilityState(double durability) {
        if (durability > 0.95f) {
            return LOW_DURABILITY;
        } else if (durability > 0.75f) {
            return QUARTER_DURABILITY;
        } else if (durability > 0.50f) {
            return HALF_DURABILITY;
        }
        return FULL_DURABILITY;
    }

    private static int getXPos(int x, int i) {
        return x + 90 - ((i + 1) * (SPRITE_WIDTH - 1));
    }

    public static void startRegenAnimation() {
        if (regenEffectTimer == 0) {
            regenEffectTimer = 10;
        }
    }
}