package com.auracode.ihalloween.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import com.auracode.ihalloween.model.Reward;
import org.jetbrains.annotations.NotNull;

/**
 * Persists rewards in rewards.yml.
 */
public final class RewardStorage {

   private final JavaPlugin plugin;
   private final List<Reward> rewards = new ArrayList<>();

   public RewardStorage(@NotNull JavaPlugin plugin) {
      this.plugin = plugin;
      this.loadFromFile();
   }

   public @NotNull List<Reward> getRewards() {
      return this.rewards;
   }

   public void loadFromFile() {
      this.rewards.clear();
      var file = new File(this.plugin.getDataFolder(), "rewards.yml");
      if (!file.exists()) {
         this.plugin.saveResource("rewards.yml", false);
      }
      YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
      ConfigurationSection section = yaml.getConfigurationSection("rewards");
      if (section == null) {
         return;
      }
      for (String key : section.getKeys(false)) {
         try {
            int slot = Integer.parseInt(key.replace("slot-", ""));
            int cost = section.getInt(key + ".cost");
            String icon = section.getString(key + ".icon", Material.PUMPKIN.name());
            String name = section.getString(key + ".name", "Награда");
            List<String> commands = section.getStringList(key + ".commands");
            Material iconMaterial = Material.matchMaterial(icon);
            if (iconMaterial == null) {
               iconMaterial = Material.PUMPKIN;
            }
            this.rewards.add(new Reward(slot, cost, iconMaterial.name(), name, commands));
         } catch (NumberFormatException e) {
            this.plugin.getLogger().log(Level.WARNING, "Invalid reward slot key: " + key);
         }
      }
      this.rewards.sort(java.util.Comparator.comparingInt(Reward::slot));
   }

   public void saveToFile() {
      var file = new File(this.plugin.getDataFolder(), "rewards.yml");
      YamlConfiguration yaml = new YamlConfiguration();
      for (Reward reward : this.rewards) {
         String path = "rewards.slot-" + reward.slot();
         yaml.set(path + ".cost", reward.cost());
         yaml.set(path + ".icon", reward.icon());
         yaml.set(path + ".name", reward.name());
         yaml.set(path + ".commands", reward.commands());
      }
      try {
         yaml.save(file);
      } catch (IOException e) {
         this.plugin.getLogger().severe("Cannot save rewards.yml: " + e.getMessage());
      }
   }
}
