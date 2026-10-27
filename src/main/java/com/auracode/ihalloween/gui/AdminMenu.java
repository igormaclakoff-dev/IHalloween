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
 * Admin menu (single row of 9): open slots to configure rewards.
 * Clicking an empty slot starts chat-capture edit flow for that slot.
 * Shift+click on an existing reward removes it.
 */
public final class AdminMenu implements InventoryHolder {

   public static final int SIZE = 27;
   public static final String ADMIN_PERMISSION = "halloween.admin";

   private final IHalloweenPlugin plugin;
   private final Inventory inventory;

   public AdminMenu(@NotNull IHalloweenPlugin plugin, @NotNull Player viewer) {
      this.plugin = plugin;
      this.inventory = Bukkit.createInventory(this, SIZE,
         MessageUtil.format(plugin.getHalloweenConfig().getMessages().getString("admin-title", "")));
      this.render();
   }

   public void render() {
      this.inventory.clear();
      var empty = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
      var emptyMeta = empty.getItemMeta();
      emptyMeta.displayName(MessageUtil.format("<green>+ Добавить награду</green>"));
      emptyMeta.lore(List.of(
         MessageUtil.format("<gray>В основной руке держите предмет-иконку.</gray>"),
         MessageUtil.format("<gray>Клик — начнёт настройку в чате.</gray>")
      ));
      empty.setItemMeta(emptyMeta);

      for (int slot = 0; slot < 9; slot++) {
         var opt = this.plugin.getRewardService().findReward(slot);
         if (opt.isEmpty()) {
            this.inventory.setItem(slot, empty);
            continue;
         }
         Reward reward = opt.get();
         var icon = new ItemStack(Material.matchMaterial(reward.icon()) == null
            ? Material.BARRIER : Material.matchMaterial(reward.icon()));
         var meta = icon.getItemMeta();
         meta.displayName(MessageUtil.format("<#FF7000>{0}</#FF7000> <white>{1}</white>", slot, reward.name()));
         meta.lore(List.of(
            MessageUtil.format("<gray>Цена:</gray> <white>{0} 🍬</white>", reward.cost()),
            MessageUtil.format("<gray>Команды:</gray>"),
            MessageUtil.format("<dark_gray>" + String.join(", ", reward.commands()) + "</dark_gray>"),
            MessageUtil.format("<yellow>Shift+Клик — удалить</yellow>")
         ));
         icon.setItemMeta(meta);
         this.inventory.setItem(slot, icon);
      }
      for (int slot = 9; slot < 27; slot++) {
         this.inventory.setItem(slot, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
      }
   }

   @Override
   public @NotNull Inventory getInventory() {
      return this.inventory;
   }
}
