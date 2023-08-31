package me.makkuusen.timing.system;

import me.makkuusen.timing.system.boatutils.BoatUtilsManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class PluginMessageReceiver implements PluginMessageListener {
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (channel.equalsIgnoreCase("openboatutils:settings")){
            BoatUtilsManager.pluginMessageListener(channel, player, message);
            return;
        }

        System.out.println("Unknown channel: " + channel);
    }
}
