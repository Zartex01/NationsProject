package fr.nations.grade;

import fr.nations.NationsPlugin;

public enum GradeType {

    JOUEUR    ("Joueur",    "&7", "",                       10),
    SOUTIEN   ("Soutien",   "&a", "nations.grade.soutien",  20),
    PREMIUM   ("Premium",   "&6", "nations.grade.premium",  35),
    CHEVALIER ("Chevalier", "&b", "nations.grade.chevalier",55),
    ROI       ("Roi",       "&d", "nations.grade.roi",      80);

    private final String defaultDisplay;
    private final String defaultColor;
    private final String permission;
    private final int defaultMaxClaims;

    GradeType(String defaultDisplay, String defaultColor, String permission, int defaultMaxClaims) {
        this.defaultDisplay   = defaultDisplay;
        this.defaultColor     = defaultColor;
        this.permission       = permission;
        this.defaultMaxClaims = defaultMaxClaims;
    }

    private NationsPlugin plugin() { return NationsPlugin.getInstance(); }

    public String getDisplayName() {
        NationsPlugin p = plugin();
        if (p != null) return p.getConfigManager().getGradeDisplay(name().toLowerCase(), defaultDisplay);
        return defaultDisplay;
    }

    public String getColor() {
        NationsPlugin p = plugin();
        if (p != null) return p.getConfigManager().getGradeColorCode(name().toLowerCase(), defaultColor);
        return colorize(defaultColor);
    }

    public int getMaxClaims() {
        NationsPlugin p = plugin();
        if (p != null) return p.getConfigManager().getMaxClaimsForGrade(name().toLowerCase());
        return defaultMaxClaims;
    }

    public String getPermission() { return permission; }

    public String getColoredDisplay() {
        return getColor() + getDisplayName();
    }

    public static GradeType fromPermission(org.bukkit.entity.Player player) {
        if (player.hasPermission("nations.grade.roi"))       return ROI;
        if (player.hasPermission("nations.grade.chevalier")) return CHEVALIER;
        if (player.hasPermission("nations.grade.premium"))   return PREMIUM;
        if (player.hasPermission("nations.grade.soutien"))   return SOUTIEN;
        return JOUEUR;
    }

    private static String colorize(String s) {
        return s == null ? "" : s.replace("&", "§");
    }
}
