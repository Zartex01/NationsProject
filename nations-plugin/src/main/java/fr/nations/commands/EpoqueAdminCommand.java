package fr.nations.commands;

import fr.nations.NationsPlugin;
import fr.nations.epoque.EpoqueCondition;
import fr.nations.epoque.EpoqueLevel;
import fr.nations.epoque.EpoqueNationProgress;
import fr.nations.epoque.EpoqueReward;
import fr.nations.nation.Nation;
import fr.nations.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /nepoqueadmin <sous-commande>
 *   reload
 *   list
 *   level add <name> <duration-minutes>
 *   level <num> setname <name...>
 *   level <num> setduration <minutes>
 *   level <num> condition add <TYPE> <value> <description...>
 *   level <num> condition remove <index>
 *   level <num> reward add <TYPE> <value> <description...>
 *   level <num> reward remove <index>
 *   setlevel <nationName> <level>
 */
public class EpoqueAdminCommand implements CommandExecutor, TabCompleter {

    private final NationsPlugin plugin;

    public EpoqueAdminCommand(NationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nations.admin")) {
            MessageUtil.sendError(sender instanceof Player p ? p : null, "Permission insuffisante.");
            if (!(sender instanceof Player)) sender.sendMessage("Permission insuffisante.");
            return true;
        }

        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "reload"   -> handleReload(sender);
            case "list"     -> handleList(sender);
            case "setlevel" -> handleSetLevel(sender, args);
            case "level"    -> handleLevel(sender, args);
            default         -> sendHelp(sender);
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        plugin.getEpoqueManager().reload();
        sender.sendMessage(MessageUtil.colorize("&a[Époque] Config rechargée avec succès."));
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage(MessageUtil.colorize("&6&l=== Niveaux d'Époque ==="));
        for (EpoqueLevel level : plugin.getEpoqueManager().getLevels()) {
            sender.sendMessage(MessageUtil.colorize(
                "&e" + level.getNumber() + ". &f" + level.getName()
                + " &7| &e" + level.getDurationMinutes() + "min"
                + " &7| &a" + level.getConditions().size() + " cond."
                + " | &6" + level.getRewards().size() + " récomp."));
            for (int i = 0; i < level.getConditions().size(); i++) {
                EpoqueCondition c = level.getConditions().get(i);
                sender.sendMessage(MessageUtil.colorize(
                    "  &7[" + i + "] Cond: &f" + c.getDescription()
                    + " &8(" + c.getType().name() + " >= " + (int)c.getValue() + ")"));
            }
            for (int i = 0; i < level.getRewards().size(); i++) {
                EpoqueReward r = level.getRewards().get(i);
                sender.sendMessage(MessageUtil.colorize(
                    "  &7[" + i + "] Récomp: &f" + r.getDescription()
                    + " &8(" + r.getType().name() + " " + r.getValue() + ")"));
            }
        }
    }

    private void handleSetLevel(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtil.colorize("&cUsage: /nepoqueadmin setlevel <nomNation> <niveau>"));
            return;
        }
        Nation nation = plugin.getNationManager().getNationByName(args[1]);
        if (nation == null) {
            sender.sendMessage(MessageUtil.colorize("&cNation introuvable: " + args[1]));
            return;
        }
        int level;
        try { level = Integer.parseInt(args[2]); }
        catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.colorize("&cNiveau invalide."));
            return;
        }
        plugin.getEpoqueManager().setNationLevel(nation.getId(), level);
        sender.sendMessage(MessageUtil.colorize(
            "&a[Époque] Nation &e" + nation.getName() + " &arégulée au niveau &e" + level + "&a."));
    }

    private void handleLevel(CommandSender sender, String[] args) {
        if (args.length < 2) { sendHelp(sender); return; }

        if (args[1].equalsIgnoreCase("add")) {
            if (args.length < 4) {
                sender.sendMessage(MessageUtil.colorize("&cUsage: /nepoqueadmin level add <nom> <durée-minutes>"));
                return;
            }
            int dur;
            try { dur = Integer.parseInt(args[args.length - 1]); }
            catch (NumberFormatException e) { sender.sendMessage(MessageUtil.colorize("&cDurée invalide.")); return; }

            String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length - 1));
            int num = plugin.getEpoqueManager().getTotalLevels() + 1;
            EpoqueLevel newLevel = new EpoqueLevel(num, name, dur);
            plugin.getEpoqueManager().addLevel(newLevel);
            sender.sendMessage(MessageUtil.colorize(
                "&a[Époque] Niveau &e" + num + " &f(" + name + ") &acréé avec durée &e" + dur + "min&a."));
            return;
        }

        int levelNum;
        try { levelNum = Integer.parseInt(args[1]); }
        catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.colorize("&cNuméro de niveau invalide."));
            return;
        }

        EpoqueLevel level = plugin.getEpoqueManager().getLevel(levelNum);
        if (level == null) {
            sender.sendMessage(MessageUtil.colorize("&cNiveau " + levelNum + " introuvable."));
            return;
        }

        if (args.length < 3) { sendHelp(sender); return; }

        switch (args[2].toLowerCase()) {
            case "setname" -> {
                if (args.length < 4) { sender.sendMessage(MessageUtil.colorize("&cUsage: /nepoqueadmin level <num> setname <nom>")); return; }
                String name = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                level.setName(name);
                plugin.getEpoqueManager().saveConfig();
                sender.sendMessage(MessageUtil.colorize("&a[Époque] Niveau " + levelNum + " renommé en &f" + name + "&a."));
            }
            case "setduration" -> {
                if (args.length < 4) { sender.sendMessage(MessageUtil.colorize("&cUsage: /nepoqueadmin level <num> setduration <minutes>")); return; }
                try {
                    int dur = Integer.parseInt(args[3]);
                    level.setDurationMinutes(dur);
                    plugin.getEpoqueManager().saveConfig();
                    sender.sendMessage(MessageUtil.colorize("&a[Époque] Durée du niveau " + levelNum + " → &e" + dur + " min&a."));
                } catch (NumberFormatException e) { sender.sendMessage(MessageUtil.colorize("&cDurée invalide.")); }
            }
            case "condition" -> handleConditionSubCommand(sender, level, args);
            case "reward"    -> handleRewardSubCommand(sender, level, args);
            default -> sendHelp(sender);
        }
    }

    private void handleConditionSubCommand(CommandSender sender, EpoqueLevel level, String[] args) {
        if (args.length < 4) { sendHelp(sender); return; }
        switch (args[3].toLowerCase()) {
            case "add" -> {
                if (args.length < 7) {
                    sender.sendMessage(MessageUtil.colorize("&cUsage: /nepoqueadmin level <num> condition add <TYPE> <valeur> <description...>"));
                    sender.sendMessage(MessageUtil.colorize("&7Types: MEMBERS, COINS, TERRITORIES"));
                    return;
                }
                EpoqueCondition.Type type;
                try { type = EpoqueCondition.Type.valueOf(args[4].toUpperCase()); }
                catch (IllegalArgumentException e) {
                    sender.sendMessage(MessageUtil.colorize("&cType invalide. Types: MEMBERS, COINS, TERRITORIES"));
                    return;
                }
                double value;
                try { value = Double.parseDouble(args[5]); }
                catch (NumberFormatException e) { sender.sendMessage(MessageUtil.colorize("&cValeur invalide.")); return; }
                String desc = String.join(" ", Arrays.copyOfRange(args, 6, args.length));
                level.addCondition(new EpoqueCondition(type, value, desc));
                plugin.getEpoqueManager().saveConfig();
                sender.sendMessage(MessageUtil.colorize("&a[Époque] Condition ajoutée au niveau " + level.getNumber() + "."));
            }
            case "remove" -> {
                if (args.length < 5) { sender.sendMessage(MessageUtil.colorize("&cUsage: /nepoqueadmin level <num> condition remove <index>")); return; }
                try {
                    int idx = Integer.parseInt(args[4]);
                    if (idx < 0 || idx >= level.getConditions().size()) {
                        sender.sendMessage(MessageUtil.colorize("&cIndex invalide. Il y a " + level.getConditions().size() + " condition(s)."));
                        return;
                    }
                    level.removeCondition(idx);
                    plugin.getEpoqueManager().saveConfig();
                    sender.sendMessage(MessageUtil.colorize("&a[Époque] Condition " + idx + " supprimée du niveau " + level.getNumber() + "."));
                } catch (NumberFormatException e) { sender.sendMessage(MessageUtil.colorize("&cIndex invalide.")); }
            }
            default -> sender.sendMessage(MessageUtil.colorize("&cSous-commande inconnue: add ou remove."));
        }
    }

    private void handleRewardSubCommand(CommandSender sender, EpoqueLevel level, String[] args) {
        if (args.length < 4) { sendHelp(sender); return; }
        switch (args[3].toLowerCase()) {
            case "add" -> {
                if (args.length < 7) {
                    sender.sendMessage(MessageUtil.colorize("&cUsage: /nepoqueadmin level <num> reward add <TYPE> <valeur> <description...>"));
                    sender.sendMessage(MessageUtil.colorize("&7Types: COINS_PER_MEMBER, NATION_COINS"));
                    return;
                }
                EpoqueReward.Type type;
                try { type = EpoqueReward.Type.valueOf(args[4].toUpperCase()); }
                catch (IllegalArgumentException e) {
                    sender.sendMessage(MessageUtil.colorize("&cType invalide. Types: COINS_PER_MEMBER, NATION_COINS"));
                    return;
                }
                double value;
                try { value = Double.parseDouble(args[5]); }
                catch (NumberFormatException e) { sender.sendMessage(MessageUtil.colorize("&cValeur invalide.")); return; }
                String desc = String.join(" ", Arrays.copyOfRange(args, 6, args.length));
                level.addReward(new EpoqueReward(type, value, desc));
                plugin.getEpoqueManager().saveConfig();
                sender.sendMessage(MessageUtil.colorize("&a[Époque] Récompense ajoutée au niveau " + level.getNumber() + "."));
            }
            case "remove" -> {
                if (args.length < 5) { sender.sendMessage(MessageUtil.colorize("&cUsage: /nepoqueadmin level <num> reward remove <index>")); return; }
                try {
                    int idx = Integer.parseInt(args[4]);
                    if (idx < 0 || idx >= level.getRewards().size()) {
                        sender.sendMessage(MessageUtil.colorize("&cIndex invalide. Il y a " + level.getRewards().size() + " récompense(s)."));
                        return;
                    }
                    level.removeReward(idx);
                    plugin.getEpoqueManager().saveConfig();
                    sender.sendMessage(MessageUtil.colorize("&a[Époque] Récompense " + idx + " supprimée du niveau " + level.getNumber() + "."));
                } catch (NumberFormatException e) { sender.sendMessage(MessageUtil.colorize("&cIndex invalide.")); }
            }
            default -> sender.sendMessage(MessageUtil.colorize("&cSous-commande inconnue: add ou remove."));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtil.colorize("&6&l=== /nepoqueadmin — Aide ==="));
        sender.sendMessage(MessageUtil.colorize("&e/nepoqueadmin reload &7— Recharger la config"));
        sender.sendMessage(MessageUtil.colorize("&e/nepoqueadmin list &7— Lister tous les niveaux"));
        sender.sendMessage(MessageUtil.colorize("&e/nepoqueadmin setlevel <nation> <niveau> &7— Définir le niveau"));
        sender.sendMessage(MessageUtil.colorize("&e/nepoqueadmin level add <nom> <durée-min> &7— Ajouter un niveau"));
        sender.sendMessage(MessageUtil.colorize("&e/nepoqueadmin level <N> setname <nom> &7— Renommer un niveau"));
        sender.sendMessage(MessageUtil.colorize("&e/nepoqueadmin level <N> setduration <min> &7— Changer la durée"));
        sender.sendMessage(MessageUtil.colorize("&e/nepoqueadmin level <N> condition add <TYPE> <val> <desc> &7— Ajouter condition"));
        sender.sendMessage(MessageUtil.colorize("&e/nepoqueadmin level <N> condition remove <index> &7— Supprimer condition"));
        sender.sendMessage(MessageUtil.colorize("&e/nepoqueadmin level <N> reward add <TYPE> <val> <desc> &7— Ajouter récompense"));
        sender.sendMessage(MessageUtil.colorize("&e/nepoqueadmin level <N> reward remove <index> &7— Supprimer récompense"));
        sender.sendMessage(MessageUtil.colorize("&7Types condition: &fMEMBERS, COINS, TERRITORIES"));
        sender.sendMessage(MessageUtil.colorize("&7Types récompense: &fCOINS_PER_MEMBER, NATION_COINS"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List.of("reload", "list", "setlevel", "level")
                .stream().filter(s -> s.startsWith(args[0].toLowerCase())).forEach(completions::add);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("level")) {
            plugin.getEpoqueManager().getLevels()
                .forEach(l -> completions.add(String.valueOf(l.getNumber())));
            completions.add("add");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("level") && !args[1].equalsIgnoreCase("add")) {
            List.of("setname", "setduration", "condition", "reward")
                .stream().filter(s -> s.startsWith(args[2].toLowerCase())).forEach(completions::add);
        } else if (args.length == 4 && args[0].equalsIgnoreCase("level")) {
            if (args[2].equalsIgnoreCase("condition") || args[2].equalsIgnoreCase("reward")) {
                List.of("add", "remove").stream().filter(s -> s.startsWith(args[3].toLowerCase())).forEach(completions::add);
            }
        } else if (args.length == 5 && args[0].equalsIgnoreCase("level") && args[3].equalsIgnoreCase("condition") && args[4-1].equalsIgnoreCase("add")) {
            Arrays.stream(EpoqueCondition.Type.values())
                .map(Enum::name).filter(s -> s.startsWith(args[4].toUpperCase())).forEach(completions::add);
        } else if (args.length == 5 && args[0].equalsIgnoreCase("level") && args[3].equalsIgnoreCase("reward") && args[3].equalsIgnoreCase("add")) {
            Arrays.stream(EpoqueReward.Type.values())
                .map(Enum::name).filter(s -> s.startsWith(args[4].toUpperCase())).forEach(completions::add);
        }
        return completions;
    }
}
