package me.makkuusen.timing.system.permissions;

import co.aikar.commands.CommandReplacements;

public enum PermissionTrackExchange implements Permissions {
    CUT,
    COPY,
    PASTE,
    PASTEOUTDATED;

    @Override
    public String getNode() {
        return getNodeBase() + this.toString().replace("_", ".").toLowerCase();
    }

    public static String getNodeBase() {
        return "timingsystem.trackexchange.";
    }

    public static void init(CommandReplacements replacements) {
        for(PermissionTrackExchange perm : values()) {
            Permissions.register(perm, replacements);
        }
    }
}
