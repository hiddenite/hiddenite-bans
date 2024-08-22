package eu.hiddenite.bans;

import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.bans.helpers.HttpHelper;

public class ProxyCheckManager {
    private final BansPlugin plugin;
    public ProxyCheckManager(BansPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean checkPlayer(Player player) {
        if (!plugin.getConfig().proxyCheck.enabled) return true;

        if (player.hasPermission("hiddenite.vpn.bypass")) {
            plugin.getLogger().info("{} is whitelisted, not checking for VPN", player.getUsername());
            return false;
        }

        String ip = player.getRemoteAddress().getAddress().toString();
        String httpResult = HttpHelper.get("https://proxycheck.io/v2/" + ip + "?key=" + plugin.getConfig().proxyCheck.apiKey + "&vpn=1");
        if (httpResult == null) {
            plugin.getLogger().warn("Could not check if {} was using a VPN.", player.getUsername());
            return false;
        }
        if (httpResult.contains("\"proxy\": \"yes\"")) {
            plugin.getLogger().info("{} is probably using a VPN!!", player.getUsername());
            return true;
        } else {
            plugin.getLogger().info("{} is probably not using a VPN.", player.getUsername());
            return false;
        }
    }
}
