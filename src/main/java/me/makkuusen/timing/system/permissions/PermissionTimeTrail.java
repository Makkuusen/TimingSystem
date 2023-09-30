package me.makkuusen.timing.system.permissions;

public enum PermissionTimeTrail implements Permissions {
    MENU,
    TELEPORT,
    CANCEL,
    RANDOM;

    @Override
    public final String getNode() {
        return getNodeBase() + this.toString().toLowerCase();
    }

    public static String getNodeBase() {
        return "timingsystem.timetrail.";
    }
}
