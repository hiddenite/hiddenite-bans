package eu.hiddenite.bans;

import com.google.gson.Gson;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WebhookManager {
    private final BansPlugin plugin;
    private URL url;

    public WebhookManager(BansPlugin plugin) {
        this.plugin = plugin;
        try {
            this.url = new URL(plugin.getConfig().discord.webhookUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void send(String json) {
        plugin.getProxy().getScheduler().buildTask(plugin, (selfTask) -> {
            try {
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.addRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                OutputStream stream = connection.getOutputStream();
                stream.write(json.getBytes(StandardCharsets.UTF_8));
                stream.flush();
                stream.close();

                connection.getInputStream().close();
                connection.disconnect();
            } catch (Exception exception) {
                plugin.getLogger().warn("Could not send data to webhook.");
                exception.printStackTrace();
            }
        }).schedule();
    }

    public void sendMessage(int embedColor, String playerName, String playerUUID, String punishmentType, String reason, String moderator) {
        send(new WebhookMessage(embedColor, playerName, playerUUID, punishmentType, reason, moderator).toJson());
    }

    public class WebhookMessage {
        private final List<WebhookEmbed> embeds = new ArrayList<>();

        public WebhookMessage(int embedColor, String playerName, String playerUUID, String punishmentType, String reason, String moderator) {
            embeds.add(new WebhookEmbed(embedColor, playerName, playerUUID, punishmentType, reason, moderator));
        }

        public String toJson() {
            return new Gson().toJson(this);
        }
    }

    public class WebhookEmbed {
        private final int color;
        private String description = "";

        public WebhookEmbed(int embedColor, String playerName, String playerUUID, String punishmentType, String reason, String moderator) {
            this.color = embedColor;
            description += createLine(plugin.getConfig().discord.strings.playerName, raw(playerName));
            description += createLine(plugin.getConfig().discord.strings.playerUuid, raw(playerUUID));
            description += createLine(plugin.getConfig().discord.strings.punishmentType, punishmentType);
            description += createLine(plugin.getConfig().discord.strings.reason, reason);
            description += createLine(plugin.getConfig().discord.strings.moderator, raw(moderator));
        }
    }

    private static String createLine(String name, String value) {
        return bold(name) + " " + value + "\n";
    }

    private static String bold(String string) {
        return "**" + string + "**";
    }

    private static String raw(String string) {
        return "`" + string + "`";
    }

}