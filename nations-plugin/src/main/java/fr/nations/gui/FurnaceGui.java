package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.util.GuiUtil;
import fr.nations.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.FurnaceRecipe;

import java.util.*;

/**
 * Fourneau virtuel accessible via /furnace.
 * Supporte toutes les recettes de fusion + combustibles vanille (bois, charbon, etc.)
 */
public class FurnaceGui {

    private final NationsPlugin plugin;
    private final Player player;

    // Slots du GUI (inventaire 3x3)
    private static final int SLOT_INPUT   = 11;  // Matière première (milieu gauche)
    private static final int SLOT_FUEL    = 20;  // Combustible (bas gauche)
    private static final int SLOT_OUTPUT  = 15;  // Résultat (milieu droite)
    private static final int SLOT_SMELT   = 13;  // Bouton fondre
    private static final int SLOT_CLOSE   = 26;  // Fermer

    // Valeurs de combustible (en nombre de fusions possibles par item)
    private static final Map<Material, Integer> FUEL_VALUES = new HashMap<>();

    static {
        // Charbon / Charbon de bois
        FUEL_VALUES.put(Material.COAL, 8);
        FUEL_VALUES.put(Material.CHARCOAL, 8);
        // Blocs de charbon
        FUEL_VALUES.put(Material.COAL_BLOCK, 80);
        // Bûches / Bois (comptes comme charbon de bois)
        for (Material m : Material.values()) {
            String name = m.name();
            if ((name.endsWith("_LOG") || name.endsWith("_WOOD")) && !name.contains("STRIPPED")) {
                FUEL_VALUES.put(m, 1);
            }
            if (name.endsWith("_PLANKS")) {
                FUEL_VALUES.put(m, 1);
            }
            if (name.endsWith("_SLAB") && (name.contains("OAK") || name.contains("SPRUCE")
                    || name.contains("BIRCH") || name.contains("JUNGLE") || name.contains("ACACIA")
                    || name.contains("DARK_OAK") || name.contains("MANGROVE") || name.contains("CHERRY")
                    || name.contains("BAMBOO") || name.contains("CRIMSON") || name.contains("WARPED"))) {
                FUEL_VALUES.put(m, 1); // demi-slab = 0.5 mais on arrondit à 1
            }
        }
        // Autres combustibles courants
        FUEL_VALUES.put(Material.BLAZE_ROD, 12);
        FUEL_VALUES.put(Material.LAVA_BUCKET, 100);
        FUEL_VALUES.put(Material.DRIED_KELP_BLOCK, 20);
        FUEL_VALUES.put(Material.BAMBOO, 1);
        FUEL_VALUES.put(Material.STICK, 1);
        FUEL_VALUES.put(Material.WOODEN_SWORD, 1);
        FUEL_VALUES.put(Material.WOODEN_AXE, 1);
        FUEL_VALUES.put(Material.WOODEN_PICKAXE, 1);
        FUEL_VALUES.put(Material.WOODEN_HOE, 1);
        FUEL_VALUES.put(Material.WOODEN_SHOVEL, 1);
        FUEL_VALUES.put(Material.BOW, 1);
        FUEL_VALUES.put(Material.BOOKSHELF, 3);
        FUEL_VALUES.put(Material.CRAFTING_TABLE, 2);
        FUEL_VALUES.put(Material.NOTE_BLOCK, 2);
        FUEL_VALUES.put(Material.CHEST, 2);
        FUEL_VALUES.put(Material.TRAPPED_CHEST, 2);
        FUEL_VALUES.put(Material.JUKEBOX, 2);
    }

    public FurnaceGui(NationsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public Inventory build() {
        Inventory inv = GuiUtil.createGui("&8⚒ Fourneau", 3);

        // Remplissage décoratif
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, GuiUtil.createFillerItem(Material.GRAY_STAINED_GLASS_PANE));
        }

        // Slots fonctionnels (vides pour que le joueur pose des items)
        inv.setItem(SLOT_INPUT, null);
        inv.setItem(SLOT_FUEL, null);
        inv.setItem(SLOT_OUTPUT, null);

        // Labels autour des slots
        inv.setItem(10, GuiUtil.createItem(Material.FURNACE, "&fMatière première",
                "&7Placez l'item à fondre ici"));
        inv.setItem(19, GuiUtil.createItem(Material.COAL, "&fCombustible",
                "&7Bois, charbon, lave...",
                "&7Tout combustible vanille fonctionne"));
        inv.setItem(14, GuiUtil.createItem(Material.HOPPER, "&fRésultat",
                "&7Le résultat apparaîtra ici"));

        // Flèche de progression
        inv.setItem(12, GuiUtil.createItem(Material.WHITE_STAINED_GLASS_PANE, " "));

        // Bouton fondre
        inv.setItem(SLOT_SMELT, GuiUtil.createItem(Material.MAGMA_CREAM,
                "&6&l▶ Fondre",
                "&7Cliquez pour fondre l'item",
                "&7(consomme le combustible nécessaire)"
        ));

        // Fermer
        inv.setItem(SLOT_CLOSE, GuiUtil.createItem(Material.BARRIER, "&cFermer &7(récupère tous les items)"));

        return inv;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        Inventory inv = event.getInventory();

        // Permettre de poser/prendre dans les slots fonctionnels
        if (slot == SLOT_INPUT || slot == SLOT_FUEL || slot == SLOT_OUTPUT) {
            event.setCancelled(false);
            return;
        }

        // Bouton fermer → rendre tous les items
        if (slot == SLOT_CLOSE) {
            returnItems(inv);
            player.closeInventory();
            return;
        }

        // Bouton fondre
        if (slot == SLOT_SMELT) {
            handleSmelt(inv);
            return;
        }
    }

    private void handleSmelt(Inventory inv) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack inputItem = inv.getItem(SLOT_INPUT);
            ItemStack fuelItem  = inv.getItem(SLOT_FUEL);

            if (inputItem == null || inputItem.getType() == Material.AIR) {
                MessageUtil.sendError(player, "Placez un item à fondre dans le slot gauche.");
                return;
            }
            if (fuelItem == null || fuelItem.getType() == Material.AIR) {
                MessageUtil.sendError(player, "Placez un combustible dans le slot du bas.");
                return;
            }

            // Trouver la recette
            ItemStack result = getSmeltResult(inputItem);
            if (result == null) {
                MessageUtil.sendError(player, "§c" + formatName(inputItem.getType()) + " §cn'est pas fondable.");
                return;
            }

            // Vérifier le combustible
            int fuelValue = FUEL_VALUES.getOrDefault(fuelItem.getType(), 0);
            if (fuelValue == 0) {
                MessageUtil.sendError(player, "§c" + formatName(fuelItem.getType()) + " §cn'est pas un combustible valide.");
                return;
            }

            // Calculer combien d'items on peut fondre
            int inputCount = inputItem.getAmount();
            int fuelAvailable = fuelItem.getAmount() * fuelValue;
            int maxSmeltable = Math.min(inputCount, fuelAvailable);
            // Maximum 64 par opération
            int toSmelt = Math.min(maxSmeltable, 64);

            if (toSmelt <= 0) {
                MessageUtil.sendError(player, "Combustible insuffisant.");
                return;
            }

            // Calculer la consommation de combustible
            int fuelUnitsNeeded = (int) Math.ceil((double) toSmelt / fuelValue);

            // Mettre à jour les slots
            if (inputCount == toSmelt) {
                inv.setItem(SLOT_INPUT, null);
            } else {
                inputItem.setAmount(inputCount - toSmelt);
                inv.setItem(SLOT_INPUT, inputItem);
            }

            if (fuelUnitsNeeded >= fuelItem.getAmount()) {
                inv.setItem(SLOT_FUEL, null);
            } else {
                fuelItem.setAmount(fuelItem.getAmount() - fuelUnitsNeeded);
                inv.setItem(SLOT_FUEL, fuelItem);
            }

            // Résultat
            ItemStack resultStack = result.clone();
            resultStack.setAmount(toSmelt * result.getAmount());
            // Si résultat dépasse 64, on donne le reste en main
            if (resultStack.getAmount() > 64) {
                int overflow = resultStack.getAmount() - 64;
                resultStack.setAmount(64);
                ItemStack overflowStack = result.clone();
                overflowStack.setAmount(overflow);
                player.getInventory().addItem(overflowStack).forEach((k, v) ->
                    player.getWorld().dropItemNaturally(player.getLocation(), v));
            }

            // Combiner avec le résultat existant si même type
            ItemStack existing = inv.getItem(SLOT_OUTPUT);
            if (existing != null && existing.getType() == resultStack.getType()
                    && existing.getAmount() + resultStack.getAmount() <= 64) {
                existing.setAmount(existing.getAmount() + resultStack.getAmount());
                inv.setItem(SLOT_OUTPUT, existing);
            } else if (existing == null || existing.getType() == Material.AIR) {
                inv.setItem(SLOT_OUTPUT, resultStack);
            } else {
                // Slot plein → donner au joueur
                player.getInventory().addItem(resultStack).forEach((k, v) ->
                    player.getWorld().dropItemNaturally(player.getLocation(), v));
            }

            MessageUtil.sendSuccess(player, "Fondu §e" + toSmelt + "x " + formatName(inputItem.getType())
                    + " §a→ §e" + (toSmelt * result.getAmount()) + "x " + formatName(result.getType()) + "§a.");
        });
    }

    private ItemStack getSmeltResult(ItemStack input) {
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe recipe = it.next();
            if (recipe instanceof FurnaceRecipe furnaceRecipe) {
                if (furnaceRecipe.getInputChoice().test(input)) {
                    return furnaceRecipe.getResult();
                }
            }
        }
        return null;
    }

    private void returnItems(Inventory inv) {
        for (int slot : new int[]{SLOT_INPUT, SLOT_FUEL, SLOT_OUTPUT}) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                player.getInventory().addItem(item).forEach((k, v) ->
                    player.getWorld().dropItemNaturally(player.getLocation(), v));
                inv.setItem(slot, null);
            }
        }
    }

    private String formatName(Material mat) {
        String name = mat.name().toLowerCase().replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
