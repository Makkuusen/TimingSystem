package me.makkuusen.timing.system;

import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import me.makkuusen.timing.system.api.events.BoatSpawnEvent;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.heat.Lap;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.DriverState;
import me.makkuusen.timing.system.round.FinalRound;
import me.makkuusen.timing.system.timetrial.TimeTrial;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.makkuusen.timing.system.track.TrackRegion;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.persistence.PersistentDataType;

import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class TSListener implements Listener {

    static TimingSystem plugin;
    static Set<UUID> inPits = new HashSet();

    @EventHandler
    public void onTick(ServerTickStartEvent e) {
        TimingSystem.currentTime = Instant.now();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {

        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {

            TPlayer TPlayer = Database.getPlayer(event.getUniqueId(), event.getName());

            if (TPlayer == null) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Your player profile could not be loaded. Notify the server owner!");
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
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        TimeTrialController.playerLeavingMap(e.getEntity().getUniqueId());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {

        var maybeDriver = EventDatabase.getDriverFromRunningHeat(event.getPlayer().getUniqueId());
        if (maybeDriver.isPresent()) {
            if (maybeDriver.get().getState() == DriverState.LOADED) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getCause().equals(PlayerTeleportEvent.TeleportCause.PLUGIN) ||
                event.getCause().equals(PlayerTeleportEvent.TeleportCause.COMMAND) ||
                event.getCause().equals(PlayerTeleportEvent.TeleportCause.ENDER_PEARL) ||
                event.getCause().equals(PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) ||
                event.getCause().equals(PlayerTeleportEvent.TeleportCause.NETHER_PORTAL)) {
            TimeTrialController.playerLeavingMap(event.getPlayer().getUniqueId());
            if (ApiUtilities.hasBoatUtilsEffects(event.getPlayer())){
                ApiUtilities.removeBoatUtilsEffects(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        TimeTrialController.playerLeavingMap(e.getPlayer().getUniqueId());

        if (TimeTrialController.timeTrialSessions.containsKey(e.getPlayer().getUniqueId()) && e.getReason() != PlayerQuitEvent.QuitReason.TIMED_OUT) {
            var ttSession = TimeTrialController.timeTrialSessions.get(e.getPlayer().getUniqueId());
            ttSession.clearScoreboard();
            TimeTrialController.timeTrialSessions.remove(e.getPlayer().getUniqueId());
        }
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

        if (event.getVehicle() instanceof Boat boat && boat.getPersistentDataContainer().has(Objects.requireNonNull(NamespacedKey.fromString("spawned", plugin)))) {
            if (event.getExited() instanceof Player player) {
                var maybeDriver = EventDatabase.getDriverFromRunningHeat(player.getUniqueId());
                if (maybeDriver.isPresent()) {
                    if (maybeDriver.get().getState() == DriverState.LOADED) {
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

            var maybeDriver = EventDatabase.getDriverFromRunningHeat(player.getUniqueId());
            if (maybeDriver.isPresent()) {
                return;
            }

            if (TimeTrialController.timeTrials.containsKey(player.getUniqueId())) {
                Track track = TimeTrialController.timeTrials.get(player.getUniqueId()).getTrack();
                if (track.hasOption('b')) {
                    plugin.sendMessage(player, "messages.error.leftBoat");
                    TimeTrialController.playerLeavingMap(player.getUniqueId());
                    if (ApiUtilities.hasBoatUtilsEffects(player)){
                        ApiUtilities.removeBoatUtilsEffects(player);
                    }
                }
            } else {
                if (ApiUtilities.hasBoatUtilsEffects(player)){
                    ApiUtilities.removeBoatUtilsEffects(player);
                }
            }

        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        var maybeDriver = EventDatabase.getDriverFromRunningHeat(event.getPlayer().getUniqueId());
        if (maybeDriver.isPresent()) {
            if (maybeDriver.get().getState() == DriverState.LOADED) {
                event.setCancelled(true);
                return;
            }
        }
    }
    @EventHandler
    public void onBlockBreak(BlockPlaceEvent event) {
        var maybeDriver = EventDatabase.getDriverFromRunningHeat(event.getPlayer().getUniqueId());
        if (maybeDriver.isPresent()) {
            if (maybeDriver.get().getState() == DriverState.LOADED) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        var maybeDriver = EventDatabase.getDriverFromRunningHeat(event.getPlayer().getUniqueId());
        if (maybeDriver.isPresent()) {
            if (maybeDriver.get().getState() == DriverState.LOADED) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
        var maybeDriver = EventDatabase.getDriverFromRunningHeat(event.getPlayer().getUniqueId());
        if (maybeDriver.isPresent()) {
            if (maybeDriver.get().getState() == DriverState.LOADED) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
         if (event.getDamager() instanceof Player player) {
             var maybeDriver = EventDatabase.getDriverFromRunningHeat(player.getUniqueId());
             if (maybeDriver.isPresent()) {
                 if (maybeDriver.get().getState() == DriverState.LOADED) {
                     event.setCancelled(true);
                     return;
                 }
             }
         }
    }

    @EventHandler
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        if (event.getAttacker() instanceof Player player) {
            var maybeDriver = EventDatabase.getDriverFromRunningHeat(player.getUniqueId());
            if (maybeDriver.isPresent()) {
                if (maybeDriver.get().getState() == DriverState.LOADED) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
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
                e.getPlayer().sendMessage("§cDu får inte kroka någon annan");
                e.setCancelled(true);
                return;
            }
        }

        if (e.getCaught() instanceof Player player) {
            if (TimeTrialController.timeTrials.containsKey(player.getUniqueId())) {
                e.getPlayer().sendMessage("§cDu får inte fiska någon annan");
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
                player.sendMessage("§cDu får inte ha elytra på den här banan.");
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            } else if (!player.isGliding() && track.hasOption('g')) {
                player.sendMessage("§cDu slutade flyga och tiden avbröts.");
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            } else if (player.getActivePotionEffects().size() > 0 && track.hasOption('p')) {
                player.sendMessage("§cDu får inte ha effekter på den här banan.");
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            } else if (player.isRiptiding() && track.hasOption('t')) {
                player.sendMessage("§cDu får inte använda trident på den här banan.");
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            } else if (player.getInventory().getBoots() != null && player.getInventory().getBoots().containsEnchantment(Enchantment.SOUL_SPEED) && track.hasOption('s')) {
                player.sendMessage("§cDu får inte ha själhastighet på dina skor.");
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            } else if (ApiUtilities.hasBoatUtilsREffect(player) && !track.hasOption('r')) {
                player.sendMessage("§cYou are not allowed to use BoatUtils step assist on this track");
                ApiUtilities.removeBoatUtilsEffects(player);
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            } else if (ApiUtilities.hasBoatUtilsIEffect(player) && !track.hasOption('i')) {
                player.sendMessage("§cYou are not allowed to use BoatUtils slippery effect on this track");
                ApiUtilities.removeBoatUtilsEffects(player);
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            }

        }
    }

    @EventHandler
    public void onRegionEnter(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        var track = TimingSystem.playerEditingSession.get(player.getUniqueId());
        if (track == null) {
            return;
        }

        for (TrackRegion region : track.getRegions()) {
            if (region.contains(player.getLocation())) {
                ApiUtilities.sendActionBar("§a" + region.getRegionType() + " : " + region.getRegionIndex(), player);
                return;
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
                handleHeat(maybeDriver.get(), player, e.getFrom());
                return;
            }

            if (TimeTrialController.timeTrials.containsKey(player.getUniqueId())) {
                handleTimeTrials(player);
                // don't need to check for starting new track
                return;
            }

            // Check for starting new tracks
            Iterator regions = TrackDatabase.getTrackStartRegions().iterator();
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
                var maybeTrack = TrackDatabase.getTrackById(region.getTrackId());
                if (maybeTrack.isPresent()) {
                    Track track_ = maybeTrack.get();

                    if (track_.getMode().equals(Track.TrackMode.TIMETRIAL)) {
                        TimeTrial timeTrial = new TimeTrial(track_, tPlayer);
                        timeTrial.playerStartingMap();
                        if (track_.hasOption('i') && !ApiUtilities.hasBoatUtilsIEffect(player)) {
                            ApiUtilities.giveBoatUtilsIEffect(player);
                        }
                        if (track_.hasOption('r') && !ApiUtilities.hasBoatUtilsREffect(player)) {
                            ApiUtilities.giveBoatUtilsREffect(player);
                        }
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

        var startRegion = track.getRegion(TrackRegion.RegionType.START);
        var endRegion = track.getRegion(TrackRegion.RegionType.END);

        if (startRegion.isEmpty() /*|| endRegion.isEmpty()*/) {
            return;
        }

        if (endRegion.isEmpty()){
            if (startRegion.get().contains(player.getLocation())) {
                if (timeTrial.getLatestCheckpoint() != 0) {
                    timeTrial.playerRestartMap();
                    return;
                }
            }
        } else {
            if (startRegion.get().contains(player.getLocation()) && endRegion.get().contains(player.getLocation())) {
                if (timeTrial.getLatestCheckpoint() != 0) {
                    timeTrial.playerRestartMap();
                    return;
                }
            } else if (endRegion.get().contains(player.getLocation())){
                timeTrial.playerEndedMap();
                return;
            }
        }


        for (TrackRegion r : track.getRegions(TrackRegion.RegionType.RESET)) {
            if (r.contains(player.getLocation())) {
                if (!track.hasOption('c')) {
                    timeTrial.playerResetMap();
                } else {
                    var maybeRegion = track.getRegion(TrackRegion.RegionType.CHECKPOINT, timeTrial.getLatestCheckpoint());
                    if (maybeRegion.isEmpty()){
                        timeTrial.playerResetMap();
                        return;
                    }
                    ApiUtilities.teleportPlayerAndSpawnBoat(player, track, maybeRegion.get().getSpawnLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                }
                return;
            }
        }

        if (!timeTrial.isLagStart() && track.hasRegion(TrackRegion.RegionType.LAGSTART)) {
            if (track.getRegion(TrackRegion.RegionType.LAGSTART).get().contains(player.getLocation())) {
                timeTrial.setLagStartTrue();
                if (ApiUtilities.getRoundedToTick(timeTrial.getTimeSinceStart(TimingSystem.currentTime)) == 0) {
                    player.sendMessage("§cTimingSystem detected some lag and unfortunately your time has been invalidated.");
                    plugin.logger.warning(player.getName() + " failed lagstart on " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeTrial.getTimeSinceStart(TimingSystem.currentTime)));
                    timeTrial.playerResetMap();
                    return;
                } else {
                    ApiUtilities.msgConsole(player.getName() + " passed lagstart on " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeTrial.getTimeSinceStart(TimingSystem.currentTime)));
                }
            }
        }

        if (!timeTrial.isLagEnd() && track.hasRegion(TrackRegion.RegionType.LAGEND)) {
            if (track.getRegion(TrackRegion.RegionType.LAGEND).get().contains(player.getLocation())) {
                timeTrial.setLagEnd(true);
                if (!timeTrial.isLagStart()) {
                    player.sendMessage("§cTimingSystem detected some lag and unfortunately your time has been invalidated.");
                    plugin.logger.warning(player.getName() + " failed lagend on " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeTrial.getTimeSinceStart(TimingSystem.currentTime)));
                    timeTrial.playerResetMap();
                    return;
                } else if (TimingSystem.currentTime.toEpochMilli() == timeTrial.getLagStart().toEpochMilli()) {
                    player.sendMessage("§cTimingSystem detected some lag and unfortunately your time has been invalidated.");
                    plugin.logger.warning(player.getName() + " failed lagend on " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeTrial.getTimeSinceStart(TimingSystem.currentTime)));
                    timeTrial.playerResetMap();
                    return;
                } else {
                    ApiUtilities.msgConsole(player.getName() + " passed lagend on " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeTrial.getTimeSinceStart(TimingSystem.currentTime)));
                }
            }
        }

        // Check for next checkpoint in current map
        int nextCheckpoint = timeTrial.getNextCheckpoint();
        if (nextCheckpoint == timeTrial.getLatestCheckpoint()) {
            return;
        }
        var checkpoint = track.getRegions(TrackRegion.RegionType.CHECKPOINT).stream().filter(trackRegion -> trackRegion.getRegionIndex() == nextCheckpoint).findFirst();
        if (checkpoint.isPresent() && checkpoint.get().contains(player.getLocation())) {
            timeTrial.playerPassingCheckpoint(nextCheckpoint);
        }
    }

    private void handleHeat(Driver driver, Player player, Location from) {
        Heat heat = driver.getHeat();

        if (!heat.getHeatState().equals(HeatState.RACING)) {
            return;
        }

        if (driver.isFinished()) {
            return;
        }
        var track = heat.getEvent().getTrack();

        var startRegion = track.getRegion(TrackRegion.RegionType.START);
        if (startRegion.isEmpty()) {
            return;
        }

        if (startRegion.get().contains(player.getLocation())) {
            if (driver.getState() == DriverState.STARTING) {
                driver.start();
                heat.updatePositions();
                ApiUtilities.msgConsole("Starting : " + player.getName() + " in " + heat.getName());
                return;
            } else if (driver.getCurrentLap().getLatestCheckpoint() != 0) {
                if (!driver.getCurrentLap().hasPassedAllCheckpoints()) {
                    int checkpoint = driver.getCurrentLap().getLatestCheckpoint();
                    var maybeCheckpoint = track.getRegions(TrackRegion.RegionType.CHECKPOINT).stream().filter(trackRegion -> trackRegion.getRegionIndex() == checkpoint).findFirst();
                    if (maybeCheckpoint.isPresent()) {
                        ApiUtilities.teleportPlayerAndSpawnBoat(player, track, maybeCheckpoint.get().getSpawnLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                    }
                    plugin.sendMessage(driver.getTPlayer().getPlayer(), "messages.error.timer.missedCheckpoints");
                    return;
                }
                heat.passLap(driver);
                heat.updatePositions();
                return;
            }
        }

        if (track.isStage()) {
            if (track.getRegion(TrackRegion.RegionType.END).get().contains(player.getLocation())) {
                if (!driver.getCurrentLap().hasPassedAllCheckpoints()) {
                    int checkpoint = driver.getCurrentLap().getLatestCheckpoint();
                    var maybeCheckpoint = track.getRegions(TrackRegion.RegionType.CHECKPOINT).stream().filter(trackRegion -> trackRegion.getRegionIndex() == checkpoint).findFirst();
                    if (maybeCheckpoint.isPresent()) {
                        ApiUtilities.teleportPlayerAndSpawnBoat(player, track, maybeCheckpoint.get().getSpawnLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                    }
                    plugin.sendMessage(driver.getTPlayer().getPlayer(), "messages.error.timer.missedCheckpoints");
                    return;
                }
                heat.passLap(driver);
                heat.updatePositions();
                return;
            }
        }


        if (driver.getState() == DriverState.RUNNING) {
            Lap lap = driver.getCurrentLap();

            if (driver.getHeat().getRound() instanceof FinalRound) {
                // Check for pitstop
                var maybePit = track.getRegion(TrackRegion.RegionType.PIT);
                if (maybePit.isPresent() && maybePit.get().contains(player.getLocation())) {
                    if (driver.passPit()) {
                        heat.updatePositions();
                    }
                }
            }

            // Check for reset
            for (TrackRegion r : track.getRegions(TrackRegion.RegionType.RESET)) {
                if (r.contains(player.getLocation())) {
                    var maybeRegion = track.getRegion(TrackRegion.RegionType.CHECKPOINT, lap.getLatestCheckpoint());
                    TrackRegion region = maybeRegion.isEmpty() ? startRegion.get() : maybeRegion.get();
                    ApiUtilities.teleportPlayerAndSpawnBoat(player, track, region.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                    return;
                }
            }

            // Update if in pit
            var inPitRegions = track.getRegions(TrackRegion.RegionType.INPIT);
            for (TrackRegion trackRegion : inPitRegions) {
                if (trackRegion.contains(player.getLocation()) && !inPits.contains(player.getUniqueId())){
                    inPits.add(player.getUniqueId());
                    heat.updatePositions();
                } else if (!trackRegion.contains(player.getLocation()) && inPits.contains(player.getUniqueId())) {
                    inPits.remove(player.getUniqueId());
                    heat.updatePositions();
                }
            }

            // Check for next checkpoint in current map
            if (lap.hasPassedAllCheckpoints()) {
                return;
            }

            var maybeCheckpoint = track.getRegions(TrackRegion.RegionType.CHECKPOINT).stream().filter(trackRegion -> trackRegion.contains(player.getLocation())).findFirst();
            if (maybeCheckpoint.isPresent() && maybeCheckpoint.get().getRegionIndex() == lap.getNextCheckpoint()) {
                lap.passNextCheckpoint(TimingSystem.currentTime);
                heat.updatePositions();
            } else if (maybeCheckpoint.isPresent() && maybeCheckpoint.get().getRegionIndex() > lap.getNextCheckpoint()) {
                var maybeRegion = track.getRegion(TrackRegion.RegionType.CHECKPOINT, lap.getLatestCheckpoint());
                TrackRegion region = maybeRegion.isEmpty() ? startRegion.get() : maybeRegion.get();
                ApiUtilities.teleportPlayerAndSpawnBoat(player, track, region.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                plugin.sendMessage(driver.getTPlayer().getPlayer(), "messages.error.timer.missedCheckpoints");
                return;
            }
        }
    }
}
