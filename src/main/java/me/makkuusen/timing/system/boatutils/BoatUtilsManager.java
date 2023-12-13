package me.makkuusen.timing.system.boatutils;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Hover;
import me.makkuusen.timing.system.theme.messages.Warning;
import me.makkuusen.timing.system.track.Track;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class BoatUtilsManager {

    public static Map<UUID, BoatUtilsMode> playerBoatUtilsMode = new HashMap<>();

    public static void pluginMessageListener(@NotNull String channel, @NotNull Player player, byte[] message) {
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        short packetID = in.readShort();
        if (packetID == 0) {
            int version = in.readInt();
            TPlayer tPlayer = TSDatabase.getPlayer(player.getUniqueId());
            tPlayer.setBoatUtilsVersion(version);
        }
    }

    public static void sendBoatUtilsModePluginMessage(Player player, BoatUtilsMode mode, Track track, boolean sameAsLastTrack){
        TPlayer tPlayer = TSDatabase.getPlayer(player.getUniqueId());
        if (mode != BoatUtilsMode.VANILLA) {
            if (!tPlayer.hasBoatUtils()) {
                if (track != null) {
                    if (track.isBoatUtils() && !track.getTimeTrials().hasPlayedTrack(tPlayer) && !sameAsLastTrack) {
                        var boatUtilsWarning = tPlayer.getTheme().warning(">> ").append(Text.get(player, Warning.TRACK_REQUIRES_BOAT_UTILS)).append(tPlayer.getTheme().warning(" <<"))
                                .hoverEvent(HoverEvent.showText(Text.get(player, Hover.CLICK_TO_OPEN)))
                                .clickEvent(ClickEvent.openUrl("https://modrinth.com/mod/openboatutils"));
                        player.sendMessage(boatUtilsWarning);
                    }
                }
            } else {
                // Need to update OpenBoatUtils
                if (tPlayer.getBoatUtilsVersion() < mode.getRequiredVersion()) {
                    var boatUtilsWarning = tPlayer.getTheme().warning(">> ").append(Text.get(player, Warning.TRACK_REQUIRES_NEWER_BOAT_UTILS)).append(tPlayer.getTheme().warning(" <<"))
                            .hoverEvent(HoverEvent.showText(Text.get(player, Hover.CLICK_TO_OPEN)))
                            .clickEvent(ClickEvent.openUrl("https://modrinth.com/mod/openboatutils"));
                    player.sendMessage(boatUtilsWarning);
                    return;
                }
            }
        }

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            if (mode == BoatUtilsMode.VANILLA) {
                out.writeShort(0);
            } else {
                out.writeShort(8);
                out.writeShort(mode.getId());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", b.toByteArray());
        if (tPlayer.isVerbose() && !(playerBoatUtilsMode.get(player.getUniqueId()) != null && playerBoatUtilsMode.get(player.getUniqueId()) == mode)) {
            player.sendMessage(Component.text("BU Mode: " + mode.name(), tPlayer.getTheme().getPrimary()));
        }
        playerBoatUtilsMode.put(player.getUniqueId(), mode);
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
    public static List<BoatUtilsMode> getAvailableModes(int version) {
        return Arrays.stream(BoatUtilsMode.values()).filter(mode -> mode.getRequiredVersion() <= version).toList();
    }
}
