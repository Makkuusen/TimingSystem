package me.makkuusen.timing.system;

import com.destroystokyo.paper.event.server.ServerTickStartEvent;
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

public class TSListener implements Listener
{

    static TimingSystem plugin;
    @EventHandler
    public void onTick(ServerTickStartEvent e) {
        TimingSystem.getPlugin().currentTime = Instant.now();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event)
    {

        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED)
        {

            TSPlayer TSPlayer = ApiDatabase.getPlayer(event.getUniqueId(), event.getName());

            if (TSPlayer == null)
            {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Din spelarprofil kunde inte laddas.");
                return;
            }
        }
    }
    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event) {
        TSPlayer TSPlayer = ApiDatabase.getPlayer(event.getPlayer().getUniqueId());

        TSPlayer.setPlayer(event.getPlayer());

        if (!TSPlayer.getName().equals(event.getPlayer().getName())) {
            // Update name
            TSPlayer.setName(event.getPlayer().getName());
            TSPlayer.updateNameChanges();
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e)
    {
        TimeTrialController.playerLeavingMap(e.getEntity().getUniqueId());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event)
    {

        for (Track Track : TrackDatabase.getTracks())
        {
            if (Track.getSpawnLocation().getWorld() == event.getTo().getWorld())
            {
                if (Track.getSpawnLocation().distance(event.getTo()) < 1 && event.getPlayer().getGameMode() != GameMode.SPECTATOR)
                {
                    Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(), () -> Track.spawnBoat(event.getPlayer()), 1);
                }
            }
        }

        if (!event.getCause().equals(PlayerTeleportEvent.TeleportCause.UNKNOWN))
        {
            TimeTrialController.playerLeavingMap(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e)
    {
        TimeTrialController.playerLeavingMap(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent e)
    {
        if (!e.getVehicle().getPassengers().isEmpty())
        {
            var passenger = e.getVehicle().getPassengers().get(0);
            if (passenger instanceof Player player)
            {
                if (TimeTrialController.timeTrials.containsKey(player.getUniqueId()))
                {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event)
    {
        if (event.getVehicle() instanceof Boat && event.getVehicle().hasMetadata("spawned") && event.getVehicle().getPassengers().size() < 2)
        {
            Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(), () -> event.getVehicle().remove(), 10);
        }

        if (event.getExited() instanceof Player player)
        {

            if (TimeTrialController.timeTrials.containsKey(player.getUniqueId()))
            {
                Track track = TimeTrialController.timeTrials.get(player.getUniqueId()).getTrack();
                if (track.hasOption('b'))
                {
                    plugin.sendMessage(player,"messages.error.leftBoat");
                    TimeTrialController.playerLeavingMap(player.getUniqueId());
                }
            }

        }
    }

    @EventHandler
    public void onVehicleDestroy(VehicleDestroyEvent event)
    {
        if (event.getVehicle() instanceof Boat && event.getVehicle().hasMetadata("spawned"))
        {
            event.getVehicle().remove();
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByBlock(EntityDamageByBlockEvent event)
    {
        if (event.getEntity() instanceof Player && event.getEntity().isInsideVehicle() && event.getEntity().getVehicle().getType() == EntityType.BOAT && event.getEntity().getVehicle().hasMetadata("spawned"))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e)
    {
        Player player = e.getPlayer();
        PlayerRegionData.instanceOf(player).remove();
    }

    @EventHandler
    public void onPlayerFishEvent(PlayerFishEvent e)
    {
        if (e.getHook().getHookedEntity() instanceof Player hooked)
        {
            if (TimeTrialController.timeTrials.containsKey(hooked.getUniqueId()))
            {
                e.getPlayer().sendMessage("§cDu får inte kroka någon annan");
                e.setCancelled(true);
                return;
            }
        }

        if (e.getCaught() instanceof Player player)
        {
            if (TimeTrialController.timeTrials.containsKey(player.getUniqueId()))
            {
                e.getPlayer().sendMessage("§cDu får inte fiska någon annan");
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent e)
    {
        Player player = e.getPlayer();
        if (TimeTrialController.timeTrials.containsKey(player.getUniqueId()))
        {
            Track track = TimeTrialController.timeTrials.get(player.getUniqueId()).getTrack();
            if (player.getInventory().getChestplate() != null && player.getInventory().getChestplate().getType().equals(Material.ELYTRA) && track.hasOption('e'))
            {
                player.sendMessage("§cDu får inte ha elytra på den här banan.");
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            }
            else if (!player.isGliding() && track.hasOption('g'))
            {
                player.sendMessage("§cDu slutade flyga och tiden avbröts.");
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            }
            else if (player.getActivePotionEffects().size() > 0 && track.hasOption('p'))
            {
                player.sendMessage("§cDu får inte ha effekter på den här banan.");
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            }
            else if (player.isRiptiding() && track.hasOption('t')){
                player.sendMessage("§cDu får inte använda trident på den här banan.");
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            }
            else if (player.getInventory().getBoots() != null && player.getInventory().getBoots().containsEnchantment(Enchantment.SOUL_SPEED) && track.hasOption('s')){
                player.sendMessage("§cDu får inte ha själhastighet på dina skor.");
                TimeTrialController.playerLeavingMap(player.getUniqueId());
            }

        }
    }

    @EventHandler
    public void onRegionEnterV2(PlayerMoveEvent e)
    {
        Player player = e.getPlayer();
        TSPlayer TSPlayer = ApiDatabase.getPlayer(player.getUniqueId());

        var maybeRaceDriver = RaceController.getDriverFromActiveRace(TSPlayer);
        if (maybeRaceDriver.isPresent()) {
            var race = maybeRaceDriver.get();
            handleRace(race, player);

        }
        if (TimeTrialController.timeTrials.containsKey(player.getUniqueId())) {
            handleTimeTrials(player);
            // don't need to check for starting new track
            return;
        }

        // Check for starting new tracks
        Iterator regions = TrackDatabase.getTrackStartRegions().iterator();
        while (true)
        {
            Integer regionId;
            TrackRegion region;
            do
            {
                label:
                do
                {
                    while (regions.hasNext())
                    {
                        region = (TrackRegion) regions.next();
                        regionId = region.getId();
                        if (region.contains(player.getLocation()))
                        {
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
            if (maybeTrack.isPresent())
            {
                Track track_ = maybeTrack.get();

                if (track_.getMode().equals(Track.TrackMode.TIMETRIAL)) {
                    TimeTrial timeTrial = new TimeTrial(track_, TSPlayer);
                    timeTrial.playerStartingMap();
                }
            }
            PlayerRegionData.instanceOf(player).getEntered().add(regionId);
        }
    }

    @EventHandler
    void onPlayerQuit(PlayerQuitEvent event)
    {
        TSPlayer TSPlayer = ApiDatabase.getPlayer(event.getPlayer());
        // Set to offline
        TSPlayer.setPlayer(null);
    }

    void handleTimeTrials(Player player)
    {
        TimeTrial timeTrial = TimeTrialController.timeTrials.get(player.getUniqueId());
        // Check for ending current map.
        var track = timeTrial.getTrack();

        if (track.getStartRegion().contains(player.getLocation()) && track.getEndRegion().contains(player.getLocation())) {
            if (timeTrial.getLatestCheckpoint() != 0) {
                timeTrial.playerRestartMap();
                return;
            }
        }
        else if (track.getEndRegion().contains(player.getLocation())) {
            timeTrial.playerEndedMap();
            return;
        }
        // Check for next checkpoint in current map
        int nextCheckpoint = timeTrial.getNextCheckpoint();
        if (nextCheckpoint == timeTrial.getLatestCheckpoint())
        {
            return;
        }
        var checkpoint = track.getCheckpoints().get(nextCheckpoint);
        if (checkpoint.contains(player.getLocation()))
        {
            timeTrial.playerPassingCheckpoint(nextCheckpoint);
        }

        // Check reset regions
        for (TrackRegion r : track.getResetRegions().values()) {
            if (r.contains(player.getLocation())) {
                timeTrial.playerResetMap();
            }
        }
    }

    private void handleRace(Race race, Player player) {
        var track = race.getTrack();
        if (track.getMode() != Track.TrackMode.RACE) {
            return;
        }

        if(!race.getRaceState().equals(RaceState.RACING)){
            return;
        }
        var raceDriver = race.getRaceDriver(player.getUniqueId());
        if (raceDriver.isFinished()) {
            return;
        }
        if (track.getStartRegion().contains(player.getLocation()))
        {
            if (!raceDriver.isRunning()) {
                raceDriver.start();
            }
            else if (raceDriver.getLatestCheckpoint() != 0) {


                if (!raceDriver.hasPassedAllCheckpoints())
                {
                    int checkpoint = raceDriver.getLatestCheckpoint();
                    player.teleport(race.getTrack().getCheckpoints().get(checkpoint).getSpawnLocation());
                    plugin.sendMessage(raceDriver.getTSPlayer().getPlayer(), "messages.error.timer.missedCheckpoints");
                    return;
                }
                race.passLap(player.getUniqueId());
            }

        }


        if (raceDriver.isRunning()) {

            RaceLap lap = raceDriver.getCurrentLap();

            // Check for pitstop
            if (track.getPitRegion().contains(player.getLocation()))
            {
                raceDriver.passPit();
            }

            // Check for next checkpoint in current map

            if (lap.hasPassedAllCheckpoints()){
                return;
            }
            var checkpoint = track.getCheckpoints().get(lap.getNextCheckpoint());
            if (checkpoint.contains(player.getLocation())) {
                race.passNextCheckpoint(raceDriver);
            }
        }

    }
}
