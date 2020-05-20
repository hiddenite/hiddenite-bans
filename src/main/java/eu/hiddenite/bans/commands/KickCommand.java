package eu.hiddenite.bans.commands;

import eu.hiddenite.bans.BansPlugin;
import eu.hiddenite.bans.helpers.TabCompleteHelper;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Arrays;

public class KickCommand extends Command implements TabExecutor {
    private final BansPlugin plugin;

    public KickCommand(BansPlugin plugin) {
        super("kick", "hiddenite.kick");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            String usage = plugin.getConfig().getString("command-messages.kick-usage");
            sender.sendMessage(TextComponent.fromLegacyText(usage));
            return;
        }

        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(args[0]);
        if (target == null) {
            String error = plugin.getConfig().getString("command-messages.error-player-not-online");
            error = error.replace("{PLAYER}", args[0]);
            sender.sendMessage(TextComponent.fromLegacyText(error));
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        target.disconnect(new TextComponent(reason));

        String successMessage = plugin.getConfig().getString("command-messages.kick-success");
        successMessage = successMessage.replace("{PLAYER}", target.getName());
        sender.sendMessage(TextComponent.fromLegacyText(successMessage));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return TabCompleteHelper.matchPlayer(args[0]);
        }
        return TabCompleteHelper.empty();
    }
}
