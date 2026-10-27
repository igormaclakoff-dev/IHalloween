package com.auracode.ihalloween;

import java.time.ZoneId;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Daily Headless Horseman world boss at configured time.
 */
public final class BossManager {

   private static final String HORSE_NAME = "Костяной конь";
   private static final long SCHEDULE_CHECK_INTERVAL_TICKS = 20L * 60L * 10L;

   private LivingEntity boss;
   private LivingEntity horse;

   private final IHalloweenPlugin plugin;
   private int lastSpawnedDay = -1;

   public BossManager(@NotNull IHalloweenPlugin plugin) {
      this.plugin = plugin;
      this.scheduleDailySpawn();
   }

   private void scheduleDailySpawn() {
      Bukkit.getScheduler().runTaskTimer(this.plugin, () -> this.maybeSpawnToday(), 20L * 30L, SCHEDULE_CHECK_INTERVAL_TICKS);
   }

   private void maybeSpawnToday() {
      if (this.plugin.getHalloweenConfig().isBossEnabled() && this.isBossSpawnWindow()) {
         this.spawnBoss();
      }
   }

   private boolean isBossSpawnWindow() {
      ZoneId zone;
      try {
         zone = ZoneId.of(this.plugin.getHalloweenConfig().getTimeZone());
      } catch (RuntimeException e) {
         this.plugin.getLogger().warning("Invalid boss time zone, using server default");
         zone = ZoneId.systemDefault();
      }
      var now = java.time.ZonedDateTime.now(zone);
      int dayOfYear = now.getDayOfYear();
      int hour = now.getHour();
      int minute = now.getMinute();
      boolean isTimeWindow = hour == this.plugin.getHalloweenConfig().getBossHour()
         && minute <= this.plugin.getHalloweenConfig().getBossMinute() + 5;
      if (isTimeWindow && dayOfYear != this.lastSpawnedDay) {
         this.lastSpawnedDay = dayOfYear;
         return true;
      }
      return false;
   }

   public void spawnBoss() {
      if (this.boss != null && this.boss.isValid()) {
         return;
      }
      List<Player> online = List.copyOf(Bukkit.getOnlinePlayers());
      if (online.isEmpty()) {
         return;
      }
      var config = this.plugin.getHalloweenConfig();
      Player reference = online.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(online.size()));
      Location location = reference.getLocation().clone().add(
         java.util.concurrent.ThreadLocalRandom.current().nextInt(-40, 41),
         0,
         java.util.concurrent.ThreadLocalRandom.current().nextInt(-40, 41));

      var world = location.getWorld();
      if (world == null) {
         return;
      }
      int y = world.getHighestBlockYAt(location) + 1;
      location.setY(y);

      Skeleton rider = (Skeleton) world.spawnEntity(location, EntityType.SKELETON);
      rider.customName(MessageUtil.format("<gradient:#8A5CF6:#FF7000><bold>" + config.getBossName() + "</bold></gradient>"));
      rider.setCustomNameVisible(true);
      rider.setGlowing(true);
      rider.setMaxHealth(config.getBossHealth());
      rider.setHealth(config.getBossHealth());
      rider.getEquipment().setHelmet(new ItemStack(Material.CARVED_PUMPKIN));

      SkeletonHorse horse = (SkeletonHorse) world.spawnEntity(location, EntityType.SKELETON_HORSE);
      horse.setTamed(false);
      horse.customName(MessageUtil.format("<dark_gray>Костяной конь</dark_gray>"));
      horse.setCustomNameVisible(false);
      horse.setGlowing(true);
      horse.addPassenger(rider);

      this.boss = rider;
      this.horse = horse;
      this.plugin.broadcastBossSpawn(location);
   }

   public void onBossDeath(@NotNull LivingEntity deadBoss, @NotNull Player killer) {
      if (this.boss == null || deadBoss.getUniqueId() != this.boss.getUniqueId()) {
         return;
      }
      var config = this.plugin.getHalloweenConfig();
      int reward = config.getBossRewardCandies();
      this.plugin.getCandyService().addCandies(killer, reward);
      this.plugin.broadcastBossKilled(killer, reward);
      if (this.horse != null && this.horse.isValid()) {
         this.horse.remove();
      }
      this.boss = null;
      this.horse = null;
   }

   public void onUnload(@NotNull Entity entity) {
      if (this.boss != null && entity.getUniqueId() == this.boss.getUniqueId()) {
         this.boss = null;
      }
      if (this.horse != null && entity.getUniqueId() == this.horse.getUniqueId()) {
         this.horse = null;
      }
   }

   /**
    * True if a boss is currently alive.
    */
   public boolean isBossAlive() {
      return this.boss != null && this.boss.isValid();
   }

   /**
    * Used to clean up on plugin disable: remove boss and mount.
    */
   public void despawnAll() {
      if (this.boss != null && this.boss.isValid()) {
         this.boss.remove();
      }
      if (this.horse != null && this.horse.isValid()) {
         this.horse.remove();
      }
      this.boss = null;
      this.horse = null;
   }
}
