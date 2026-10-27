package com.auracode.ihalloween;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Spawns glowing jack-o'-lantern pumpkins daily: clustered near world spawn ("city")
 * and scattered in wild lands. Persisted so breaks are not lost across restarts.
 */
public final class PumpkinManager {

   private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;
   private static final int MAX_LOAD_TRIES = 200;
   private static final int SPAWN_CHECK_LIMIT = 25;

   private final IHalloweenPlugin plugin;
   private final NamespacedKey pumpkinKey;
   private final List<Location> pumpkins = new ArrayList<>();
   private long lastSpawnMillis = 0L;

   public PumpkinManager(@NotNull IHalloweenPlugin plugin) {
      this.plugin = plugin;
      this.pumpkinKey = new NamespacedKey(plugin, "halloween_pumpkin");
      this.loadPumpkins();
      this.scheduleRespawn();
   }

   private void scheduleRespawn() {
      long millisUntilNext = this.millisUntilNextSpawning();
      Bukkit.getScheduler().runTaskLater(this.plugin, this::spawnDailyBatch, Math.max(1L, millisUntilNext / 50L));
   }

   private long millisUntilNextSpawning() {
      var config = this.plugin.getHalloweenConfig();
      try {
         ZoneId zone = ZoneId.of(config.getTimeZone());
         ZonedDateTime now = ZonedDateTime.now(zone);
         ZonedDateTime nextNoon = now.plusHours(1).toLocalDate().atStartOfDay(zone).plusDays(1).withHour(12);
         return Duration.between(now, nextNoon).toMillis();
      } catch (RuntimeException e) {
         this.plugin.getLogger().warning("Invalid time zone, respawning pumpkins in 24h");
         return DAY_MILLIS;
      }
   }

   private void spawnDailyBatch() {
      int batch = this.plugin.getHalloweenConfig().getPumpkinsPerDay();
      this.spawnPumpkins(batch);
      this.lastSpawnMillis = System.currentTimeMillis();
      this.savePumpkins();
      this.scheduleRespawn();
   }

   private void spawnPumpkins(int count) {
      var config = this.plugin.getHalloweenConfig();
      List<World> worlds = Bukkit.getWorlds();
      if (worlds.isEmpty()) {
         return;
      }
      World mainWorld = worlds.getFirst();
      Location spawnCenter = mainWorld.getSpawnLocation();

      int cityCount = count * config.getCityPumpkinsPercent() / 100;
      int wildCount = count - cityCount;

      int spawned = 0;
      for (int i = 0; i < cityCount && spawned < MAX_LOAD_TRIES; i++) {
         if (this.trySpawnPumpkin(mainWorld, spawnCenter, config.getCityRadius(), true)) {
            spawned++;
         }
      }
      for (int i = 0; i < wildCount && spawned < MAX_LOAD_TRIES * 2; i++) {
         if (this.trySpawnPumpkin(mainWorld, spawnCenter, config.getWildRadius(), false)) {
            spawned++;
         }
      }
      this.plugin.getLogger().info("Spawned " + (spawned >= 0 ? spawned : 0) + " halloween pumpkins");
   }

   private boolean trySpawnPumpkin(@NotNull World world, @NotNull Location center, int radius, boolean city) {
      Random random = ThreadLocalRandom.current();
      for (int attempts = 0; attempts < SPAWN_CHECK_LIMIT; attempts++) {
         int x = center.getBlockX() + random.nextInt(-radius, radius);
         int z = center.getBlockZ() + random.nextInt(-radius, radius);
         int y = world.getHighestBlockYAt(x, z);
         Block surface = world.getBlockAt(x, y, z);
         Block surfaceBlock = world.getBlockAt(x, y + 1, z);
         Material surfaceType = surface.getType();
         if (!surfaceType.isSolid() || city && surfaceType == Material.WATER) {
            continue;
         }
         if (!surfaceBlock.getType().isAir()) {
            continue;
         }
         surfaceBlock.setType(Material.JACK_O_LANTERN, false);
         this.registerPumpkin(surfaceBlock.getLocation());
         return true;
      }
      return false;
   }

   public void registerPumpkin(@NotNull Location location) {
      this.pumpkins.add(location.toBlockLocation());
   }

   public boolean isPumpkin(@NotNull Block block) {
      if (block.getType() != Material.JACK_O_LANTERN) {
         return false;
      }
      return this.pumpkins.stream().anyMatch(location -> location.equals(block.getLocation().toBlockLocation()));
   }

   public void removePumpkin(@NotNull Block block) {
      this.pumpkins.removeIf(location -> location.equals(block.getLocation().toBlockLocation()));
      this.savePumpkins();
   }

   private void loadPumpkins() {
      Path file = this.plugin.getDataFolder().toPath().resolve("pumpkins.yml");
      if (Files.notExists(file)) {
         // First start: spawn immediately.
         this.plugin.getLogger().info("First start — spawning halloween pumpkins");
         Bukkit.getScheduler().runTask(this.plugin, () -> {
            this.spawnPumpkins(this.plugin.getHalloweenConfig().getPumpkinsPerDay());
            this.savePumpkins();
         });
         return;
      }
      YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
      List<String> entries = yaml.getStringList("pumpkins");
      entries.forEach(entry -> {
         String[] parts = entry.split(";");
         if (parts.length == 4) {
            World world = Bukkit.getWorld(parts[0]);
            if (world != null) {
               try {
                  Location location = new Location(world,
                     Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
                  world.getBlockAt(location).setType(Material.JACK_O_LANTERN, false);
                  this.pumpkins.add(location.toBlockLocation());
               } catch (NumberFormatException e) {
                  this.plugin.getLogger().warning("Invalid pumpkin entry: " + entry);
               }
            }
         }
      });
      this.plugin.getLogger().info("Restored " + this.pumpkins.size() + " halloween pumpkins");
   }

   public void savePumpkins() {
      Path file = this.plugin.getDataFolder().toPath().resolve("pumpkins.yml");
      YamlConfiguration yaml = new YamlConfiguration();
      List<String> entries = new ArrayList<>(this.pumpkins.size());
      this.pumpkins.forEach(location -> {
         if (location.getWorld() != null) {
            entries.add(location.getWorld().getName() + ";" + location.getX() + ";" + location.getY() + ";" + location.getZ());
         }
      });
      yaml.set("pumpkins", entries);
      try {
         java.nio.file.Files.createDirectories(file.getParent());
         yaml.save(file.toFile());
      } catch (IOException e) {
         this.plugin.getLogger().severe("Cannot save pumpkins.yml: " + e.getMessage());
      }
   }

   public @NotNull ItemStack createCursedPumpkinItem() {
      var plugin = this.plugin;
      var item = new org.bukkit.inventory.ItemStack(Material.CARVED_PUMPKIN);
      var meta = item.getItemMeta();
      meta.displayName(MessageUtil.format("<gradient:#8A5CF6:#FF7000><bold>Проклятая тыква</bold></gradient>"));
      meta.lore(List.of(
         MessageUtil.format("<gray>Редкий артефакт ночи тыкв.</gray>"),
         MessageUtil.format("<gray>Обменяйте на <white>" + this.plugin.getHalloweenConfig().getCursedExchangeCost() + " 🍬</white> у магазина.</gray>")
      ));
      meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "halloween_cursed"), PersistentDataType.BYTE, (byte) 1);
      item.setItemMeta(meta);
      return item;
   }

   public boolean isCursedPumpkin(@NotNull org.bukkit.inventory.ItemStack stack) {
      var meta = stack.getItemMeta();
      if (meta == null) {
         return false;
      }
      return meta.getPersistentDataContainer().has(new NamespacedKey(this.plugin, "halloween_cursed"), PersistentDataType.BYTE);
   }

   public long getLastSpawnMillis() {
      return this.lastSpawnMillis;
   }
}
