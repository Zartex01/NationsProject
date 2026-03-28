package fr.nations.hdv;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class HdvListing {

    private final UUID id;
    private final UUID sellerUUID;
    private final String sellerName;
    private final ItemStack item;
    private double price;
    private boolean pubEnabled;
    private final long listedAt;

    public HdvListing(UUID id, UUID sellerUUID, String sellerName, ItemStack item, double price, boolean pubEnabled, long listedAt) {
        this.id = id;
        this.sellerUUID = sellerUUID;
        this.sellerName = sellerName;
        this.item = item.clone();
        this.price = price;
        this.pubEnabled = pubEnabled;
        this.listedAt = listedAt;
    }

    public UUID getId() { return id; }
    public UUID getSellerUUID() { return sellerUUID; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItem() { return item.clone(); }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public boolean isPubEnabled() { return pubEnabled; }
    public void setPubEnabled(boolean pubEnabled) { this.pubEnabled = pubEnabled; }
    public long getListedAt() { return listedAt; }

    public double getSellerEarnings() {
        double tax = price * 0.05;
        double pubFee = pubEnabled ? price * 0.02 : 0;
        return price - tax - pubFee;
    }

    public double getTotalFeePercent() {
        return pubEnabled ? 7.0 : 5.0;
    }
}
