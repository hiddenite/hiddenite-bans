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
            this.url = new URL(plugin.getConfig().getString("discord.webhook-url"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void send(String json) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
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
            } catch (Exception e) {
                plugin.getLogger().warning("Could not send data to webhook.");
                e.printStackTrace();
            }
        });
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
        private final List<WebhookField> fields = new ArrayList<>();

        public WebhookEmbed(int embedColor, String playerName, String playerUUID, String punishmentType, String reason, String moderator) {
            this.color = embedColor;
            fields.add(new WebhookField(plugin.getConfig().getString("discord.strings.player-name"), playerName));
            fields.add(new WebhookField(plugin.getConfig().getString("discord.strings.player-uuid"), playerUUID));
            fields.add(new WebhookField(plugin.getConfig().getString("discord.strings.punishment-type"), punishmentType));
            fields.add(new WebhookField(plugin.getConfig().getString("discord.strings.reason"), reason));
            fields.add(new WebhookField(plugin.getConfig().getString("discord.strings.moderator"), moderator));
        }
    }

    public static class WebhookField {
        private final String name;
        private final String value;

        public WebhookField(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

}