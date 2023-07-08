package eu.hiddenite.bans;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class Configuration {
    public MySQL mysql;
    public BanMessage banMessage;
    public CommandMessages commandMessages;
    public DefaultReasons defaultReasons;
    public Discord discord;

    @ConfigSerializable
    public static class MySQL {
        public String host;
        public String user;
        public String password;
        public String database;
        public Tables tables;
        public Fields fields;

        @ConfigSerializable
        public static class Tables {
            public String players;
            public String bans;
        }

        @ConfigSerializable
        public static class Fields {
            public Field players;

            @ConfigSerializable
            public static class Field {
                public String id;
                public String name;
            }
        }
    }

    @ConfigSerializable
    public static class BanMessage {
        public String header;
        public String permanent;
        public String temporary;
        public String footer;
        public String untilFormat;
        public Remaining remaining;
        public String consoleUsername;

        @ConfigSerializable
        public static class Remaining {
            public String day;
            public String hour;
            public String minute;
            public String second;
            public String separator;
            public String lastSeparator;
        }
    }

    @ConfigSerializable
    public static class CommandMessages {
        public String dateFormat;
        public String kickUsage;
        public String banUsage;
        public String unbanUsage;
        public String kickSuccess;
        public String banSuccess;
        public String unbanSuccess;
        public String errorPlayerNotFound;
        public String errorPlayerNotOnline;
        public String errorAlreadyBanned;
        public String errorNotBanned;
        public String errorInvalidDuration;
        public String errorMissingReason;
        public String errorDatabase;
    }

    @ConfigSerializable
    public static class DefaultReasons {
        public String kick;
        public String ban;
        public String unban;
    }

    @ConfigSerializable
    public static class Discord {
        public boolean enabled;
        public String webhookUrl;
        public Punishments punishments;
        public Strings strings;

        @ConfigSerializable
        public static class Punishments {
            public Punishment ban;
            public Punishment unban;
            public Punishment kick;

            @ConfigSerializable
            public static class Punishment {
                public int color;
                public String display;
            }
        }

        @ConfigSerializable
        public static class Strings {
            public String playerName;
            public String playerUuid;
            public String punishmentType;
            public String reason;
            public String moderator;
        }
    }
}
