package com.auracode.ihalloween;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import com.auracode.ihalloween.command.HalloweenCommand;
import com.auracode.ihalloween.listener.HalloweenListener;
import com.auracode.ihalloween.util.RewardStorage;
import org.jetbrains.annotations.NotNull;

/**
 * Main IHalloween plugin class. Wires modules.
 */
public final class IHalloweenPlugin extends JavaPlugin {

   private HalloweenConfig halloweenConfig;
   private CandyService candyService;
   private PumpkinManager pumpkinManager;
   private BossManager bossManager;
   private RewardService rewardService;

   @Override
   public void onEnable() {
      this.halloweenConfig = new HalloweenConfig(this);
      this.halloweenConfig.load();

      this.candyService = new CandyService(this);
      this.pumpkinManager = new PumpkinManager(this);
      this.bossManager = new BossManager(this);
      this.rewardService = new RewardService(this, new RewardStorage(this));

      PluginCommand command = this.getCommand("halloween");
      if (command != null) {
         HalloweenCommand executor = new HalloweenCommand(this);
         command.setExecutor(executor);
         command.setTabCompleter(executor);
      }
      this.getServer().getPluginManager().registerEvents(new HalloweenListener(this), this);
      this.getLogger().info("IHalloween enabled");
   }

   @Override
   public void onDisable() {
      if (this.pumpkinManager != null) {
         this.pumpkinManager.savePumpkins();
      }
      if (this.bossManager != null) {
         this.bossManager.despawnAll();
      }
      this.getLogger().info("IHalloween disabled");
   }

   public void broadcastBossSpawn(@NotNull org.bukkit.Location location) {
      var config = this.halloweenConfig;
      String message = config.getMessages().getString("boss-spawned", "");
      Bukkit.getOnlinePlayers().forEach(player -> MessageUtil.send(player, config.getPrefix(), message,
         location.getBlockX(), location.getBlockY(), location.getBlockZ()));
   }

   public void broadcastBossKilled(@NotNull Player killer, int reward) {
      var config = this.halloweenConfig;
      String message = config.getMessages().getString("boss-killed", "");
      Bukkit.getOnlinePlayers().forEach(player -> MessageUtil.send(player, config.getPrefix(), message,
         killer.getName(), reward));
   }

   public @NotNull HalloweenConfig getHalloweenConfig() {
      return this.halloweenConfig;
   }

   public @NotNull CandyService getCandyService() {
      return this.candyService;
   }

   public @NotNull PumpkinManager getPumpkinManager() {
      return this.pumpkinManager;
   }

   public @NotNull BossManager getBossManager() {
      return this.bossManager;
   }

   public @NotNull RewardService getRewardService() {
      return this.rewardService;
   }
}
