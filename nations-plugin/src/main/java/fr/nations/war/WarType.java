package fr.nations.war;

public enum WarType {

    PILLAGE(
        "Pillage",
        "§7Voler des ressources à la nation ennemie. Victoire par kills.",
        24, 1000, false
    ),
    CONQUEST(
        "Conquête",
        "§7Capturer des territoires ennemis. Victoire par domination territoriale.",
        48, 2000, false
    ),
    EXTERMINATION(
        "Extermination",
        "§7Maximiser les kills ennemis. Victoire par score de kills.",
        72, 3000, false
    ),
    SIEGE(
        "Siège",
        "§7Bloquer et assiéger la capitale ennemie. Durée limitée.",
        48, 2500, false
    ),
    RAID(
        "Raid",
        "§7Attaque éclair courte. Victoire rapide par kills.",
        12, 500, false
    ),
    ANNIHILATION(
        "Annihilation",
        "§7Guerre totale jusqu'à destruction complète. Victoire par kills massifs.",
        96, 5000, false
    ),
    ASSAULT(
        "Assaut",
        "§4§lGuerre d'assaut — sans limite de temps. Se termine UNIQUEMENT par:\n" +
        "  §c• Reddition volontaire du défenseur\n" +
        "  §c• Dissolution de la nation défenseure\n" +
        "  §c§lGuerre la plus rare et dévastatrice. Validation staff très stricte.",
        0, 10000, true
    );

    private final String displayName;
    private final String description;
    private final int durationHours;
    private final double cost;
    private final boolean isAssault;

    WarType(String displayName, String description, int durationHours, double cost, boolean isAssault) {
        this.displayName = displayName;
        this.description = description;
        this.durationHours = durationHours;
        this.cost = cost;
        this.isAssault = isAssault;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getDurationHours() { return durationHours; }
    public double getCost() { return cost; }
    public boolean isAssault() { return isAssault; }

    public long getDurationMillis() {
        if (isAssault) return Long.MAX_VALUE / 2;
        return (long) durationHours * 60 * 60 * 1000;
    }

    public String getColoredName() {
        return switch (this) {
            case PILLAGE -> "§6Pillage";
            case CONQUEST -> "§a Conquête";
            case EXTERMINATION -> "§cExtermination";
            case SIEGE -> "§9Siège";
            case RAID -> "§eRaid";
            case ANNIHILATION -> "§4Annihilation";
            case ASSAULT -> "§4§lASSAUT";
        };
    }
}
