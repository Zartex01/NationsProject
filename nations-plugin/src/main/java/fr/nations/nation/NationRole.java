package fr.nations.nation;

public enum NationRole {

    LEADER("Chef", 5, true, true, true, true, true, true),
    CO_LEADER("Co-Chef", 4, true, true, true, true, true, false),
    OFFICER("Officier", 3, true, true, true, false, false, false),
    MEMBER("Membre", 2, true, false, false, false, false, false),
    RECRUIT("Recrue", 1, false, false, false, false, false, false);

    private final String displayName;
    private final int rank;
    private final boolean canBuild;
    private final boolean canInvite;
    private final boolean canKick;
    private final boolean canManageWar;
    private final boolean canManageBank;
    private final boolean canDissolve;

    NationRole(String displayName, int rank, boolean canBuild, boolean canInvite,
               boolean canKick, boolean canManageWar, boolean canManageBank, boolean canDissolve) {
        this.displayName = displayName;
        this.rank = rank;
        this.canBuild = canBuild;
        this.canInvite = canInvite;
        this.canKick = canKick;
        this.canManageWar = canManageWar;
        this.canManageBank = canManageBank;
        this.canDissolve = canDissolve;
    }

    public String getDisplayName() { return displayName; }
    public int getRank() { return rank; }
    public boolean canBuild() { return canBuild; }
    public boolean canInvite() { return canInvite; }
    public boolean canKick() { return canKick; }
    public boolean canManageWar() { return canManageWar; }
    public boolean canManageBank() { return canManageBank; }
    public boolean canDissolve() { return canDissolve; }

    public boolean isHigherThan(NationRole other) {
        return this.rank > other.rank;
    }

    public String getColoredDisplay() {
        return switch (this) {
            case LEADER -> "§6" + displayName;
            case CO_LEADER -> "§e" + displayName;
            case OFFICER -> "§a" + displayName;
            case MEMBER -> "§7" + displayName;
            case RECRUIT -> "§8" + displayName;
        };
    }
}
