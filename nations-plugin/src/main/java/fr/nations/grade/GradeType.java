package fr.nations.grade;

public enum GradeType {

    JOUEUR("Joueur", "§7", "", 10),
    SOUTIEN("Soutien", "§a", "nations.grade.soutien", 20),
    PREMIUM("Premium", "§6", "nations.grade.premium", 35),
    CHEVALIER("Chevalier", "§b", "nations.grade.chevalier", 55),
    ROI("Roi", "§d", "nations.grade.roi", 80);

    private final String displayName;
    private final String color;
    private final String permission;
    private final int maxClaims;

    GradeType(String displayName, String color, String permission, int maxClaims) {
        this.displayName = displayName;
        this.color = color;
        this.permission = permission;
        this.maxClaims = maxClaims;
    }

    public String getDisplayName() { return displayName; }
    public String getColor() { return color; }
    public String getPermission() { return permission; }
    public int getMaxClaims() { return maxClaims; }

    public String getColoredDisplay() {
        return color + displayName;
    }

    public static GradeType fromPermission(org.bukkit.entity.Player player) {
        if (player.hasPermission("nations.grade.roi")) return ROI;
        if (player.hasPermission("nations.grade.chevalier")) return CHEVALIER;
        if (player.hasPermission("nations.grade.premium")) return PREMIUM;
        if (player.hasPermission("nations.grade.soutien")) return SOUTIEN;
        return JOUEUR;
    }
}
