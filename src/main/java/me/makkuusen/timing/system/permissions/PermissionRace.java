package me.makkuusen.timing.system.permissions;

import co.aikar.commands.CommandReplacements;

public enum PermissionRace implements Permissions {
    START,
    END,
    CREATE,
    JOIN,
    LEAVE;

    @Override
    public String getNode() {
        return "timingsystem.race." + this.toString().replace("_", ".").toLowerCase();
    }

    public static void init(CommandReplacements replacements) {
        for(PermissionRace perm : values()) {
            Permissions.register(perm, replacements);
        }
    }
}
