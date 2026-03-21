package fr.nations.economy;

import fr.nations.NationsPlugin;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class EconomyManager {

    private final NationsPlugin plugin;
    private final Map<UUID, PlayerAccount> accounts;

    public EconomyManager(NationsPlugin plugin) {
        this.plugin = plugin;
        this.accounts = new HashMap<>();
    }

    public void addAccount(PlayerAccount account) {
        accounts.put(account.getPlayerId(), account);
    }

    public PlayerAccount getOrCreateAccount(UUID playerId) {
        return accounts.computeIfAbsent(playerId, id -> {
            double startingBalance = plugin.getConfigManager().getStartingBalance();
            return new PlayerAccount(id, startingBalance);
        });
    }

    public PlayerAccount getAccount(UUID playerId) {
        return accounts.get(playerId);
    }

    public double getBalance(UUID playerId) {
        return getOrCreateAccount(playerId).getBalance();
    }

    public boolean has(UUID playerId, double amount) {
        return getOrCreateAccount(playerId).has(amount);
    }

    public void deposit(UUID playerId, double amount) {
        getOrCreateAccount(playerId).deposit(amount);
        plugin.getDataManager().saveEconomy();
    }

    public boolean withdraw(UUID playerId, double amount) {
        boolean result = getOrCreateAccount(playerId).withdraw(amount);
        if (result) plugin.getDataManager().saveEconomy();
        return result;
    }

    public boolean transfer(UUID fromId, UUID toId, double amount) {
        if (!has(fromId, amount)) return false;
        withdraw(fromId, amount);
        deposit(toId, amount);
        return true;
    }

    public boolean setBalance(UUID playerId, double amount) {
        if (amount < 0) return false;
        getOrCreateAccount(playerId).setBalance(amount);
        plugin.getDataManager().saveEconomy();
        return true;
    }

    public Collection<PlayerAccount> getAllAccounts() {
        return Collections.unmodifiableCollection(accounts.values());
    }

    public List<PlayerAccount> getTopAccounts(int limit) {
        return accounts.values().stream()
            .sorted(Comparator.comparingDouble(PlayerAccount::getBalance).reversed())
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }

    public void formatBalance(double amount, StringBuilder sb) {
        if (amount >= 1_000_000) {
            sb.append(String.format("%.1fM", amount / 1_000_000));
        } else if (amount >= 1_000) {
            sb.append(String.format("%.1fK", amount / 1_000));
        } else {
            sb.append(String.format("%.0f", amount));
        }
        sb.append(" coins");
    }

    public String formatBalance(double amount) {
        StringBuilder sb = new StringBuilder();
        formatBalance(amount, sb);
        return sb.toString();
    }

    public void loadFromDatabase() {
        String sql = "SELECT player_id, balance FROM player_accounts";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int count = 0;
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("player_id"));
                double balance = rs.getDouble("balance");
                accounts.put(id, new PlayerAccount(id, balance));
                count++;
            }
            plugin.getLogger().info("[Economy] " + count + " comptes chargés depuis la DB.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Economy] Erreur chargement DB", e);
        }
    }

    public void saveAccountToDatabase(UUID playerId) {
        if (!plugin.getDatabaseManager().isConnected()) return;
        PlayerAccount account = accounts.get(playerId);
        if (account == null) return;
        String sql = """
            INSERT INTO player_accounts (player_id, balance) VALUES (?,?)
            ON CONFLICT (player_id) DO UPDATE SET balance=EXCLUDED.balance
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            ps.setDouble(2, account.getBalance());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Economy] Erreur sauvegarde compte", e);
        }
    }
}
