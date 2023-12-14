package me.makkuusen.timing.system.permissions;

import co.aikar.commands.CommandReplacements;

public enum PermissionTrackEdit implements Permissions{
    INFO,
    SELECT,
    CREATE,
    MOVE,
    VIEW,
    DELETE_TRACK,
    OPEN,
    CLOSE,
    ITEM,
    NAME,
    WEIGHT,
    OWNER,
    SPAWN,
    REGIONSPAWN,
    CONTRIBUTORS,
    BOATUTILSMODE,
    TYPE,
    OPTION,
    TAG,
    OVERLOAD,
    REGION,
    LOCATION;

    @Override
    public String getNode() {
        return "timingsystem.track." + this.toString().replace("_", ".").toLowerCase();
    }

    public static void init(CommandReplacements replacements) {
        for(PermissionTrackEdit perm : values()) {
            Permissions.register(perm, replacements);
        }
    }
}
