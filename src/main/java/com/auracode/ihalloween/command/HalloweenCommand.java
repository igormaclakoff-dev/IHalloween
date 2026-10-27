package com.auracode.ihalloween.command;

import com.auracode.ihalloween.IHalloweenPlugin;
import com.auracode.ihalloween.MessageUtil;
import com.auracode.ihalloween.gui.AdminMenu;
import com.auracode.ihalloween.gui.ShopMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * /halloween command: shop, balance, lang, boss, admin, reload.
 */
public final class HalloweenCommand implements CommandExecutor, TabCompleter {

   private static final String SUB_SHOP = "shop";
   private static final String SUB_BALANCE = "balance";
   private static final String SUB_LANG = "lang";
   private static final String SUB_BOSS = "boss";
   private static final String SUB_ADMIN = "admin";
   private static final String SUB_RELOAD = "reload";
   private static final String ADMIN_PERMISSION = "halloween.admin";
   private static final String RELOAD_PERMISSION = "halloween.admin";

   private final IHalloweenPlugin plugin;

   public HalloweenCommand(@NotNull IHalloweenPlugin plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      var config = this.plugin.getHalloweenConfig();

      if (args.length == 1 && SUB_RELOAD.equalsIgnoreCase(args[0])) {
         if (!sender.hasPermission(RELOAD_PERMISSION)) {
            MessageUtil.send(sender, config.getPrefix(), config.getMessages().getString("no-permission", ""));
            return true;
         }
         config.load();
         this.plugin.getRewardService().save();
         MessageUtil.send(sender, config.getPrefix(), config.getMessages().getString("reload-done", ""));
         return true;
      }

      if (args.length >= 1 && SUB_LANG.equalsIgnoreCase(args[0])) {
         if (!sender.hasPermission(RELOAD_PERMISSION)) {
            MessageUtil.send(sender, config.getPrefix(), config.getMessages().getString("no-permission", ""));
            return true;
         }
         this.handleLang(sender, args);
         return true;
      }

      if (args.length >= 1 && SUB_BOSS.equalsIgnoreCase(args[0])) {
         if (!sender.hasPermission(ADMIN_PERMISSION)) {
            MessageUtil.send(sender, config.getPrefix(), config.getMessages().getString("no-permission", ""));
            return true;
         }
         this.plugin.getBossManager().spawnBoss();
         return true;
      }

      if (!(sender instanceof Player player)) {
         MessageUtil.send(sender, config.getPrefix(), config.getMessages().getString("player-only", ""));
         return true;
      }

      String sub = args.length < 1 ? SUB_SHOP : args[0].toLowerCase(Locale.ROOT);
      switch (sub) {
         case SUB_SHOP -> player.openInventory(new ShopMenu(this.plugin, player).getInventory());
         case SUB_ADMIN -> this.handleAdmin(player);
         case SUB_BALANCE -> MessageUtil.send(player, config.getPrefix(), config.getMessages().getString("candies-balance", ""),
            this.plugin.getCandyService().getCandies(player));
         default -> MessageUtil.send(player, config.getPrefix(), config.getMessages().getString("candies-balance", ""),
            this.plugin.getCandyService().getCandies(player));
      }
      return true;
   }

   private void handleAdmin(@NotNull Player player) {
      if (!player.hasPermission(ADMIN_PERMISSION)) {
         MessageUtil.send(player, this.plugin.getHalloweenConfig().getPrefix(),
            this.plugin.getHalloweenConfig().getMessages().getString("no-permission", ""));
         return;
      }
      player.openInventory(new AdminMenu(this.plugin, player).getInventory());
   }

   private void handleLang(@NotNull CommandSender sender, @NotNull String[] args) {
      var config = this.plugin.getHalloweenConfig();
      if (args.length < 2) {
         MessageUtil.send(sender, config.getPrefix(), config.getMessages().getString("lang-list", "en|ru {0}"), config.getLanguage());
         return;
      }
      String language = args[1].toLowerCase(Locale.ROOT);
      if (!config.getSupportedLanguages().contains(language)) {
         MessageUtil.send(sender, config.getPrefix(), config.getMessages().getString("lang-list", ""), config.getLanguage());
         return;
      }
      config.setLanguage(language);
      MessageUtil.send(sender, config.getPrefix(), config.getMessages().getString("lang-set", ""), language);
   }

   @Override
   public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (args.length == 0) {
         return List.of();
      }
      if (args.length == 1) {
         List<String> options = Stream.of(SUB_SHOP, SUB_BALANCE, SUB_LANG).toList();
         return sender.hasPermission(ADMIN_PERMISSION) || sender.hasPermission(RELOAD_PERMISSION)
            ? Stream.of(SUB_SHOP, SUB_BALANCE, SUB_LANG, SUB_BOSS, SUB_ADMIN, SUB_RELOAD).toList()
            : options;
      }
      if (SUB_LANG.equalsIgnoreCase(args[0]) && args.length == 2) {
         return Stream.of("en", "ru").filter(l -> l.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
      }
      return List.of();
   }
}
