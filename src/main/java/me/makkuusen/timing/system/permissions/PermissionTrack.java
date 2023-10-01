package me.makkuusen.timing.system.permissions;

import co.aikar.commands.CommandReplacements;

public enum PermissionTrack implements Permissions {
    MENU,
    MOVE,
    TP,
    CREATE,
    INFO,
    VIEW_REGIONS,
    VIEW_LOCATIONS,
    VIEW_HERE,
    VIEW_OPTIONS,
    VIEW_MYTIMES,
    VIEW_TIMES,
    VIEW_ALLTIMES,
    VIEW_PLAYERSTATS,
    SESSION_TIMETRAIL,
    SESSION_EDIT,
    RELOAD,
    DELETE_TRACK,
    DELETE_BESTTIME,
    DELETE_ALLTIMES,
    UPDATELEADERBOARDS,
    SET_FINISHTP,
    SET_OPEN,
    SET_WEIGHT,
    SET_TAG,
    SET_TYPE,
    SET_MODE,
    SET_BOATUTILSMODE,
    SET_NAME,
    SET_ITEM,
    SET_OWNER,
    SET_REGION_START,
    SET_REGION_END,
    SET_REGION_PIT,
    SET_REGION_INPIT,
    SET_REGION_RESET,
    SET_REGION_LAGSTART,
    SET_REGION_LAGEND,
    SET_REGION_CHECKPOINT,
    SET_LOCATION_SPAWN,
    SET_LOCATION_LEADERBOARD,
    SET_LOCATION_GRID,
    SET_LOCATION_QUALIGRID;

    @Override
    public String getNode() {
        return getNodeBase() + this.toString().replace("_", ".").toLowerCase();
    }

    public static String getNodeBase() {
        return "timingsystem.track.";
    }

    public static void init(CommandReplacements replacements) {
        for(PermissionTrack perm : values()) {
            Permissions.register(perm, replacements);
        }
    }
}
