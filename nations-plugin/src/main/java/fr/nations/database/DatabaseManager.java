package fr.nations.database;

import fr.nations.NationsPlugin;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;

public class DatabaseManager {

    private final NationsPlugin plugin;
    private Connection connection;

    public DatabaseManager(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        String fileName = plugin.getConfig().getString("database.file", "data.db");
        File dbFile = new File(plugin.getDataFolder(), fileName);
        dbFile.getParentFile().mkdirs();

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
                stmt.execute("PRAGMA foreign_keys=ON");
                stmt.execute("PRAGMA cache_size=-8000");
            }

            plugin.getLogger().info("[DB] Connexion SQLite établie: " + dbFile.getName());
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[DB] Erreur de connexion SQLite: " + e.getMessage(), e);
            return false;
        }
    }

    public void createTables() {
        try (Statement stmt = connection.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS nations (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL UNIQUE,
                    description TEXT,
                    leader_id TEXT NOT NULL,
                    bank_balance REAL DEFAULT 0,
                    season_points INTEGER DEFAULT 0,
                    level INTEGER DEFAULT 1,
                    xp REAL DEFAULT 0,
                    created_at INTEGER NOT NULL
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS nation_members (
                    player_id TEXT NOT NULL,
                    nation_id TEXT NOT NULL,
                    role_name TEXT DEFAULT 'MEMBER',
                    joined_at INTEGER NOT NULL,
                    PRIMARY KEY (player_id),
                    FOREIGN KEY (nation_id) REFERENCES nations(id) ON DELETE CASCADE
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS nation_allies (
                    nation_a TEXT NOT NULL,
                    nation_b TEXT NOT NULL,
                    PRIMARY KEY (nation_a, nation_b)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS coalitions (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL UNIQUE,
                    leader_nation_id TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS coalition_members (
                    coalition_id TEXT NOT NULL,
                    nation_id TEXT NOT NULL,
                    PRIMARY KEY (coalition_id, nation_id)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS claimed_chunks (
                    id TEXT PRIMARY KEY,
                    nation_id TEXT NOT NULL,
                    world_name TEXT NOT NULL,
                    chunk_x INTEGER NOT NULL,
                    chunk_z INTEGER NOT NULL,
                    claimed_at INTEGER NOT NULL,
                    FOREIGN KEY (nation_id) REFERENCES nations(id) ON DELETE CASCADE,
                    UNIQUE (world_name, chunk_x, chunk_z)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_accounts (
                    player_id TEXT PRIMARY KEY,
                    balance REAL DEFAULT 0
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_grades (
                    player_id TEXT PRIMARY KEY,
                    grade TEXT DEFAULT 'JOUEUR',
                    level INTEGER DEFAULT 1,
                    xp REAL DEFAULT 0
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS wars (
                    id TEXT PRIMARY KEY,
                    attacker_nation_id TEXT NOT NULL,
                    defender_nation_id TEXT NOT NULL,
                    type TEXT NOT NULL,
                    status TEXT NOT NULL,
                    declared_at INTEGER NOT NULL,
                    ends_at INTEGER NOT NULL,
                    reason TEXT,
                    attacker_kills INTEGER DEFAULT 0,
                    defender_kills INTEGER DEFAULT 0,
                    staff_note TEXT,
                    validated_by TEXT,
                    surrender_requested INTEGER DEFAULT 0,
                    surrender_requested_at INTEGER DEFAULT 0
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS season_stats (
                    player_id TEXT NOT NULL,
                    season_number INTEGER NOT NULL,
                    kills INTEGER DEFAULT 0,
                    deaths INTEGER DEFAULT 0,
                    wars_won INTEGER DEFAULT 0,
                    claims INTEGER DEFAULT 0,
                    PRIMARY KEY (player_id, season_number)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS seasons (
                    season_number INTEGER PRIMARY KEY,
                    started_at INTEGER NOT NULL,
                    ended_at INTEGER,
                    current INTEGER DEFAULT 0
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS custom_roles (
                    id TEXT PRIMARY KEY,
                    nation_id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    display_name TEXT NOT NULL,
                    rank INTEGER NOT NULL DEFAULT 50,
                    perm_build INTEGER DEFAULT 0,
                    perm_invite INTEGER DEFAULT 0,
                    perm_kick INTEGER DEFAULT 0,
                    perm_manage_war INTEGER DEFAULT 0,
                    perm_manage_bank INTEGER DEFAULT 0,
                    perm_manage_claims INTEGER DEFAULT 0,
                    perm_manage_allies INTEGER DEFAULT 0,
                    perm_manage_roles INTEGER DEFAULT 0,
                    perm_rename INTEGER DEFAULT 0,
                    perm_disband INTEGER DEFAULT 0,
                    FOREIGN KEY (nation_id) REFERENCES nations(id) ON DELETE CASCADE,
                    UNIQUE (nation_id, name)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_custom_roles (
                    player_id TEXT NOT NULL,
                    nation_id TEXT NOT NULL,
                    role_id TEXT NOT NULL,
                    PRIMARY KEY (player_id, nation_id),
                    FOREIGN KEY (role_id) REFERENCES custom_roles(id) ON DELETE CASCADE
                )
            """);

            plugin.getLogger().info("[DB] Tables créées / vérifiées avec succès.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[DB] Erreur lors de la création des tables: " + e.getMessage(), e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("La connexion SQLite n'est pas initialisée.");
        }
        return connection;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("[DB] Connexion SQLite fermée.");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "[DB] Erreur fermeture connexion", e);
            }
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
