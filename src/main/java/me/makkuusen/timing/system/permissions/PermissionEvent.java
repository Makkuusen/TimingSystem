package me.makkuusen.timing.system.permissions;

import co.aikar.commands.CommandReplacements;

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
        return "timingsystem.event." + this.toString().replace("_", ".").toLowerCase();
    }

    public static void init(CommandReplacements replacements) {
        for(PermissionEvent perm : values()) {
            Permissions.register(perm, replacements);
        }
    }
}
