package me.makkuusen.timing.system;

import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.UUID;

public class Tasks {

    public Tasks(TimingSystem plugin){

        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();

        scheduler.scheduleSyncRepeatingTask(plugin, new Runnable()
        {
            @Override
            public void run() {
                for (UUID uuid : TimingSystem.playerEditingSession.keySet()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) continue;
                    Track track = TimingSystem.playerEditingSession.get(uuid);
                    TrackRegion startRegion = track.getStartRegion();
                    setParticles(player, startRegion, Particle.VILLAGER_HAPPY);

                    TrackRegion endRegion = track.getEndRegion();
                    if(!(startRegion.getMinP().equals(endRegion.getMinP()) && startRegion.getMaxP().equals(endRegion.getMaxP()))) {
                        setParticles(player, endRegion, Particle.VILLAGER_ANGRY);
                    }

                    TrackRegion pitRegion = track.getPitRegion();
                    if (pitRegion != null) {
                        setParticles(player, pitRegion, Particle.HEART);
                    }

                    track.getGridRegions().values().forEach(trackRegion -> setParticles(player, trackRegion, Particle.WAX_OFF));
                    track.getCheckpoints().values().forEach(trackRegion -> setParticles(player, trackRegion, Particle.GLOW));
                    track.getResetRegions().values().forEach(trackRegion -> setParticles(player, trackRegion, Particle.WAX_ON));
                }
            }
        }, 0, 10);
    }

    private void setParticles(Player player, TrackRegion region, Particle particle){

        if(region.getMinP() == null || region.getMaxP() == null){
            return;
        }

        Location min = region.getMinP();
        Location max = region.getMaxP();

        for (int x = min.getBlockX(); x <= max.getBlockX() + 1; x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY() + 1; y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ() + 1; z++) {
                     if (x == min.getBlockX() || x == max.getBlockX() + 1 || y == min.getBlockY() || y == max.getBlockY() + 1 || z == min.getBlockZ() || z == max.getBlockZ() + 1) {
                        player.spawnParticle(particle, x, y, z, 1);
                     }
                }
            }
        }

    }
}


