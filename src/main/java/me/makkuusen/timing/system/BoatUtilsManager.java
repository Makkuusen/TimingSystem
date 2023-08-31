package me.makkuusen.timing.system;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BoatUtilsManager {

    public static Map<UUID, BoatUtilsMode> playerBoatUtilsMode = new HashMap<>();

    public static void sendBoatUtilsModePluginMessage(Player p, BoatUtilsMode mode){

        //Currently disable for non-ops
        if (!p.isOp()) {
            return;
        }
        if (playerBoatUtilsMode.get(p.getUniqueId()) != null && playerBoatUtilsMode.get(p.getUniqueId()) == mode) {
            return;
        }
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            if (mode == BoatUtilsMode.STANDARD) {
                out.writeShort(0);
            } else {
                out.writeShort(8);
                out.writeShort(mode.getId());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        p.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", b.toByteArray());
        if (p.getName().contains("Makkuusen")) {
            p.sendMessage(Component.text("Applied: " + mode.name()));
        }
        playerBoatUtilsMode.put(p.getUniqueId(), mode);
    }


    public static ContextResolver<BoatUtilsMode, BukkitCommandExecutionContext> getBoatUtilsModeContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            try {
                return BoatUtilsMode.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                //no matching boat types
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }
}
