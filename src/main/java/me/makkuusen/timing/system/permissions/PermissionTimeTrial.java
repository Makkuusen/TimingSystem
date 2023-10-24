package me.makkuusen.timing.system.permissions;

import co.aikar.commands.CommandReplacements;

public enum PermissionTimeTrial implements Permissions {
    MENU,
    CANCEL,
    RANDOM;

    @Override
    public final String getNode() {
        return "timingsystem.timetrial." + this.toString().replace("_", ".").toLowerCase();
    }

    public static void init(CommandReplacements replacements) {
        for(PermissionTimeTrial perm : values()) {
            Permissions.register(perm, replacements);
        }
    }
}
