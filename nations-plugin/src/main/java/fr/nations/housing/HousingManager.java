package fr.nations.housing;

import fr.nations.NationsPlugin;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class HousingManager {

    private final NationsPlugin plugin;
    private final Map<UUID, Housing> housings = new HashMap<>();

    public HousingManager(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    // ─── DB Setup ───────────────────────────────────────────────────────────────

    public void createTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS housings (
                id TEXT PRIMARY KEY,
                nation_id TEXT NOT NULL,
                name TEXT NOT NULL,
                price REAL DEFAULT 0,
                owner_id TEXT,
                owner_name TEXT,
                min_x INTEGER NOT NULL,
                min_y INTEGER NOT NULL,
                min_z INTEGER NOT NULL,
                max_x INTEGER NOT NULL,
                max_y INTEGER NOT NULL,
                max_z INTEGER NOT NULL,
                world TEXT NOT NULL,
                sign_x INTEGER,
                sign_y INTEGER,
                sign_z INTEGER,
                sign_world TEXT,
                sign_placed INTEGER DEFAULT 0
            )
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Housing] Erreur création table", e);
        }
    }

    // ─── Load ────────────────────────────────────────────────────────────────────

    public void loadFromDatabase() {
        String sql = "SELECT * FROM housings";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("id"));
                UUID nationId = UUID.fromString(rs.getString("nation_id"));
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                int minX = rs.getInt("min_x"), minY = rs.getInt("min_y"), minZ = rs.getInt("min_z");
                int maxX = rs.getInt("max_x"), maxY = rs.getInt("max_y"), maxZ = rs.getInt("max_z");
                String world = rs.getString("world");

                Housing h = new Housing(id, nationId, name, price, minX, minY, minZ, maxX, maxY, maxZ, world);

                String ownerId = rs.getString("owner_id");
                if (ownerId != null) h.setOwnerId(UUID.fromString(ownerId));
                h.setOwnerName(rs.getString("owner_name"));

                boolean signPlaced = rs.getInt("sign_placed") == 1;
                if (signPlaced) {
                    h.setSign(rs.getString("sign_world"), rs.getInt("sign_x"), rs.getInt("sign_y"), rs.getInt("sign_z"));
                }

                housings.put(id, h);
            }
            plugin.getLogger().info("[Housing] " + housings.size() + " logement(s) chargé(s).");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Housing] Erreur chargement", e);
        }
    }

    // ─── CRUD ────────────────────────────────────────────────────────────────────

    public void createHousing(Housing h) {
        housings.put(h.getId(), h);
        saveToDatabase(h);
    }

    public void saveToDatabase(Housing h) {
        String sql = """
            INSERT OR REPLACE INTO housings
            (id, nation_id, name, price, owner_id, owner_name,
             min_x, min_y, min_z, max_x, max_y, max_z, world,
             sign_x, sign_y, sign_z, sign_world, sign_placed)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, h.getId().toString());
            ps.setString(2, h.getNationId().toString());
            ps.setString(3, h.getName());
            ps.setDouble(4, h.getPrice());
            ps.setString(5, h.getOwnerId() != null ? h.getOwnerId().toString() : null);
            ps.setString(6, h.getOwnerName());
            ps.setInt(7, h.getMinX());
            ps.setInt(8, h.getMinY());
            ps.setInt(9, h.getMinZ());
            ps.setInt(10, h.getMaxX());
            ps.setInt(11, h.getMaxY());
            ps.setInt(12, h.getMaxZ());
            ps.setString(13, h.getWorld());
            ps.setObject(14, h.isSignPlaced() ? h.getSignX() : null);
            ps.setObject(15, h.isSignPlaced() ? h.getSignY() : null);
            ps.setObject(16, h.isSignPlaced() ? h.getSignZ() : null);
            ps.setString(17, h.isSignPlaced() ? h.getSignWorld() : null);
            ps.setInt(18, h.isSignPlaced() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Housing] Erreur sauvegarde", e);
        }
    }

    public void deleteHousing(UUID id) {
        housings.remove(id);
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM housings WHERE id=?")) {
            ps.setString(1, id.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Housing] Erreur suppression", e);
        }
    }

    // ─── Queries ─────────────────────────────────────────────────────────────────

    public Housing getHousingBySign(String world, int x, int y, int z) {
        return housings.values().stream()
            .filter(h -> h.isSignAt(world, x, y, z))
            .findFirst().orElse(null);
    }

    public Housing getHousingContaining(String world, int x, int y, int z) {
        return housings.values().stream()
            .filter(h -> h.contains(world, x, y, z))
            .findFirst().orElse(null);
    }

    public List<Housing> getHousingsForNation(UUID nationId) {
        return housings.values().stream()
            .filter(h -> h.getNationId().equals(nationId))
            .collect(Collectors.toList());
    }

    public Housing getHousingById(UUID id) { return housings.get(id); }

    public Collection<Housing> getAllHousings() { return Collections.unmodifiableCollection(housings.values()); }
}
