package fr.nations.gui;

import fr.nations.NationsPlugin;
import fr.nations.epoque.EpoqueCondition;
import fr.nations.epoque.EpoqueLevel;
import fr.nations.epoque.EpoqueNationProgress;
import fr.nations.epoque.EpoqueReward;
import fr.nations.nation.Nation;
import fr.nations.util.GuiUtil;
import fr.nations.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class EpoqueGui {

    private static final int[] CONDITION_SLOTS = { 10, 11, 12, 13, 14, 15, 16 };
    private static final int[] REWARD_SLOTS    = { 19, 20, 21, 22, 23, 24, 25 };
    private static final int   ACTION_SLOT     = 40;
    private static final int   CLOSE_SLOT      = 44;

    private final NationsPlugin plugin;
    private final Player player;
    private final Nation nation;

    public EpoqueGui(NationsPlugin plugin, Player player, Nation nation) {
        this.plugin = plugin;
        this.player = player;
        this.nation = nation;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Build
    // ─────────────────────────────────────────────────────────────────────────

    public Inventory build() {
        EpoqueNationProgress prog   = plugin.getEpoqueManager().getProgress(nation.getId());
        boolean allDone             = plugin.getEpoqueManager().allLevelsCompleted(nation.getId());
        EpoqueLevel level           = allDone ? null : plugin.getEpoqueManager().getLevel(prog.getCurrentLevel());
        int total                   = plugin.getEpoqueManager().getTotalLevels();

        String title = allDone
            ? "&6✦ Époque &8— &aTout accompli !"
            : "&6✦ Époque " + prog.getCurrentLevel() + "/" + total
              + " &8— &e" + (level != null ? level.getName() : "?");

        Inventory inv = GuiUtil.createGui(title, 5);
        GuiUtil.fillBorder(inv, Material.ORANGE_STAINED_GLASS_PANE);

        // ── Row 0: Level header ──
        ItemStack header = buildLevelHeader(prog, level, allDone, total);
        inv.setItem(4, header);

        if (allDone) {
            inv.setItem(ACTION_SLOT, GuiUtil.createItem(Material.NETHER_STAR,
                "&6&l✦ Toutes les époques maîtrisées !",
                "&7Votre nation a traversé toutes",
                "&7les ères de l'histoire.",
                "",
                "&eFélicitations !"
            ));
            inv.setItem(CLOSE_SLOT, GuiUtil.createItem(Material.BARRIER, "&c◄ Fermer"));
            GuiUtil.fillAll(inv);
            return inv;
        }

        if (level == null) {
            GuiUtil.fillAll(inv);
            return inv;
        }

        // ── Conditions ──
        List<EpoqueCondition> conditions = level.getConditions();
        for (int i = 0; i < Math.min(conditions.size(), CONDITION_SLOTS.length); i++) {
            EpoqueCondition cond = conditions.get(i);
            boolean met = cond.check(nation, plugin);
            String current = cond.getCurrentValueString(nation, plugin);
            inv.setItem(CONDITION_SLOTS[i], buildConditionItem(cond, met, current));
        }

        // ── Rewards ──
        List<EpoqueReward> rewards = level.getRewards();
        for (int i = 0; i < Math.min(rewards.size(), REWARD_SLOTS.length); i++) {
            inv.setItem(REWARD_SLOTS[i], buildRewardItem(rewards.get(i)));
        }

        // ── Research progress bar (row 3) ──
        if (prog.isResearching()) {
            long total_ms = plugin.getEpoqueManager().getLevel(prog.getCurrentLevel()) != null
                ? plugin.getEpoqueManager().getLevel(prog.getCurrentLevel()).getDurationMillis() : 1;
            long remaining = prog.getTimeRemainingMillis();
            long elapsed   = total_ms - remaining;
            int filled = (int) Math.round((double) elapsed / total_ms * 7);
            for (int i = 0; i < 7; i++) {
                Material mat = i < filled ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
                inv.setItem(28 + i, GuiUtil.createFillerItem(mat));
            }
        }

        // ── Action button ──
        inv.setItem(ACTION_SLOT, buildActionButton(prog, level));

        // ── Close ──
        inv.setItem(CLOSE_SLOT, GuiUtil.createItem(Material.BARRIER, "&c◄ Fermer"));

        GuiUtil.fillAll(inv);
        return inv;
    }

    private ItemStack buildLevelHeader(EpoqueNationProgress prog, EpoqueLevel level,
                                        boolean allDone, int total) {
        if (allDone) {
            return GuiUtil.createItem(Material.NETHER_STAR,
                "&6&l✦ Époque Terminée",
                "&7Tous les niveaux accomplis !");
        }
        if (level == null) {
            return GuiUtil.createItem(Material.COMPASS, "&eÉpoque", "&7Niveau inconnu.");
        }
        List<String> lore = new ArrayList<>();
        lore.add("&7Nation : &e" + nation.getName());
        lore.add("&7Niveau : &6" + prog.getCurrentLevel() + "&7 / &6" + total);
        lore.add("&7Durée de recherche : &e" + level.getDurationMinutes() + " min");
        if (prog.isResearching()) {
            lore.add("&aRecherche en cours...");
            lore.add("&7Temps restant : &e" + prog.formatTimeRemaining());
        }
        return GuiUtil.createItem(Material.COMPASS, "&6&l" + level.getName(), lore.toArray(new String[0]));
    }

    private ItemStack buildConditionItem(EpoqueCondition cond, boolean met, String current) {
        Material mat   = met ? Material.LIME_WOOL : Material.RED_WOOL;
        String   check = met ? "&a✔" : "&c✘";
        return GuiUtil.createItem(mat,
            check + " &f" + cond.getDescription(),
            "&7Progression : &e" + current,
            "",
            met ? "&aCondition remplie !" : "&cCondition non remplie"
        );
    }

    private ItemStack buildRewardItem(EpoqueReward reward) {
        return GuiUtil.createItem(Material.GOLD_INGOT,
            "&6✦ Récompense",
            "&7" + reward.getDescription(),
            "&8(" + reward.getType().getLabel() + " : " + MessageUtil.formatNumber(reward.getValue()) + ")"
        );
    }

    private ItemStack buildActionButton(EpoqueNationProgress prog, EpoqueLevel level) {
        if (prog.isResearching()) {
            return GuiUtil.createItem(Material.CLOCK,
                "&e&lRecherche en cours...",
                "&7Temps restant : &a" + prog.formatTimeRemaining(),
                "",
                "&7La recherche se termine automatiquement."
            );
        }

        boolean allConditionsMet = level.getConditions().stream()
            .allMatch(c -> c.check(nation, plugin));

        if (allConditionsMet) {
            return GuiUtil.createItem(Material.EMERALD,
                "&a&l▶ Lancer la recherche !",
                "&7Toutes les conditions sont remplies.",
                "&7Durée : &e" + level.getDurationMinutes() + " min",
                "",
                "&eCliquez pour démarrer !"
            );
        } else {
            long unmet = level.getConditions().stream()
                .filter(c -> !c.check(nation, plugin)).count();
            return GuiUtil.createItem(Material.BARRIER,
                "&c✘ Conditions non remplies",
                "&7Il reste &c" + unmet + " condition(s) à remplir.",
                "",
                "&7Remplissez toutes les conditions",
                "&7pour démarrer la recherche."
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Click handling
    // ─────────────────────────────────────────────────────────────────────────

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }

        if (slot == ACTION_SLOT) {
            EpoqueNationProgress prog = plugin.getEpoqueManager().getProgress(nation.getId());
            if (prog.isResearching()) {
                MessageUtil.send(player, "&6[Époque] &7Une recherche est déjà en cours (&e"
                    + prog.formatTimeRemaining() + "&7 restant).");
                return;
            }
            if (plugin.getEpoqueManager().allLevelsCompleted(nation.getId())) return;

            boolean started = plugin.getEpoqueManager().startResearch(nation);
            if (started) {
                EpoqueLevel level = plugin.getEpoqueManager().getLevel(prog.getCurrentLevel());
                MessageUtil.send(player,
                    "&6&l[Époque] &aRecherche &e" + (level != null ? level.getName() : "") 
                    + " &adémarrée ! Durée : &e" + (level != null ? level.getDurationMinutes() : 0) + " min");
                refresh();
            } else {
                MessageUtil.sendError(player, "Conditions non remplies ou recherche déjà active.");
                refresh();
            }
        }
    }

    public void open() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }

    private void refresh() {
        player.openInventory(build());
        GuiManager.registerGui(player.getUniqueId(), this);
    }
}
