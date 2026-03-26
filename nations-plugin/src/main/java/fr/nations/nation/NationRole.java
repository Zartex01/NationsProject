package fr.nations.nation;

import fr.nations.NationsPlugin;

public enum NationRole {

    LEADER    ("Chef",     "§6", 5, true,  true,  true,  true,  true,  true),
    CO_LEADER ("Co-Chef",  "§e", 4, true,  true,  true,  true,  true,  false),
    OFFICER   ("Officier", "§a", 3, true,  true,  true,  false, false, false),
    MEMBER    ("Membre",   "§7", 2, true,  false, false, false, false, false),
    RECRUIT   ("Recrue",   "§8", 1, false, false, false, false, false, false);

    private final String defaultDisplay;
    private final String defaultColor;
    private final int rank;
    private final boolean defaultCanBuild;
    private final boolean defaultCanInvite;
    private final boolean defaultCanKick;
    private final boolean defaultCanManageWar;
    private final boolean defaultCanManageBank;
    private final boolean defaultCanDissolve;

    NationRole(String defaultDisplay, String defaultColor, int rank,
               boolean canBuild, boolean canInvite, boolean canKick,
               boolean canManageWar, boolean canManageBank, boolean canDissolve) {
        this.defaultDisplay    = defaultDisplay;
        this.defaultColor      = defaultColor;
        this.rank              = rank;
        this.defaultCanBuild   = canBuild;
        this.defaultCanInvite  = canInvite;
        this.defaultCanKick    = canKick;
        this.defaultCanManageWar  = canManageWar;
        this.defaultCanManageBank = canManageBank;
        this.defaultCanDissolve   = canDissolve;
    }

    private NationsPlugin plugin() { return NationsPlugin.getInstance(); }

    public String getDisplayName() {
        NationsPlugin p = plugin();
        if (p != null) return p.getConfigManager().getRoleDisplay(name(), defaultDisplay);
        return defaultDisplay;
    }

    public String getColor() {
        NationsPlugin p = plugin();
        if (p != null) return p.getConfigManager().getRoleColor(name(), defaultColor);
        return defaultColor;
    }

    public int getRank() { return rank; }

    public boolean canBuild() {
        NationsPlugin p = plugin();
        if (p != null) return p.getConfigManager().getRolePerm(name(), "can-build", defaultCanBuild);
        return defaultCanBuild;
    }

    public boolean canInvite() {
        NationsPlugin p = plugin();
        if (p != null) return p.getConfigManager().getRolePerm(name(), "can-invite", defaultCanInvite);
        return defaultCanInvite;
    }

    public boolean canKick() {
        NationsPlugin p = plugin();
        if (p != null) return p.getConfigManager().getRolePerm(name(), "can-kick", defaultCanKick);
        return defaultCanKick;
    }

    public boolean canManageWar() {
        NationsPlugin p = plugin();
        if (p != null) return p.getConfigManager().getRolePerm(name(), "can-manage-war", defaultCanManageWar);
        return defaultCanManageWar;
    }

    public boolean canManageBank() {
        NationsPlugin p = plugin();
        if (p != null) return p.getConfigManager().getRolePerm(name(), "can-manage-bank", defaultCanManageBank);
        return defaultCanManageBank;
    }

    public boolean canDissolve() {
        NationsPlugin p = plugin();
        if (p != null) return p.getConfigManager().getRolePerm(name(), "can-dissolve", defaultCanDissolve);
        return defaultCanDissolve;
    }

    public boolean isHigherThan(NationRole other) {
        return this.rank > other.rank;
    }

    public String getColoredDisplay() {
        return getColor() + getDisplayName();
    }
}
