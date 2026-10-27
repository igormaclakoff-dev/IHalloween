package com.auracode.ihalloween.listener;

import com.auracode.ihalloween.gui.AdminMenu;
import com.auracode.ihalloween.IHalloweenPlugin;
import com.auracode.ihalloween.MessageUtil;
import com.auracode.ihalloween.gui.ShopMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event wiring: pumpkin breaks, shop/admin clicks, admin chat capture.
 */
public final class HalloweenListener implements Listener {

   private final IHalloweenPlugin plugin;
   private final Map<UUID, Integer> captureSlot = new ConcurrentHashMap<>();
   private final Map<UUID, Integer> captureStage = new ConcurrentHashMap<>(); // 1 = cost, 2 = command
   private final Map<UUID, Material> captureIcon = new ConcurrentHashMap<>();

   public HalloweenListener(@NotNull IHalloweenPlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onPumpkinBreak(@NotNull BlockBreakEvent event) {
      var config = this.plugin.getHalloweenConfig();
      if (!this.plugin.getPumpkinManager().isPumpkin(event.getBlock())) {
         return;
      }
      event.setDropItems(false);
      this.plugin.getPumpkinManager().removePumpkin(event.getBlock());

      Player player = event.getPlayer();
      int candies = java.util.concurrent.ThreadLocalRandom.current().nextInt(config.getCandiesMin(), config.getCandiesMax() + 1);
      int total = this.plugin.getCandyService().addCandies(player, candies);
      MessageUtil.send(player, config.getPrefix(), config.getMessages().getString("candies-gain", ""), candies, total);

      if (java.util.concurrent.ThreadLocalRandom.current().nextInt(100) < config.getCursedChancePercent()) {
         player.getInventory().addItem(this.plugin.getPumpkinManager().createCursedPumpkinItem());
         MessageUtil.send(player, config.getPrefix(), config.getMessages().getString("cursed-gain", ""));
      }
      event.getBlock().setType(Material.AIR, false);
   }

   @EventHandler
   public void onBossDeath(@NotNull EntityDeathEvent event) {
      LivingEntity entity = event.getEntity();
      Player killer = entity.getKiller();
      if (killer != null) {
         this.plugin.getBossManager().onBossDeath(entity, killer);
      } else {
         this.plugin.getBossManager().onUnload(entity);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onInventoryClick(@NotNull InventoryClickEvent event) {
      var top = event.getView().getTopInventory();
      InventoryHolder holder = top.getHolder(false);
      if (holder instanceof ShopMenu shopMenu) {
         this.handleShopClick(event, shopMenu);
      } else if (holder instanceof AdminMenu adminMenu) {
         this.handleAdminClick(event, adminMenu);
      }
   }

   private void handleShopClick(@NotNull InventoryClickEvent event, @NotNull ShopMenu menu) {
      event.setCancelled(true);
      if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != menu.getInventory()) {
         return;
      }
      var config = this.plugin.getHalloweenConfig();
      int slot = event.getSlot();

      if (slot == ShopMenu.SLOT_EXCHANGE) {
         ItemStack hand = player.getInventory().getItemInMainHand();
         if (hand.isEmpty() || !this.plugin.getPumpkinManager().isCursedPumpkin(hand)) {
            MessageUtil.send(player, config.getPrefix(), config.getMessages().getString("exchange-no-item", ""));
            return;
         }
         hand.setAmount(hand.getAmount() - 1);
         int gain = config.getCursedExchangeCost();
         int total = this.plugin.getCandyService().addCandies(player, gain);
         MessageUtil.send(player, config.getPrefix(), config.getMessages().getString("exchange-done", ""), gain);
         MessageUtil.send(player, config.getPrefix(), config.getMessages().getString("candies-balance", ""), total);
         return;
      }

      if (slot == ShopMenu.SLOT_ADMIN) {
         if (!player.hasPermission(AdminMenu.ADMIN_PERMISSION)) {
            return;
         }
         Inventory inventory = new AdminMenu(this.plugin, player).getInventory();
         player.openInventory(inventory);
         return;
      }

      if (slot < ShopMenu.SLOT_REWARD_START || slot >= ShopMenu.SLOT_REWARD_START + 9) {
         return;
      }
      int rewardSlot = slot - ShopMenu.SLOT_REWARD_START;
      var opt = this.plugin.getRewardService().findReward(rewardSlot);
      if (opt.isEmpty()) {
         return;
      }
      var reward = opt.get();
      if (!this.plugin.getCandyService().trySpend(player, reward.cost())) {
         MessageUtil.send(player, config.getPrefix(), config.getMessages().getString("shop-not-enough", ""), reward.cost());
         return;
      }
      this.plugin.getRewardService().give(player, reward);
      int left = this.plugin.getCandyService().getCandies(player);
      MessageUtil.send(player, config.getPrefix(), config.getMessages().getString("shop-bought", ""), left);
      player.openInventory(new ShopMenu(this.plugin, player).getInventory());
   }

   private void handleAdminClick(@NotNull InventoryClickEvent event, @NotNull AdminMenu menu) {
      event.setCancelled(true);
      if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != menu.getInventory()) {
         return;
      }
      int slot = event.getSlot();
      if (slot < 0 || slot > 8) {
         return;
      }
      var config = this.plugin.getHalloweenConfig();
      boolean existing = this.plugin.getRewardService().hasReward(slot);

      if (existing && event.isShiftClick()) {
         this.plugin.getRewardService().removeReward(slot);
         MessageUtil.send(player, config.getPrefix(), config.getMessages().getString("reward-removed", ""), slot);
         player.openInventory(new AdminMenu(this.plugin, player).getInventory());
         return;
      }
      if (!existing) {
         ItemStack icon = player.getInventory().getItemInMainHand();
         if (icon.isEmpty()) {
            MessageUtil.send(player, config.getPrefix(), config.getMessages().getString("slot-taken", "Пустой слот."), slot);
            return;
         }
         this.captureIcon.put(player.getUniqueId(), icon.getType());
         this.captureSlot.put(player.getUniqueId(), slot);
         this.captureStage.put(player.getUniqueId(), 1);
         player.closeInventory();
         MessageUtil.send(player, config.getPrefix(), config.getMessages().getString("chat-capture-cost", ""), slot);
      }
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onChat(@NotNull AsyncPlayerChatEvent event) {
      Player player = event.getPlayer();
      Integer stage = this.captureStage.get(player.getUniqueId());
      if (stage == null) {
         return;
      }
      event.setCancelled(true);
      String input = event.getMessage().trim();
      Integer slot = this.captureSlot.get(player.getUniqueId());
      if (slot == null) {
         this.captureStage.remove(player.getUniqueId());
         return;
      }
      var config = this.plugin.getHalloweenConfig();
      if (input.equalsIgnoreCase("cancel")) {
         this.captureStage.remove(player.getUniqueId());
         this.captureSlot.remove(player.getUniqueId());
         this.captureIcon.remove(player.getUniqueId());
         return;
      }
      if (stage == 1) {
         try {
            int cost = Integer.parseInt(input);
            if (cost < 1) {
               throw new NumberFormatException("cost must be positive");
            }
            Material icon = this.captureIcon.get(player.getUniqueId());
            this.plugin.getRewardService().setReward(slot, cost, icon == null ? Material.PUMPKIN : icon, "Награда " + slot, List.of());
            this.captureStage.put(player.getUniqueId(), 2);
            MessageUtil.send(player, config.getPrefix(), config.getMessages().getString("chat-captured", ""), "цена: " + cost);
            MessageUtil.send(player, config.getPrefix(), config.getMessages().getString("chat-capture-command", ""), slot);
         } catch (NumberFormatException e) {
            MessageUtil.send(player, config.getPrefix(), config.getMessages().getString("invalid-number", ""));
         }
         return;
      }
      var finalSlot = slot;
      this.plugin.getRewardService().findReward(slot).ifPresent(reward -> {
         var commands = new java.util.ArrayList<>(reward.commands());
         commands.add(input);
         Material iconMaterial = Material.matchMaterial(reward.icon());
         this.plugin.getRewardService().setReward(finalSlot, reward.cost(), iconMaterial == null ? Material.PUMPKIN : iconMaterial, reward.name(), commands);
         this.captureStage.remove(player.getUniqueId());
         this.captureSlot.remove(player.getUniqueId());
         this.captureIcon.remove(player.getUniqueId());
         MessageUtil.send(player, config.getPrefix(), config.getMessages().getString("reward-saved", ""), finalSlot, reward.cost(), input);
      });
      this.plugin.getRewardService().save();
   }
}
