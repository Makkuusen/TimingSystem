package me.makkuusen.timing.system.permissions;

public enum PermissionRace implements Permissions {
    START,
    END,
    CREATE,
    JOIN,
    LEAVE;

    @Override
    public String getNode() {
        return getNodeBase() + this.toString().replace("_", ".").toLowerCase();
    }

    public static String getNodeBase() {
        return "timingsystem.race.";
    }
}
