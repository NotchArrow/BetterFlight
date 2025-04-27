package com.notcharrow.betterflight;

import dev.emi.trinkets.api.TrinketComponent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import com.notcharrow.betterflight.client.gui.ClassicHudOverlay;
import com.notcharrow.betterflight.client.gui.StaminaHudOverlay;
import com.notcharrow.betterflight.client.ClientData;
import net.minecraft.entity.EquipmentSlot;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import com.notcharrow.betterflight.config.CommonConfig;
import com.notcharrow.betterflight.util.InputHandler;

import java.util.Optional;

public class BetterFlightClient implements ClientModInitializer {
    public static KeyBinding FLAP_KEY;
    public static KeyBinding FLARE_KEY;
    public static KeyBinding TOGGLE_KEY;
    public static KeyBinding WIDGET_POS_KEY;
    private boolean wasFlying = false;

    @Override
    public void onInitializeClient() {
        registerKeyBindings();
        registerClientEvents();
        registerHudOverlays();
    }

    private void registerKeyBindings() {
        FLAP_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.betterflight.flap", 
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "category.betterflight"
        ));
        
        FLARE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.betterflight.flare", 
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            "category.betterflight"
        ));
        
        TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.betterflight.toggle", 
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F8, 
            "category.betterflight"
        ));
        
        WIDGET_POS_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.betterflight.widget_pos", 
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F10, 
            "category.betterflight"
        ));
    }

    private void registerClientEvents() {
        // Add a counter for activation grace period
        final int[] activationGraceTicks = {0};
        final int GRACE_PERIOD = 5; // 5 ticks = 0.25 seconds
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.currentScreen == null) {
                boolean isFlying = client.player.isFallFlying();
                
                // Always set the wing status if the player is flying
                if (isFlying) {
                    ClientData.setWingStatus(true);
                }
                
                // Detect flight state change
                if (isFlying && !wasFlying) {
                    // Set grace period when starting to fly
                    activationGraceTicks[0] = GRACE_PERIOD;
                } else if (!isFlying && wasFlying) {
                    // Reset flare state when landing
                    ClientData.setIsFlaring(false);
                }
                
                // Update previous state
                wasFlying = isFlying;
                
                // Decrement grace period timer if active
                if (activationGraceTicks[0] > 0) {
                    activationGraceTicks[0]--;
                }
                
                // Update cooldown timer every tick
                if (ClientData.getCooldown() > 0) {
                    ClientData.subCooldown(1);
                }
                
                // Check vanilla elytra slot
                ItemStack chestItem = client.player.getEquippedStack(EquipmentSlot.CHEST);
                boolean hasVanillaElytra = chestItem.isOf(Items.ELYTRA);
                if (!hasVanillaElytra) {
                    Optional<TrinketComponent> trinketComponent = TrinketsApi.getTrinketComponent(client.player);
                    if (trinketComponent.isPresent()) {
                        hasVanillaElytra = trinketComponent.get().isEquipped(Items.ELYTRA);
                    }
                }
                
                // Handle elytra functionality based on flight status or vanilla elytra
                if (isFlying || hasVanillaElytra) {
                    ClientData.setWingStatus(true);
                    InputHandler.handleRecharge(client.player);
                    
                    // Only handle flight-specific controls during flight
                    if (isFlying) {
                        // Handle flare - only during flight
                        if (FLARE_KEY.isPressed()) {
                            ClientData.setIsFlaring(true);
                            InputHandler.tryFlare(client.player);
                        } else {
                            ClientData.setIsFlaring(false);
                        }
                        
                        // Handle flap - only if not in grace period
                        if (FLAP_KEY.wasPressed() && activationGraceTicks[0] <= 0) {
                            if (CommonConfig.CONFIG.classicMode) {
                                InputHandler.classicFlap(client.player);
                            } else {
                                InputHandler.modernFlight(client.player);
                            }
                        }
                        
                        // These keys can be handled regardless of flight state
                        if (TOGGLE_KEY.wasPressed()) {
                            ClientData.setFlightEnabled(!ClientData.isFlightEnabled());
                        }
                        if (WIDGET_POS_KEY.wasPressed()) {
                            ClassicHudOverlay.cycleWidgetLocation();
                        }
                    }
                } else if (!isFlying && !hasVanillaElytra) {
                    ClientData.setWingStatus(false);
                }
            }
        });
    }

    private void registerHudOverlays() {
        ClassicHudOverlay.init();
        StaminaHudOverlay.init();
    }
}