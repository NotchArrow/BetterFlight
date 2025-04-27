package com.notcharrow.betterflight.util;

import net.minecraft.item.ItemStack;

public record ElytraData(ItemStack itemStack, int durabilityRemaining, float durabilityPercent) {
}