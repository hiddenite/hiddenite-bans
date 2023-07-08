package eu.hiddenite.bans.helpers;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.bans.BansPlugin;

import java.util.ArrayList;
import java.util.List;

public class TabCompleteHelper {
    public static List<String> matchPlayer(String argument) {
        List<String> matches = new ArrayList<>();
        String search = argument.toUpperCase();
        for (Player player : BansPlugin.getInstance().getProxy().getAllPlayers()) {
            if (player.getUsername().toUpperCase().startsWith(search)) {
                matches.add(player.getUsername());
            }
        }
        return matches;
    }

    public static List<String> empty() {
        return ImmutableList.of();
    }
}
