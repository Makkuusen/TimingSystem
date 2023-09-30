package me.makkuusen.timing.system.permissions;

public enum PermissionTrackExchange implements Permissions {
    CUT,
    COPY,
    PASTE,
    PASTEOUTDATED;

    @Override
    public String getNode() {
        return getNodeBase() + this.toString().replace("_", ".").toLowerCase();
    }

    public static String getNodeBase() {
        return "timingsystem.trackexchange.";
    }
}
