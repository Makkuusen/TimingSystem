package me.makkuusen.timing.system.permissions;

public enum PermissionEvent implements Permissions {
    LIST,
    INFO,
    START,
    FINISH,
    CREATE,
    DELETE,
    SELECT,
    SET_TRACK,
    SET_SIGNS,
    SPECTATE,
    SIGN,
    SIGNOTHERS,
    LISTSIGNS,
    RESERVE,
    BROADCAST_CLICKTOSIGN,
    BROADCAST_CLICKTORESERVE;

    @Override
    public String getNode() {
        return getNodeBase() + this.toString().replace("_", ".").toLowerCase();
    }

    public static String getNodeBase() {
        return "timingsystem.event.";
    }
}
