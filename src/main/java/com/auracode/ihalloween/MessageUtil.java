package com.auracode.ihalloween;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * MiniMessage formatting with brand placeholders.
 */
public final class MessageUtil {

   private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

   private MessageUtil() {
   }

   public static @NotNull Component format(@NotNull String raw, @NotNull Object... args) {
      return MINI_MESSAGE.deserialize(formatString(raw, args));
   }

   public static void send(@NotNull CommandSender sender, @NotNull String prefix, @NotNull String raw, @NotNull Object... args) {
      if (raw == null || raw.isEmpty()) {
         return;
      }
      sender.sendMessage(MINI_MESSAGE.deserialize(prefix + formatString(raw, args)));
   }

   public static @NotNull String formatString(@NotNull String raw, @NotNull Object... args) {
      if (args.length == 0) {
         return raw.replace("{NL}", "\n");
      }
      var result = new StringBuilder(raw.length());
      int length = raw.length();
      for (int i = 0; i < length; i++) {
         char current = raw.charAt(i);
         if (current == '{' && i + 1 < length) {
            int end = raw.indexOf('}', i);
            if (end > i) {
               String index = raw.substring(i + 1, end);
               try {
                  int argIndex = Integer.parseInt(index);
                  if (argIndex >= 0 && argIndex < args.length) {
                     result.append(java.util.Objects.toString(args[argIndex], ""));
                     i = end;
                     continue;
                  }
               } catch (NumberFormatException ignored) {
                  // literal brace
               }
            }
         }
         result.append(current);
      }
      return result.toString().replace("{NL}", "\n");
   }
}
