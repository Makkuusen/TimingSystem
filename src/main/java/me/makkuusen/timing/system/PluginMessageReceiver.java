package me.makkuusen.timing.system;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
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

        if(channel.equalsIgnoreCase("oinkscoreboard:settings")) {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            short packetID = in.readByte();
            if (packetID == 0) {
                TPlayer tPlayer = Database.getPlayer(player.getUniqueId());
                tPlayer.setOinkScoreboardRows(in.readByte());
                System.out.println(player.getName() + " " + tPlayer.getOinkScoreboardRows());
            }
            return;
        }

        System.out.println("Unknown channel: " + channel);
    }
}
