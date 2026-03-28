package fr.nations.hdv;

import fr.nations.NationsPlugin;
import fr.nations.util.MessageUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class HdvManager {

    private final NationsPlugin plugin;
    private final File dataFile;
    private YamlConfiguration cfg;

    private final LinkedHashMap<Integer, HdvListing> listings = new LinkedHashMap<>();
    private int nextId = 1;

    private final Map<UUID, ItemStack> pendingPriceInput = new HashMap<>();

    public HdvManager(NationsPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "hdv_data.yml");
        load();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Listing management
    // ─────────────────────────────────────────────────────────────────────────

    public boolean addListing(Player seller, ItemStack item, double price) {
        if (item == null || item.getType().isAir()) return false;
        HdvListing listing = new HdvListing(
            nextId++,
            seller.getUniqueId(),
            seller.getName(),
            item,
            price,
            System.currentTimeMillis()
        );
        listings.put(listing.getId(), listing);
        save();
        return true;
    }

    public HdvListing removeListing(int id) {
        HdvListing listing = listings.remove(id);
        if (listing != null) save();
        return listing;
    }

    public HdvListing getListing(int id) {
        return listings.get(id);
    }

    public List<HdvListing> getActiveListings() {
        List<HdvListing> list = new ArrayList<>(listings.values());
        list.sort(Comparator.comparingLong(HdvListing::getListedAt).reversed());
        return list;
    }

    public List<HdvListing> getListingsBySeller(UUID uuid) {
        return listings.values().stream()
            .filter(l -> l.getSellerUuid().equals(uuid))
            .sorted(Comparator.comparingLong(HdvListing::getListedAt).reversed())
            .collect(Collectors.toList());
    }

    public int getActiveListingCount(UUID uuid) {
        return (int) listings.values().stream()
            .filter(l -> l.getSellerUuid().equals(uuid))
            .count();
    }

    public boolean buyListing(Player buyer, int listingId) {
        HdvListing listing = listings.get(listingId);
        if (listing == null) return false;
        if (listing.getSellerUuid().equals(buyer.getUniqueId())) return false;

        double balance = plugin.getEconomyManager().getBalance(buyer.getUniqueId());
        if (balance < listing.getPrice()) return false;

        Map<Integer, ItemStack> leftover = buyer.getInventory().addItem(listing.getItem());
        if (!leftover.isEmpty()) return false;

        plugin.getEconomyManager().withdraw(buyer.getUniqueId(), listing.getPrice());
        plugin.getEconomyManager().deposit(listing.getSellerUuid(), listing.getPrice());

        listings.remove(listingId);
        save();

        Player seller = plugin.getServer().getPlayer(listing.getSellerUuid());
        if (seller != null && seller.isOnline()) {
            MessageUtil.send(seller,
                "&a&lHDV &8» &7" + buyer.getName() + " a acheté votre &e"
                + listing.getItem().getType().name().replace("_", " ").toLowerCase()
                + " &7pour &e" + MessageUtil.formatNumber(listing.getPrice()) + " coins&7 !");
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Slot limits
    // ─────────────────────────────────────────────────────────────────────────

    public int getMaxSlots(Player player) {
        if (player.hasPermission("nations.admin")) return 50;
        if (player.hasPermission("nations.grade.premium"))   return 30;
        if (player.hasPermission("nations.grade.chevalier")) return 20;
        if (player.hasPermission("nations.grade.heros"))     return 10;
        return 5;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Pending price input (chat)
    // ─────────────────────────────────────────────────────────────────────────

    public void setPendingPriceInput(UUID uuid, ItemStack item) {
        pendingPriceInput.put(uuid, item.clone());
    }

    public ItemStack getPendingPriceInput(UUID uuid) {
        return pendingPriceInput.get(uuid);
    }

    public void clearPendingPriceInput(UUID uuid) {
        pendingPriceInput.remove(uuid);
    }

    public boolean hasPendingPriceInput(UUID uuid) {
        return pendingPriceInput.containsKey(uuid);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Formatting helpers
    // ─────────────────────────────────────────────────────────────────────────

    public static String timeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long mins = diff / 60_000;
        if (mins < 1)  return "à l'instant";
        if (mins < 60) return "il y a " + mins + "min";
        long h = mins / 60;
        if (h < 24)   return "il y a " + h + "h";
        return "il y a " + (h / 24) + "j";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Persistence
    // ─────────────────────────────────────────────────────────────────────────

    private void load() {
        cfg = dataFile.exists()
            ? YamlConfiguration.loadConfiguration(dataFile)
            : new YamlConfiguration();

        nextId = cfg.getInt("next-id", 1);
        if (cfg.isConfigurationSection("listings")) {
            for (String key : cfg.getConfigurationSection("listings").getKeys(false)) {
                try {
                    String path = "listings." + key;
                    int id         = Integer.parseInt(key);
                    UUID seller    = UUID.fromString(cfg.getString(path + ".seller-uuid"));
                    String name    = cfg.getString(path + ".seller-name", "?");
                    ItemStack item = cfg.getItemStack(path + ".item");
                    double price   = cfg.getDouble(path + ".price");
                    long listedAt  = cfg.getLong(path + ".listed-at");
                    if (item != null) {
                        listings.put(id, new HdvListing(id, seller, name, item, price, listedAt));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("HDV: entrée corrompue ignorée (id=" + key + ")");
                }
            }
        }
    }

    private void save() {
        cfg.set("next-id", nextId);
        cfg.set("listings", null);
        for (HdvListing l : listings.values()) {
            String path = "listings." + l.getId();
            cfg.set(path + ".seller-uuid",  l.getSellerUuid().toString());
            cfg.set(path + ".seller-name",  l.getSellerName());
            cfg.set(path + ".item",         l.getItem());
            cfg.set(path + ".price",        l.getPrice());
            cfg.set(path + ".listed-at",    l.getListedAt());
        }
        try {
            if (!dataFile.getParentFile().exists()) dataFile.getParentFile().mkdirs();
            cfg.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Impossible de sauvegarder hdv_data.yml : " + ex.getMessage());
        }
    }
}
