package fr.nations.epoque;

import fr.nations.NationsPlugin;
import fr.nations.nation.Nation;

public class EpoqueCondition {

    public enum Type {
        MEMBERS("Membres"),
        COINS("Coins en banque"),
        TERRITORIES("Territoires");

        private final String label;
        Type(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private Type type;
    private double value;
    private String description;

    public EpoqueCondition(Type type, double value, String description) {
        this.type        = type;
        this.value       = value;
        this.description = description;
    }

    public boolean check(Nation nation, NationsPlugin plugin) {
        return switch (type) {
            case MEMBERS     -> nation.getMembers().size() >= value;
            case COINS       -> nation.getBankBalance() >= value;
            case TERRITORIES -> plugin.getTerritoryManager().getClaimCountForNation(nation.getId()) >= value;
        };
    }

    public String getDescription() { return description; }
    public Type getType()          { return type; }
    public double getValue()       { return value; }

    public void setType(Type type)             { this.type = type; }
    public void setValue(double value)         { this.value = value; }
    public void setDescription(String description) { this.description = description; }

    public String getCurrentValueString(Nation nation, NationsPlugin plugin) {
        double current = switch (type) {
            case MEMBERS     -> nation.getMembers().size();
            case COINS       -> nation.getBankBalance();
            case TERRITORIES -> plugin.getTerritoryManager().getClaimCountForNation(nation.getId());
        };
        if (type == Type.COINS) {
            return fr.nations.util.MessageUtil.formatNumber(current) + " / " + fr.nations.util.MessageUtil.formatNumber(value);
        }
        return (int) current + " / " + (int) value;
    }
}
