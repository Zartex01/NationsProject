package fr.nations.hdv;

import fr.nations.NationsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class HdvManager {

    private final NationsPlugin plugin;
    private final Map<UUID, HdvListing> listings = new LinkedHashMap<>();

    public static final double SELL_TAX = 0.05;
    public static final double PUB_FEE = 0.02;

    public HdvManager(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    public void createTable() {
        if (!plugin.getDatabaseManager().isConnected()) return;
        String sql = """
            CREATE TABLE IF NOT EXISTS hdv_listings (
                id TEXT PRIMARY KEY,
                seller_uuid TEXT NOT NULL,
                seller_name TEXT NOT NULL,
                item_data TEXT NOT NULL,
                price REAL NOT NULL,
                pub_enabled INTEGER DEFAULT 0,
                listed_at INTEGER NOT NULL
            )
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            plugin.getLogger().info("[HDV] Table hdv_listings prête.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[HDV] Erreur création table", e);
        }
    }

    public void loadFromDatabase() {
        if (!plugin.getDatabaseManager().isConnected()) return;
        String sql = "SELECT id, seller_uuid, seller_name, item_data, price, pub_enabled, listed_at FROM hdv_listings";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int count = 0;
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("id"));
                UUID sellerUUID = UUID.fromString(rs.getString("seller_uuid"));
                String sellerName = rs.getString("seller_name");
                String itemData = rs.getString("item_data");
                double price = rs.getDouble("price");
                boolean pub = rs.getInt("pub_enabled") == 1;
                long listedAt = rs.getLong("listed_at");

                ItemStack item = deserializeItem(itemData);
                if (item == null) continue;

                HdvListing listing = new HdvListing(id, sellerUUID, sellerName, item, price, pub, listedAt);
                listings.put(id, listing);
                count++;
            }
            plugin.getLogger().info("[HDV] " + count + " annonces chargées.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[HDV] Erreur chargement", e);
        }
    }

    public boolean addListing(HdvListing listing) {
        listings.put(listing.getId(), listing);
        saveListing(listing);
        return true;
    }

    public void removeListing(UUID listingId) {
        listings.remove(listingId);
        if (!plugin.getDatabaseManager().isConnected()) return;
        String sql = "DELETE FROM hdv_listings WHERE id = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, listingId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[HDV] Erreur suppression annonce", e);
        }
    }

    private void saveListing(HdvListing listing) {
        if (!plugin.getDatabaseManager().isConnected()) return;
        String itemData = serializeItem(listing.getItem());
        if (itemData == null) return;
        String sql = """
            INSERT INTO hdv_listings (id, seller_uuid, seller_name, item_data, price, pub_enabled, listed_at)
            VALUES (?,?,?,?,?,?,?)
            ON CONFLICT (id) DO UPDATE SET price=excluded.price, pub_enabled=excluded.pub_enabled
        """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, listing.getId().toString());
            ps.setString(2, listing.getSellerUUID().toString());
            ps.setString(3, listing.getSellerName());
            ps.setString(4, itemData);
            ps.setDouble(5, listing.getPrice());
            ps.setInt(6, listing.isPubEnabled() ? 1 : 0);
            ps.setLong(7, listing.getListedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[HDV] Erreur sauvegarde annonce", e);
        }
    }

    public List<HdvListing> getAllListings() {
        return new ArrayList<>(listings.values());
    }

    public HdvListing getListing(UUID id) {
        return listings.get(id);
    }

    private String serializeItem(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[HDV] Erreur sérialisation item", e);
            return null;
        }
    }

    private ItemStack deserializeItem(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[HDV] Erreur désérialisation item", e);
            return null;
        }
    }
}
