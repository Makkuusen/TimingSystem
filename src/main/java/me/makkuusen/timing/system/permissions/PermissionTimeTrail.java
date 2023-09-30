package me.makkuusen.timing.system.permissions;

public enum PermissionTimeTrail implements Permissions {
    MENU,
    CANCEL,
    RANDOM;

    @Override
    public final String getNode() {
        return getNodeBase() + this.toString().replace("_", ".").toLowerCase();
    }

    public static String getNodeBase() {
        return "timingsystem.timetrail.";
    }
}
