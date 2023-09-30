package me.makkuusen.timing.system.permissions;

public enum PermissionRound implements Permissions {
    INFO,
    LIST,
    RESULTS,
    CREATE,
    FINISH,
    DELETE,
    FILLHEATS,
    REMOVEDRIVERS;

    @Override
    public String getNode() {
        return getNodeBase() + this.toString().replace("_", ".").toLowerCase();
    }

    public static String getNodeBase() {
        return "timingsystem.round.";
    }
}
