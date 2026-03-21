package fr.nations.war;

public enum WarStatus {

    PENDING_VALIDATION("En attente de validation du staff"),
    ACTIVE("En cours"),
    ATTACKER_WON("Victoire de l'attaquant"),
    DEFENDER_WON("Victoire du défenseur"),
    DRAW("Match nul"),
    REJECTED("Rejetée par le staff"),
    CANCELLED("Annulée");

    private final String displayName;

    WarStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    public boolean isActive() { return this == ACTIVE; }
    public boolean isPending() { return this == PENDING_VALIDATION; }
    public boolean isFinished() {
        return this == ATTACKER_WON || this == DEFENDER_WON || this == DRAW
            || this == REJECTED || this == CANCELLED;
    }
}
