package fr.nations.war;

public enum WarType {

    PILLAGE("Pillage", "Guerre de pillage — voler des ressources à la nation ennemie", 24, 1000),
    CONQUEST("Conquête", "Guerre de conquête — capturer des territoires ennemis", 48, 2000),
    EXTERMINATION("Extermination", "Guerre d'extermination — maximiser les kills ennemis", 72, 3000),
    SIEGE("Siège", "Guerre de siège — bloquer et assiéger la capitale ennemie", 48, 2500),
    RAID("Raid", "Raid éclair — attaque surprise de courte durée", 12, 500),
    ANNIHILATION("Annihilation", "Guerre totale — destruction complète de la nation ennemie", 96, 5000);

    private final String displayName;
    private final String description;
    private final int durationHours;
    private final double cost;

    WarType(String displayName, String description, int durationHours, double cost) {
        this.displayName = displayName;
        this.description = description;
        this.durationHours = durationHours;
        this.cost = cost;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getDurationHours() { return durationHours; }
    public double getCost() { return cost; }

    public long getDurationMillis() {
        return (long) durationHours * 60 * 60 * 1000;
    }
}
