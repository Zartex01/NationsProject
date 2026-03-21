package fr.nations.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuiManager {

    private static final Map<UUID, Object> openGuis = new HashMap<>();
    private static final Map<UUID, String> pendingActions = new HashMap<>();

    public static void registerGui(UUID playerId, Object gui) {
        openGuis.put(playerId, gui);
    }

    public static Object getOpenGui(UUID playerId) {
        return openGuis.get(playerId);
    }

    public static void closeGui(UUID playerId) {
        openGuis.remove(playerId);
    }

    public static void setPendingAction(UUID playerId, String action) {
        pendingActions.put(playerId, action);
    }

    public static String getPendingAction(UUID playerId) {
        return pendingActions.get(playerId);
    }

    public static void clearPendingAction(UUID playerId) {
        pendingActions.remove(playerId);
    }

    public static boolean hasPendingAction(UUID playerId) {
        return pendingActions.containsKey(playerId);
    }
}
