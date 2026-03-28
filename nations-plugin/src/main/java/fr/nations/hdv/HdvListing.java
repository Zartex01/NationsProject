package fr.nations.hdv;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class HdvListing {

    public static final double TAX_RATE = 0.05;
    public static final double PUB_RATE = 0.02;

    private final int id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack item;
    private final double price;
    private final boolean pub;
    private final long listedAt;

    public HdvListing(int id, UUID sellerUuid, String sellerName, ItemStack item, double price, boolean pub, long listedAt) {
        this.id         = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.item       = item.clone();
        this.price      = price;
        this.pub        = pub;
        this.listedAt   = listedAt;
    }

    public double getSellerEarnings() {
        double deduction = TAX_RATE + (pub ? PUB_RATE : 0);
        return price * (1 - deduction);
    }

    public double getTotalFeePercent() {
        return (TAX_RATE + (pub ? PUB_RATE : 0)) * 100;
    }

    public int getId()             { return id; }
    public UUID getSellerUuid()    { return sellerUuid; }
    public String getSellerName()  { return sellerName; }
    public ItemStack getItem()     { return item.clone(); }
    public double getPrice()       { return price; }
    public boolean isPub()         { return pub; }
    public long getListedAt()      { return listedAt; }
}
