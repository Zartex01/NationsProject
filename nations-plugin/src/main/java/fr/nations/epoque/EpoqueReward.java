package fr.nations.epoque;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;
import fr.nations.nation.NationMember;
import fr.nations.util.MessageUtil;
import org.bukkit.entity.Player;

public class EpoqueReward {

    public enum Type {
        COINS_PER_MEMBER("Coins par membre"),
        NATION_COINS("Coins en banque nation");

        private final String label;
        Type(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private Type type;
    private double value;
    private String description;

    public EpoqueReward(Type type, double value, String description) {
        this.type        = type;
        this.value       = value;
        this.description = description;
    }

    public void apply(Nation nation, NationsPlugin plugin) {
        switch (type) {
            case COINS_PER_MEMBER -> {
                for (NationMember member : nation.getMembers().values()) {
                    plugin.getEconomyManager().deposit(member.getPlayerId(), value);
                    Player online = plugin.getServer().getPlayer(member.getPlayerId());
                    if (online != null) {
                        MessageUtil.send(online,
                            "&6&l[Époque] &7Récompense reçue : &a+" + MessageUtil.formatNumber(value) + " coins !");
                    }
                }
            }
            case NATION_COINS -> {
                nation.depositToBank(value);
                plugin.getDataManager().saveNations();
            }
        }
    }

    public String getDescription() { return description; }
    public Type getType()          { return type; }
    public double getValue()       { return value; }

    public void setType(Type type)             { this.type = type; }
    public void setValue(double value)         { this.value = value; }
    public void setDescription(String d)       { this.description = d; }
}
