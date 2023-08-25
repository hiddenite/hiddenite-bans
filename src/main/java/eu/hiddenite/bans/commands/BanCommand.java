package eu.hiddenite.bans.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.bans.BansPlugin;
import eu.hiddenite.bans.helpers.DurationParser;
import eu.hiddenite.bans.helpers.TabCompleteHelper;
import net.kyori.adventure.text.Component;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BanCommand implements SimpleCommand {
    private final BansPlugin plugin;
    private final Format dateFormat;

    public BanCommand(BansPlugin plugin) {
        this.plugin = plugin;

        dateFormat = new SimpleDateFormat(plugin.getConfig().commandMessages.dateFormat);
    }

    @Override
    public void execute(final Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 2) {
            String usage = plugin.getConfig().commandMessages.banUsage;
            source.sendMessage(Component.text(usage));
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (reason.isEmpty()) {
            String defaultReason = plugin.getConfig().defaultReasons.ban;
            if (defaultReason == null || defaultReason.isEmpty()) {
                String error = plugin.getConfig().commandMessages.errorMissingReason;
                source.sendMessage(Component.text(error));
                return;
            }
            reason = defaultReason;
        }

        BansPlugin.OfflinePlayerInfo targetInfo = plugin.getOfflinePlayer(args[0]);
        if (targetInfo == null) {
            String error = plugin.getConfig().commandMessages.errorPlayerNotFound;
            error = error.replace("{PLAYER}", args[0]);
            source.sendMessage(Component.text(error));
            return;
        }

        Timestamp untilDate = DurationParser.parse(args[1]);
        if (untilDate == null) {
            String error = plugin.getConfig().commandMessages.errorInvalidDuration;
            error = error.replace("{DURATION}", args[1]);
            source.sendMessage(Component.text(error));
            return;
        }

        boolean alreadyBanned;
        try {
            alreadyBanned = plugin.isPlayerBanned(targetInfo.uniqueId());
        } catch (SQLException e) {
            String error = plugin.getConfig().commandMessages.errorDatabase;
            source.sendMessage(Component.text(error));
            e.printStackTrace();
            return;
        }
        if (alreadyBanned) {
            String error = plugin.getConfig().commandMessages.errorAlreadyBanned;
            error = error.replace("{PLAYER}", targetInfo.name());
            source.sendMessage(Component.text(error));
            return;
        }

        UUID moderatorId = source instanceof Player player ? player.getUniqueId() : null;
        try {
            plugin.banPlayer(targetInfo.uniqueId(), moderatorId, untilDate, reason);
        } catch (SQLException e) {
            String error = plugin.getConfig().commandMessages.errorDatabase;
            source.sendMessage(Component.text(error));
            e.printStackTrace();
            return;
        }

        String successMessage = plugin.getConfig().commandMessages.banSuccess;
        successMessage = successMessage.replace("{PLAYER}", targetInfo.name());
        successMessage = successMessage.replace("{DATE}", dateFormat.format(untilDate));
        source.sendMessage(Component.text(successMessage));


        if (plugin.getProxy().getPlayer(targetInfo.uniqueId()).isPresent()) {
            Player targetPlayer = plugin.getProxy().getPlayer(targetInfo.uniqueId()).get();

            String moderatorName = source instanceof Player player ? player.getUsername() : null;
            targetPlayer.disconnect(plugin.generateBanMessage(reason, moderatorName, untilDate));
        }

        if (plugin.getWebhook() != null) {
            int banColor = plugin.getConfig().discord.punishments.ban.color;
            String banDisplay = plugin.getConfig().discord.punishments.ban.display.replace("{TIME}", args[1]);
            String moderatorName = source instanceof Player player ? player.getUsername() : plugin.getConfig().banMessage.consoleUsername;
            plugin.getWebhook().sendMessage(banColor, targetInfo.name(), targetInfo.uniqueId().toString(), banDisplay, reason, moderatorName);
        }
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("hiddenite.ban");
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 1) {
            return TabCompleteHelper.matchPlayer(args[0]);
        }
        if (args.length == 2) {
            return Arrays.asList("7d", "48h", "2h");
        }
        return TabCompleteHelper.empty();
    }

}
