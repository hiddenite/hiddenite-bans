package eu.hiddenite.bans.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.bans.BansPlugin;
import eu.hiddenite.bans.helpers.TabCompleteHelper;
import net.kyori.adventure.text.Component;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class UnbanCommand implements SimpleCommand {
    private final BansPlugin plugin;

    public UnbanCommand(BansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(final Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 1) {
            String usage = plugin.getConfig().commandMessages.unbanUsage;
            source.sendMessage(Component.text(usage));
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (reason.isEmpty()) {
            String defaultReason = plugin.getConfig().defaultReasons.unban;
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

        boolean alreadyBanned;
        try {
            alreadyBanned = plugin.isPlayerBanned(targetInfo.uniqueId());
        } catch (SQLException e) {
            String error = plugin.getConfig().commandMessages.errorDatabase;
            source.sendMessage(Component.text(error));
            e.printStackTrace();
            return;
        }
        if (!alreadyBanned) {
            String error = plugin.getConfig().commandMessages.errorNotBanned;
            error = error.replace("{PLAYER}", targetInfo.name());
            source.sendMessage(Component.text(error));
            return;
        }

        try {
            plugin.unbanPlayer(targetInfo.uniqueId());
        } catch (SQLException e) {
            String error = plugin.getConfig().commandMessages.errorDatabase;
            source.sendMessage(Component.text(error));
            e.printStackTrace();
            return;
        }

        String successMessage = plugin.getConfig().commandMessages.unbanSuccess;
        successMessage = successMessage.replace("{PLAYER}", targetInfo.name());
        source.sendMessage(Component.text(successMessage));

        if (plugin.getWebhook() != null) {
            int unbanColor = plugin.getConfig().discord.punishments.unban.color;
            String unbanDisplay = plugin.getConfig().discord.punishments.unban.display;
            String moderatorName = source instanceof Player player ? player.getUsername() : plugin.getConfig().banMessage.consoleUsername;
            plugin.getWebhook().sendMessage(unbanColor, targetInfo.name(), targetInfo.uniqueId().toString(), unbanDisplay, reason, moderatorName);
        }
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("hiddenite.ban");
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
        return TabCompleteHelper.empty();
    }

}
