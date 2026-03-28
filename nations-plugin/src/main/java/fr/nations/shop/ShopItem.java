package fr.nations.shop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ShopItem {

    private final ItemStack baseItem;
    private final double buyPrice;
    private final double sellPrice;
    private final int buyAmount;

    public ShopItem(Material material, double buyPrice, double sellPrice, int buyAmount) {
        this.baseItem  = new ItemStack(material);
        this.buyPrice  = buyPrice;
        this.sellPrice = sellPrice;
        this.buyAmount = buyAmount;
    }

    public ShopItem(Material material, double buyPrice, double sellPrice) {
        this(material, buyPrice, sellPrice, 1);
    }

    public ShopItem(ItemStack customStack, double buyPrice, double sellPrice, int buyAmount) {
        this.baseItem  = customStack.clone();
        this.baseItem.setAmount(1);
        this.buyPrice  = buyPrice;
        this.sellPrice = sellPrice;
        this.buyAmount = buyAmount;
    }

    public ItemStack getBaseItem()   { return baseItem.clone(); }
    public Material getMaterial()    { return baseItem.getType(); }
    public double getBuyPrice()      { return buyPrice; }
    public double getSellPrice()     { return sellPrice; }
    public int getBuyAmount()        { return buyAmount; }

    public boolean isBuyable()  { return buyPrice  > 0; }
    public boolean isSellable() { return sellPrice > 0; }

    public ItemStack createStack(int amount) {
        ItemStack stack = baseItem.clone();
        stack.setAmount(Math.min(amount, 64));
        return stack;
    }
}
