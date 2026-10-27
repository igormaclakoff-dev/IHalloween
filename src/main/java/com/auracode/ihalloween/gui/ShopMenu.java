package com.auracode.ihalloween.gui;

import com.auracode.ihalloween.IHalloweenPlugin;
import com.auracode.ihalloween.MessageUtil;
import com.auracode.ihalloween.model.Reward;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Halloween shop GUI: [exchange][rewards 0-8][admin][frame].
 */
public final class ShopMenu implements InventoryHolder {

   public static final int SIZE = 27;
   public static final int SLOT_EXCHANGE = 0;
   public static final int SLOT_ADMIN = 8;
   public static final int SLOT_REWARD_START = 9;
   public static final int REWARD_SLOTS = 9;
   public static final String ADMIN_PERMISSION = "halloween.admin";

   private final IHalloweenPlugin plugin;
   private final Player viewer;
   private final Inventory inventory;

   public ShopMenu(@NotNull IHalloweenPlugin plugin, @NotNull Player viewer) {
      this.plugin = plugin;
      this.viewer = viewer;
      var config = plugin.getHalloweenConfig();
      this.inventory = Bukkit.createInventory(this, SIZE,
         MessageUtil.format(config.getMessages().getString("shop-title", ""),
            plugin.getCandyService().getCandies(viewer)));
      this.render();
   }

   public void render() {
      this.inventory.clear();
      var config = this.plugin.getHalloweenConfig();

      var exchange = new ItemStack(Material.SUGAR);
      var exchangeMeta = exchange.getItemMeta();
      exchangeMeta.displayName(MessageUtil.format("<gradient:#8A5CF6:#FF7000><bold>Обмен Проклятой тыквы</bold></gradient>"));
      exchangeMeta.lore(List.of(
         MessageUtil.format("<gray>Обменяет Проклятую тыкву в руке:</gray>"),
         MessageUtil.format("<white>+{0} 🍬</white> конфет", config.getCursedExchangeCost())
      ));
      exchange.setItemMeta(exchangeMeta);

      var admin = new ItemStack(Material.COMMAND_BLOCK);
      var adminMeta = admin.getItemMeta();
      adminMeta.displayName(MessageUtil.format("<gray>Админ-меню наград</gray>"));
      admin.setItemMeta(adminMeta);

      this.inventory.setItem(SLOT_EXCHANGE, exchange);
      if (this.viewer.hasPermission(ADMIN_PERMISSION)) {
         this.inventory.setItem(SLOT_ADMIN, admin);
      }

      for (var reward : this.plugin.getRewardService().getRewards()) {
         if (reward.slot() < 0 || reward.slot() >= REWARD_SLOTS) {
            continue;
         }
         this.inventory.setItem(SLOT_REWARD_START + reward.slot(), this.rewardIcon(reward));
      }
      this.fillFrame();
   }

   private @NotNull ItemStack rewardIcon(@NotNull Reward reward) {
      Material material = Material.matchMaterial(reward.icon());
      if (material == null) {
         material = Material.BARRIER;
      }
      var icon = new ItemStack(material);
      var meta = icon.getItemMeta();
      meta.displayName(MessageUtil.format("<#FF7000>🎁</#FF7000> <white>" + reward.name() + "</white>"));
      meta.lore(List.of(
         MessageUtil.format("<gray>Цена:</gray> <white>{0} 🍬</white>", reward.cost()),
         MessageUtil.format("<gray>Кликните, чтобы купить.</gray>")
      ));
      icon.setItemMeta(meta);
      return icon;
   }

   private void fillFrame() {
      var frame = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
      var meta = frame.getItemMeta();
      meta.displayName(MessageUtil.format("<dark_gray>Halloween 🎃</dark_gray>"));
      frame.setItemMeta(meta);
      for (int slot = 0; slot < SIZE; slot++) {
         if (this.inventory.getItem(slot) == null) {
            this.inventory.setItem(slot, frame);
         }
      }
   }

   public @NotNull Player getViewer() {
      return this.viewer;
   }

   @Override
   public @NotNull Inventory getInventory() {
      return this.inventory;
   }
}
