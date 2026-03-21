package fr.nations.role;

public enum RolePermission {

    BUILD("perm_build", "Construire/Détruire dans les territoires de la nation", "§aConstruction"),
    INVITE("perm_invite", "Inviter des joueurs dans la nation", "§aInvitations"),
    KICK("perm_kick", "Expulser des membres", "§6Expulsion"),
    MANAGE_WAR("perm_manage_war", "Déclarer et gérer les guerres", "§cGuerres"),
    MANAGE_BANK("perm_manage_bank", "Déposer/retirer de la banque nationale", "§6Banque"),
    MANAGE_CLAIMS("perm_manage_claims", "Claim et unclaim des territoires", "§aTerrains"),
    MANAGE_ALLIES("perm_manage_allies", "Gérer les alliances", "§9Alliances"),
    MANAGE_ROLES("perm_manage_roles", "Créer/modifier les rôles personnalisés", "§dRôles"),
    RENAME("perm_rename", "Renommer / changer la description de la nation", "§7Renommer"),
    DISBAND("perm_disband", "Dissoudre la nation", "§4Dissolution");

    private final String columnName;
    private final String description;
    private final String label;

    RolePermission(String columnName, String description, String label) {
        this.columnName = columnName;
        this.description = description;
        this.label = label;
    }

    public String getColumnName() { return columnName; }
    public String getDescription() { return description; }
    public String getLabel() { return label; }
}
