package me.makkuusen.timing.system;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import me.makkuusen.timing.system.gui.GUITrack;
import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.makkuusen.timing.system.track.TrackRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@CommandAlias("track|t")
public class CommandTrack extends BaseCommand {

    static TimingSystem plugin;

    @Default
    public static void onTrack(Player player) {
        GUITrack.openTrackGUI(player);
    }

    @Subcommand("tp")
    @CommandPermission("track.admin")
    @CommandCompletion("@track @region")
    public static void onTrackTp(Player player, Track track, @Optional TrackRegion region) {
        if (!track.getSpawnLocation().isWorldLoaded()) {
            player.sendMessage("§cWorld is not loaded!");
            return;
        }

        if (region != null) {
            player.teleport(region.getSpawnLocation());
            player.sendMessage("§aYou have been teleported to " + region.getRegionType().name() + " : " + region.getRegionIndex());
        } else {
            player.teleport(track.getSpawnLocation());
            player.sendMessage("§aYou have been teleported to " + track.getDisplayName());
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
        LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
    }

    @Subcommand("info")
    @CommandCompletion("@track")
    public static void onInfo(Player player, Track track) {
        player.sendMessage(" ");
        plugin.sendMessage(player, "messages.info.track.name", "%name%", track.getDisplayName(), "%id%", String.valueOf(track.getId()));
        if (track.isOpen()) {
            plugin.sendMessage(player, "messages.info.track.open", "%type%", track.getTypeAsString());
        } else {
            plugin.sendMessage(player, "messages.info.track.closed", "%type%", track.getTypeAsString());
        }
        plugin.sendMessage(player, "messages.info.track.created", "%date%", ApiUtilities.niceDate(track.getDateCreated()), "%owner%", track.getOwner().getName());
        plugin.sendMessage(player, "messages.info.track.options", "%options%", ApiUtilities.formatPermissions(track.getOptions()));
        plugin.sendMessage(player, "messages.info.track.mode", "%mode%", track.getModeAsString());
        plugin.sendMessage(player, "messages.info.track.checkpoints", "%size%", String.valueOf(track.getRegions(TrackRegion.RegionType.CHECKPOINT).size()));
        if (track.getGrids().size() != 0) {
            player.sendMessage("§2Grids: §a" + track.getGrids().size());
        }
        plugin.sendMessage(player, "messages.info.track.resets", "%size%", String.valueOf(track.getRegions(TrackRegion.RegionType.RESET).size()));
        plugin.sendMessage(player, "messages.info.track.spawn", "%location%", ApiUtilities.niceLocation(track.getSpawnLocation()));
        plugin.sendMessage(player, "messages.info.track.leaderboard", "%location%", ApiUtilities.niceLocation(track.getLeaderboardLocation()));

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

    @Subcommand("delete")
    @CommandCompletion("@track")
    @CommandPermission("track.admin")
    public static void onDelete(Player player, Track track) {
        TrackDatabase.removeTrack(track);
        plugin.sendMessage(player, "messages.remove.track");
    }

    @Subcommand("times")
    @CommandCompletion("@track <page>")
    public static void onTimes(Player player, Track track, @Optional Integer pageStart) {
        if (pageStart == null) {
            pageStart = 1;
        }
        int itemsPerPage = TimingSystem.configuration.getTimesPageSize();
        int start = (pageStart * itemsPerPage) - itemsPerPage;
        int stop = pageStart * itemsPerPage;

        if (start >= track.getTopList().size()) {
            plugin.sendMessage(player, "messages.error.missing.page");
            return;
        }

        var trackText = Component.text("§2--- Times for §a" + track.getDisplayName() + " §2--- ");
        //plugin.sendMessage(player, "messages.list.times", "%track%", track.getDisplayName(), "%startPage%", String.valueOf(pageStart), "%totalPages%", String.valueOf((int) Math.ceil(((double) track.getTopList().size()) / ((double) itemsPerPage))));
        player.sendMessage(trackText);
        int count = 0;
        for (int i = start; i < stop; i++) {
            if (i == track.getTopList().size()) {
                break;
            }
            TimeTrialFinish finish = track.getTopList().get(i);
            plugin.sendMessage(player, "messages.list.timesrow", "%pos%", String.valueOf(i + 1), "%player%", finish.getPlayer().getName(), "%time%", ApiUtilities.formatAsTime(finish.getTime()));
            count++;
        }

        var pageText = Component.text("§2--- ");
        if (pageStart > 1) {
            pageText = pageText.append(Component.text("§a<<< ").clickEvent(ClickEvent.runCommand("/t times " + track.getCommandName() + " " + (pageStart - 1))));
        }
        int pageEnd = (int) Math.ceil(((double) track.getTopList().size()) / ((double) itemsPerPage));
        pageText = pageText.append(Component.text("§2page §a§l" + pageStart + " §r§2of §a§l" + pageEnd + " "));
        if (pageEnd > pageStart) {
            pageText = pageText.append(Component.text("§a>>>").clickEvent(ClickEvent.runCommand("/t times " + track.getCommandName() + " " + (pageStart + 1))));
        }
        pageText = pageText.append(Component.text(" §2---"));
        player.sendMessage(pageText);
    }

    @Subcommand("list")
    @CommandCompletion("@track <page>")
    public static void onList(Player player, @Optional Integer pageStart) {
        if (TrackDatabase.getTracks().size() == 0) {
            plugin.sendMessage(player, "messages.error.missing.tracks");
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
            plugin.sendMessage(player, "messages.error.missing.page");
            return;
        }

        for (int i = start; i < stop; i++) {
            if (i == TrackDatabase.getTracks().size()) {
                break;
            }

            Track track = TrackDatabase.getTracks().get(i);

            tmpMessage.append(track.getDisplayName()).append(", ");

        }
        plugin.sendMessage(player, "messages.list.tracks", "%startPage%", String.valueOf(pageStart), "%totalPages%", String.valueOf((int) Math.ceil(((double) TrackDatabase.getTracks().size()) / ((double) itemsPerPage))));
        player.sendMessage("§2" + tmpMessage.substring(0, tmpMessage.length() - 2));

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
    public static void onOptions(Player player, Track track, String options){
        String newOptions = ApiUtilities.parseFlagChange(track.getOptions(), options);
        if (newOptions == null) {
            plugin.sendMessage(player, "messages.save.generic");
            return;
        }

        if (newOptions.length() == 0) {
            plugin.sendMessage(player, "messages.options.allRemoved");
        } else {
            plugin.sendMessage(player, "messages.options.list", "%options%", ApiUtilities.formatPermissions(newOptions.toCharArray()));
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
    public static void onReload(Player player){
        player.sendMessage("§cYou are doing this on your own risk, everything might break!");
        Database.reload();
    }

    @Subcommand("deletebesttime")
    @CommandPermission("track.admin")
    @CommandCompletion("@track <playername>")
    public static void onDeleteBestTime(Player player, Track track, String name){
        TPlayer TPlayer = Database.getPlayer(name);
        if (TPlayer == null) {
            plugin.sendMessage(player, "messages.error.missing.player");
            return;
        }

        TimeTrialFinish bestFinish = track.getBestFinish(TPlayer);
        if (bestFinish == null) {
            plugin.sendMessage(player, "messages.error.missing.bestTime");
            return;
        }
        track.deleteBestFinish(TPlayer, bestFinish);
        plugin.sendMessage(player, "messages.remove.bestTime", "%player%", TPlayer.getName(), "%map%", track.getDisplayName());
        LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
    }

    @Subcommand("deletealltimes")
    @CommandPermission("track.admin")
    @CommandCompletion("@track <player>")
    public static void onDeleteAllTimes(Player player, Track track, @Optional String playerName){
        if (playerName != null) {
            TPlayer tPlayer = Database.getPlayer(playerName);
            if (tPlayer == null) {
                player.sendMessage("§cCould not find player");
                return;
            }
            player.sendMessage("§aAll finishes has been reset for " + tPlayer.getNameDisplay());
            track.deleteAllFinishes(tPlayer);
            return;
        }
        if (track.deleteAllFinishes()){
            player.sendMessage("§aAll finishes has been reset");
            LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
            return;
        }
        player.sendMessage("§cFailed to delete all finishes");
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
        @CommandCompletion("@track")
        public static void onLeaderboard(Player player, Track track) {
            Location loc = player.getLocation();
            loc.setY(loc.getY() + 3);
            track.setLeaderboardLocation(loc);
            plugin.sendMessage(player, "messages.save.generic");
        }

        @Subcommand("name")
        @CommandCompletion("@track name")
        public static void onName(Player player, Track track, String name) {
            int maxLength = 25;
            if (name.length() > maxLength) {
                plugin.sendMessage(player, "messages.error.nametoLong", "%length%", String.valueOf(maxLength));
                return;
            }

            if(ApiUtilities.checkTrackName(name)){
                plugin.sendMessage(player, "messages.error.trackExists");
                return;
            }

            if (!name.matches("[A-Za-zÅÄÖåäöØÆøæ0-9 ]+")) {
                plugin.sendMessage(player, "messages.error.nameRegexException");
                return;
            }

            if (!TrackDatabase.trackNameAvailable(name)) {
                plugin.sendMessage(player, "messages.error.trackExists");
                return;
            }
            track.setName(name);
            plugin.sendMessage(player, "messages.save.generic");
            LeaderboardManager.updateFastestTimeLeaderboard(track.getId());

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
        public static void onOwner(Player player, Track track, String name) {
            TPlayer TPlayer = Database.getPlayer(name);
            if (TPlayer == null) {
                plugin.sendMessage(player, "messages.error.missing.player");
                return;
            }
            track.setOwner(TPlayer);
            plugin.sendMessage(player, "messages.save.generic");
        }

        @Subcommand("startregion")
        @CommandCompletion("@track <->")
        public static void onStartRegion(Player player, Track track, @Optional String remove) {
            boolean toRemove = false;
            if (remove != null) {
                toRemove = getParsedRemoveFlag(remove);
            }

            if (toRemove) {
                var maybeRegion = track.getRegion(TrackRegion.RegionType.START);
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
            if (createOrUpdateRegion(track, TrackRegion.RegionType.START, player)) {
                plugin.sendMessage(player, "messages.create.region");
                return;
            }
        }

        @Subcommand("endregion")
        @CommandCompletion("@track <->")
        public static void onEndRegion(Player player, Track track, @Optional String remove) {
            boolean toRemove = false;
            if (remove != null) {
                toRemove = getParsedRemoveFlag(remove);
            }

            if (toRemove) {
                var maybeRegion = track.getRegion(TrackRegion.RegionType.END);
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
            if (createOrUpdateRegion(track, TrackRegion.RegionType.END, player)) {
                plugin.sendMessage(player, "messages.create.region");
                return;
            }
        }

        @Subcommand("pitregion")
        @CommandCompletion("@track <->")
        public static void onPitRegion(Player player, Track track, @Optional String remove) {
            boolean toRemove = false;
            if (remove != null) {
                toRemove = getParsedRemoveFlag(remove);
            }

            if (toRemove) {
                var maybeRegion = track.getRegion(TrackRegion.RegionType.PIT);
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

            if (createOrUpdateRegion(track, TrackRegion.RegionType.PIT, player)) {
                plugin.sendMessage(player, "messages.create.region");
                return;
            }
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

        @Subcommand("grid")
        @CommandCompletion("@track <index>")
        public static void onGridRegion(Player player, Track track, @Optional String index) {
            int regionIndex;
            boolean remove = false;
            if (index != null) {
                remove = getParsedRemoveFlag(index);
                if (getParsedIndex(index) == null) {
                    plugin.sendMessage(player, "messages.error.numberException");
                    return;
                }
                regionIndex = getParsedIndex(index);
            } else {
                regionIndex = track.getGridLocations().size() + 1;
            }
            if (remove) {
                if (track.removeGridLocation(regionIndex)) {
                    plugin.sendMessage(player, "messages.remove.region");
                } else {
                    plugin.sendMessage(player, "messages.error.remove.region");
                }
            } else {
                track.setGridLocation(player.getLocation(), regionIndex);
                plugin.sendMessage(player, "messages.create.region");
            }
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
                regionIndex = track.getRegions(regionType).size() + 1;
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
