package eu.hiddenite.bans;

import com.google.inject.Inject;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.hiddenite.bans.commands.BanCommand;
import eu.hiddenite.bans.commands.KickCommand;
import eu.hiddenite.bans.commands.UnbanCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.slf4j.Logger;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.UUID;

@Plugin(id="hiddenite-bans", name="HiddeniteBans", version="1.1.1", authors={"Hiddenite"})
public class BansPlugin {
    private static BansPlugin instance;

    private final ProxyServer proxy;
    private final Logger logger;
    private final File dataDirectory;

    private Configuration config;
    private DatabaseManager database;
    private WebhookManager webhook;

    @Inject
    public BansPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory.toFile();

        instance = this;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        if (!loadConfiguration()) {
            return;
        }

        if (config.discord.enabled) {
            webhook = new WebhookManager(this);
        }

        database = new DatabaseManager(config, logger);
        if (!database.open()) {
            logger.warn("Could not connect to the database. Plugin disabled.");
            return;
        }

        registerCommands();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        database.close();
    }

    private boolean loadConfiguration() {
        if (!dataDirectory.exists()) {
            if (!dataDirectory.mkdir()) {
                logger.warn("Could not create the configuration folder.");
                return false;
            }
        }

        File file = new File(dataDirectory, "config.yml");
        if (!file.exists()) {
            logger.warn("No configuration file found, creating a default one.");

            try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException exception) {
                exception.printStackTrace();
                return false;
            }
        }

        YamlConfigurationLoader reader = YamlConfigurationLoader.builder().path(dataDirectory.toPath().resolve("config.yml")).build();

        try {
            config = reader.load().get(Configuration.class);
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }

        return true;
    }

    private void registerCommands() {
        registerCommand("ban", new BanCommand(this));
        registerCommand("unban", new UnbanCommand(this));
        registerCommand("kick", new KickCommand(this));
    }

    private void registerCommand(String name, Command command) {
        CommandManager manager = proxy.getCommandManager();

        CommandMeta meta = manager.metaBuilder(name).plugin(this).build();
        manager.register(meta, command);
    }

    public static BansPlugin getInstance() {
        return instance;
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public Logger getLogger() {
        return logger;
    }

    public Configuration getConfig() {
        return config;
    }

    public WebhookManager getWebhook() {
        return webhook;
    }

    @Subscribe
    public void onPlayerLogin(LoginEvent event) {
        boolean isBanned = false;
        Timestamp expirationDate = null;
        String banReason = null;
        String moderatorName = null;

        UUID playerId = event.getPlayer().getUniqueId();

        try {
            String bansTable = config.mysql.tables.bans;
            String playersTable = "`" + config.mysql.tables.players + "`";
            String playerIdField = playersTable + "." + config.mysql.fields.players.id;
            String playerNameField =  playersTable + "." + config.mysql.fields.players.name;

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
            logger.info("Login from " + event.getPlayer().getUsername() + " rejected: " + banReason);
            event.setResult(ResultedEvent.ComponentResult.denied(generateBanMessage(banReason, moderatorName, expirationDate)));
        }
    }

    public TextComponent generateBanMessage(String reason, String moderatorName, Timestamp expirationDate) {
        String fullMessage = config.banMessage.header + "\n\n";
        if (expirationDate != null) {
            fullMessage += config.banMessage.temporary + "\n\n";
        } else {
            fullMessage += config.banMessage.permanent + "\n\n";
        }
        fullMessage += config.banMessage.footer;

        fullMessage = fullMessage.replace("{REASON}", reason);
        if (moderatorName != null) {
            fullMessage = fullMessage.replace("{MODERATOR}", moderatorName);
        } else {
            fullMessage = fullMessage.replace("{MODERATOR}", config.banMessage.consoleUsername);
        }
        if (expirationDate != null) {
            Format untilDateFormat = new SimpleDateFormat(config.banMessage.untilFormat);
            fullMessage = fullMessage.replace("{UNTIL}", untilDateFormat.format(expirationDate));
            fullMessage = fullMessage.replace("{REMAINING}", generateRemainingTime(expirationDate));
        }

        return Component.text(fullMessage);
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
            sb.append(day).append(config.banMessage.remaining.day).append(day > 1 ? "s" : "");
            firstElement = false;
        }
        if (hour > 0) {
            if (!firstElement) {
                sb.append(config.banMessage.remaining.separator);
            }
            sb.append(hour).append(config.banMessage.remaining.hour).append(hour > 1 ? "s" : "");
            firstElement = false;
        }
        if (min > 0) {
            if (!firstElement) {
                sb.append(config.banMessage.remaining.separator);
            }
            sb.append(min).append(config.banMessage.remaining.minute).append(min > 1 ? "s" : "");
            firstElement = false;
        }
        if (sec > 0 || (day == 0 && hour == 0 && min == 0)) {
            if (!firstElement) {
                sb.append(config.banMessage.remaining.separator);
            }
            sb.append(sec).append(config.banMessage.remaining.second).append(sec > 1 ? "s" : "");
        }

        return sb.toString();
    }

    public record OfflinePlayerInfo(UUID uniqueId, String name) {}

    public OfflinePlayerInfo getOfflinePlayer(String username) {
        String playersTable = "`" + config.mysql.tables.players + "`";
        String playerIdField = config.mysql.fields.players.id;
        String playerNameField = config.mysql.fields.players.name;

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
        final String bansTable = config.mysql.tables.bans;
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
        final String bansTable = config.mysql.tables.bans;
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
        final String bansTable = config.mysql.tables.bans;
        try (PreparedStatement ps = database.prepareStatement("UPDATE " + bansTable +
                " SET expiration_date = NOW()" +
                " WHERE player_id = ? AND (expiration_date IS NULL OR expiration_date > NOW())")) {
            ps.setString(1, playerId.toString());
            ps.executeUpdate();
        }
    }
}
