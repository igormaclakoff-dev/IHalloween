package com.auracode.ihalloween;

import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Candy economy stored in player PersistentDataContainer (saves with player data).
 */
public final class CandyService {

   private static final String CANDIES_KEY = "halloween_candies";
   private static final String PREFIX = "ihalloween";

   private final JavaPlugin plugin;
   private final NamespacedKey candiesKey;

   public CandyService(@NotNull JavaPlugin plugin) {
      this.plugin = plugin;
      this.candiesKey = new NamespacedKey(PREFIX, CANDIES_KEY);
   }

   public int getCandies(@NotNull Player player) {
      return player.getPersistentDataContainer().get(this.candiesKey, PersistentDataType.INTEGER);
   }

   public int addCandies(@NotNull Player player, int amount) {
      int current = Math.max(0, this.getCandies(player));
      int total = Math.max(0, current + amount);
      player.getPersistentDataContainer().set(this.candiesKey, PersistentDataType.INTEGER, total);
      return total;
   }

   public boolean trySpend(@NotNull Player player, int cost) {
      int current = Math.max(0, this.getCandies(player));
      if (current < cost) {
         return false;
      }
      player.getPersistentDataContainer().set(this.candiesKey, PersistentDataType.INTEGER, current - cost);
      return true;
   }

   public void setCandies(@NotNull Player player, int amount) {
      player.getPersistentDataContainer().set(this.candiesKey, PersistentDataType.INTEGER, Math.max(0, amount));
   }

   public @NotNull String debugPointer(@NotNull Player player) {
      PersistentDataContainerView view = player.getPersistentDataContainer();
      return view.getOrDefault(this.candiesKey, PersistentDataType.INTEGER, 0) + " @" + this.plugin.getName();
   }
}
