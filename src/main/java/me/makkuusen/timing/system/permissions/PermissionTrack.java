package me.makkuusen.timing.system.permissions;

import co.aikar.commands.CommandReplacements;

public enum PermissionTrack implements Permissions {
    MENU,
    TP,
    INFO,
    VIEW_REGIONS,
    VIEW_LOCATIONS,
    VIEW_HERE,
    VIEW_OPTIONS,
    VIEW_MYTIMES,
    VIEW_TIMES,
    VIEW_ALLTIMES,
    VIEW_PLAYERSTATS,
    SESSION_TIMETRIAL,
    RELOAD,
    DELETE_BESTTIME,
    DELETE_ALLTIMES,
    UPDATELEADERBOARDS,
    SET_MODE;

    @Override
    public String getNode() {
        return "timingsystem.track." + this.toString().replace("_", ".").toLowerCase();
    }

    public static void init(CommandReplacements replacements) {
        for(PermissionTrack perm : values()) {
            Permissions.register(perm, replacements);
        }
    }
}
