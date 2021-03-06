package me.makkuusen.timing.system;

import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.heat.Lap;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.FinalDriver;
import me.makkuusen.timing.system.race.Race;
import me.makkuusen.timing.system.race.RaceController;
import me.makkuusen.timing.system.race.RaceLap;
import me.makkuusen.timing.system.timetrial.TimeTrial;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackRegion;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

import java.time.Instant;
import java.util.Iterator;

public class TSListener implements Listener {

    static TimingSystem plugin;

    @EventHandler
    public void onTick(ServerTickStartEvent e) {
        TimingSystem.currentTime = Instant.now();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {

        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {

            TPlayer TPlayer = Database.getPlayer(event.getUniqueId(), event.getName());

            if (TPlayer == null) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Din spelarprofil kunde inte laddas.");
                return;
            }
        }
    }

    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event) {
        TPlayer TPlayer = Database.getPlayer(event.getPlayer().getUniqueId());

        TPlayer.setPlayer(event.getPlayer());

        if (!TPlayer.getName().equals(event.getPlayer().getName())) {
            // Update name
            TPlayer.setName(event.getPlayer().getName());
            TPlayer.updateNameChanges();
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        TimeTrialController.playerLeavingMap(e.getEntity().getUniqueId());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {

        for (me.makkuusen.timing.system.track.Track Track : DatabaseTrack.getTracks()) {
            if (Track.getSpawnLocation().getWorld() == event.getTo().getWorld()) {
                if (Track.getSpawnLocation().distance(event.getTo()) < 1 && event.getPlayer().getGameMode() != GameMode.SPECTATOR) {
                    if (Track.isBoatTrack()) {
                        Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(), () -> ApiUtilities.spawnBoat(event.getPlayer(), Track.getSpawnLocation()), 1);
                    }
                }
            }
        }

        if (!event.getCause().equals(PlayerTeleportEvent.TeleportCause.UNKNOWN)) {
            TimeTrialController.playerLeavingMap(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        TimeTrialController.playerLeavingMap(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent e) {
        if (!e.getVehicle().getPassengers().isEmpty()) {
            var passenger = e.getVehicle().getPassengers().get(0);
            if (passenger instanceof Player player) {
                if (TimeTrialController.timeTrials.containsKey(player.getUniqueId())) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {

        if (event.getVehicle() instanceof Boat && event.getVehicle().hasMetadata("spawned")) {
            if (event.getExited() instanceof Player player) {
                var maybeDriver = EventDatabase.getDriverFromRunningHeat(player.getUniqueId());
                if (maybeDriver.isPresent()) {
                    if (maybeDriver.get().getHeat().getHeatState() == HeatState.LOADED) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }

            if (event.getVehicle().getPassengers().size() < 2) {
                Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(), () -> event.getVehicle().remove(), 10);
            }
        }


        if (event.getExited() instanceof Player player) {
            if (TimeTrialController.timeTrials.containsKey(player.getUniqueId())) {
                Track track = TimeTrialController.timeTrials.get(player.getUniqueId()).getTrack();
                if (track.hasOption('b')) {
                    plugin.sendMessage(player, "messages.error.leftBoat");
                    TimeTrialController.playerLeavingMap(player.getUniqueId());
                }
            }

        }
    }

    @EventHandler
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        if (event.getVehicle() instanceof Boat && event.getVehicle().hasMetadata("spawned")) {
            event.getVehicle().remove();
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByBlock(EntityDamageByBlockEvent event) {
        if (event.getEntity() instanceof Player && event.getEntity().isInsideVehicle() && event.getEntity().getVehicle().getType() == EntityType.BOAT && event.getEntity().getVehicle().hasMetadata("spawned")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        PlayerRegionData.instanceOf(player).remove();
    }

    @EventHandler
    public void onPlayerFishEvent(PlayerFishEvent e) {
        if (e.getHook().getHookedEntity() instanceof Player hooked) {
            if (TimeTrialController.timeTrials.containsKey(hooked.getUniqueId())) {
                e.getPlayer().sendMessage("??cDu f??r inte kroka n??gon annan");
                e.setCancelled(true);
                return;
            }
        }

        if (e.getCaught() instanceof Player player) {
            if (TimeTrialController.timeTrials.containsKey(player.getUniqueId())) {
                e.getPlayer().sendMessage("??cDu f??r inte fiska n??gon annan");
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if (TimeTrialController.timeTrials.containsKey(player.getUniqueId())) {
            Track track = TimeTrialController.timeTrials.get(player.getUniqueId()).getTrack();
            if (player.getInventory().getChestplate() != null && player.getInventory().getChestplate().getType().equals(Material.ELYTRA) && track.hasOption('e')) {
                player.sendMessage("??cDu f??r inte ha elytra p?? den h??r banan.");
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            } else if (!player.isGliding() && track.hasOption('g')) {
                player.sendMessage("??cDu slutade flyga och tiden avbr??ts.");
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            } else if (player.getActivePotionEffects().size() > 0 && track.hasOption('p')) {
                player.sendMessage("??cDu f??r inte ha effekter p?? den h??r banan.");
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            } else if (player.isRiptiding() && track.hasOption('t')) {
                player.sendMessage("??cDu f??r inte anv??nda trident p?? den h??r banan.");
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            } else if (player.getInventory().getBoots() != null && player.getInventory().getBoots().containsEnchantment(Enchantment.SOUL_SPEED) && track.hasOption('s')) {
                player.sendMessage("??cDu f??r inte ha sj??lhastighet p?? dina skor.");
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            }

        }
    }

    @EventHandler
    public void onRegionEnterV2(PlayerMoveEvent e) {
        if ((int) (e.getFrom().getX() - 0.5) != (int) e.getTo().getX() || (int) e.getFrom().getY() != (int) e.getTo().getY() || (int) (e.getFrom().getZ() - 0.5) != (int) e.getTo().getZ()) {
            Player player = e.getPlayer();
            TPlayer tPlayer = Database.getPlayer(player.getUniqueId());

            var maybeDriver = EventDatabase.getDriverFromRunningHeat(tPlayer.getUniqueId());
            if (maybeDriver.isPresent()) {
                handleHeat(maybeDriver.get(), player);
                return;
            }

            var maybeRaceDriver = RaceController.getDriverFromActiveRace(tPlayer);
            if (maybeRaceDriver.isPresent()) {
                var race = maybeRaceDriver.get();
                handleRace(race, player);
                return;
            }
            if (TimeTrialController.timeTrials.containsKey(player.getUniqueId())) {
                handleTimeTrials(player);
                // don't need to check for starting new track
                return;
            }

            // Check for starting new tracks
            Iterator regions = DatabaseTrack.getTrackStartRegions().iterator();
            while (true) {
                Integer regionId;
                TrackRegion region;
                do {
                    label:
                    do {
                        while (regions.hasNext()) {
                            region = (TrackRegion) regions.next();
                            regionId = region.getId();
                            if (region.contains(player.getLocation())) {
                                continue label;
                            }
                            // Leaving Region
                            PlayerRegionData.instanceOf(player).getEntered().remove(regionId);
                        }

                        return;
                    } while (!player.getWorld().getName().equals(region.getWorldName()));
                } while (PlayerRegionData.instanceOf(player).getEntered().contains(regionId));

                //Entering region
                var maybeTrack = DatabaseTrack.getTrackById(region.getTrackId());
                if (maybeTrack.isPresent()) {
                    Track track_ = maybeTrack.get();

                    if (track_.getMode().equals(Track.TrackMode.TIMETRIAL)) {
                        TimeTrial timeTrial = new TimeTrial(track_, tPlayer);
                        timeTrial.playerStartingMap();
                    }
                }
                PlayerRegionData.instanceOf(player).getEntered().add(regionId);
            }
        }
    }

    @EventHandler
    void onPlayerQuit(PlayerQuitEvent event) {
        TPlayer TPlayer = Database.getPlayer(event.getPlayer());
        // Set to offline
        TPlayer.setPlayer(null);
    }

    void handleTimeTrials(Player player) {
        TimeTrial timeTrial = TimeTrialController.timeTrials.get(player.getUniqueId());
        // Check for ending current map.
        var track = timeTrial.getTrack();

        if (track.getStartRegion().contains(player.getLocation()) && track.getEndRegion().contains(player.getLocation())) {
            if (timeTrial.getLatestCheckpoint() != 0) {
                timeTrial.playerRestartMap();
                return;
            }
        } else if (track.getEndRegion().contains(player.getLocation())) {
            timeTrial.playerEndedMap();
            return;
        }

        // Check reset regions
        for (TrackRegion r : track.getResetRegions().values()) {
            if (r.contains(player.getLocation())) {
                timeTrial.playerResetMap();
            }
        }
        // Check for next checkpoint in current map
        int nextCheckpoint = timeTrial.getNextCheckpoint();
        if (nextCheckpoint == timeTrial.getLatestCheckpoint()) {
            return;
        }
        var checkpoint = track.getCheckpoints().get(nextCheckpoint);
        if (checkpoint.contains(player.getLocation())) {
            timeTrial.playerPassingCheckpoint(nextCheckpoint);
        }
    }

    private void handleRace(Race race, Player player) {
        var track = race.getTrack();

        if (!race.getRaceState().equals(HeatState.RACING)) {
            return;
        }
        var raceDriver = race.getRaceDriver(player.getUniqueId());
        if (raceDriver.isFinished()) {
            return;
        }
        if (track.getStartRegion().contains(player.getLocation())) {
            if (!raceDriver.isRunning()) {
                raceDriver.start();
            } else if (raceDriver.getLatestCheckpoint() != 0) {


                if (!raceDriver.hasPassedAllCheckpoints()) {
                    int checkpoint = raceDriver.getLatestCheckpoint();
                    if (race.getTrack().hasOption('c')) {
                        player.teleport(race.getTrack().getCheckpoints().get(checkpoint).getSpawnLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                        if (track.isBoatTrack()) {
                            Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(), () -> ApiUtilities.spawnBoat(player, race.getTrack().getCheckpoints().get(checkpoint).getSpawnLocation()), 1);
                        }
                    }
                    plugin.sendMessage(raceDriver.getTSPlayer().getPlayer(), "messages.error.timer.missedCheckpoints");
                    return;
                }
                race.passLap(player.getUniqueId());
            }

        }


        if (raceDriver.isRunning()) {

            RaceLap lap = raceDriver.getCurrentLap();

            // Check for pitstop
            if (track.getPitRegion() != null && track.getPitRegion().contains(player.getLocation())) {
                raceDriver.passPit();
            }

            // Check for next checkpoint in current map
            if (lap.hasPassedAllCheckpoints()) {
                return;
            }
            var checkpoint = track.getCheckpoints().get(lap.getNextCheckpoint());
            if (checkpoint.contains(player.getLocation())) {
                race.passNextCheckpoint(raceDriver);
            }
        }

    }

    private void handleHeat(Driver driver, Player player) {
        Heat heat = driver.getHeat();

        if (!heat.getHeatState().equals(HeatState.RACING)) {
            return;
        }

        if (driver.isFinished()) {
            return;
        }
        var track = heat.getEvent().getTrack();
        if (track.getStartRegion().contains(player.getLocation())) {
            if (!driver.isRunning()) {
                driver.start();
                heat.updatePositions();
                ApiUtilities.msgConsole("Starting :" + player.getName());
                return;
            } else if (driver.getCurrentLap().getLatestCheckpoint() != 0) {

                if (!driver.getCurrentLap().hasPassedAllCheckpoints()) {
                    int checkpoint = driver.getCurrentLap().getLatestCheckpoint();
                    if (track.hasOption('c')) {
                        player.teleport(track.getCheckpoints().get(checkpoint).getSpawnLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                        if (track.isBoatTrack()) {
                            Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(), () -> ApiUtilities.spawnBoat(player, track.getCheckpoints().get(checkpoint).getSpawnLocation()), 1);
                        }
                    }
                    plugin.sendMessage(driver.getTPlayer().getPlayer(), "messages.error.timer.missedCheckpoints");
                    return;
                }
                heat.passLap(driver);
                heat.updatePositions();
                return;
            }

        }


        if (driver.isRunning()) {
            Lap lap = driver.getCurrentLap();

            if (driver instanceof FinalDriver finalDriver) {
                // Check for pitstop
                if (track.getPitRegion() != null && track.getPitRegion().contains(player.getLocation())) {
                    finalDriver.passPit();
                }
            }

            // Check for next checkpoint in current map

            if (lap.hasPassedAllCheckpoints()) {
                return;
            }
            var checkpoint = track.getCheckpoints().get(lap.getNextCheckpoint());
            if (checkpoint.contains(player.getLocation())) {
                lap.passNextCheckpoint(TimingSystem.currentTime);
                heat.updatePositions();
            }
        }
    }
}
