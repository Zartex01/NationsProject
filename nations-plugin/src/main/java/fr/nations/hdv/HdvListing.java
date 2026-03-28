package fr.nations.hdv;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class HdvListing {

    private final int id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack item;
    private final double price;
    private final long listedAt;

    public HdvListing(int id, UUID sellerUuid, String sellerName, ItemStack item, double price, long listedAt) {
        this.id         = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.item       = item.clone();
        this.price      = price;
        this.listedAt   = listedAt;
    }

    public int getId()             { return id; }
    public UUID getSellerUuid()    { return sellerUuid; }
    public String getSellerName()  { return sellerName; }
    public ItemStack getItem()     { return item.clone(); }
    public double getPrice()       { return price; }
    public long getListedAt()      { return listedAt; }
}
