package me.makkuusen.timing.system.permissions;

import co.aikar.commands.CommandReplacements;

public enum PermissionTimeTrail implements Permissions {
    MENU,
    CANCEL,
    RANDOM;

    @Override
    public final String getNode() {
        return "timingsystem.timetrail." + this.toString().replace("_", ".").toLowerCase();
    }

    public static void init(CommandReplacements replacements) {
        for(PermissionTimeTrail perm : values()) {
            Permissions.register(perm, replacements);
        }
    }
}
