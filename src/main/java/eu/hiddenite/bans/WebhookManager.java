package eu.hiddenite.bans;

import net.md_5.bungee.config.Configuration;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class WebhookManager {
    private final Logger logger;
    private final Configuration config;
    private URL url;

    public WebhookManager(Configuration config, Logger logger) {
        this.config = config;
        this.logger = logger;
        try {
            this.url = new URL(config.getString("discord.webhook-url"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void send(String json) {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.addRequestProperty("Content-Type", "application/json");
            connection.addRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux i686) Gecko/20071127 Firefox/2.0.0.11");
            connection.setDoOutput(true);

            OutputStream stream = connection.getOutputStream();
            stream.write(json.getBytes(StandardCharsets.UTF_8));
            stream.flush();
            stream.close();

            connection.getInputStream().close();
            connection.disconnect();
        } catch (Exception e) {
            logger.warning("Could not send data to webhook.");
            e.printStackTrace();
        }
    }

    public void sendMessage(int embedColor, String playerName, String playerUUID, String punishmentType, String reason, String moderator) {
        send("""
                {
                    "embeds": [
                            {
                                "color": {EMBED_COLOR},
                                "fields": [
                                    {
                                        "name": "{STRING_PLAYER_NAME}",
                                        "value": "{PLAYER_NAME}"
                                    },
                                    {
                                        "name": "{STRING_PLAYER_UUID}",
                                        "value": "{PLAYER_UUID}"
                                    },
                                    {
                                        "name": "{STRING_PUNISHMENT_TYPE}",
                                        "value": "{PUNISHMENT_TYPE}"
                                    },
                                    {
                                        "name": "{STRING_REASON}",
                                        "value": "{REASON}"
                                    },
                                    {
                                        "name": "{STRING_MODERATOR}",
                                        "value": "{MODERATOR}"
                                    }
                                ]
                            }
                        ]
                    }
                    """
                .replace("{EMBED_COLOR}", Integer.toString(embedColor))
                .replace("{STRING_PLAYER_NAME}", config.getString("discord.strings.player-name"))
                .replace("{PLAYER_NAME}", playerName)
                .replace("{STRING_PLAYER_UUID}", config.getString("discord.strings.player-uuid"))
                .replace("{PLAYER_UUID}", playerUUID)
                .replace("{STRING_PUNISHMENT_TYPE}", config.getString("discord.strings.punishment-type"))
                .replace("{PUNISHMENT_TYPE}", punishmentType)
                .replace("{STRING_REASON}", config.getString("discord.strings.reason"))
                .replace("{REASON}", reason)
                .replace("{STRING_MODERATOR}", config.getString("discord.strings.moderator"))
                .replace("{MODERATOR}", moderator));
    }

}