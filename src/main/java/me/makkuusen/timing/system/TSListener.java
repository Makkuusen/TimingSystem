package me.makkuusen.timing.system;

import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import me.makkuusen.timing.system.boatutils.BoatUtilsManager;
import me.makkuusen.timing.system.boatutils.BoatUtilsMode;
import me.makkuusen.timing.system.commands.CommandRace;
import me.makkuusen.timing.system.database.EventDatabase;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.heat.Lap;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.DriverState;
import me.makkuusen.timing.system.round.FinalRound;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.timetrial.TimeTrial;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackOption;
import me.makkuusen.timing.system.track.TrackRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Boat;
import org.bukkit.entity.ChestBoat;
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

import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class TSListener implements Listener {

    static TimingSystem plugin;
    static Set<UUID> inPits = new HashSet<>();

    @EventHandler
    public void onTick(ServerTickStartEvent e) {
        TimingSystem.currentTime = Instant.now();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {

        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {

            TPlayer TPlayer = TSDatabase.getPlayer(event.getUniqueId(), event.getName());

            if (TPlayer == null) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("Your player profile could not be loaded. Notify the server owner!"));
            }
        }
    }

    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        TPlayer TPlayer = TSDatabase.getPlayer(player.getUniqueId());
        TPlayer.setPlayer(player);

        if (!TPlayer.getName().equals(player.getName())) {
            // Update name
            TPlayer.setName(player.getName());
        }

        Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(), () -> {
            if (player.isInsideVehicle() && (player.getVehicle() instanceof Boat || player.getVehicle() instanceof ChestBoat) && TimeTrialController.lastTimeTrialTrack.containsKey(player.getUniqueId())) {
                Track track = TimeTrialController.lastTimeTrialTrack.get(player.getUniqueId());
                boolean sameAsLastTrack = TimeTrialController.lastTimeTrialTrack.containsKey(player.getUniqueId()) && TimeTrialController.lastTimeTrialTrack.get(player.getUniqueId()).getId() == track.getId();
                BoatUtilsManager.sendBoatUtilsModePluginMessage(player, track.getBoatUtilsMode(), TimeTrialController.lastTimeTrialTrack.get(player.getUniqueId()), sameAsLastTrack);
                ApiUtilities.teleportPlayerAndSpawnBoat(player, TimeTrialController.lastTimeTrialTrack.get(player.getUniqueId()), player.getLocation().add(0,1,0));
            } else {
                BoatUtilsManager.sendBoatUtilsModePluginMessage(player, BoatUtilsMode.VANILLA, null, true);
            }
        }, 3);
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
            //Don't care about removing BU effects or playerLeavingMap if driver is an active race.
            return;
        }

        if (event.getCause().equals(PlayerTeleportEvent.TeleportCause.PLUGIN) || event.getCause().equals(PlayerTeleportEvent.TeleportCause.COMMAND) || event.getCause().equals(PlayerTeleportEvent.TeleportCause.ENDER_PEARL) || event.getCause().equals(PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) || event.getCause().equals(PlayerTeleportEvent.TeleportCause.NETHER_PORTAL)) {
            TimeTrialController.playerLeavingMap(event.getPlayer().getUniqueId());
            if (ApiUtilities.hasBoatUtilsEffects(event.getPlayer())) {
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

        //Remove driver from loaded heats.
        Heat heat = CommandRace.heat;
        if (heat == null) {
            return;
        }

        if (CommandRace.heat.getDrivers().containsKey(e.getPlayer().getUniqueId())) {
            if (heat.getHeatState() != HeatState.LOADED) {
                return;
            }
            heat.resetHeat();
            if (heat.removeDriver(heat.getDrivers().get(e.getPlayer().getUniqueId()))) {
                heat.getEvent().removeSpectator(e.getPlayer().getUniqueId());
            }
            heat.loadHeat();
        }
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent e) {
        if (!e.getVehicle().getPassengers().isEmpty()) {
            var passenger = e.getVehicle().getPassengers().get(0);
            if (passenger instanceof Player player) {
                if (TimeTrialController.timeTrials.containsKey(player.getUniqueId())) {
                    e.setCancelled(true);
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
                if (track.hasTrackOption(TrackOption.FORCE_BOAT)) {
                    TimeTrialController.playerLeavingMap(player.getUniqueId());
                    if (ApiUtilities.hasBoatUtilsEffects(player)) {
                        ApiUtilities.removeBoatUtilsEffects(player);
                    }
                }
            } else {
                if (ApiUtilities.hasBoatUtilsEffects(player)) {
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
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockPlaceEvent event) {
        var maybeDriver = EventDatabase.getDriverFromRunningHeat(event.getPlayer().getUniqueId());
        if (maybeDriver.isPresent()) {
            if (maybeDriver.get().getState() == DriverState.LOADED) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        var maybeDriver = EventDatabase.getDriverFromRunningHeat(event.getPlayer().getUniqueId());
        if (maybeDriver.isPresent()) {
            if (maybeDriver.get().getState() == DriverState.LOADED) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
        var maybeDriver = EventDatabase.getDriverFromRunningHeat(event.getPlayer().getUniqueId());
        if (maybeDriver.isPresent()) {
            if (maybeDriver.get().getState() == DriverState.LOADED) {
                event.setCancelled(true);
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
        if (event.getEntity() instanceof Player && event.getEntity().getVehicle() != null && event.getEntity().getVehicle().getType() == EntityType.BOAT && event.getEntity().getVehicle().hasMetadata("spawned")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        PlayerRegionData.instanceOf(player).remove();
    }

    @EventHandler
    public static void onPlayerFishEvent(PlayerFishEvent e) {
        if (e.getHook().getHookedEntity() instanceof Player hooked) {
            if (TimeTrialController.timeTrials.containsKey(hooked.getUniqueId())) {
                Text.send(e.getPlayer(), Error.NO_HOOKING_OTHERS);
                e.setCancelled(true);
                return;
            }
        }

        if (e.getCaught() instanceof Player player) {
            if (TimeTrialController.timeTrials.containsKey(player.getUniqueId())) {
                Text.send(e.getPlayer(), Error.NO_FISHING_OTHERS);
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerMoveEventRemoveElytra(PlayerMoveEvent e) {

        if (!TimeTrialController.timeTrials.containsKey(e.getPlayer().getUniqueId())) {
            Player player = e.getPlayer();
            if (player.getInventory().getChestplate() == null || !player.getInventory().getChestplate().getItemMeta().hasCustomModelData()) {
                return;
            }
            var elytra = player.getInventory().getChestplate();
            if (!player.isGliding() && elytra.getItemMeta().getCustomModelData() == 747) {
                if (TimeTrialController.elytraProtection.get(player.getUniqueId()) != null) {
                    if (TimingSystem.currentTime.getEpochSecond() > TimeTrialController.elytraProtection.get(player.getUniqueId())) {
                        player.getInventory().setChestplate(null);
                        TimeTrialController.elytraProtection.remove(player.getUniqueId());
                    }
                } else {
                    player.getInventory().setChestplate(null);
                }
            }
        }
    }

    @EventHandler
    public static void onPlayerMoveEvent(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        BoatUtilsMode mode = BoatUtilsManager.playerBoatUtilsMode.get(player.getUniqueId());
        if (TimeTrialController.timeTrials.containsKey(player.getUniqueId())) {
            TimeTrial timeTrial = TimeTrialController.timeTrials.get(player.getUniqueId());
            Track track = timeTrial.getTrack();
            if (player.getInventory().getChestplate() != null && player.getInventory().getChestplate().getType().equals(Material.ELYTRA) && track.hasTrackOption(TrackOption.NO_ELYTRA)) {
                Text.send(player, Error.NO_ELYTRA);
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            } else if (player.getGameMode().equals(GameMode.CREATIVE) && track.hasTrackOption(TrackOption.NO_CREATIVE)) {
                Text.send(player, Error.NO_CREATIVE);
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            } else if (!player.isGliding() && track.hasTrackOption(TrackOption.FORCE_ELYTRA)) {
                Text.send(player, Error.STOPPED_FLYING);
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            } else if (!player.getActivePotionEffects().isEmpty() && track.hasTrackOption(TrackOption.NO_POTION_EFFECTS)) {
                Text.send(player, Error.NO_POTION_EFFECTS);
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            } else if (player.isRiptiding() && track.hasTrackOption(TrackOption.NO_RIPTIDE)) {
                Text.send(player, Error.NO_RIPTIDE);
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            } else if (player.getInventory().getBoots() != null && player.getInventory().getBoots().containsEnchantment(Enchantment.SOUL_SPEED) && track.hasTrackOption(TrackOption.NO_SOUL_SPEED)) {
                Text.send(player, Error.NO_SOUL_SPEED);
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            } else if (mode != null && mode != track.getBoatUtilsMode()) {
                Text.send(player, Error.WRONG_BOAT_UTILS_MODE);
                ApiUtilities.removeBoatUtilsEffects(player);
                timeTrial.playerResetMap();
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
                player.sendActionBar(Component.text(region.getRegionType() + " : " + region.getRegionIndex()).color(NamedTextColor.GREEN));
                return;
            }
        }
    }

    @EventHandler
    public void onRegionEnterV2(PlayerMoveEvent e) {
        if ((int) (e.getFrom().getX() - 0.5) != (int) e.getTo().getX() || (int) e.getFrom().getY() != (int) e.getTo().getY() || (int) (e.getFrom().getZ() - 0.5) != (int) e.getTo().getZ()) {
            Player player = e.getPlayer();

            if (player.getGameMode() == GameMode.SPECTATOR) {
                return;
            }

            var maybeDriver = EventDatabase.getDriverFromRunningHeat(player.getUniqueId());
            if (maybeDriver.isPresent()) {
                handleHeat(maybeDriver.get(), player);
                return;
            }

            if (TimeTrialController.timeTrials.containsKey(player.getUniqueId())) {
                handleTimeTrials(player);
                // don't need to check for starting new track
                return;
            }

            // Check for starting new tracks
            Iterator<TrackRegion> regions = TrackDatabase.getTrackStartRegions().iterator();
            while (true) {
                Integer regionId;
                TrackRegion region;
                do {
                    label:
                    do {
                        while (regions.hasNext()) {
                            region = regions.next();
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
                        TimeTrial timeTrial = new TimeTrial(track_, TSDatabase.getPlayer(player.getUniqueId()));
                        timeTrial.playerStartingTimeTrial();
                        TimeTrialController.elytraProtection.remove(player.getUniqueId());
                        TimeTrialController.lastTimeTrialTrack.put(player.getUniqueId(), track_);
                    }
                }
                PlayerRegionData.instanceOf(player).getEntered().add(regionId);
            }
        }
    }

    @EventHandler
    void onPlayerQuit(PlayerQuitEvent event) {
        TPlayer TPlayer = TSDatabase.getPlayer(event.getPlayer());
        // Set to offline
        TPlayer.setPlayer(null);
        TPlayer.clearScoreboard();
    }

    static void handleTimeTrials(Player player) {
        TimeTrial timeTrial = TimeTrialController.timeTrials.get(player.getUniqueId());
        // Check for ending current map.
        var track = timeTrial.getTrack();

        var startRegions = track.getRegions(TrackRegion.RegionType.START);
        var endRegions = track.getRegions(TrackRegion.RegionType.END);

        if (startRegions.isEmpty()) {
            return;
        }

        if (endRegions.isEmpty()) {
            for (var r : startRegions) {
                if (r.contains(player.getLocation())) {
                    if (timeTrial.getLatestCheckpoint() != 0) {
                        timeTrial.playerRestartMap();
                        return;
                    }
                }
            }
        } else {
            for (var r : endRegions) {
                if (r.contains(player.getLocation())) {
                    if (timeTrial.getLatestCheckpoint() != 0) {
                        timeTrial.playerEndedMap();
                        return;
                    }
                }
            }
        }


        for (TrackRegion r : track.getRegions(TrackRegion.RegionType.RESET)) {
            if (r.contains(player.getLocation())) {
                if (!track.hasTrackOption(TrackOption.RESET_TO_LATEST_CHECKPOINT)) {
                    timeTrial.playerResetMap();
                } else {
                    var maybeRegion = track.getRegion(TrackRegion.RegionType.CHECKPOINT, timeTrial.getLatestCheckpoint());
                    if (maybeRegion.isEmpty()) {
                        timeTrial.playerResetMap();
                        return;
                    }
                    ApiUtilities.teleportPlayerAndSpawnBoat(player, track, maybeRegion.get().getSpawnLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                }
                return;
            }
        }

        if (!timeTrial.isLagStart() && track.getRegion(TrackRegion.RegionType.LAGSTART).isPresent()) {
            if (track.getRegion(TrackRegion.RegionType.LAGSTART).get().contains(player.getLocation())) {
                timeTrial.setLagStartTrue();
                timeTrial.playerPassingLagStart();
                if (ApiUtilities.getRoundedToTick(timeTrial.getTimeSinceStart(TimingSystem.currentTime)) == 0) {
                    Text.send(player, Error.LAG_DETECTED);
                    plugin.getLogger().warning(player.getName() + " failed lagstart on " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeTrial.getTimeSinceStart(TimingSystem.currentTime)));
                    timeTrial.playerResetMap();
                    return;
                } else {
                    ApiUtilities.msgConsole(player.getName() + " passed lagstart on " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeTrial.getTimeSinceStart(TimingSystem.currentTime)));
                }
            }
        }

        if (!timeTrial.isLagEnd() && track.getRegion(TrackRegion.RegionType.LAGEND).isPresent()) {
            if (track.getRegion(TrackRegion.RegionType.LAGEND).get().contains(player.getLocation())) {
                timeTrial.setLagEnd(true);
                timeTrial.playerPassingLagEnd();
                if (!timeTrial.isLagStart()) {
                    Text.send(player, Error.LAG_DETECTED);
                    plugin.getLogger().warning(player.getName() + " failed lagend on " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeTrial.getTimeSinceStart(TimingSystem.currentTime)));
                    timeTrial.playerResetMap();
                    return;
                } else if (TimingSystem.currentTime.toEpochMilli() == timeTrial.getLagStart().toEpochMilli()) {
                    Text.send(player, Error.LAG_DETECTED);
                    plugin.getLogger().warning(player.getName() + " failed lagend on " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeTrial.getTimeSinceStart(TimingSystem.currentTime)));
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
        for (TrackRegion checkpoint : track.getCheckpointRegions(nextCheckpoint)) {
            if (checkpoint.contains(player.getLocation())){
                timeTrial.playerPassingNextCheckpoint();
            }
        }
    }

    private static void handleHeat(Driver driver, Player player) {
        Heat heat = driver.getHeat();

        if (!heat.getHeatState().equals(HeatState.RACING)) {
            return;
        }

        if (driver.isFinished()) {
            return;
        }
        var track = heat.getEvent().getTrack();

        var startRegions = track.getRegions(TrackRegion.RegionType.START);
        if (startRegions.isEmpty()) {
            return;
        }

        for (var r : startRegions) {
            if (r.contains(player.getLocation())) {
                if (driver.getState() == DriverState.STARTING) {
                    driver.start();
                    heat.updatePositions();
                    ApiUtilities.msgConsole("Starting : " + player.getName() + " in " + heat.getName());
                    return;
                } else if (driver.getCurrentLap() != null && driver.getCurrentLap().getLatestCheckpoint() != 0) {
                    if (!driver.getCurrentLap().hasPassedAllCheckpoints()) {
                        int checkpoint = driver.getCurrentLap().getLatestCheckpoint();
                        var maybeCheckpoint = track.getRegions(TrackRegion.RegionType.CHECKPOINT).stream().filter(trackRegion -> trackRegion.getRegionIndex() == checkpoint).findFirst();
                        maybeCheckpoint.ifPresent(trackRegion -> ApiUtilities.teleportPlayerAndSpawnBoat(player, track, trackRegion.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN));
                        Text.send(driver.getTPlayer().getPlayer(), Error.MISSED_CHECKPOINTS);

                        return;
                    }
                    heat.passLap(driver);
                    heat.updatePositions();
                    return;
                }
            }
        }

        if (track.isStage()) {
            for (var r : track.getRegions(TrackRegion.RegionType.END)) {
                if (r.contains(player.getLocation())) {
                    if (!(driver.getCurrentLap() != null && driver.getCurrentLap().hasPassedAllCheckpoints())) {
                        int checkpoint = driver.getCurrentLap().getLatestCheckpoint();
                        var maybeCheckpoint = track.getRegions(TrackRegion.RegionType.CHECKPOINT).stream().filter(trackRegion -> trackRegion.getRegionIndex() == checkpoint).findFirst();
                        maybeCheckpoint.ifPresent(trackRegion -> ApiUtilities.teleportPlayerAndSpawnBoat(player, track, trackRegion.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN));
                        Text.send(driver.getTPlayer().getPlayer(), Error.MISSED_CHECKPOINTS);
                        return;
                    }
                    heat.passLap(driver);
                    heat.updatePositions();
                    return;
                }
            }
        }


        if (driver.getState() == DriverState.RUNNING) {
            Lap lap = driver.getCurrentLap();

            if (lap == null) {
                return;
            }

            if (driver.getHeat().getRound() instanceof FinalRound) {
                // Check for pitstop
                for (var r : track.getRegions(TrackRegion.RegionType.PIT)) {
                    if (r.contains(player.getLocation())) {
                        if (driver.passPit()) {
                            heat.updatePositions();
                            break;
                        }
                    }
                }
            }

            // Check for reset
            for (TrackRegion r : track.getRegions(TrackRegion.RegionType.RESET)) {
                if (r.contains(player.getLocation())) {
                    var maybeRegion = track.getRegion(TrackRegion.RegionType.CHECKPOINT, lap.getLatestCheckpoint());

                    TrackRegion region;
                    region = maybeRegion.orElseGet(() -> track.getStartRegion().get());
                    ApiUtilities.teleportPlayerAndSpawnBoat(player, track, region.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                    return;
                }
            }

            // Update if in pit
            var inPitRegions = track.getRegions(TrackRegion.RegionType.INPIT);
            for (TrackRegion trackRegion : inPitRegions) {
                if (trackRegion.contains(player.getLocation()) && !inPits.contains(player.getUniqueId())) {
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
                TrackRegion region = maybeRegion.orElseGet(() -> track.getStartRegion().get());
                ApiUtilities.teleportPlayerAndSpawnBoat(player, track, region.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                Text.send(driver.getTPlayer().getPlayer(), Error.MISSED_CHECKPOINTS);
            }
        }
    }
}
