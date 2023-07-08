package eu.hiddenite.bans.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.bans.BansPlugin;
import eu.hiddenite.bans.helpers.TabCompleteHelper;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.List;

public class KickCommand implements SimpleCommand {
    private final BansPlugin plugin;

    public KickCommand(BansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(final Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 1) {
            String usage = plugin.getConfig().commandMessages.kickUsage;
            source.sendMessage(Component.text(usage));
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (reason.isEmpty()) {
            String defaultReason = plugin.getConfig().defaultReasons.kick;
            if (defaultReason == null || defaultReason.isEmpty()) {
                String error = plugin.getConfig().commandMessages.errorMissingReason;
                source.sendMessage(Component.text(error));
                return;
            }
            reason = defaultReason;
        }

        if (!plugin.getProxy().getPlayer(args[0]).isPresent()) {
            String error = plugin.getConfig().commandMessages.errorPlayerNotOnline;
            error = error.replace("{PLAYER}", args[0]);
            source.sendMessage(Component.text(error));
            return;
        }

        Player target = plugin.getProxy().getPlayer(args[0]).get();

        target.disconnect(Component.text(reason));

        String successMessage = plugin.getConfig().commandMessages.kickSuccess;
        successMessage = successMessage.replace("{PLAYER}", target.getUsername());
        source.sendMessage(Component.text(successMessage));

        if (plugin.getWebhook() != null) {
            int kickColor = plugin.getConfig().discord.punishments.kick.color;
            String kickDisplay = plugin.getConfig().discord.punishments.kick.display;
            String moderatorName = source instanceof Player player ? player.getUsername() : plugin.getConfig().banMessage.consoleUsername;
            plugin.getWebhook().sendMessage(kickColor, target.getUsername(), target.getUniqueId().toString(), kickDisplay, reason, moderatorName);
        }
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("hiddenite.kick");
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1) {
            return TabCompleteHelper.matchPlayer(args[0]);
        }

        return TabCompleteHelper.empty();
    }

}
