package fr.nations.shop;

import org.bukkit.Material;

import java.util.List;

public class ShopCategory {

    private final String id;
    private final String displayName;
    private final Material icon;
    private final Material glassColor;
    private final List<ShopItem> items;

    public ShopCategory(String id, String displayName, Material icon, Material glassColor, List<ShopItem> items) {
        this.id          = id;
        this.displayName = displayName;
        this.icon        = icon;
        this.glassColor  = glassColor;
        this.items       = items;
    }

    public String getId()          { return id; }
    public String getDisplayName() { return displayName; }
    public Material getIcon()      { return icon; }
    public Material getGlassColor(){ return glassColor; }
    public List<ShopItem> getItems(){ return items; }
}
