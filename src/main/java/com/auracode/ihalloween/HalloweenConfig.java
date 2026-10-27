package com.auracode.ihalloween;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Typed configuration for IHalloween.
 */
public final class HalloweenConfig {

   private static final String DEFAULT_PREFIX = "<#FF7000>🎃</#FF7000> <gradient:#8A5CF6:#FF7000><bold>IHalloween</bold></gradient> <dark_gray>»</dark_gray> <gray>";
   private static final Set<String> SUPPORTED_LANGUAGES = Set.of("en", "ru");
   private static final String DEFAULT_LANGUAGE = "ru";

   private final JavaPlugin plugin;

   private String language;
   private String prefix;
   private int candiesMin;
   private int candiesMax;
   private int cursedChancePercent;
   private int cursedExchangeCost;
   private int pumpkinsPerDay;
   private int cityPumpkinsPercent;
   private int cityRadius;
   private int wildRadius;
   private boolean bossEnabled;
   private String timeZone;
   private int bossHour;
   private int bossMinute;
   private int bossRewardCandies;
   private String bossName;
   private double bossHealth;
   private ConfigurationSection messages;

   public HalloweenConfig(@NotNull JavaPlugin plugin) {
      this.plugin = plugin;
   }

   public void load() {
      this.plugin.saveDefaultConfig();
      this.plugin.reloadConfig();
      var config = this.plugin.getConfig();

      this.language = normalizeLanguage(resolveLanguage(config));
      this.messages = loadLanguageSection();
      this.prefix = this.getLangPrefix();

      this.candiesMin = Math.max(1, config.getInt("settings.candies-min", 1));
      this.candiesMax = Math.max(this.candiesMin, config.getInt("settings.candies-max", 3));
      this.cursedChancePercent = Math.min(50, Math.max(1, config.getInt("settings.cursed-chance-percent", 8)));
      this.cursedExchangeCost = Math.max(1, config.getInt("settings.cursed-exchange-cost", 25));
      this.pumpkinsPerDay = Math.max(1, config.getInt("settings.pumpkins-per-day", 120));
      this.cityPumpkinsPercent = Math.min(100, Math.max(0, config.getInt("settings.city-pumpkins-percent", 60)));
      this.cityRadius = Math.max(50, config.getInt("settings.city-radius", 300));
      this.wildRadius = Math.max(500, config.getInt("settings.wild-radius", 2000));

      this.bossEnabled = config.getBoolean("settings.boss.enabled", true);
      this.timeZone = config.getString("settings.boss.time-zone", "Europe/Moscow");
      this.bossHour = Math.min(23, Math.max(0, config.getInt("settings.boss.hour", 20)));
      this.bossMinute = Math.min(59, Math.max(0, config.getInt("settings.boss.minute", 0)));
      this.bossRewardCandies = Math.max(0, config.getInt("settings.boss.reward-candies", 25));
      this.bossName = config.getString("settings.boss.name", "Всадник без головы");
      this.bossHealth = Math.max(20.0D, config.getDouble("settings.boss.health", 200.0D));
   }

   private @NotNull String resolveLanguage(@NotNull org.bukkit.configuration.Configuration config) {
      var override = new java.io.File(this.plugin.getDataFolder(), "language.yml");
      if (override.exists()) {
         String value = YamlConfiguration.loadConfiguration(override).getString("language", "");
         if (value != null && !value.isEmpty()) {
            return value;
         }
      }
      return config.getString("language", DEFAULT_LANGUAGE);
   }

   public void setLanguage(@NotNull String language) {
      try {
         var override = new java.io.File(this.plugin.getDataFolder(), "language.yml");
         if (!override.exists()) {
            java.nio.file.Files.writeString(override.toPath(), "language: " + language + "\n");
         } else {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(override);
            yaml.set("language", language);
            yaml.save(override);
         }
      } catch (java.io.IOException e) {
         this.plugin.getLogger().severe("Cannot save language override: " + e.getMessage());
      }
      this.language = normalizeLanguage(language);
      this.messages = loadLanguageSection();
      this.prefix = this.getLangPrefix();
   }

   private @NotNull ConfigurationSection loadLanguageSection() {
      String resource = "lang/" + this.language + ".yml";
      var file = new java.io.File(this.plugin.getDataFolder(), resource);
      if (!file.exists()) {
         this.plugin.saveResource(resource, false);
      }
      YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
      var section = yaml.getConfigurationSection("messages");
      if (section == null) {
         throw new IllegalStateException("Language file " + resource + " is missing 'messages' section");
      }
      return section;
   }

   public static @NotNull String normalizeLanguage(String raw) {
      String value = raw == null ? DEFAULT_LANGUAGE : raw.toLowerCase(Locale.ROOT);
      return SUPPORTED_LANGUAGES.contains(value) ? value : DEFAULT_LANGUAGE;
   }

   public @NotNull ConfigurationSection getMessages() { return this.messages; }

   public @NotNull String getLangPrefix() {
      var root = this.messages.getRoot();
      return root != null ? root.getString("prefix", DEFAULT_PREFIX) : DEFAULT_PREFIX;
   }

   // ---- Getters ----
   public @NotNull String getPrefix() { return this.prefix; }
   public @NotNull String getLanguage() { return this.language; }
   public @NotNull Set<String> getSupportedLanguages() { return SUPPORTED_LANGUAGES; }
   public int getCandiesMin() { return this.candiesMin; }
   public int getCandiesMax() { return this.candiesMax; }
   public int getCursedChancePercent() { return this.cursedChancePercent; }
   public int getCursedExchangeCost() { return this.cursedExchangeCost; }
   public int getPumpkinsPerDay() { return this.pumpkinsPerDay; }
   public int getCityPumpkinsPercent() { return this.cityPumpkinsPercent; }
   public int getCityRadius() { return this.cityRadius; }
   public int getWildRadius() { return this.wildRadius; }
   public boolean isBossEnabled() { return this.bossEnabled; }
   public @NotNull String getTimeZone() { return this.timeZone; }
   public int getBossHour() { return this.bossHour; }
   public int getBossMinute() { return this.bossMinute; }
   public int getBossRewardCandies() { return this.bossRewardCandies; }
   public @NotNull String getBossName() { return this.bossName; }
   public double getBossHealth() { return this.bossHealth; }
}
