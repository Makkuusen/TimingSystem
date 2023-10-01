package me.makkuusen.timing.system.permissions;

import co.aikar.commands.CommandReplacements;

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

    public static void init(CommandReplacements replacements) {
        for(PermissionRound perm : values()) {
            Permissions.register(perm, replacements);
        }
    }
}
