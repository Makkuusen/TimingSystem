package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.sk89q.worldedit.math.BlockVector2;
import me.makkuusen.timing.system.*;
import me.makkuusen.timing.system.boatutils.BoatUtilsMode;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.gui.TrackGui;
import me.makkuusen.timing.system.permissions.PermissionTrack;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Info;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.theme.messages.Warning;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import me.makkuusen.timing.system.timetrial.TimeTrialDateComparator;
import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import me.makkuusen.timing.system.timetrial.TimeTrialFinishComparator;
import me.makkuusen.timing.system.timetrial.TimeTrialSession;
import me.makkuusen.timing.system.track.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

@CommandAlias("track|t")
public class CommandTrack extends BaseCommand {

    @Subcommand("move")
    @CommandCompletion("@track")
    @CommandPermission("%permissiontrack_move")
    public static void onMove(Player player, Track track) {
        var moveTo = player.getLocation().toBlockLocation();
        var moveFrom = track.getSpawnLocation().toBlockLocation();
        World newWorld = moveTo.getWorld();
        track.setSpawnLocation(moveTo);
        var offset = getOffset(moveFrom, moveTo);

        var trackLocations = track.getTrackLocations();
        for (TrackLocation tl : trackLocations) {
            Location loc = tl.getLocation();
            var newLoc = getNewLocation(newWorld, loc, offset);
            track.updateTrackLocation(tl, newLoc);
        }

        var regions = track.getRegions();
        for (TrackRegion region : regions) {
            if (region.getSpawnLocation() != null) {
                region.setSpawn(getNewLocation(newWorld, region.getSpawnLocation(), offset));
            }

            if (region.getMaxP() != null) {
                region.setMaxP(getNewLocation(newWorld, region.getMaxP(), offset));
            }

            if (region.getMinP() != null) {
                region.setMinP(getNewLocation(newWorld, region.getMinP(), offset));
            }

            if (region instanceof TrackPolyRegion polyRegion) {
                var oldPoints = polyRegion.getPolygonal2DRegion().getPoints();
                List<BlockVector2> newPoints = new ArrayList<>();
                for (BlockVector2 b : oldPoints) {
                    newPoints.add(getNewBlockVector2(b, offset));
                }
                polyRegion.updateRegion(newPoints);
            }
        }

        Bukkit.getScheduler().runTaskAsynchronously(TimingSystem.getPlugin(), LeaderboardManager::updateAllFastestTimeLeaderboard);
        Text.send(player, Success.TRACK_MOVED, "to", ApiUtilities.niceLocation(moveTo), "from", ApiUtilities.niceLocation(moveFrom));
    }

    public static Vector getOffset(Location moveFrom, Location moveTo) {
        var vector = new Vector();
        vector.setX(moveFrom.getX() - moveTo.getX());
        vector.setY(moveFrom.getY() - moveTo.getY());
        vector.setZ(moveFrom.getZ() - moveTo.getZ());
        return vector;
    }

    public static Location getNewLocation(World newWorld, Location oldLocation, Vector offset) {
        var referenceNewWorld = new Location(newWorld, oldLocation.getX(), oldLocation.getY(), oldLocation.getZ(), oldLocation.getYaw(), oldLocation.getPitch());
        referenceNewWorld.subtract(offset);
        return referenceNewWorld;
    }

    public static BlockVector2 getNewBlockVector2(BlockVector2 old, Vector offset) {
        return BlockVector2.at(old.getX() - offset.getX(), old.getZ() - offset.getZ());
    }


    @Default
    @CommandPermission("%permissiontrack_menu")
    public static void onTrack(Player player) {
        new TrackGui(TSDatabase.getPlayer(player.getUniqueId())).show(player);
    }

    @Subcommand("tp")
    @CommandCompletion("@track @region")
    @CommandPermission("%permissiontrack_tp")
    public static void onTrackTp(Player player, Track track, @Optional String region) {
        if (!track.getSpawnLocation().isWorldLoaded()) {
            Text.send(player, Error.WORLD_NOT_LOADED);
            return;
        }

        if (region != null) {
            var rg = region.split("-");
            if (rg.length != 2) {
                Text.send(player, Error.SYNTAX);
                return;
            }
            String name = rg[0];
            String index = rg[1];

            var trackRegion = getRegion(track, name.toUpperCase(), index);

            if (trackRegion != null) {
                player.teleport(trackRegion.getSpawnLocation());
                Text.send(player, Success.TELEPORT_TO_TRACK, "%track%", trackRegion.getRegionType().name() + " : " + trackRegion.getRegionIndex());
                return;
            }

            var trackLocation = getTrackLocation(track, name, index);

            if (trackLocation != null) {
                player.teleport(trackLocation.getLocation());
                Text.send(player, Success.TELEPORT_TO_TRACK, "%track%", trackLocation.getLocationType().name() + " : " + trackLocation.getIndex());
                return;
            }
            Text.send(player, Error.FAILED_TELEPORT);
        } else {
            player.teleport(track.getSpawnLocation());
            Text.send(player, Success.TELEPORT_TO_TRACK, "%track%", track.getDisplayName());
        }
    }

    private static TrackRegion getRegion(Track track, String name, String index) {
        try {
            var regionType = TrackRegion.RegionType.valueOf(name);
            var regionIndex = Integer.parseInt(index);

            var trackRegion = track.getRegion(regionType, regionIndex);
            return trackRegion.orElse(null);

        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static TrackLocation getTrackLocation(Track track, String name, String index) {
        try {
            var locationType = TrackLocation.Type.valueOf(name);
            var regionIndex = Integer.parseInt(index);

            var trackLocation = track.getTrackLocation(locationType, regionIndex);
            return trackLocation.orElse(null);

        } catch (IllegalArgumentException e) {
            return null;
        }
    }


    @Subcommand("create")
    @CommandCompletion("@trackType name")
    @CommandPermission("%permissiontrack_create")
    public static void onCreate(Player player, Track.TrackType trackType, String name) {
        int maxLength = 25;
        if (name.length() > maxLength) {
            Text.send(player, Error.LENGTH_EXCEEDED, "%length%", String.valueOf(maxLength));
            return;
        }

        if (!name.matches("[A-Za-z0-9 ]+")) {
            Text.send(player, Error.NAME_FORMAT);
            return;
        }

        if (ApiUtilities.checkTrackName(name)) {
            Text.send(player, Error.INVALID_TRACK_NAME);
            return;
        }

        if (TrackDatabase.trackNameNotAvailable(name)) {
            Text.send(player, Error.TRACK_EXISTS);
            return;
        }

        ItemStack item;
        if (player.getInventory().getItemInMainHand().getItemMeta() == null) {
            switch (trackType) {
                case ELYTRA -> item = new ItemBuilder(Material.FEATHER).build();
                case PARKOUR -> item = new ItemBuilder(Material.RED_CONCRETE).build();
                default -> item = new ItemBuilder(Material.PACKED_ICE).build();
            }
        } else {
            item = player.getInventory().getItemInMainHand();
        }

        Track track = TrackDatabase.trackNew(name, player.getUniqueId(), player.getLocation(), trackType, item);
        if (track == null) {
            Text.send(player, Error.GENERIC);
            return;
        }
        if (trackType.equals(Track.TrackType.BOAT)) {
            track.createTrackOption(TrackOption.FORCE_BOAT);
        } else if (trackType.equals(Track.TrackType.PARKOUR)) {
            track.createTrackOption(TrackOption.NO_ELYTRA);
            track.createTrackOption(TrackOption.NO_CREATIVE);
        }

        Text.send(player, Success.CREATED_TRACK, "%track%", name);
        LeaderboardManager.updateFastestTimeLeaderboard(track);
    }

    @Subcommand("info")
    @CommandCompletion("@track @players")
    @CommandPermission("%permissiontrack_info")
    public static void onInfo(CommandSender commandSender, Track track, @Optional String name) {
        TPlayer tPlayer;
        tPlayer = TSDatabase.getPlayer(name);
        if (name != null && commandSender.hasPermission(PermissionTrack.VIEW_PLAYERSTATS.getNode())) {
            if (tPlayer == null) {
                Text.send(commandSender, Error.PLAYER_NOT_FOUND);
                return;
            }
            sendPlayerStatsInfo(commandSender, tPlayer, track);
            return;
        }
        sendTrackInfo(commandSender, track);

    }

    private static void sendPlayerStatsInfo(CommandSender commandSender, TPlayer tPlayer, Track track) {
        commandSender.sendMessage(Component.empty());
        Text.send(commandSender, Info.PLAYER_STATS_TITLE, "%player%", tPlayer.getName(), "%track%", track.getDisplayName());
        Text.send(commandSender, Info.PLAYER_STATS_POSITION, "%pos%", (track.getCachedPlayerPosition(tPlayer) == -1 ? "(-)" : String.valueOf(track.getCachedPlayerPosition(tPlayer))));
        if (track.getBestFinish(tPlayer) == null) {
            Text.send(commandSender, Info.PLAYER_STATS_BEST_LAP, "%time%", "(-)");
        } else {
            Text.send(commandSender, Info.PLAYER_STATS_BEST_LAP, "%time%", ApiUtilities.formatAsTime(track.getBestFinish(tPlayer).getTime()));
        }
        Text.send(commandSender, Info.PLAYER_STATS_FINISHES, "%size%", String.valueOf(track.getPlayerTotalFinishes(tPlayer)));
        Text.send(commandSender, Info.PLAYER_STATS_ATTEMPTS, "%size%", String.valueOf(track.getPlayerTotalFinishes(tPlayer) + track.getPlayerTotalAttempts(tPlayer)));
        Text.send(commandSender, Info.PLAYER_STATS_TIME_SPENT, "%size%", ApiUtilities.formatAsTimeSpent(track.getPlayerTotalTimeSpent(tPlayer)));
    }

    private static void sendTrackInfo(CommandSender commandSender, Track track) {
        commandSender.sendMessage(Component.empty());
        Text.send(commandSender, Info.TRACK_TITLE, "%name%", track.getDisplayName(), "%id%", String.valueOf(track.getId()));
        if (track.isOpen()) {
            Text.send(commandSender, Info.TRACK_OPEN);
        } else {
            Text.send(commandSender, Info.TRACK_CLOSED);
        }
        Text.send(commandSender, Info.TRACK_TYPE, "%type%", track.getTypeAsString());
        Text.send(commandSender, Info.TRACK_DATE_CREATED, "%date%", ApiUtilities.niceDate(track.getDateCreated()), "%owner%", track.getOwner().getName());
        Text.send(commandSender, Info.TRACK_OPTIONS, "%options%", ApiUtilities.listOfOptions(track.getTrackOptions()));
        Text.send(commandSender, Info.TRACK_MODE, "%mode%", track.getModeAsString());
        Text.send(commandSender, Info.TRACK_BOATUTILS_MODE, "%mode%", track.getBoatUtilsMode().name());
        Text.send(commandSender, Info.TRACK_CHECKPOINTS, "%size%", String.valueOf(track.getRegions(TrackRegion.RegionType.CHECKPOINT).size()));
        if (!track.getTrackLocations(TrackLocation.Type.GRID).isEmpty()) {
            Text.send(commandSender, Info.TRACK_GRIDS, "%size%", String.valueOf(track.getTrackLocations(TrackLocation.Type.GRID).size()));
        }
        if (!track.getTrackLocations(TrackLocation.Type.QUALYGRID).isEmpty()) {
            Text.send(commandSender, Info.TRACK_QUALIFICATION_GRIDS, "%size%", String.valueOf(track.getTrackLocations(TrackLocation.Type.QUALYGRID).size()));
        }
        Text.send(commandSender, Info.TRACK_RESET_REGIONS, "%size%", String.valueOf(track.getRegions(TrackRegion.RegionType.RESET).size()));
        Text.send(commandSender, Info.TRACK_SPAWN_LOCATION, "%location%", ApiUtilities.niceLocation(track.getSpawnLocation()));

        Text.send(commandSender, Info.TRACK_WEIGHT, "%size%", String.valueOf(track.getWeight()));
        Component tags = Component.empty();
        boolean notFirst = false;

        List<TrackTag> trackTags;
        if (commandSender.hasPermission("timingsystem.packs.trackadmin")) {
            trackTags = track.getTags();
        } else {
            trackTags = track.getDisplayTags();
        }
        for (TrackTag tag : trackTags) {
            if (notFirst) {
                tags = tags.append(Component.text(", ").color(Theme.getTheme(commandSender).getSecondary()));
            }
            tags = tags.append(Component.text(tag.getValue()).color(tag.getColor()));
            notFirst = true;
        }
        commandSender.sendMessage(Text.get(commandSender, Info.TRACK_TAGS).append(tags));

        Component contributors = Component.empty();
        if(!track.getContributors().isEmpty()) {
            contributors = contributors.append(Component.text(track.getContributors().get(0).getName()));

            for(TPlayer tp : track.getContributors().subList(1, track.getContributors().size())) {
                contributors = contributors.append(Component.text(", ", Theme.getTheme(commandSender).getSecondary())).append(Component.text(tp.getName(), Theme.getTheme(commandSender).getSecondary()));
            }
        }

        commandSender.sendMessage(Text.get(commandSender, Info.TRACK_CONTRIBUTORS).append(contributors));
    }

    @Subcommand("regions")
    @CommandCompletion("@track")
    @CommandPermission("%permissiontrack_view_regions")
    public static void onRegions(CommandSender sender, Track track) {
        Text.send(sender, Info.REGIONS_TITLE, "%track%", track.getDisplayName());

        Theme theme = Theme.getTheme(sender);

        for (var regionType : TrackRegion.RegionType.values()) {
            for (TrackRegion trackRegion : track.getRegions(regionType)) {

                String regionText = trackRegion.getRegionType().name() + "-" + trackRegion.getRegionIndex();
                sender.sendMessage(theme.arrow().append(Component.text(regionText).clickEvent(ClickEvent.runCommand("/t tp " + track.getCommandName() + " " + regionText))));
            }
        }
    }

    @Subcommand("locations")
    @CommandCompletion("@track")
    @CommandPermission("%permissiontrack_view_locations")
    public static void onLocations(CommandSender sender, Track track) {
        Text.send(sender, Info.LOCATIONS_TITLE, "%track%", track.getDisplayName());

        Theme theme = Theme.getTheme(sender);

        for (var locationType : TrackLocation.Type.values()) {
            for (TrackLocation trackLocation : track.getTrackLocations(locationType)) {
                String locationText = trackLocation.getLocationType().name() + "-" + trackLocation.getIndex();
                sender.sendMessage(theme.arrow().append(Component.text(locationText).clickEvent(ClickEvent.runCommand("/t tp " + track.getCommandName() + " " + locationText))));
            }
        }
    }

    @Subcommand("here")
    @CommandPermission("%permissiontrack_view_here")
    public static void onHere(Player player) {
        if(!player.hasPermission(PermissionTrack.VIEW_HERE.getNode())) {
            Text.send(player, Error.PERMISSION_DENIED);
            return;
        }
        boolean inRegion = false;
        for (Track track : TrackDatabase.tracks) {
            for (TrackRegion region : track.getRegions()) {
                if (region.contains(player.getLocation())) {
                    inRegion = true;
                    player.sendMessage(Component.text(track.getDisplayName() + " - " + region.getRegionType() + " : " + region.getRegionIndex()).color(Theme.getTheme(player).getSecondary()));
                }
            }
        }

        if (!inRegion) {
            Text.send(player, Error.REGIONS_NOT_FOUND);
        }
    }

    @Subcommand("session")
    @CommandCompletion("@track")
    @CommandPermission("%permissiontrack_session_timetrial")
    public static void toggleSession(Player player, @Optional Track track) {
        var maybeDriver = TimingSystemAPI.getDriverFromRunningHeat(player.getUniqueId());
        if (maybeDriver.isPresent()) {
            if (maybeDriver.get().isRunning()) {
                Text.send(player, Error.NOT_NOW);
                return;
            }
        }

        if (TimeTrialController.timeTrialSessions.containsKey(player.getUniqueId())) {
            var ttSession = TimeTrialController.timeTrialSessions.get(player.getUniqueId());
            ttSession.clearScoreboard();
            TimeTrialController.timeTrialSessions.remove(player.getUniqueId());
            if (track == null) {
                Text.send(player, Success.SESSION_ENDED);
                return;
            }
            if (!track.getSpawnLocation().isWorldLoaded()) {
                Text.send(player, Error.WORLD_NOT_LOADED);
                return;
            }

            if (track.getId() != ttSession.getTrack().getId()) {
                var newSession = new TimeTrialSession(TSDatabase.getPlayer(player.getUniqueId()), track);
                newSession.updateScoreboard();
                TimeTrialController.timeTrialSessions.put(player.getUniqueId(), newSession);
                Text.send(player, Success.SESSION_STARTED, "%track%", track.getDisplayName());
                ApiUtilities.teleportPlayerAndSpawnBoat(player, track, track.getSpawnLocation());
                return;
            }
            Text.send(player, Success.SESSION_ENDED);
            return;
        }

        if (track == null) {
            Text.send(player, Error.NOT_NOW);
            return;
        }

        if (!track.isOpen() && !player.hasPermission("timingsystem.packs.trackadmin")) {
            Text.send(player, Error.TRACK_IS_CLOSED);
            return;
        }

        TimeTrialSession ttSession = new TimeTrialSession(TSDatabase.getPlayer(player.getUniqueId()), track);
        ttSession.updateScoreboard();
        TimeTrialController.timeTrialSessions.put(player.getUniqueId(), ttSession);
        Text.send(player, Success.SESSION_STARTED, "%track%", track.getDisplayName());
        ApiUtilities.teleportPlayerAndSpawnBoat(player, track, track.getSpawnLocation());

    }

    @Subcommand("delete")
    @CommandCompletion("@track")
    @CommandPermission("%permissiontrack_delete_track")
    public static void onDelete(Player player, Track track) {
        TrackDatabase.removeTrack(track);
        Text.send(player, Success.REMOVED_TRACK, "%track%", track.getDisplayName());
    }


    @Subcommand("times")
    @CommandCompletion("@track <page>")
    @CommandPermission("%permissiontrack_view_times")
    public static void onTimes(CommandSender commandSender, Track track, @Optional Integer pageStart) {
        if (pageStart == null) {
            pageStart = 1;
        }
        Theme theme = Theme.getTheme(commandSender);
        int itemsPerPage = TimingSystem.configuration.getTimesPageSize();
        int start = (pageStart * itemsPerPage) - itemsPerPage;
        int stop = pageStart * itemsPerPage;

        if (start >= track.getTopList().size()) {
            Text.send(commandSender, Error.PAGE_NOT_FOUND);
            return;
        }

        Text.send(commandSender, Info.TRACK_TIMES_TITLE, "%track%", track.getDisplayName());
        for (int i = start; i < stop; i++) {
            if (i == track.getTopList().size()) {
                break;
            }
            TimeTrialFinish finish = track.getTopList().get(i);
            commandSender.sendMessage(theme.getTimesRow(String.valueOf(i + 1), finish.getPlayer().getName(), ApiUtilities.formatAsTime(finish.getTime())));
        }

        int pageEnd = (int) Math.ceil(((double) track.getTopList().size()) / ((double) itemsPerPage));
        commandSender.sendMessage(theme.getPageSelector(commandSender, pageStart, pageEnd, "/t times " + track.getCommandName()));
    }

    @Subcommand("mytimes")
    @CommandCompletion("@track <page>")
    @CommandPermission("%permissiontrack_view_mytimes")
    public static void onMyTimes(Player player, Track track, @Optional Integer pageStart) {
        if (pageStart == null) {
            pageStart = 1;
        }

        var tPlayer = TSDatabase.getPlayer(player.getUniqueId());
        Theme theme = tPlayer.getTheme();
        List<TimeTrialFinish> allTimes = new ArrayList<>();
        if (track.getTimeTrialFinishes().containsKey(tPlayer)) {
            allTimes.addAll(track.getTimeTrialFinishes().get(tPlayer));
            allTimes.sort(new TimeTrialFinishComparator());
        }

        int itemsPerPage = TimingSystem.configuration.getTimesPageSize();
        int start = (pageStart * itemsPerPage) - itemsPerPage;
        int stop = pageStart * itemsPerPage;

        if (start >= allTimes.size()) {
            Text.send(player, Error.PAGE_NOT_FOUND);
            return;
        }

        Text.send(player, Info.PLAYER_BEST_TIMES_TITLE, "%player%", player.getName(), "%track%", track.getDisplayName());

        for (int i = start; i < stop; i++) {
            if (i == allTimes.size()) {
                break;
            }
            TimeTrialFinish finish = allTimes.get(i);
            Component row = theme.getTimesRow(String.valueOf(i + 1), ApiUtilities.formatAsTime(finish.getTime()), ApiUtilities.niceDate(finish.getDate()));
            if (finish.hasCheckpointTimes()) {
                row = theme.getCheckpointHovers(finish, track.getBestFinish(tPlayer), row);
            }
            player.sendMessage(row);
        }

        int pageEnd = (int) Math.ceil(((double) allTimes.size()) / ((double) itemsPerPage));
        player.sendMessage(theme.getPageSelector(player, pageStart, pageEnd, "/t mytimes " + track.getCommandName()));
    }

    @Subcommand("alltimes")
    @CommandCompletion("@players <page>")
    @CommandPermission("%permissiontrack_view_alltimes")
    public static void onAllTimes(Player player, @Optional String name, @Optional Integer pageStart) {
        if (pageStart == null) {
            pageStart = 1;
        }
        TPlayer tPlayer;
        if (name != null) {

            if (!player.hasPermission("timingsystem.packs.trackadmin")) {
                Text.send(player, Error.PERMISSION_DENIED);
                return;
            }
            tPlayer = TSDatabase.getPlayer(name);
            if (tPlayer == null) {
                Text.send(player, Error.PLAYER_NOT_FOUND);
                return;
            }
        } else {
            tPlayer = TSDatabase.getPlayer(player.getUniqueId());
        }

        List<TimeTrialFinish> allTimes = new ArrayList<>();
        var tracks = TrackDatabase.getOpenTracks();
        for (Track t : tracks) {
            if (t.getTimeTrialFinishes().containsKey(tPlayer)) {
                allTimes.addAll(t.getTimeTrialFinishes().get(tPlayer));
            }
        }
        allTimes.sort(new TimeTrialDateComparator());

        Theme theme = TSDatabase.getPlayer(player).getTheme();
        int itemsPerPage = TimingSystem.configuration.getTimesPageSize();
        int start = (pageStart * itemsPerPage) - itemsPerPage;
        int stop = pageStart * itemsPerPage;

        if (start >= allTimes.size()) {
            Text.send(player, Error.PAGE_NOT_FOUND);
            return;
        }

        Text.send(player, Info.PLAYER_RECENT_TIMES_TITLE, "%player%", tPlayer.getName());

        for (int i = start; i < stop; i++) {
            if (i == allTimes.size()) {
                break;
            }
            TimeTrialFinish finish = allTimes.get(i);
            Track track = TrackDatabase.getTrackById(finish.getTrack()).get();
            Component row = theme.getTimesRow(String.valueOf(i + 1), ApiUtilities.formatAsTime(finish.getTime()), track.getDisplayName(), ApiUtilities.niceDate(finish.getDate()));
            if (finish.hasCheckpointTimes()) {
                row = theme.getCheckpointHovers(finish, track.getBestFinish(tPlayer), row);
            }
            player.sendMessage(row);
        }
        int pageEnd = (int) Math.ceil(((double) allTimes.size()) / ((double) itemsPerPage));
        player.sendMessage(theme.getPageSelector(player, pageStart, pageEnd, "/t alltimes " + tPlayer.getName()));
    }

    @Subcommand("edit")
    @CommandCompletion("@track")
    @CommandPermission("%permissiontrack_session_edit")
    public static void onEdit(Player player, @Optional Track track) {
        if (track == null) {
            TimingSystem.playerEditingSession.remove(player.getUniqueId());
            Text.send(player, Success.SESSION_ENDED);
            return;
        }
        TimingSystem.playerEditingSession.put(player.getUniqueId(), track);
        Text.send(player, Success.SAVED);
    }

    @Subcommand("reload")
    @CommandPermission("%permissiontrack_reload")
    public static void onReload(CommandSender commandSender, @Optional String confirmText) {
        if(confirmText != null && confirmText.equals("confirm")) {
            TSDatabase.reload();
            Text.send(commandSender, Success.SAVED);
            return;
        }

        Text.send(commandSender, Warning.DANGEROUS_COMMAND, "%command%", "/track reload confirm");
    }

    @Subcommand("deletebesttime")
    @CommandCompletion("@track <playername>")
    @CommandPermission("%permissiontrack_delete_besttime")
    public static void onDeleteBestTime(CommandSender commandSender, Track track, String name) {
        TPlayer TPlayer = TSDatabase.getPlayer(name);
        if (TPlayer == null) {
            Text.send(commandSender, Error.PLAYER_NOT_FOUND);
            return;
        }

        TimeTrialFinish bestFinish = track.getBestFinish(TPlayer);
        if (bestFinish == null) {
            Text.send(commandSender, Error.NOTHING_TO_REMOVE);
            return;
        }
        track.deleteBestFinish(TPlayer, bestFinish);
        Text.send(commandSender, Success.REMOVED_BEST_FINISH, "%player%", TPlayer.getName(), "%track%", track.getDisplayName());
        LeaderboardManager.updateFastestTimeLeaderboard(track);
    }

    @Subcommand("deletealltimes")
    @CommandCompletion("@track <player>")
    @CommandPermission("%permissiontrack_delete_alltimes")
    public static void onDeleteAllTimes(CommandSender commandSender, Track track, @Optional String playerName) {
        if (playerName != null) {
            TPlayer tPlayer = TSDatabase.getPlayer(playerName);
            if (tPlayer == null) {
                Text.send(commandSender, Error.PLAYER_NOT_FOUND);
                return;
            }
            var message = Text.get(commandSender, Success.REMOVED_ALL_FINISHES).append(tPlayer.getTheme().success(" -> " + tPlayer.getNameDisplay()));
            commandSender.sendMessage(message);
            track.deleteAllFinishes(tPlayer);
            LeaderboardManager.updateFastestTimeLeaderboard(track);
            return;
        }
        track.deleteAllFinishes();
        Text.send(commandSender, Success.REMOVED_ALL_FINISHES);
        LeaderboardManager.updateFastestTimeLeaderboard(track);

    }

    @Subcommand("updateleaderboards")
    @CommandPermission("%permissiontrack_updateleaderboards")
    public static void onUpdateLeaderboards(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(TimingSystem.getPlugin(), LeaderboardManager::updateAllFastestTimeLeaderboard);
        Text.send(player, Info.UPDATING_LEADERBOARDS);
    }

    @Subcommand("set")
    public class Set extends BaseCommand {

        public static List<TrackRegion> restoreRegions = new ArrayList<>();
        public static List<TrackLocation> restoreLocations = new ArrayList<>();
        public static Track restoreTrack = null;

        @Subcommand("finishtp")
        @CommandCompletion("@track <index>")
        @CommandPermission("%permissiontrack_set_finishtp")
        public static void onSetTpFinish(Player player, Track track, @Optional String index) {
            if(index == null) {
                createOrUpdateTrackLocation(track, TrackLocation.Type.FINISH_TP_ALL, 1, player.getLocation());
            } else {
                if(index.equals("remove")) {
                    createOrUpdateTrackIndexLocation(track, TrackLocation.Type.FINISH_TP_ALL, "-1", player, player.getLocation());
                    return;
                }
                createOrUpdateTrackIndexLocation(track, TrackLocation.Type.FINISH_TP, index, player, player.getLocation());
            }
        }

        @Subcommand("open")
        @CommandCompletion("true|false @track")
        @CommandPermission("%permissiontrack_set_open")
        public static void onOpen(Player player, boolean open, Track track) {
            track.setOpen(open);
            if (track.isOpen()) {
                Text.send(player, Success.TRACK_NOW_OPEN);
            } else {
                Text.send(player, Success.TRACK_NOW_CLOSED);
            }
        }

        @Subcommand("weight")
        @CommandCompletion("<value> @track")
        @CommandPermission("%permissiontrack_set_weight")
        public static void onWeight(Player player, int weight, Track track) {
            track.setWeight(weight);
            Text.send(player, Success.SAVED);
        }

        @Subcommand("tag")
        @CommandCompletion("@track +/- @trackTag")
        @CommandPermission("%permissiontrack_set_tag")
        public static void onTag(CommandSender sender, Track track, String plusOrMinus, TrackTag tag) {
            if (!TrackTagManager.hasTag(tag)) {
                Text.send(sender, Error.TAG_NOT_FOUND);
                return;
            }
            if (plusOrMinus.equalsIgnoreCase("-")) {
                if (track.removeTag(tag)) {
                    Text.send(sender, Success.REMOVED);
                    return;
                }
                Text.send(sender, Error.FAILED_TO_REMOVE_TAG);
                return;
            }

            if (track.createTag(tag)) {
                Text.send(sender, Success.ADDED_TAG, "%tag%", tag.getValue());
                return;
            }

            Text.send(sender, Error.FAILED_TO_ADD_TAG);
        }

        @Subcommand("option")
        @CommandCompletion("@track +/- @trackOption")
        @CommandPermission("%permissiontrack_set_option")
        public static void onOption(CommandSender sender, Track track, String plusOrMinus, TrackOption option) {
            if (plusOrMinus.equalsIgnoreCase("-")) {
                if (track.removeTrackOption(option)) {
                    Text.send(sender, Success.REMOVED);
                    return;
                }
                Text.send(sender, Error.FAILED_TO_REMOVE_OPTION);
                return;
            }

            if (track.createTrackOption(option)) {
                Text.send(sender, Success.ADDED_OPTION, "%option%", option.toString());
                return;
            }

            Text.send(sender, Error.FAILED_TO_ADD_OPTION);
        }

        @Subcommand("type")
        @CommandCompletion("@trackType @track")
        @CommandPermission("%permissiontrack_set_type")
        public static void onType(Player player, Track.TrackType type, Track track) {
            track.setTrackType(type);
            Text.send(player, Success.SAVED);
        }

        @Subcommand("mode")
        @CommandCompletion("@trackMode @track")
        @CommandPermission("%permissiontrack_set_mode")
        public static void onMode(Player player, Track.TrackMode mode, Track track) {
            track.setMode(mode);
            Text.send(player, Success.SAVED);
        }

        @Subcommand("boatutils")
        @CommandCompletion("@allBoatUtilsMode @track")
        @CommandPermission("%permissiontrack_set_boatutilsmode")
        public static void onMode(Player player, BoatUtilsMode mode, Track track) {
            track.setBoatUtilsMode(mode);
            Text.send(player, Success.SAVED);
        }

        @Subcommand("spawn")
        @CommandCompletion("@track @region")
        @CommandPermission("%permissiontrack_set_location_spawn")
        public static void onSpawn(Player player, Track track, @Optional TrackRegion region) {
            if (region != null) {
                region.setSpawn(player.getLocation());
                Text.send(player, Success.SAVED);
                return;
            }
            track.setSpawnLocation(player.getLocation());
            Text.send(player, Success.SAVED);
        }

        @Subcommand("leaderboard")
        @CommandCompletion("@track <index>")
        @CommandPermission("%permissiontrack_set_location_leaderboard")
        public static void onLeaderboard(Player player, Track track, @Optional String index) {
            Location loc = player.getLocation();
            loc.setY(loc.getY() + 3);
            createOrUpdateTrackIndexLocation(track, TrackLocation.Type.LEADERBOARD, index, player, loc);
        }

        @Subcommand("name")
        @CommandCompletion("@track name")
        @CommandPermission("%permissiontrack_set_name")
        public static void onName(CommandSender commandSender, Track track, String name) {
            int maxLength = 25;
            if (name.length() > maxLength) {
                Text.send(commandSender, Error.LENGTH_EXCEEDED, "%length%", String.valueOf(maxLength));
                return;
            }

            if (ApiUtilities.checkTrackName(name)) {
                Text.send(commandSender, Error.TRACK_EXISTS);
                return;
            }

            if (!name.matches("[A-Za-z0-9 ]+")) {
                Text.send(commandSender, Error.NAME_FORMAT);
                return;
            }

            if (TrackDatabase.trackNameNotAvailable(name)) {
                Text.send(commandSender, Error.TRACK_EXISTS);
                return;
            }
            track.setName(name);
            Text.send(commandSender, Success.SAVED);
            LeaderboardManager.updateFastestTimeLeaderboard(track);

        }

        @Subcommand("gui")
        @CommandCompletion("@track")
        @CommandPermission("%permissiontrack_set_item")
        public static void onGui(Player player, Track track) {
            var item = player.getInventory().getItemInMainHand();
            if (item.getItemMeta() == null) {
                Text.send(player, Error.ITEM_NOT_FOUND);
                return;
            }
            track.setGuiItem(item);
            Text.send(player, Success.SAVED);
        }

        @Subcommand("owner")
        @CommandCompletion("@track <player>")
        @CommandPermission("%permissiontrack_set_owner")
        public static void onAddOwner(CommandSender commandSender, Track track, String name) {
            TPlayer TPlayer = TSDatabase.getPlayer(name);
            if (TPlayer == null) {
                Text.send(commandSender, Error.PLAYER_NOT_FOUND);
                return;
            }
            track.setOwner(TPlayer);
            Text.send(commandSender, Success.SAVED);
        }

        @Subcommand("contributors add")
        @CommandCompletion("@track <player>")
        @CommandPermission("%permissiontrack_set_contributors")
        public static void onAddContributor(CommandSender sender, Track track, String name) {
            TPlayer tPlayer = TSDatabase.getPlayer(name);
            if(tPlayer == null) {
                Text.send(sender, Error.PLAYER_NOT_FOUND);
                return;
            }
            track.addContributor(tPlayer);
            Text.send(sender, Success.SAVED);
        }

        @Subcommand("contributors remove")
        @CommandCompletion("@track <player>")
        @CommandPermission("%permissiontrack_set_contributors")
        public static void onRemoveContributor(CommandSender sender, Track track, String name) {
            TPlayer tPlayer = TSDatabase.getPlayer(name);
            if(tPlayer == null) {
                Text.send(sender, Error.PLAYER_NOT_FOUND);
                return;
            }
            track.removeContributor(tPlayer);
            Text.send(sender, Success.SAVED);
        }

        @Subcommand("startregion")
        @CommandCompletion("@track <index>")
        @CommandPermission("%permissiontrack_set_region_start")
        public static void onStartRegion(Player player, Track track, @Optional String index) {
            createOrUpdateIndexRegion(track, TrackRegion.RegionType.START, index, player, false);
        }

        @Subcommand("endregion")
        @CommandCompletion("@track <index>")
        @CommandPermission("%permissiontrack_set_region_end")
        public static void onEndRegion(Player player, Track track, @Optional String index) {
            createOrUpdateIndexRegion(track, TrackRegion.RegionType.END, index, player, false);
        }

        @Subcommand("pitregion")
        @CommandCompletion("@track <index>")
        @CommandPermission("%permissiontrack_set_region_pit")
        public static void onPitRegion(Player player, Track track, @Optional String index) {
            createOrUpdateIndexRegion(track, TrackRegion.RegionType.PIT, index, player, false);
        }

        @Subcommand("resetregion")
        @CommandCompletion("@track <index>")
        @CommandPermission("%permissiontrack_set_region_reset")
        public static void onResetRegion(Player player, Track track, @Optional String index) {
            createOrUpdateIndexRegion(track, TrackRegion.RegionType.RESET, index, player, false);
        }

        @Subcommand("inpit")
        @CommandCompletion("@track <index>")
        @CommandPermission("%permissiontrack_set_region_inpit")
        public static void onInPit(Player player, Track track, @Optional String index) {
            createOrUpdateIndexRegion(track, TrackRegion.RegionType.INPIT, index, player, false);
        }

        @Subcommand("lagstart")
        @CommandCompletion("@track <->")
        @CommandPermission("%permissiontrack_set_region_lagstart")
        public static void onLagStart(Player player, Track track, @Optional String remove) {
            boolean toRemove = false;
            if (remove != null) {
                toRemove = getParsedRemoveFlag(remove);
            }

            if (toRemove) {
                var maybeRegion = track.getRegion(TrackRegion.RegionType.LAGSTART);
                if (maybeRegion.isPresent()) {
                    if (track.removeRegion(maybeRegion.get())) {
                        Text.send(player, Success.REMOVED);
                    } else {
                        Text.send(player, Error.NOTHING_TO_REMOVE);
                    }
                } else {
                    Text.send(player, Error.FAILED_TO_REMOVE);
                }
                return;
            }
            if (createOrUpdateRegion(track, TrackRegion.RegionType.LAGSTART, player)) {
                Text.send(player, Success.CREATED);
            }
        }

        @Subcommand("lagend")
        @CommandCompletion("@track <->")
        @CommandPermission("%permissiontrack_set_region_lagend")
        public static void onLagEnd(Player player, Track track, @Optional String remove) {
            boolean toRemove = false;
            if (remove != null) {
                toRemove = getParsedRemoveFlag(remove);
            }

            if (toRemove) {
                var maybeRegion = track.getRegion(TrackRegion.RegionType.LAGEND);
                if (maybeRegion.isPresent()) {
                    if (track.removeRegion(maybeRegion.get())) {
                        Text.send(player, Success.REMOVED);
                    } else {
                        Text.send(player, Error.NOTHING_TO_REMOVE);
                    }
                } else {
                    Text.send(player, Error.FAILED_TO_REMOVE);
                }
                return;
            }
            if (createOrUpdateRegion(track, TrackRegion.RegionType.LAGEND, player)) {
                Text.send(player, Success.CREATED);
            }
        }

        @Subcommand("grid")
        @CommandCompletion("@track <index>")
        @CommandPermission("%permissiontrack_set_location_grid")
        public static void onGridLocation(Player player, Track track, @Optional String index) {
            createOrUpdateTrackIndexLocation(track, TrackLocation.Type.GRID, index, player, player.getLocation());
        }

        @Subcommand("qualygrid")
        @CommandCompletion("@track <index>")
        @CommandPermission("%permissiontrack_set_location_qualigrid")
        public static void onQualificationGridLocation(Player player, Track track, @Optional String index) {
            createOrUpdateTrackIndexLocation(track, TrackLocation.Type.QUALYGRID, index, player, player.getLocation());
        }

        @Subcommand("checkpoint")
        @CommandCompletion("@track <index>")
        @CommandPermission("%permissiontrack_set_region_checkpoint")
        public static void onCheckpoint(Player player, Track track, @Optional String index) {
            createOrUpdateIndexRegion(track, TrackRegion.RegionType.CHECKPOINT, index, player, false);
        }

        @Subcommand("checkpointoverload")
        @CommandCompletion("@track <index>")
        @CommandPermission("%permissiontrack_set_region_checkpoint")
        public static void onCheckpointOverload(Player player, Track track, String index) {
            createOrUpdateIndexRegion(track, TrackRegion.RegionType.CHECKPOINT, index, player, true);
        }

        private static boolean createOrUpdateRegion(Track track, TrackRegion.RegionType regionType, Player player) {
            var maybeSelection = ApiUtilities.getSelection(player);
            if (maybeSelection.isEmpty()) {
                Text.send(player, Error.SELECTION);
                return false;
            }
            var selection = maybeSelection.get();

            if (track.hasRegion(regionType)) {
                return track.updateRegion(regionType, selection, player.getLocation());
            } else {
                return track.createRegion(regionType, selection, player.getLocation());
            }
        }

        private static boolean createOrUpdateRegion(Track track, TrackRegion.RegionType regionType, int index, Player player, boolean overload) {
            var maybeSelection = ApiUtilities.getSelection(player);
            if (maybeSelection.isEmpty()) {
                Text.send(player, Error.SELECTION);
                return false;
            }
            var selection = maybeSelection.get();

            if (overload) {
                return track.createRegion(regionType, index, selection, player.getLocation());
            } else if (track.hasRegion(regionType, index)) {
                return track.updateRegion(track.getRegion(regionType, index).get(), selection, player.getLocation());
            } else {
                return track.createRegion(regionType, index, selection, player.getLocation());
            }
        }

        private static void createOrUpdateTrackLocation(Track track, TrackLocation.Type type, int index, Location location) {
            if (track.hasTrackLocation(type, index)) {
                track.updateTrackLocation(track.getTrackLocation(type, index).get(), location);
            } else {
                track.createTrackLocation(type, index, location);
            }
        }

        private static void createOrUpdateTrackIndexLocation(Track track, TrackLocation.Type type, String index, Player player, Location location) {
            int locationIndex;
            boolean remove = false;
            if (index != null) {

                if (index.equalsIgnoreCase("-all")) {
                    var trackLocations = track.getTrackLocations(type);
                    restoreLocations = trackLocations;
                    trackLocations.forEach(track::removeTrackLocation);
                    restoreTrack = track;
                    Text.send(player, Success.REMOVED_LOCATIONS, "%type%", type.name());
                    return;
                }

                if (index.equalsIgnoreCase("+all")) {
                    if (restoreLocations.isEmpty()) {
                        Text.send(player, Error.NOTHING_TO_RESTORE);
                        return;
                    }

                    if (restoreTrack != track) {
                        Text.send(player, Error.NOTHING_TO_RESTORE);
                        return;
                    }

                    for (TrackLocation restoreLocation : restoreLocations) {
                        var maybeLocation = track.getTrackLocation(restoreLocation.getLocationType(), restoreLocation.getIndex());
                        if (maybeLocation.isPresent()) {
                            continue;
                        }
                        track.createTrackLocation(restoreLocation.getLocationType(), restoreLocation.getIndex(), restoreLocation.getLocation());
                    }
                    Text.send(player, Success.RESTORED_LOCATIONS);
                    return;
                }

                remove = getParsedRemoveFlag(index);
                if (getParsedIndex(index) == null) {
                    Text.send(player, Error.NUMBER_FORMAT);
                    return;
                }
                locationIndex = getParsedIndex(index);

                if (locationIndex == 0) {
                    Text.send(player, Error.NO_ZERO_INDEX);
                    return;
                }
            } else {
                if (type == TrackLocation.Type.GRID || type == TrackLocation.Type.QUALYGRID) {
                    locationIndex = track.getTrackLocations(type).size() + 1;
                } else {
                    locationIndex = 1;
                }
            }
            if (remove) {
                var maybeLocation = track.getTrackLocation(type, locationIndex);
                if (maybeLocation.isPresent()) {
                    if (track.removeTrackLocation(maybeLocation.get())) {
                        Text.send(player, Success.REMOVED);
                    } else {
                        Text.send(player, Error.FAILED_TO_REMOVE);
                    }
                } else {
                    Text.send(player, Error.NOTHING_TO_REMOVE);
                }
                return;
            }
            createOrUpdateTrackLocation(track, type, locationIndex, location);
            Text.send(player, Success.SAVED);

        }

        private static void createOrUpdateIndexRegion(Track track, TrackRegion.RegionType regionType, String index, Player player, boolean overload) {
            int regionIndex;
            boolean remove = false;
            if (index != null) {
                if (index.equalsIgnoreCase("-all")) {
                    var regions = track.getRegions(regionType);
                    regions.forEach(track::removeRegion);
                    Text.send(player, Success.REMOVED_REGIONS, "%type%", regionType.name());
                    return;
                }

                remove = getParsedRemoveFlag(index);
                if (getParsedIndex(index) == null) {
                    Text.send(player, Error.NUMBER_FORMAT);
                    return;
                }
                regionIndex = getParsedIndex(index);

                if (regionIndex == 0) {
                    Text.send(player, Error.NO_ZERO_INDEX);
                    return;
                }
            } else {
                if (regionType == TrackRegion.RegionType.START || regionType == TrackRegion.RegionType.END || regionType == TrackRegion.RegionType.PIT) {
                    regionIndex = 1;
                } else if (regionType == TrackRegion.RegionType.CHECKPOINT) {
                    regionIndex = track.getNumberOfCheckpoints() + 1;
                } else {
                    regionIndex = track.getRegions(regionType).size() + 1;
                }
            }
            if (remove) {
                if (regionType == TrackRegion.RegionType.CHECKPOINT) {
                    var checkpointRegions = track.getCheckpointRegions(regionIndex);
                    if (!checkpointRegions.isEmpty()) {
                        for (TrackRegion region : checkpointRegions) {
                            if (track.removeRegion(region)) {
                                Text.send(player, Success.REMOVED);
                            } else {
                                Text.send(player, Error.FAILED_TO_REMOVE);
                            }
                        }
                    } else {
                        Text.send(player, Error.NOTHING_TO_REMOVE);
                    }
                } else {
                    var maybeRegion = track.getRegion(regionType, regionIndex);
                    if (maybeRegion.isPresent()) {
                        if (track.removeRegion(maybeRegion.get())) {
                            Text.send(player, Success.REMOVED);
                        } else {
                            Text.send(player, Error.FAILED_TO_REMOVE);
                        }
                    } else {
                        Text.send(player, Error.NOTHING_TO_REMOVE);
                    }
                }

                return;
            }
            if (createOrUpdateRegion(track, regionType, regionIndex, player, overload)) {
                Text.send(player, Success.SAVED);
            }
        }

        private static boolean getParsedRemoveFlag(String index) {
            return index.startsWith("-");
        }

        private static Integer getParsedIndex(String index) {
            if (index.startsWith("-")) {
                index = index.substring(1);
            } else if (index.startsWith("+")) {
                index = index.substring(1);
            }
            try {
                return Integer.parseInt(index);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
    }
}
