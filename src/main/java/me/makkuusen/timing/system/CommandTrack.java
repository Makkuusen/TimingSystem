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
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

@CommandAlias("track|t")
public class CommandTrack extends BaseCommand {

    static TimingSystem plugin;

    @Default
    public static void onTrack(Player player) {
        GUITrack.openTrackGUI(player);
    }

    @Subcommand("tp")
    @CommandCompletion("@track")
    public static void onTrackTp(Player player, Track track) {
        player.teleport(track.getSpawnLocation());
    }


    @Subcommand("create")
    @CommandCompletion("@trackType name")
    @CommandPermission("track.admin")
    public static void onCreate(Player player, Track.TrackType trackType, String name) {
        player.sendMessage(name);

        int maxLength = 25;
        if (name.length() > maxLength) {
            plugin.sendMessage(player, "messages.error.nametoLong", "%length%", String.valueOf(maxLength));
            return;
        }

        if (!name.matches("[A-Za-zÅÄÖåäöØÆøæ0-9 ]+")) {
            plugin.sendMessage(player, "messages.error.nameRegexException");
            return;
        }

        if (!DatabaseTrack.trackNameAvailable(name)) {
            plugin.sendMessage(player, "messages.error.trackExists");
            return;
        }

        if (player.getInventory().getItemInMainHand().getItemMeta() == null) {
            plugin.sendMessage(player, "messages.error.missing.item");
            return;
        }

        Track track = DatabaseTrack.trackNew(name, player.getUniqueId(), player.getLocation(), trackType, player.getInventory().getItemInMainHand());
        if (track == null) {
            plugin.sendMessage(player, "messages.error.generic");
            return;
        }

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
        plugin.sendMessage(player, "messages.info.track.checkpoints", "%size%", String.valueOf(track.getCheckpoints().size()));
        plugin.sendMessage(player, "messages.info.track.resets", "%size%", String.valueOf(track.getResetRegions().size()));
        plugin.sendMessage(player, "messages.info.track.spawn", "%location%", ApiUtilities.niceLocation(track.getSpawnLocation()));
        plugin.sendMessage(player, "messages.info.track.leaderboard", "%location%", ApiUtilities.niceLocation(track.getLeaderboardLocation()));
    }

    @Subcommand("delete")
    @CommandCompletion("@track")
    @CommandPermission("track.admin")
    public static void onDelete(Player player, Track track) {
        DatabaseTrack.removeTrack(track);
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

        plugin.sendMessage(player, "messages.list.times", "%track%", track.getDisplayName(), "%startPage%", String.valueOf(pageStart), "%totalPages%", String.valueOf((int) Math.ceil(((double) track.getTopList().size()) / ((double) itemsPerPage))));

        for (int i = start; i < stop; i++) {
            if (i == track.getTopList().size()) {
                break;
            }

            TimeTrialFinish finish = track.getTopList().get(i);
            plugin.sendMessage(player, "messages.list.timesrow", "%pos%", String.valueOf(i + 1), "%player%", finish.getPlayer().getName(), "%time%", ApiUtilities.formatAsTime(finish.getTime()));
        }
    }

    @Subcommand("list")
    @CommandCompletion("@track <page>")
    public static void onList(Player player, @Optional Integer pageStart) {
        if (DatabaseTrack.getTracks().size() == 0) {
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

        if (start >= DatabaseTrack.getTracks().size()) {
            plugin.sendMessage(player, "messages.error.missing.page");
            return;
        }

        for (int i = start; i < stop; i++) {
            if (i == DatabaseTrack.getTracks().size()) {
                break;
            }

            Track track = DatabaseTrack.getTracks().get(i);

            tmpMessage.append(track.getDisplayName()).append(", ");

        }
        plugin.sendMessage(player, "messages.list.tracks", "%startPage%", String.valueOf(pageStart), "%totalPages%", String.valueOf((int) Math.ceil(((double) DatabaseTrack.getTracks().size()) / ((double) itemsPerPage))));
        player.sendMessage("§2" + tmpMessage.substring(0, tmpMessage.length() - 2));

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

        @Subcommand("name")
        @CommandCompletion("@track name")
        public static void onName(Player player, Track track, String name) {
            int maxLength = 25;
            if (name.length() > maxLength) {
                plugin.sendMessage(player, "messages.error.nametoLong", "%length%", String.valueOf(maxLength));
                return;
            }

            if (!name.matches("[A-Za-zÅÄÖåäöØÆøæ0-9 ]+")) {
                plugin.sendMessage(player, "messages.error.nameRegexException");
                return;
            }

            if (!DatabaseTrack.trackNameAvailable(name)) {
                plugin.sendMessage(player, "messages.error.trackExists");
                return;
            }
            track.setName(name);
            plugin.sendMessage(player, "messages.save.generic");
            LeaderboardManager.updateFastestTimeLeaderboard(track.getId());

        }

        @Subcommand("startregion")
        @CommandCompletion("@track")
        public static void onStartRegion(Player player, Track track) {
            List<Location> positions = ApiUtilities.getPositions(player);
            if (positions == null) {
                return;
            }
            track.updateStartRegion(positions.get(0), positions.get(1));
            plugin.sendMessage(player, "messages.create.region");
        }

        @Subcommand("endregion")
        @CommandCompletion("@track")
        public static void onEndRegion(Player player, Track track) {
            List<Location> positions = ApiUtilities.getPositions(player);
            if (positions == null) {
                return;
            }
            track.updateEndRegion(positions.get(0), positions.get(1));
            plugin.sendMessage(player, "messages.create.region");
        }

        @Subcommand("pitregion")
        @CommandCompletion("@track")
        public static void onPitRegion(Player player, Track track) {
            List<Location> positions = ApiUtilities.getPositions(player);
            if (positions == null) {
                return;
            }
            track.setPitRegion(positions.get(0), positions.get(1), player.getLocation());
            plugin.sendMessage(player, "messages.create.region");
        }

        @Subcommand("resetregion")
        @CommandCompletion("@track <index>")
        public static void onResetRegion(Player player, Track track, @Optional String index) {
            int regionIndex;
            boolean remove = false;
            if (index.startsWith("-")) {
                index = index.substring(1);
                remove = true;
            } else if (index.startsWith("+")) {
                index = index.substring(1);
            }
            try {
                regionIndex = Integer.parseInt(index);
            } catch (NumberFormatException exception) {
                plugin.sendMessage(player, "messages.error.numberException");
                return;
            }
            if (remove) {
                if (track.removeResetRegion(regionIndex)) {
                    plugin.sendMessage(player, "messages.remove.region");
                } else {
                    plugin.sendMessage(player, "messages.error.remove.region");
                }
            } else {
                List<Location> positions = ApiUtilities.getPositions(player);
                if (positions == null) {
                    return;
                }
                track.setResetRegion(positions.get(0), positions.get(1), player.getLocation(), regionIndex);
                plugin.sendMessage(player, "messages.create.region");
            }
        }

        @Subcommand("gridregion")
        @CommandCompletion("@track <index>")
        public static void onGridRegion(Player player, Track track, @Optional String index) {
            int regionIndex;
            boolean remove = false;
            if (index.startsWith("-")) {
                index = index.substring(1);
                remove = true;
            } else if (index.startsWith("+")) {
                index = index.substring(1);
            }
            try {
                regionIndex = Integer.parseInt(index);
            } catch (NumberFormatException exception) {
                plugin.sendMessage(player, "messages.error.numberException");
                return;
            }
            if (remove) {
                if (track.removeGridRegion(regionIndex)) {
                    plugin.sendMessage(player, "messages.remove.region");
                } else {
                    plugin.sendMessage(player, "messages.error.remove.region");
                }
            } else {
                List<Location> positions = ApiUtilities.getPositions(player);
                if (positions == null) {
                    return;
                }
                track.setGridRegion(positions.get(0), positions.get(1), player.getLocation(), regionIndex);
                plugin.sendMessage(player, "messages.create.region");
            }
        }

        @Subcommand("checkpoint")
        @CommandCompletion("@track <index>")
        public static void onCheckpoint(Player player, Track track, @Optional String index) {
            int regionIndex;
            boolean remove = false;
            if (index.startsWith("-")) {
                index = index.substring(1);
                remove = true;
            } else if (index.startsWith("+")) {
                index = index.substring(1);
            }

            try {
                regionIndex = Integer.parseInt(index);
            } catch (NumberFormatException exception) {
                plugin.sendMessage(player, "messages.error.numberException");
                return;
            }

            if (remove) {
                if (track.removeCheckpoint(regionIndex)) {
                    plugin.sendMessage(player, "messages.remove.checkpoint");
                } else {
                    plugin.sendMessage(player, "messages.error.remove.checkpoint");
                }
            } else {
                List<Location> positions = ApiUtilities.getPositions(player);
                if (positions == null) {
                    return;
                }
                track.setCheckpoint(positions.get(0), positions.get(1), player.getLocation(), regionIndex);
                plugin.sendMessage(player, "messages.create.checkpoint");
            }
        }
    }
}
