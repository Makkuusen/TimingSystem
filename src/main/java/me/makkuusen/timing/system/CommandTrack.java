package me.makkuusen.timing.system;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import com.sk89q.worldedit.math.BlockVector2;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.gui.TrackGui;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import me.makkuusen.timing.system.timetrial.TimeTrialDateComparator;
import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import me.makkuusen.timing.system.timetrial.TimeTrialFinishComparator;
import me.makkuusen.timing.system.timetrial.TimeTrialSession;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.makkuusen.timing.system.track.TrackLocation;
import me.makkuusen.timing.system.track.TrackPolyRegion;
import me.makkuusen.timing.system.track.TrackRegion;
import me.makkuusen.timing.system.track.TrackTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

@CommandAlias("track|t")
public class CommandTrack extends BaseCommand {
    static TimingSystem plugin;

    @Subcommand("move")
    @CommandCompletion("@track")
    @CommandPermission("track.admin")
    public static void onMove(Player player, Track track) {
        var moveTo = player.getLocation().toBlockLocation();
        var moveFrom = track.getSpawnLocation().toBlockLocation();
        Track moveTrack = track;
        World newWorld = moveTo.getWorld();
        track.setSpawnLocation(moveTo);
        var offset = getOffset(moveFrom, moveTo);
        player.sendMessage("§2Offset is X: " + offset.getX() + ", Y: " + offset.getY() + ", Z: " + offset.getZ());

        var trackLocations = track.getTrackLocations();
        for (TrackLocation tl: trackLocations) {
            Location loc = tl.getLocation();
            var newLoc = getNewLocation(newWorld, loc, offset);
            track.updateTrackLocation(tl, newLoc);
        }

        var regions = track.getRegions();
        for (TrackRegion region : regions){
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

        Bukkit.getScheduler().runTaskAsynchronously(TimingSystem.getPlugin(), () -> LeaderboardManager.updateAllFastestTimeLeaderboard(player));
        player.sendMessage("§aTrack was moved from " + ApiUtilities.niceLocation(moveTo) + " to " + ApiUtilities.niceLocation(moveFrom));
    }

    public static Vector getOffset(Location moveFrom, Location moveTo){
        var vector = new Vector();
        vector.setX(moveFrom.getX() - moveTo.getX());
        vector.setY(moveFrom.getY() - moveTo.getY());
        vector.setZ(moveFrom.getZ() - moveTo.getZ());
        return vector;
    }

    public static Location getNewLocation(World newWorld, Location oldLocation, Vector offset) {
        var referenceNewWorld = new Location(newWorld, oldLocation.getX(), oldLocation.getY(), oldLocation.getZ());
        referenceNewWorld.subtract(offset);
        return referenceNewWorld;
    }

    public static BlockVector2 getNewBlockVector2(BlockVector2 old, Vector offset) {
        return BlockVector2.at(old.getX() - offset.getX(), old.getZ() - offset.getZ());
    }


    @Default
    @CommandPermission("track.admin")
    public static void onTrack(Player player) {
        new TrackGui(Database.getPlayer(player.getUniqueId()),0).show(player);
    }

    @Subcommand("tp")
    @CommandPermission("track.admin")
    @CommandCompletion("@track @region")
    public static void onTrackTp(Player player, Track track, @Optional String region) {
        if (!track.getSpawnLocation().isWorldLoaded()) {
            player.sendMessage("§cWorld is not loaded!");
            return;
        }


        if (region != null) {
            var rg = region.split("-");
            if (rg.length != 2) {
                player.sendMessage("§cIncorrect syntax");
                return;
            }
            String name = rg[0];
            String index = rg[1];

            var trackRegion = getRegion(track, name, index);

            if (trackRegion != null) {
                player.teleport(trackRegion.getSpawnLocation());
                player.sendMessage("§aYou have been teleported to " + trackRegion.getRegionType().name() + " : " + trackRegion.getRegionIndex());
                return;
            }

            var trackLocation = getTrackLocation(track, name, index);

            if (trackLocation != null) {
                player.teleport(trackLocation.getLocation());
                player.sendMessage("§aYou have been teleported to " + trackLocation.getLocationType().name() + " : " + trackLocation.getIndex());
                return;
            }

            player.sendMessage("§cYour teleport location could not be determined, does it exist?");
        } else {
            player.teleport(track.getSpawnLocation());
            player.sendMessage("§aYou have been teleported to " + track.getDisplayName());
        }
    }

    private static TrackRegion getRegion(Track track, String name, String index) {
        try {
            var regionType = TrackRegion.RegionType.valueOf(name);
            var regionIndex = Integer.valueOf(index);

            var trackRegion = track.getRegion(regionType, regionIndex);
            if (trackRegion.isPresent()) {
                return trackRegion.get();
            } else {
                return null;
            }

        } catch (NumberFormatException ex) {
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static TrackLocation getTrackLocation(Track track, String name, String index) {
        try {
            var locationType = TrackLocation.Type.valueOf(name);
            var regionIndex = Integer.valueOf(index);

            var trackLocation = track.getTrackLocation(locationType, regionIndex);
            if (trackLocation.isPresent()) {
                return trackLocation.get();
            } else {
                return null;
            }

        } catch (NumberFormatException ex) {
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }


    @Subcommand("create")
    @CommandCompletion("@trackType name")
    @CommandPermission("track.admin")
    public static void onCreate(Player player, Track.TrackType trackType, String name) {
        int maxLength = 25;
        if (name.length() > maxLength) {
            plugin.sendMessage(player, "messages.error.nametoLong", "%length%", String.valueOf(maxLength));
            return;
        }

        if (!name.matches("[A-Za-zÅÄÖåäöØÆøæ0-9 ]+")) {
            plugin.sendMessage(player, "messages.error.nameRegexException");
            return;
        }

        if(ApiUtilities.checkTrackName(name)){
            TimingSystem.getPlugin().sendMessage(player, "messages.error.trackExists");
            return;
        }

        if (!TrackDatabase.trackNameAvailable(name)) {
            plugin.sendMessage(player, "messages.error.trackExists");
            return;
        }

        if (player.getInventory().getItemInMainHand().getItemMeta() == null) {
            plugin.sendMessage(player, "messages.error.missing.item");
            return;
        }

        Track track = TrackDatabase.trackNew(name, player.getUniqueId(), player.getLocation(), trackType, player.getInventory().getItemInMainHand());
        if (track == null) {
            plugin.sendMessage(player, "messages.error.generic");
            return;
        }
        track.setOptions("b");

        plugin.sendMessage(player, "messages.create.name", "%name%", name);
        LeaderboardManager.updateFastestTimeLeaderboard(track);
    }

    @Subcommand("info")
    @CommandCompletion("@track @players")
    public static void onInfo(CommandSender commandSender, Track track, @Optional String name) {

        TPlayer tPlayer;
        if (name != null && commandSender.isOp()) {
            tPlayer = Database.getPlayer(name);
            if (tPlayer == null) {
                commandSender.sendMessage("§cCould not find player");
                return;
            }
            commandSender.sendMessage(" ");
            commandSender.sendMessage(Component.text("§2--- §a" + tPlayer.getName() + "§2 statistics on §a" + track.getDisplayName() + "§2--- "));
            String bestTime;
            if (track.getBestFinish(tPlayer) == null) {
                bestTime = "§2" + tPlayer.getName() + "'s best time: §a(none)";
            } else {
                bestTime = "§2"+ tPlayer.getName() + "'s best time: §a" + ApiUtilities.formatAsTime(track.getBestFinish(tPlayer).getTime());
            }

            commandSender.sendMessage(Component.text("§2Position: §a" + (track.getPlayerTopListPosition(tPlayer) == -1 ? "(none)" : track.getPlayerTopListPosition(tPlayer))));
            commandSender.sendMessage(Component.text(bestTime));
            commandSender.sendMessage(Component.text("§2Total Finishes: §a" + track.getPlayerTotalFinishes(tPlayer)));
            commandSender.sendMessage(Component.text("§2Total Attempts: §a" + (track.getPlayerTotalFinishes(tPlayer) + track.getPlayerTotalAttempts(tPlayer))));
            commandSender.sendMessage(Component.text("§2Time spent: §a" + ApiUtilities.formatAsTimeSpent(track.getPlayerTotalTimeSpent(tPlayer))));
            return;
        }


        commandSender.sendMessage(" ");
        plugin.sendMessage(commandSender, "messages.info.track.name", "%name%", track.getDisplayName(), "%id%", String.valueOf(track.getId()));
        if (track.isOpen()) {
            plugin.sendMessage(commandSender, "messages.info.track.open", "%type%", track.getTypeAsString());
        } else {
            plugin.sendMessage(commandSender, "messages.info.track.closed", "%type%", track.getTypeAsString());
        }
        plugin.sendMessage(commandSender, "messages.info.track.created", "%date%", ApiUtilities.niceDate(track.getDateCreated()), "%owner%", track.getOwner().getName());
        plugin.sendMessage(commandSender, "messages.info.track.options", "%options%", ApiUtilities.formatPermissions(track.getOptions()));
        plugin.sendMessage(commandSender, "messages.info.track.mode", "%mode%", track.getModeAsString());
        plugin.sendMessage(commandSender, "messages.info.track.checkpoints", "%size%", String.valueOf(track.getRegions(TrackRegion.RegionType.CHECKPOINT).size()));
        if (track.getTrackLocations(TrackLocation.Type.GRID).size() != 0) {
            commandSender.sendMessage("§2Grids: §a" + track.getTrackLocations(TrackLocation.Type.GRID).size());
        }
        if (track.getTrackLocations(TrackLocation.Type.QUALYGRID).size() != 0) {
            commandSender.sendMessage("§2QualyGrids: §a" + track.getTrackLocations(TrackLocation.Type.QUALYGRID).size());
        }
        plugin.sendMessage(commandSender, "messages.info.track.resets", "%size%", String.valueOf(track.getRegions(TrackRegion.RegionType.RESET).size()));
        plugin.sendMessage(commandSender, "messages.info.track.spawn", "%location%", ApiUtilities.niceLocation(track.getSpawnLocation()));
        commandSender.sendMessage("§2Weight: §a" + track.getWeight());
        List<String> tagList = new ArrayList<>();
        for (TrackTag tag : track.getTags()) {
            tagList.add(tag.getValue());
        }
        String tags = String.join(", ", tagList);
        commandSender.sendMessage("§2Tags: §a" + tags);

    }

    @Subcommand("regions")
    @CommandCompletion("@track")
    @CommandPermission("track.admin")
    public static void onRegions(CommandSender sender, Track track) {
        sender.sendMessage("§2--- Regions for §a" + track.getDisplayName() + "§2 ---");

        for (var regionType : TrackRegion.RegionType.values()) {
            for (TrackRegion trackRegion : track.getRegions(regionType)) {

                String regionText = trackRegion.getRegionType().name() + "-" + trackRegion.getRegionIndex();
                var message = Component.text("§2-> §a" + regionText).clickEvent(ClickEvent.runCommand("/t tp " + track.getCommandName() + " " + regionText));
                sender.sendMessage(message);
            }
        }
    }

    @Subcommand("locations")
    @CommandCompletion("@track")
    @CommandPermission("track.admin")
    public static void onLocations(CommandSender sender, Track track) {
        sender.sendMessage("§2--- Locations for §a" + track.getDisplayName() + "§2 ---");

        for (var locationType : TrackLocation.Type.values()) {
            for (TrackLocation trackLocation : track.getTrackLocations(locationType)) {
                String locationText = trackLocation.getLocationType().name() + "-" + trackLocation.getIndex();
                var message = Component.text("§2-> §a" + locationText).clickEvent(ClickEvent.runCommand("/t tp " + track.getCommandName() + " " + locationText));
                sender.sendMessage(message);
            }
        }
    }

    @Subcommand("here")
    public static void onHere(Player player) {
        boolean inRegion = false;
        for (Track track : TrackDatabase.getTracks()) {
            for (TrackRegion region : track.getRegions()) {
                if (region.contains(player.getLocation())) {
                    inRegion = true;
                    player.sendMessage("§a" + track.getDisplayName() + " - " + region.getRegionType() + " : " + region.getRegionIndex());
                }
            }
        }

        if (!inRegion) {
            player.sendMessage("§cThere are no regions here.");
        }
    }

    @Subcommand("session")
    @CommandCompletion("@track")
    public static void toggleSession(Player player, @Optional Track track) {

        var maybeDriver = TimingSystemAPI.getDriverFromRunningHeat(player.getUniqueId());
        if (maybeDriver.isPresent()) {
            if (maybeDriver.get().isRunning()) {
                player.sendMessage("§cYou can't start a session when you are in a heat.");
                return;
            }
        }

        if (TimeTrialController.timeTrialSessions.containsKey(player.getUniqueId())) {
            var ttSession = TimeTrialController.timeTrialSessions.get(player.getUniqueId());
            ttSession.clearScoreboard();
            TimeTrialController.timeTrialSessions.remove(player.getUniqueId());
            if (track == null) {
                player.sendMessage("§aYour session has ended");
                return;
            }
            if (!track.getSpawnLocation().isWorldLoaded()) {
                player.sendMessage("§cWorld is not loaded!");
                return;
            }

            if (!track.isOpen() && !(player.isOp() || player.hasPermission("track.admin"))) {
                player.sendMessage("§cTrack is closed!");
                return;
            }

            if (track.getId() != ttSession.getTrack().getId()) {
                var newSession = new TimeTrialSession(Database.getPlayer(player.getUniqueId()), track);
                newSession.updateScoreboard();
                TimeTrialController.timeTrialSessions.put(player.getUniqueId(), newSession);
                player.sendMessage("§aSession started on " + track.getDisplayName());
                ApiUtilities.teleportPlayerAndSpawnBoat(player, track, track.getSpawnLocation());
                return;
            }
            player.sendMessage("§aYour session has ended");
            return;
        }

        if (track == null) {
            player.sendMessage("§cYou have no session to end");
            return;
        }

        TimeTrialSession ttSession = new TimeTrialSession(Database.getPlayer(player.getUniqueId()), track);
        ttSession.updateScoreboard();
        TimeTrialController.timeTrialSessions.put(player.getUniqueId(), ttSession);
        player.sendMessage("§aSession started on " + track.getDisplayName());
        ApiUtilities.teleportPlayerAndSpawnBoat(player, track, track.getSpawnLocation());

    }

    @Subcommand("delete")
    @CommandCompletion("@track")
    @CommandPermission("track.admin")
    public static void onDelete(Player player, Track track) {
        TrackDatabase.removeTrack(track);
        plugin.sendMessage(player, "messages.remove.track");
    }


    @Subcommand("times")
    @CommandCompletion("@track <page>")
    public static void onTimes(CommandSender commandSender, Track track, @Optional Integer pageStart) {
        if (pageStart == null) {
            pageStart = 1;
        }
        int itemsPerPage = TimingSystem.configuration.getTimesPageSize();
        int start = (pageStart * itemsPerPage) - itemsPerPage;
        int stop = pageStart * itemsPerPage;

        if (start >= track.getTopList().size()) {
            plugin.sendMessage(commandSender, "messages.error.missing.page");
            return;
        }

        var trackText = Component.text("--- Times for ").color(NamedTextColor.DARK_GREEN)
                .append(Component.text(track.getDisplayName()).color(NamedTextColor.GREEN))
                .append(Component.text(" ---").color(NamedTextColor.DARK_GREEN));
        commandSender.sendMessage(trackText);
        int count = 0;
        for (int i = start; i < stop; i++) {
            if (i == track.getTopList().size()) {
                break;
            }
            TimeTrialFinish finish = track.getTopList().get(i);
            plugin.sendMessage(commandSender, "messages.list.timesrow", "%pos%", String.valueOf(i + 1), "%player%", finish.getPlayer().getName(), "%time%", ApiUtilities.formatAsTime(finish.getTime()));
            count++;
        }

        var pageText = Component.text("--- ").color(NamedTextColor.DARK_GREEN);
        if (pageStart > 1) {
            pageText = pageText.append(Component.text("<<< ").color(NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/t times " + track.getCommandName() + " " + (pageStart - 1))));
        }

        int pageEnd = (int) Math.ceil(((double) track.getTopList().size()) / ((double) itemsPerPage));
        pageText = pageText.append(Component.text("page ").color(NamedTextColor.DARK_GREEN))
                .append(Component.text(pageStart + " ").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .append(Component.text("of ").color(NamedTextColor.DARK_GREEN))
                .append(Component.text(pageEnd + " ").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));

        if (pageEnd > pageStart) {
            pageText = pageText.append(Component.text(">>> ").color(NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/t times " + track.getCommandName() + " " + (pageStart + 1))));
        }
        pageText = pageText.append(Component.text("---").color(NamedTextColor.DARK_GREEN));
        commandSender.sendMessage(pageText);
    }

    @Subcommand("mytimes")
    @CommandCompletion("@track <page>")
    public static void onMyTimes(Player player, Track track, @Optional Integer pageStart) {
        if (pageStart == null) {
            pageStart = 1;
        }

        var tPlayer = Database.getPlayer(player.getUniqueId());
        List<TimeTrialFinish> allTimes = new ArrayList<>();
        if (track.getTimeTrialFinishes().containsKey(tPlayer)) {
            allTimes.addAll(track.getTimeTrialFinishes().get(tPlayer));
            allTimes.sort(new TimeTrialFinishComparator());
        }

        int itemsPerPage = TimingSystem.configuration.getTimesPageSize();
        int start = (pageStart * itemsPerPage) - itemsPerPage;
        int stop = pageStart * itemsPerPage;

        if (start >= allTimes.size()) {
            plugin.sendMessage(player, "messages.error.missing.page");
            return;
        }

        var trackText = Component.text("§2--- §a" + player.getName() + "§2 best times on §a" + track.getDisplayName() + " §2--- ");

        player.sendMessage(trackText);
        for (int i = start; i < stop; i++) {
            if (i == allTimes.size()) {
                break;
            }
            TimeTrialFinish finish = allTimes.get(i);
            player.sendMessage("§2" + (i + 1) + ". §a" + ApiUtilities.formatAsTime(finish.getTime()) + " §2| §a" + ApiUtilities.niceDate(finish.getDate()));
        }

        var pageText = Component.text("§2--- ");
        if (pageStart > 1) {
            pageText = pageText.append(Component.text("§a<<< ").clickEvent(ClickEvent.runCommand("/t mytimes " + track.getCommandName() + " " + (pageStart - 1))));
        }
        int pageEnd = (int) Math.ceil(((double) allTimes.size()) / ((double) itemsPerPage));

        pageText = pageText.append(Component.text("§2page §a§l" + pageStart + " §r§2of §a§l" + pageEnd + " "));
        if (pageEnd > pageStart) {
            pageText = pageText.append(Component.text("§a>>>").clickEvent(ClickEvent.runCommand("/t mytimes " + track.getCommandName() + " " + (pageStart + 1))));
        }
        pageText = pageText.append(Component.text(" §2---"));
        player.sendMessage(pageText);
    }

    @Subcommand("alltimes")
    @CommandCompletion("@players <page>")
    public static void onAllTimes(Player player, @Optional String name, @Optional Integer pageStart) {
        if (pageStart == null) {
            pageStart = 1;
        }
        TPlayer tPlayer;
        if (name != null) {
            tPlayer = Database.getPlayer(name);
            if (tPlayer == null) {
                player.sendMessage("§cCould not find player");
                return;
            }
        } else {
            tPlayer = Database.getPlayer(player.getUniqueId());
        }

        List<TimeTrialFinish> allTimes = new ArrayList<>();
        var tracks = TrackDatabase.getOpenTracks();
        for (Track t : tracks) {
            if (t.getTimeTrialFinishes().containsKey(tPlayer)) {
                allTimes.addAll(t.getTimeTrialFinishes().get(tPlayer));
            }
        }
        allTimes.sort(new TimeTrialDateComparator());

        int itemsPerPage = TimingSystem.configuration.getTimesPageSize();
        int start = (pageStart * itemsPerPage) - itemsPerPage;
        int stop = pageStart * itemsPerPage;

        if (start >= allTimes.size()) {
            plugin.sendMessage(player, "messages.error.missing.page");
            return;
        }

        var trackText = Component.text("§2--- §a" + tPlayer.getName() + "§2 most recent times §2--- ");

        player.sendMessage(trackText);
        for (int i = start; i < stop; i++) {
            if (i == allTimes.size()) {
                break;
            }
            TimeTrialFinish finish = allTimes.get(i);
            player.sendMessage("§2" + (i + 1) + ". §a" + ApiUtilities.formatAsTime(finish.getTime()) + " §2| §a" + TrackDatabase.getTrackById(finish.getTrack()).get().getDisplayName() + " §2| §a" + ApiUtilities.niceDate(finish.getDate()));
        }

        var pageText = Component.text("§2--- ");
        if (pageStart > 1) {
            pageText = pageText.append(Component.text("§a<<< ").clickEvent(ClickEvent.runCommand("/t alltimes " + tPlayer.getName() + " " + (pageStart - 1))));
        }
        int pageEnd = (int) Math.ceil(((double) allTimes.size()) / ((double) itemsPerPage));

        pageText = pageText.append(Component.text("§2page §a§l" + pageStart + " §r§2of §a§l" + pageEnd + " "));
        if (pageEnd > pageStart) {
            pageText = pageText.append(Component.text("§a>>>").clickEvent(ClickEvent.runCommand("/t alltimes " + tPlayer.getName() + " " + (pageStart + 1))));
        }
        pageText = pageText.append(Component.text(" §2---"));
        player.sendMessage(pageText);
    }

    @Subcommand("list")
    @CommandCompletion("<page>")
    public static void onList(CommandSender commandSender, @Optional Integer pageStart) {
        if (TrackDatabase.getTracks().size() == 0) {
            plugin.sendMessage(commandSender, "messages.error.missing.tracks");
            return;
        }
        if (pageStart == null) {
            pageStart = 1;
        }
        StringBuilder tmpMessage = new StringBuilder();

        int itemsPerPage = 25;
        int start = (pageStart * itemsPerPage) - itemsPerPage;
        int stop = pageStart * itemsPerPage;

        if (start >= TrackDatabase.getTracks().size()) {
            plugin.sendMessage(commandSender, "messages.error.missing.page");
            return;
        }

        for (int i = start; i < stop; i++) {
            if (i == TrackDatabase.getTracks().size()) {
                break;
            }

            Track track = TrackDatabase.getTracks().get(i);

            tmpMessage.append(track.getDisplayName()).append(", ");

        }
        plugin.sendMessage(commandSender, "messages.list.tracks", "%startPage%", String.valueOf(pageStart), "%totalPages%", String.valueOf((int) Math.ceil(((double) TrackDatabase.getTracks().size()) / ((double) itemsPerPage))));
        commandSender.sendMessage("§2" + tmpMessage.substring(0, tmpMessage.length() - 2));

    }

    @Subcommand("edit")
    @CommandCompletion("@track")
    @CommandPermission("track.admin")
    public static void onEdit(Player player, @Optional Track track){
        if (track == null) {
            TimingSystem.playerEditingSession.remove(player.getUniqueId());
            player.sendMessage("§aRemoved from editing session");
            return;
        }
        TimingSystem.playerEditingSession.put(player.getUniqueId(), track);
        plugin.sendMessage(player, "messages.save.generic");
    }

    @Subcommand("options")
    @CommandCompletion("@track options")
    @CommandPermission("track.admin")
    public static void onOptions(CommandSender commandSender, Track track, String options){
        String newOptions = ApiUtilities.parseFlagChange(track.getOptions(), options);
        if (newOptions == null) {
            plugin.sendMessage(commandSender, "messages.save.generic");
            return;
        }

        if (newOptions.length() == 0) {
            plugin.sendMessage(commandSender, "messages.options.allRemoved");
        } else {
            plugin.sendMessage(commandSender, "messages.options.list", "%options%", ApiUtilities.formatPermissions(newOptions.toCharArray()));
        }
        track.setOptions(newOptions);
    }

    @Subcommand("override")
    @CommandPermission("track.admin")
    public static void onOverride(Player player) {
        player.sendMessage("§cDid you mean /settings override?");
    }

    @Subcommand("reload")
    @CommandPermission("track.admin")
    public static void onReload(CommandSender commandSender){
        commandSender.sendMessage("§cYou are doing this on your own risk, everything might break!");
        Database.reload();
    }

    @Subcommand("deletebesttime")
    @CommandPermission("track.admin")
    @CommandCompletion("@track <playername>")
    public static void onDeleteBestTime(CommandSender commandSender, Track track, String name){
        TPlayer TPlayer = Database.getPlayer(name);
        if (TPlayer == null) {
            plugin.sendMessage(commandSender, "messages.error.missing.player");
            return;
        }

        TimeTrialFinish bestFinish = track.getBestFinish(TPlayer);
        if (bestFinish == null) {
            plugin.sendMessage(commandSender, "messages.error.missing.bestTime");
            return;
        }
        track.deleteBestFinish(TPlayer, bestFinish);
        plugin.sendMessage(commandSender, "messages.remove.bestTime", "%player%", TPlayer.getName(), "%map%", track.getDisplayName());
        LeaderboardManager.updateFastestTimeLeaderboard(track);
    }

    @Subcommand("deletealltimes")
    @CommandPermission("track.admin")
    @CommandCompletion("@track <player>")
    public static void onDeleteAllTimes(CommandSender commandSender, Track track, @Optional String playerName){
        if (playerName != null) {
            TPlayer tPlayer = Database.getPlayer(playerName);
            if (tPlayer == null) {
                commandSender.sendMessage("§cCould not find player");
                return;
            }
            commandSender.sendMessage("§aAll finishes has been reset for " + tPlayer.getNameDisplay());
            track.deleteAllFinishes(tPlayer);
            LeaderboardManager.updateFastestTimeLeaderboard(track);
            return;
        }
        track.deleteAllFinishes();
        commandSender.sendMessage("§aAll finishes has been reset");
        LeaderboardManager.updateFastestTimeLeaderboard(track);

    }

    @Subcommand("updateleaderboards")
    @CommandPermission("track.admin")
    public static void onUpdateLeaderboards(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(TimingSystem.getPlugin(), () -> LeaderboardManager.updateAllFastestTimeLeaderboard(player));
        plugin.sendMessage(player, "messages.update.leaderboards");
    }

    @Subcommand("set")
    @CommandPermission("track.admin")
    public class Set extends BaseCommand {

        @Subcommand("open")
        @CommandCompletion("true|false @track")
        public static void onOpen(Player player, boolean open, Track track) {
            track.setOpen(open);
            if (track.isOpen()) {
                plugin.sendMessage(player, "messages.toggle.track.open");
            } else {
                plugin.sendMessage(player, "messages.toggle.track.closed");
            }
        }

        @Subcommand("weight")
        @CommandCompletion("<value> @track")
        public static void onWeight(Player player, int weight, Track track) {
            track.setWeight(weight);
            plugin.sendMessage(player, "messages.save.generic");
        }

        @Subcommand("tag")
        @CommandCompletion("@track +/- @trackTag")
        public static void onTag(CommandSender sender, Track track, String plusOrMinus, TrackTag tag) {

            if (!TrackTagManager.hasTag(tag)) {
                sender.sendMessage("§cTag does not exist");
                return;
            }
            if (plusOrMinus.equalsIgnoreCase("-")) {
                if (track.removeTag(tag)) {
                    sender.sendMessage("§aTag '" + tag.getValue() + "' was removed.");
                    return;
                }

                sender.sendMessage("§cTag '" + tag.getValue() + "' could not be removed.");
                return;
            }

            if (track.createTag(tag)) {
                sender.sendMessage("§aTag '" + tag.getValue() + "' was added.");
                return;
            }

            sender.sendMessage("§cTag '" + tag.getValue() + "' could not be added.");
            return;

        }

        @Subcommand("type")
        @CommandCompletion("@trackType @track")
        public static void onType(Player player, Track.TrackType type, Track track) {
            track.setTrackType(type);
            plugin.sendMessage(player, "messages.save.generic");
        }

        @Subcommand("mode")
        @CommandCompletion("@trackMode @track")
        public static void onMode(Player player, Track.TrackMode mode, Track track) {
            track.setMode(mode);
            plugin.sendMessage(player, "messages.save.generic");
        }

        @Subcommand("spawn")
        @CommandCompletion("@track @region")
        public static void onSpawn(Player player, Track track, @Optional TrackRegion region) {
            if (region != null) {
                region.setSpawn(player.getLocation());
                plugin.sendMessage(player, "messages.save.generic");
                return;
            }
            track.setSpawnLocation(player.getLocation());
            plugin.sendMessage(player, "messages.save.generic");
        }

        @Subcommand("leaderboard")
        @CommandCompletion("@track <index>")
        public static void onLeaderboard(Player player, Track track, @Optional String index) {
            Location loc = player.getLocation();
            loc.setY(loc.getY() + 3);
            createOrUpdateTrackIndexLocation(track, TrackLocation.Type.LEADERBOARD, index, player, loc);
        }

        @Subcommand("name")
        @CommandCompletion("@track name")
        public static void onName(CommandSender commandSender, Track track, String name) {
            int maxLength = 25;
            if (name.length() > maxLength) {
                plugin.sendMessage(commandSender, "messages.error.nametoLong", "%length%", String.valueOf(maxLength));
                return;
            }

            if(ApiUtilities.checkTrackName(name)){
                plugin.sendMessage(commandSender, "messages.error.trackExists");
                return;
            }

            if (!name.matches("[A-Za-zÅÄÖåäöØÆøæ0-9 ]+")) {
                plugin.sendMessage(commandSender, "messages.error.nameRegexException");
                return;
            }

            if (!TrackDatabase.trackNameAvailable(name)) {
                plugin.sendMessage(commandSender, "messages.error.trackExists");
                return;
            }
            track.setName(name);
            plugin.sendMessage(commandSender, "messages.save.generic");
            LeaderboardManager.updateFastestTimeLeaderboard(track);

        }

        @Subcommand("gui")
        @CommandCompletion("@track")
        public static void onGui(Player player, Track track) {
            var item = player.getInventory().getItemInMainHand();
            if (item.getItemMeta() == null) {
                plugin.sendMessage(player, "messages.error.missing.item");
                return;
            }
            track.setGuiItem(item);
            plugin.sendMessage(player, "messages.save.generic");
        }

        @Subcommand("owner")
        @CommandCompletion("@track <player>")
        public static void onOwner(CommandSender commandSender, Track track, String name) {
            TPlayer TPlayer = Database.getPlayer(name);
            if (TPlayer == null) {
                plugin.sendMessage(commandSender, "messages.error.missing.player");
                return;
            }
            track.setOwner(TPlayer);
            plugin.sendMessage(commandSender, "messages.save.generic");
        }

        @Subcommand("startregion")
        @CommandCompletion("@track <index>")
        public static void onStartRegion(Player player, Track track, @Optional String index) {
            createOrUpdateIndexRegion(track, TrackRegion.RegionType.START, index, player);
        }

        @Subcommand("endregion")
        @CommandCompletion("@track <index>")
        public static void onEndRegion(Player player, Track track, @Optional String index) {
            createOrUpdateIndexRegion(track, TrackRegion.RegionType.END, index, player);
        }

        @Subcommand("pitregion")
        @CommandCompletion("@track <index>")
        public static void onPitRegion(Player player, Track track, @Optional String index) {
            createOrUpdateIndexRegion(track, TrackRegion.RegionType.PIT, index, player);
        }

        @Subcommand("resetregion")
        @CommandCompletion("@track <index>")
        public static void onResetRegion(Player player, Track track, @Optional String index) {
            createOrUpdateIndexRegion(track, TrackRegion.RegionType.RESET, index, player);
        }

        @Subcommand("inpit")
        @CommandCompletion("@track <index>")
        public static void onInPit(Player player, Track track, @Optional String index) {
            createOrUpdateIndexRegion(track, TrackRegion.RegionType.INPIT, index, player);
        }

        @Subcommand("lagstart")
        @CommandCompletion("@track <->")
        public static void onLagStart(Player player, Track track, @Optional String remove) {
            boolean toRemove = false;
            if (remove != null) {
                toRemove = getParsedRemoveFlag(remove);
            }

            if (toRemove) {
                var maybeRegion = track.getRegion(TrackRegion.RegionType.LAGSTART);
                if (maybeRegion.isPresent()) {
                    if (track.removeRegion(maybeRegion.get())) {
                        plugin.sendMessage(player, "messages.remove.region");
                        return;
                    } else {
                        plugin.sendMessage(player, "messages.error.remove.region");
                        return;
                    }
                } else {
                    player.sendMessage("§cRegion doesn't currently exist");
                    return;
                }
            }
            if (createOrUpdateRegion(track, TrackRegion.RegionType.LAGSTART, player)) {
                plugin.sendMessage(player, "messages.create.region");
                return;
            }
        }

        @Subcommand("lagend")
        @CommandCompletion("@track <->")
        public static void onLagEnd(Player player, Track track, @Optional String remove) {
            boolean toRemove = false;
            if (remove != null) {
                toRemove = getParsedRemoveFlag(remove);
            }

            if (toRemove) {
                var maybeRegion = track.getRegion(TrackRegion.RegionType.LAGEND);
                if (maybeRegion.isPresent()) {
                    if (track.removeRegion(maybeRegion.get())) {
                        plugin.sendMessage(player, "messages.remove.region");
                        return;
                    } else {
                        plugin.sendMessage(player, "messages.error.remove.region");
                        return;
                    }
                } else {
                    player.sendMessage("§cRegion doesn't currently exist");
                    return;
                }
            }
            if (createOrUpdateRegion(track, TrackRegion.RegionType.LAGEND, player)) {
                plugin.sendMessage(player, "messages.create.region");
                return;
            }
        }

        @Subcommand("grid")
        @CommandCompletion("@track <index>")
        public static void onGridLocation(Player player, Track track, @Optional String index) {
            createOrUpdateTrackIndexLocation(track, TrackLocation.Type.GRID, index, player, player.getLocation());
        }

        @Subcommand("qualygrid")
        @CommandCompletion("@track <index>")
        public static void onQualyGridLocation(Player player, Track track, @Optional String index) {
            createOrUpdateTrackIndexLocation(track, TrackLocation.Type.QUALYGRID, index, player, player.getLocation());
        }

        @Subcommand("checkpoint")
        @CommandCompletion("@track <index>")
        public static void onCheckpoint(Player player, Track track, @Optional String index) {
            createOrUpdateIndexRegion(track, TrackRegion.RegionType.CHECKPOINT, index, player);
        }

        private static boolean createOrUpdateRegion(Track track, TrackRegion.RegionType regionType, Player player) {
            var maybeSelection = ApiUtilities.getSelection(player);
            if (maybeSelection.isEmpty()) {
                plugin.sendMessage(player, "messages.error.missing.selection");
                return false;
            }
            var selection = maybeSelection.get();

            if (track.hasRegion(regionType)) {
                return track.updateRegion(regionType, selection, player.getLocation());
            } else {
                return track.createRegion(regionType, selection, player.getLocation());
            }
        }

        private static boolean createOrUpdateRegion(Track track, TrackRegion.RegionType regionType, int index, Player player) {
            var maybeSelection = ApiUtilities.getSelection(player);
            if (maybeSelection.isEmpty()) {
                plugin.sendMessage(player, "messages.error.missing.selection");
                return false;
            }
            var selection = maybeSelection.get();

            if (track.hasRegion(regionType, index)) {
                return track.updateRegion(track.getRegion(regionType, index).get(), selection, player.getLocation());
            } else {
                return track.createRegion(regionType, index, selection, player.getLocation());
            }
        }

        private static void createOrUpdateTrackLocation(Track track, TrackLocation.Type type, int index, Player player, Location location) {
            if (track.hasTrackLocation(type, index)) {
                track.updateTrackLocation(track.getTrackLocation(type, index).get(), location);
            } else {
                track.createTrackLocation(type, index, location);
            }
        }

        private static boolean createOrUpdateTrackIndexLocation(Track track, TrackLocation.Type type, String index, Player player, Location location) {
            int locationIndex;

            boolean remove = false;
            if (index != null) {
                remove = getParsedRemoveFlag(index);
                if (getParsedIndex(index) == null) {
                    plugin.sendMessage(player, "messages.error.numberException");
                    return false;
                }
                locationIndex = getParsedIndex(index);
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
                        plugin.sendMessage(player, "messages.remove.generic");
                        return true;
                    } else {
                        player.sendMessage("§cTrack location could not be removed.");
                        return false;
                    }
                } else {
                    player.sendMessage("§cTrack location doesn't currently exist");
                    return false;
                }
            }
            createOrUpdateTrackLocation(track, type, locationIndex, player, location);
            player.sendMessage("§aTrack Location has been updated");
            return false;

        }

        private static boolean createOrUpdateIndexRegion(Track track, TrackRegion.RegionType regionType, String index, Player player) {
            int regionIndex;

            boolean remove = false;
            if (index != null) {
                remove = getParsedRemoveFlag(index);
                if (getParsedIndex(index) == null) {
                    plugin.sendMessage(player, "messages.error.numberException");
                    return false;
                }
                regionIndex = getParsedIndex(index);
            } else {
                if (regionType == TrackRegion.RegionType.START || regionType == TrackRegion.RegionType.END || regionType == TrackRegion.RegionType.PIT) {
                    regionIndex = 1;
                } else {
                    regionIndex = track.getRegions(regionType).size() + 1;
                }
            }
            if (remove) {
                var maybeRegion = track.getRegion(regionType, regionIndex);
                if (maybeRegion.isPresent()) {
                    if (track.removeRegion(maybeRegion.get())) {
                        plugin.sendMessage(player, "messages.remove.region");
                        return true;
                    } else {
                        plugin.sendMessage(player, "messages.error.remove.region");
                        return false;
                    }
                } else {
                    player.sendMessage("§cRegion doesn't currently exist");
                    return false;
                }
            }
            if (createOrUpdateRegion(track, regionType, regionIndex, player)) {
                plugin.sendMessage(player, "messages.create.region");
                return true;
            }
            player.sendMessage("§cRegion could not be created/updated");
            return false;
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
