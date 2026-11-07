package com.bearnation.bearCosmetics.database;

import java.sql.*;
import java.util.UUID;

public class DatabaseManager {
    private static DatabaseManager instance;

    private final String url;
    private final String user;
    private final String password;

    private DatabaseManager() {
        org.bukkit.configuration.file.FileConfiguration cfg =
                com.bearnation.bearCosmetics.BearCosmetics.getInstance().getConfig();
        String host = cfg.getString("database.host", "localhost");
        int port = cfg.getInt("database.port", 3306);
        String database = cfg.getString("database.name", "bearnation");
        this.user = cfg.getString("database.user", "");
        this.password = cfg.getString("database.password", "");

        this.url = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=true&requireSSL=true&verifyServerCertificate=false&autoReconnect=true&maxReconnects=3",
                host, port, database
        );
    }

    public static void initialize() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
    }

    public static Connection getConnection() throws SQLException {
        if (instance == null) {
            initialize();
        }
        return DriverManager.getConnection(instance.url, instance.user, instance.password);
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            initialize();
        }
        return instance;
    }
}
