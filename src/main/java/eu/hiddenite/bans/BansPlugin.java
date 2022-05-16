package eu.hiddenite.bans;

import com.google.gson.Gson;
import eu.hiddenite.bans.commands.BanCommand;
import eu.hiddenite.bans.commands.KickCommand;
import eu.hiddenite.bans.commands.UnbanCommand;
import eu.hiddenite.bans.helpers.HttpHelper;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.UUID;

public class BansPlugin extends Plugin implements Listener {
    private Configuration config;
    private DatabaseManager database;
    private WebhookManager webhook;

    public static class ScoreApiResult {
        public boolean success;
        public int score;
    }

    @Override
    public void onEnable() {
        if (!loadConfiguration()) {
            return;
        }

        if (config.getBoolean("discord.enabled")) {
            webhook = new WebhookManager(this);
        }

        database = new DatabaseManager(config, getLogger());
        if (!database.open()) {
            getLogger().warning("Could not connect to the database. Plugin disabled.");
            return;
        }

        getProxy().getPluginManager().registerListener(this, this);

        getProxy().getPluginManager().registerCommand(this, new KickCommand(this));
        getProxy().getPluginManager().registerCommand(this, new BanCommand(this));
        getProxy().getPluginManager().registerCommand(this, new UnbanCommand(this));
    }

    @Override
    public void onDisable() {
        database.close();
    }

    public Configuration getConfig() {
        return config;
    }

    public WebhookManager getWebhook() {
        return webhook;
    }

    private boolean loadConfiguration() {
        if (!getDataFolder().exists()) {
            if (!getDataFolder().mkdir()) {
                getLogger().warning("Could not create the configuration folder.");
                return false;
            }
        }

        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            getLogger().warning("No configuration file found, creating a default one.");

            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        try {
            config = ConfigurationProvider
                    .getProvider(YamlConfiguration.class)
                    .load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @EventHandler
    public void onPlayerLogin(LoginEvent event) {
        boolean isBanned = false;
        Timestamp expirationDate = null;
        String banReason = null;
        String moderatorName = null;

        UUID playerId = event.getConnection().getUniqueId();

        try {
            String bansTable = config.getString("mysql.tables.bans");
            String playersTable = "`" + config.getString("mysql.tables.players") + "`";
            String playerIdField = playersTable + "." + config.getString("mysql.fields.players.id");
            String playerNameField =  playersTable + "." + config.getString("mysql.fields.players.name");

            try (PreparedStatement ps = database.prepareStatement("SELECT expiration_date, reason" +
                    ", " + playerNameField +
                    " FROM " + bansTable +
                    " LEFT JOIN " + playersTable +
                    " ON mod_id = " + playerIdField +
                    " WHERE player_id = ? AND (expiration_date IS NULL OR expiration_date > NOW())" +
                    " LIMIT 1")) {
                ps.setString(1, playerId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        isBanned = true;
                        expirationDate = rs.getTimestamp(1);
                        banReason = rs.getString(2);
                        moderatorName = rs.getString(3);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (isBanned) {
            getLogger().info("Login from " + event.getConnection().getName() + " rejected: " + banReason);
            event.setCancelled(true);
            event.setCancelReason(generateBanMessage(banReason, moderatorName, expirationDate));
            return;
        }
    }

    public BaseComponent[] generateBanMessage(String reason, String moderatorName, Timestamp expirationDate) {
        String fullMessage = config.getString("ban-message.header") + "\n\n";
        if (expirationDate != null) {
            fullMessage += config.getString("ban-message.temporary") + "\n\n";
        } else {
            fullMessage += config.getString("ban-message.permanent") + "\n\n";
        }
        fullMessage += config.getString("ban-message.footer");

        fullMessage = fullMessage.replace("{REASON}", reason);
        if (moderatorName != null) {
            fullMessage = fullMessage.replace("{MODERATOR}", moderatorName);
        } else {
            fullMessage = fullMessage.replace("{MODERATOR}", config.getString("ban-message.console-username"));
        }
        if (expirationDate != null) {
            Format untilDateFormat = new SimpleDateFormat(config.getString("ban-message.until-format"));
            fullMessage = fullMessage.replace("{UNTIL}", untilDateFormat.format(expirationDate));
            fullMessage = fullMessage.replace("{REMAINING}", generateRemainingTime(expirationDate));
        }

        return TextComponent.fromLegacyText(fullMessage);
    }

    private String generateRemainingTime(Timestamp untilDate) {
        long now = System.currentTimeMillis();
        long until = untilDate.getTime();
        long delta = (until - now) / 1000;

        long sec = delta % 60;
        long min = (delta / 60) % 60;
        long hour = (delta / 60 / 60) % 24;
        long day = delta / 60 / 60 / 24;

        StringBuilder sb = new StringBuilder();
        boolean firstElement = true;
        if (day > 0) {
            sb.append(day).append(config.getString("ban-message.remaining.day")).append(day > 1 ? "s" : "");
            firstElement = false;
        }
        if (hour > 0) {
            if (!firstElement) {
                sb.append(config.getString("ban-message.remaining.separator"));
            }
            sb.append(hour).append(config.getString("ban-message.remaining.hour")).append(hour > 1 ? "s" : "");
            firstElement = false;
        }
        if (min > 0) {
            if (!firstElement) {
                sb.append(config.getString("ban-message.remaining.separator"));
            }
            sb.append(min).append(config.getString("ban-message.remaining.minute")).append(min > 1 ? "s" : "");
            firstElement = false;
        }
        if (sec > 0 || (day == 0 && hour == 0 && min == 0)) {
            if (!firstElement) {
                sb.append(config.getString("ban-message.remaining.last-separator"));
            }
            sb.append(sec).append(config.getString("ban-message.remaining.second")).append(sec > 1 ? "s" : "");
        }

        return sb.toString();
    }

    public static class OfflinePlayerInfo {
        public UUID uniqueId;
        public String name;

        private OfflinePlayerInfo(UUID uniqueId, String name) {
            this.uniqueId = uniqueId;
            this.name = name;
        }
    }

    public OfflinePlayerInfo getOfflinePlayer(String username) {
        String playersTable = "`" + config.getString("mysql.tables.players") + "`";
        String playerIdField = config.getString("mysql.fields.players.id");
        String playerNameField = config.getString("mysql.fields.players.name");

        try (PreparedStatement ps = database.prepareStatement("SELECT " + playerIdField + ", " + playerNameField +
                " FROM " + playersTable +
                " WHERE " + playerNameField + " = ?" +
                " LIMIT 1")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UUID uniqueId = UUID.fromString(rs.getString(1));
                    String name = rs.getString(2);
                    return new OfflinePlayerInfo(uniqueId, name);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isPlayerBanned(UUID playerId) throws SQLException {
        final String bansTable = config.getString("mysql.tables.bans");
        try (PreparedStatement ps = database.prepareStatement("SELECT expiration_date, reason" +
                " FROM " + bansTable +
                " WHERE player_id = ? AND (expiration_date IS NULL OR expiration_date > NOW())" +
                " LIMIT 1")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void banPlayer(UUID playerId, UUID moderatorId, Timestamp expirationDate, String reason) throws SQLException {
        final String bansTable = config.getString("mysql.tables.bans");
        try (PreparedStatement ps = database.prepareStatement("INSERT INTO " + bansTable +
                " (player_id, mod_id, expiration_date, reason)" +
                " VALUES (?, ?, ?, ?)")) {
            ps.setString(1, playerId.toString());
            ps.setString(2, moderatorId != null ? moderatorId.toString() : null);
            ps.setTimestamp(3, expirationDate);
            ps.setString(4, reason);
            ps.executeUpdate();
        }
    }

    public void unbanPlayer(UUID playerId) throws SQLException {
        final String bansTable = config.getString("mysql.tables.bans");
        try (PreparedStatement ps = database.prepareStatement("UPDATE " + bansTable +
                " SET expiration_date = NOW()" +
                " WHERE player_id = ? AND (expiration_date IS NULL OR expiration_date > NOW())")) {
            ps.setString(1, playerId.toString());
            ps.executeUpdate();
        }
    }
}
