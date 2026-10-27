package com.auracode.ihalloween.model;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Shop reward: cost in candies + console commands bound to it.
 */
public record Reward(int slot, int cost, @NotNull String icon, @NotNull String name, @NotNull List<String> commands) {
}
