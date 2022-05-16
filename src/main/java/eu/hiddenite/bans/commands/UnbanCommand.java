package eu.hiddenite.bans.commands;

import eu.hiddenite.bans.BansPlugin;
import eu.hiddenite.bans.helpers.TabCompleteHelper;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.sql.SQLException;
import java.util.Arrays;

public class UnbanCommand extends Command implements TabExecutor {
    private final BansPlugin plugin;

    public UnbanCommand(BansPlugin plugin) {
        super("unban", "hiddenite.ban");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            String usage = plugin.getConfig().getString("command-messages.unban-usage");
            sender.sendMessage(TextComponent.fromLegacyText(usage));
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (reason.isEmpty()) {
            String defaultReason = plugin.getConfig().getString("default-reasons.unban");
            if (defaultReason == null || defaultReason.isEmpty()) {
                String error = plugin.getConfig().getString("command-messages.error-missing-reason");
                sender.sendMessage(TextComponent.fromLegacyText(error));
                return;
            }
            reason = defaultReason;
        }

        BansPlugin.OfflinePlayerInfo targetInfo = plugin.getOfflinePlayer(args[0]);
        if (targetInfo == null) {
            String error = plugin.getConfig().getString("command-messages.error-player-not-found");
            error = error.replace("{PLAYER}", args[0]);
            sender.sendMessage(TextComponent.fromLegacyText(error));
            return;
        }

        boolean alreadyBanned;
        try {
            alreadyBanned = plugin.isPlayerBanned(targetInfo.uniqueId());
        } catch (SQLException e) {
            String error = plugin.getConfig().getString("command-messages.error-database");
            sender.sendMessage(TextComponent.fromLegacyText(error));
            e.printStackTrace();
            return;
        }
        if (!alreadyBanned) {
            String error = plugin.getConfig().getString("command-messages.error-not-banned");
            error = error.replace("{PLAYER}", targetInfo.name());
            sender.sendMessage(TextComponent.fromLegacyText(error));
            return;
        }

        try {
            plugin.unbanPlayer(targetInfo.uniqueId());
        } catch (SQLException e) {
            String error = plugin.getConfig().getString("command-messages.error-database");
            sender.sendMessage(TextComponent.fromLegacyText(error));
            e.printStackTrace();
            return;
        }

        String successMessage = plugin.getConfig().getString("command-messages.unban-success");
        successMessage = successMessage.replace("{PLAYER}", targetInfo.name());
        sender.sendMessage(TextComponent.fromLegacyText(successMessage));

        if (plugin.getWebhook() != null) {
            int unbanColor = plugin.getConfig().getInt("discord.punishments.unban.color");
            String unbanDisplay = plugin.getConfig().getString("discord.punishments.unban.display");
            String moderatorName = sender instanceof ProxiedPlayer ? sender.getName() : plugin.getConfig().getString("ban-message.console-username");
            plugin.getWebhook().sendMessage(unbanColor, targetInfo.name(), targetInfo.uniqueId().toString(), unbanDisplay, reason, moderatorName);
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return TabCompleteHelper.empty();
    }
}
