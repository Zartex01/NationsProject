package fr.nations.war;

public enum WarStatus {

    PENDING_VALIDATION("§eEn attente de validation"),
    ACTIVE("§aEn cours"),
    ATTACKER_WON("§aVictoire attaquant"),
    DEFENDER_WON("§9Victoire défenseur"),
    DEFENDER_SURRENDERED("§cReddition du défenseur"),
    DRAW("§7Match nul"),
    REJECTED("§cRejetée par le staff"),
    CANCELLED("§8Annulée");

    private final String displayName;

    WarStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    public boolean isActive() { return this == ACTIVE; }
    public boolean isPending() { return this == PENDING_VALIDATION; }
    public boolean isFinished() {
        return this == ATTACKER_WON || this == DEFENDER_WON || this == DEFENDER_SURRENDERED
            || this == DRAW || this == REJECTED || this == CANCELLED;
    }
}
