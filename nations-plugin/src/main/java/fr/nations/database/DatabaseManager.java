package fr.nations.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.nations.NationsPlugin;

import java.sql.*;
import java.util.logging.Level;

public class DatabaseManager {

    private final NationsPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        String url = System.getenv("DATABASE_URL");
        if (url == null || url.isEmpty()) {
            url = plugin.getConfig().getString("database.url", "");
        }
        if (url.isEmpty()) {
            plugin.getLogger().severe("[DB] Aucune DATABASE_URL trouvée. Vérifiez votre variable d'environnement.");
            return false;
        }

        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url.startsWith("jdbc:") ? url : "jdbc:postgresql://" + url.replaceFirst("postgres://", ""));
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setPoolName("NationsEpoque-Pool");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            plugin.getLogger().info("[DB] Connexion PostgreSQL établie avec succès.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[DB] Erreur de connexion: " + e.getMessage(), e);
            return false;
        }
    }

    public void createTables() {
        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS nations (
                    id UUID PRIMARY KEY,
                    name VARCHAR(32) NOT NULL UNIQUE,
                    description TEXT,
                    leader_id UUID NOT NULL,
                    bank_balance DOUBLE PRECISION DEFAULT 0,
                    season_points INT DEFAULT 0,
                    level INT DEFAULT 1,
                    xp DOUBLE PRECISION DEFAULT 0,
                    created_at BIGINT NOT NULL
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS nation_members (
                    player_id UUID NOT NULL,
                    nation_id UUID NOT NULL,
                    role_name VARCHAR(32) DEFAULT 'MEMBER',
                    joined_at BIGINT NOT NULL,
                    PRIMARY KEY (player_id),
                    FOREIGN KEY (nation_id) REFERENCES nations(id) ON DELETE CASCADE
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS nation_allies (
                    nation_a UUID NOT NULL,
                    nation_b UUID NOT NULL,
                    PRIMARY KEY (nation_a, nation_b)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS coalitions (
                    id UUID PRIMARY KEY,
                    name VARCHAR(32) NOT NULL UNIQUE,
                    leader_nation_id UUID NOT NULL,
                    created_at BIGINT NOT NULL
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS coalition_members (
                    coalition_id UUID NOT NULL,
                    nation_id UUID NOT NULL,
                    PRIMARY KEY (coalition_id, nation_id)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS claimed_chunks (
                    id UUID PRIMARY KEY,
                    nation_id UUID NOT NULL,
                    world_name VARCHAR(64) NOT NULL,
                    chunk_x INT NOT NULL,
                    chunk_z INT NOT NULL,
                    claimed_at BIGINT NOT NULL,
                    FOREIGN KEY (nation_id) REFERENCES nations(id) ON DELETE CASCADE,
                    UNIQUE (world_name, chunk_x, chunk_z)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_accounts (
                    player_id UUID PRIMARY KEY,
                    balance DOUBLE PRECISION DEFAULT 0
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_grades (
                    player_id UUID PRIMARY KEY,
                    grade VARCHAR(16) DEFAULT 'JOUEUR',
                    level INT DEFAULT 1,
                    xp DOUBLE PRECISION DEFAULT 0
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS wars (
                    id UUID PRIMARY KEY,
                    attacker_nation_id UUID NOT NULL,
                    defender_nation_id UUID NOT NULL,
                    type VARCHAR(32) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    declared_at BIGINT NOT NULL,
                    ends_at BIGINT NOT NULL,
                    reason TEXT,
                    attacker_kills INT DEFAULT 0,
                    defender_kills INT DEFAULT 0,
                    staff_note TEXT,
                    validated_by UUID,
                    surrender_requested BOOLEAN DEFAULT FALSE,
                    surrender_requested_at BIGINT DEFAULT 0
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS season_stats (
                    player_id UUID NOT NULL,
                    season_number INT NOT NULL,
                    kills INT DEFAULT 0,
                    deaths INT DEFAULT 0,
                    wars_won INT DEFAULT 0,
                    claims INT DEFAULT 0,
                    PRIMARY KEY (player_id, season_number)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS seasons (
                    season_number INT PRIMARY KEY,
                    started_at BIGINT NOT NULL,
                    ended_at BIGINT,
                    current BOOLEAN DEFAULT FALSE
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS custom_roles (
                    id UUID PRIMARY KEY,
                    nation_id UUID NOT NULL,
                    name VARCHAR(32) NOT NULL,
                    display_name VARCHAR(64) NOT NULL,
                    rank INT NOT NULL DEFAULT 50,
                    perm_build BOOLEAN DEFAULT FALSE,
                    perm_invite BOOLEAN DEFAULT FALSE,
                    perm_kick BOOLEAN DEFAULT FALSE,
                    perm_manage_war BOOLEAN DEFAULT FALSE,
                    perm_manage_bank BOOLEAN DEFAULT FALSE,
                    perm_manage_claims BOOLEAN DEFAULT FALSE,
                    perm_manage_allies BOOLEAN DEFAULT FALSE,
                    perm_manage_roles BOOLEAN DEFAULT FALSE,
                    perm_rename BOOLEAN DEFAULT FALSE,
                    perm_disband BOOLEAN DEFAULT FALSE,
                    FOREIGN KEY (nation_id) REFERENCES nations(id) ON DELETE CASCADE,
                    UNIQUE (nation_id, name)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_custom_roles (
                    player_id UUID NOT NULL,
                    nation_id UUID NOT NULL,
                    role_id UUID NOT NULL,
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
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("La datasource n'est pas initialisée.");
        }
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("[DB] Pool de connexions fermé.");
        }
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }
}
