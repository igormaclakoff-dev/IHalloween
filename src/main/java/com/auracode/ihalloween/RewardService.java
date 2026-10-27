package com.auracode.ihalloween;

import com.auracode.ihalloween.model.Reward;
import com.auracode.ihalloween.util.RewardStorage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Executes configured rewards for a player.
 */
public final class RewardService {

   private final IHalloweenPlugin plugin;
   private final RewardStorage storage;

   public RewardService(@NotNull IHalloweenPlugin plugin, @NotNull RewardStorage storage) {
      this.plugin = plugin;
      this.storage = storage;
   }

   public void save() {
      this.storage.saveToFile();
   }

   public @NotNull List<Reward> getRewards() {
      return this.storage.getRewards();
   }

   public boolean hasReward(int slot) {
      return this.findReward(slot).isPresent();
   }

   public @NotNull Optional<Reward> findReward(int slot) {
      return this.storage.getRewards().stream()
         .filter(reward -> reward.slot() == slot)
         .findFirst();
   }

   public void setReward(int slot, int cost, @NotNull org.bukkit.Material icon, @NotNull String name, @NotNull List<String> commands) {
      this.storage.getRewards().removeIf(reward -> reward.slot() == slot);
      this.storage.getRewards().add(new Reward(slot, cost, icon.name(), name, commands));
      this.storage.getRewards().sort(java.util.Comparator.comparingInt(Reward::slot));
      this.save();
   }

   public boolean removeReward(int slot) {
      boolean removed = this.storage.getRewards().removeIf(reward -> reward.slot() == slot);
      if (removed) {
         this.save();
      }
      return removed;
   }

   public void give(@NotNull Player player, @NotNull Reward reward) {
      reward.commands().forEach(command ->
         this.plugin.getServer().dispatchCommand(this.plugin.getServer().getConsoleSender(),
            command.replace("%player%", player.getName())));
   }
}
