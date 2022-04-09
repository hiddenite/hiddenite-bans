package eu.hiddenite.bans.commands;

import eu.hiddenite.bans.BansPlugin;
import eu.hiddenite.bans.helpers.DurationParser;
import eu.hiddenite.bans.helpers.TabCompleteHelper;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.UUID;

public class BanCommand extends Command implements TabExecutor {
    private final BansPlugin plugin;
    private final Format dateFormat;

    public BanCommand(BansPlugin plugin) {
        super("ban", "hiddenite.ban");
        this.plugin = plugin;

        dateFormat = new SimpleDateFormat(plugin.getConfig().getString("command-messages.date-format"));
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            String usage = plugin.getConfig().getString("command-messages.ban-usage");
            sender.sendMessage(TextComponent.fromLegacyText(usage));
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (reason.isEmpty()) {
            String defaultReason = plugin.getConfig().getString("default-reasons.ban");
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

        Timestamp untilDate = DurationParser.parse(args[1]);
        if (untilDate == null) {
            String error = plugin.getConfig().getString("command-messages.error-invalid-duration");
            error = error.replace("{DURATION}", args[1]);
            sender.sendMessage(TextComponent.fromLegacyText(error));
            return;
        }

        boolean alreadyBanned;
        try {
            alreadyBanned = plugin.isPlayerBanned(targetInfo.uniqueId);
        } catch (SQLException e) {
            String error = plugin.getConfig().getString("command-messages.error-database");
            sender.sendMessage(TextComponent.fromLegacyText(error));
            e.printStackTrace();
            return;
        }
        if (alreadyBanned) {
            String error = plugin.getConfig().getString("command-messages.error-already-banned");
            error = error.replace("{PLAYER}", targetInfo.name);
            sender.sendMessage(TextComponent.fromLegacyText(error));
            return;
        }

        UUID moderatorId = sender instanceof ProxiedPlayer ? ((ProxiedPlayer)sender).getUniqueId() : null;
        try {
            plugin.banPlayer(targetInfo.uniqueId, moderatorId, untilDate, reason);
        } catch (SQLException e) {
            String error = plugin.getConfig().getString("command-messages.error-database");
            sender.sendMessage(TextComponent.fromLegacyText(error));
            e.printStackTrace();
            return;
        }

        String successMessage = plugin.getConfig().getString("command-messages.ban-success");
        successMessage = successMessage.replace("{PLAYER}", targetInfo.name);
        successMessage = successMessage.replace("{DATE}", dateFormat.format(untilDate));
        sender.sendMessage(TextComponent.fromLegacyText(successMessage));

        ProxiedPlayer targetPlayer = ProxyServer.getInstance().getPlayer(targetInfo.uniqueId);
        if (targetPlayer != null) {
            String moderatorName = sender instanceof ProxiedPlayer ? sender.getName() : null;
            targetPlayer.disconnect(plugin.generateBanMessage(reason, moderatorName, untilDate));
        }

        if (plugin.getWebhook() != null) {
            int banColor = plugin.getConfig().getInt("discord.punishments.ban.color");
            String banDisplay = plugin.getConfig().getString("discord.punishments.ban.display").replace("{TIME}", args[1]);
            String moderatorName = sender instanceof ProxiedPlayer ? sender.getName() : plugin.getConfig().getString("ban-message.console-username");
            plugin.getWebhook().sendMessage(banColor, targetInfo.name, targetInfo.uniqueId.toString(), banDisplay, reason, moderatorName);
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return TabCompleteHelper.matchPlayer(args[0]);
        }
        if (args.length == 2) {
            return Arrays.asList("7d", "48h", "2h");
        }
        return TabCompleteHelper.empty();
    }
}
