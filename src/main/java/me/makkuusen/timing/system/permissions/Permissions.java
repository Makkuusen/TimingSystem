package me.makkuusen.timing.system.permissions;

import co.aikar.commands.CommandReplacements;

public interface Permissions {

    String getNode();

    static <T extends Permissions> void register(T perm, CommandReplacements replacements) {
        replacements.addReplacement(perm.getClass().getSimpleName().toLowerCase() + "_" + perm.toString().toLowerCase(), perm.getNode());
    }
}
