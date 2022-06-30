package me.makkuusen.timing.system;

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

import java.util.Iterator;

public class RaceListener implements Listener
{

    @EventHandler(priority = EventPriority.HIGHEST)
    void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event)
    {

        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED)
        {

            RPlayer rPlayer = ApiDatabase.getPlayer(event.getUniqueId(), event.getName());

            if (rPlayer == null)
            {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Din spelarprofil kunde inte laddas.");
                return;
            }
        }
    }
    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event) {
        RPlayer rPlayer = ApiDatabase.getPlayer(event.getPlayer().getUniqueId());

        rPlayer.setPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e)
    {
        PlayerTimer.playerLeavingMap(e.getEntity());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event)
    {

        for (RaceTrack raceTrack : RaceDatabase.getRaceTracks())
        {
            if (raceTrack.getSpawnLocation().getWorld() == event.getTo().getWorld())
            {
                if (raceTrack.getSpawnLocation().distance(event.getTo()) < 1 && event.getPlayer().getGameMode() == GameMode.SURVIVAL)
                {
                    Bukkit.getScheduler().runTaskLater(Race.getPlugin(), () -> raceTrack.spawnBoat(event.getPlayer()), 1);
                }
            }
        }

        if (!event.getCause().equals(PlayerTeleportEvent.TeleportCause.UNKNOWN))
        {
            PlayerTimer.playerLeavingMap(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e)
    {
        PlayerTimer.playerLeavingMap(e.getPlayer());
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent e)
    {
        if (!e.getVehicle().getPassengers().isEmpty())
        {
            var passenger = e.getVehicle().getPassengers().get(0);
            if (passenger instanceof Player player)
            {
                if (PlayerTimer.isPlayerInMap(player))
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
            Bukkit.getScheduler().runTaskLater(Race.getPlugin(), () -> event.getVehicle().remove(), 10);
        }

        if (event.getExited() instanceof Player player)
        {

            if (PlayerTimer.isPlayerInMap(player))
            {
                RaceTrack track = PlayerTimer.getTrackPlayerIsIn(player);
                if (track.hasOption('b'))
                {
                    player.sendMessage("§cDu lämnade båten och tiden avbröts.");
                    PlayerTimer.playerLeavingMap(player);
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
            if (PlayerTimer.isPlayerInMap(hooked))
            {
                e.getPlayer().sendMessage("§cDu får inte kroka någon annan");
                e.setCancelled(true);
                return;
            }
        }

        if (e.getCaught() instanceof Player player)
        {
            if (PlayerTimer.isPlayerInMap(player))
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
        if (PlayerTimer.isPlayerInMap(player))
        {
            RaceTrack track = PlayerTimer.getTrackPlayerIsIn(player);
            if (player.getInventory().getChestplate() != null && player.getInventory().getChestplate().getType().equals(Material.ELYTRA) && track.hasOption('e'))
            {
                player.sendMessage("§cDu får inte ha elytra på den här banan.");
                PlayerTimer.playerLeavingMap(player);
            }
            else if (!player.isGliding() && track.hasOption('g'))
            {
                player.sendMessage("§cDu slutade flyga och tiden avbröts.");
                PlayerTimer.playerLeavingMap(player);
            }
            else if (player.getActivePotionEffects().size() > 0 && track.hasOption('p'))
            {
                player.sendMessage("§cDu får inte ha effekter på den här banan.");
                PlayerTimer.playerLeavingMap(player);
            }
            else if (player.isRiptiding() && track.hasOption('t')){
                player.sendMessage("§cDu får inte använda trident på den här banan.");
                PlayerTimer.playerLeavingMap(player);
            }
            else if (player.getInventory().getBoots() != null && player.getInventory().getBoots().containsEnchantment(Enchantment.SOUL_SPEED) && track.hasOption('s')){
                player.sendMessage("§cDu får inte ha själhastighet på dina skor.");
                PlayerTimer.playerLeavingMap(player);
            }

        }
    }

    @EventHandler
    public void onRegionEnter(PlayerMoveEvent e)
    {
        Player player = e.getPlayer();
        Iterator regions = RaceDatabase.getRaceRegions().iterator();
        while (true)
        {
            Integer regionId;
            RaceRegion region;
            RaceRegion.RegionType type;
            do
            {
                label:
                do
                {
                    while (regions.hasNext())
                    {
                        region = (RaceRegion) regions.next();
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

            type = region.getRegionType();
            var maybeTrack = RaceDatabase.getTrackById(region.getTrackId());
            if (maybeTrack.isPresent())
            {
                RaceTrack track = maybeTrack.get();
                if (type.equals(RaceRegion.RegionType.START))
                {
                    PlayerTimer.playerStartingMap(player, track);
                }
                else if (type.equals(RaceRegion.RegionType.END))
                {
                    PlayerTimer.playerEndedMap(player, track);
                }
                else if (type.equals(RaceRegion.RegionType.CHECKPOINT))
                {
                    PlayerTimer.playerPassingCheckpoint(player, track, region.getRegionIndex());
                }
                else if (type.equals(RaceRegion.RegionType.RESET))
                {
                    PlayerTimer.playerResetMap(player, track);
                }
            }
            PlayerRegionData.instanceOf(player).getEntered().add(regionId);
        }
    }

    @EventHandler
    void onPlayerQuit(PlayerQuitEvent event)
    {
        RPlayer rPlayer = ApiDatabase.getPlayer(event.getPlayer());
        // Set to offline
        rPlayer.setPlayer(null);

    }
}
