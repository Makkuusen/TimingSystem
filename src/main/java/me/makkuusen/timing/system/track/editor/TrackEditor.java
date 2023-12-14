package me.makkuusen.timing.system.track.editor;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.LeaderboardManager;
import me.makkuusen.timing.system.boatutils.BoatUtilsMode;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.logger.LogEntryBuilder;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Message;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.locations.TrackLocation;
import me.makkuusen.timing.system.track.options.TrackOption;
import me.makkuusen.timing.system.track.regions.TrackRegion;
import me.makkuusen.timing.system.track.tags.TrackTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class TrackEditor {


    public static Set<UUID> playerTrackVisualisation = new HashSet<>();
    private static HashMap<UUID, Track> playerTrackSelection = new HashMap<>();


    public static Track getPlayerTrackSelection(UUID uuid) {
        return playerTrackSelection.get(uuid);
    }

    public static Component setVisualisation(Player player, boolean add, Track track) {
        if (add) {
            playerTrackVisualisation.add(player.getUniqueId());
            return Text.get(player, Success.VIEWING_STARTED, "%track%", track.getDisplayName());
        } else {
            playerTrackVisualisation.remove(player.getUniqueId());
            return Text.get(player, Success.VIEWING_ENDED);
        }
    }

    public static Component toggleVisualisation(Player player, Track track) {
        if (playerTrackVisualisation.contains(player.getUniqueId())) {
            playerTrackVisualisation.remove(player.getUniqueId());
            return Text.get(player, Success.VIEWING_ENDED);
        } else {
            playerTrackVisualisation.add(player.getUniqueId());
            return Text.get(player, Success.VIEWING_STARTED, "%track%", track.getDisplayName());
        }
    }

    public static boolean hasTrackSelected(UUID uuid) {
        return playerTrackSelection.containsKey(uuid);
    }

    public static void setPlayerTrackSelection(UUID uuid, Track track) {
        playerTrackSelection.put(uuid, track);
    }

    public static Component setName(Player player, String name, Track track) {
        if (track == null) {
            if (hasTrackSelected(player.getUniqueId())) {
                track = getPlayerTrackSelection(player.getUniqueId());
            } else {
                return Text.get(player, Error.TRACK_NOT_FOUND_FOR_EDIT);
            }
        }
        var response = validateTrackName(player, name);
        if (response != null) {
            return response;
        }
        track.setName(name);
        LeaderboardManager.updateFastestTimeLeaderboard(track);
        return Text.get(player, Success.SAVED);
    }

    public static Message setOpen(Player player, boolean open, Track track) {
        if (track == null) {
            if (hasTrackSelected(player.getUniqueId())) {
                track = getPlayerTrackSelection(player.getUniqueId());
            } else {
                return Error.TRACK_NOT_FOUND_FOR_EDIT;
            }
        }
        track.setOpen(open);
        if (track.isOpen()) {
            return Success.TRACK_NOW_OPEN;
        } else {
            return Success.TRACK_NOW_CLOSED;
        }
    }

    public static Message setWeight(Player player, int weight, Track track) {
        if (track == null) {
            if (hasTrackSelected(player.getUniqueId())) {
                track = getPlayerTrackSelection(player.getUniqueId());
            } else {
                return Error.TRACK_NOT_FOUND_FOR_EDIT;
            }
        }
        track.setWeight(weight);
        return Success.SAVED;
    }

    public static Component removeTag(Player player, TrackTag tag, Track track) {
        if (track == null) {
            if (hasTrackSelected(player.getUniqueId())) {
                track = getPlayerTrackSelection(player.getUniqueId());
            } else {
                return Text.get(player,Error.TRACK_NOT_FOUND_FOR_EDIT);
            }
        }
        if (track.getTrackTags().remove(tag)) {
            return Text.get(player, Success.REMOVED);
        }
        return Text.get(player, Error.FAILED_TO_REMOVE_TAG);
    }

    public static Component addTag(Player player, TrackTag tag, Track track) {
        if (track == null) {
            if (hasTrackSelected(player.getUniqueId())) {
                track = getPlayerTrackSelection(player.getUniqueId());
            } else {
                return Text.get(player, Error.TRACK_NOT_FOUND_FOR_EDIT);
            }
        }
        if (track.getTrackTags().create(tag)) {
            return Text.get(player, Success.ADDED_TAG, "%tag%", tag.toString());
        }
        return Text.get(player, Error.FAILED_TO_ADD_TAG);
    }

    public static Component handleOption(Player player, String options) {
        Theme theme = Theme.getTheme(player);

        Track track;
        if (hasTrackSelected(player.getUniqueId()))
            track = getPlayerTrackSelection(player.getUniqueId());
        else
            return Text.get(player, Error.TRACK_NOT_FOUND_FOR_EDIT);

        String[] separatedOptions = options.split(" ");
        List<TrackOption> trackOptions = Arrays.stream(separatedOptions).map((option) -> {
            try {
                return TrackOption.valueOf(option.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
            return null;
        }).toList();
        List<Component> results = new ArrayList<>();

        int optionIndex = -1;
        for(TrackOption op : trackOptions) {
            optionIndex++;
            if(op == null) {
                results.add(Component.text(separatedOptions[optionIndex], theme.getWarning(), TextDecoration.STRIKETHROUGH));
                continue;
            }

            if(track.getTrackOptions().getTrackOptions().contains(op)) {
                track.getTrackOptions().remove(op);
                results.add(Component.text(op.name().toLowerCase(), theme.getError()));
                continue;
            }

            track.getTrackOptions().add(op);
            results.add(Component.text(op.name().toLowerCase(), theme.getSuccess()));
        }

        Component resultsText = results.get(0);
        for(Component result : results.subList(1, results.size())) {
            resultsText = resultsText.append(Component.text(", ", theme.getPrimary(), new HashSet<>())).append(result);
        }

        return Text.get(player, Success.UPDATED_TOGGLEABLE_VALUES, "%value%", "TrackOptions").append(resultsText);
    }

    /*public static Component removeOption(Player player, TrackOption option, Track track) {
        if (track == null) {
            if (hasTrackSelected(player.getUniqueId())) {
                track = getPlayerTrackSelection(player.getUniqueId());
            } else {
                return Text.get(player, Error.TRACK_NOT_FOUND_FOR_EDIT);
            }
        }
        if (track.getTrackOptions().remove(option)) {
            return Text.get(player, Success.REMOVED);
        }
        return Text.get(player, Error.FAILED_TO_REMOVE_OPTION);
    }

    public static Component addOption(Player player, TrackOption option, Track track) {
        if (track == null) {
            if (hasTrackSelected(player.getUniqueId())) {
                track = getPlayerTrackSelection(player.getUniqueId());
            } else {
                return Text.get(player, Error.TRACK_NOT_FOUND_FOR_EDIT);
            }
        }
        if (track.getTrackOptions().create(option)) {
            return Text.get(player, Success.ADDED_OPTION, "%option%", option.toString());
        }
        return Text.get(player, Error.FAILED_TO_ADD_OPTION);
    }
     */

    public static Message setTrackType(Player player, Track.TrackType type, Track track) {
        if (track == null) {
            if (hasTrackSelected(player.getUniqueId())) {
                track = getPlayerTrackSelection(player.getUniqueId());
            } else {
                return Error.TRACK_NOT_FOUND_FOR_EDIT;
            }
        }
        track.setTrackType(type);
        return Success.SAVED;
    }

    public static Message setBoatUtilsMode(Player player, BoatUtilsMode mode, Track track) {
        if (track == null) {
            if (hasTrackSelected(player.getUniqueId())) {
                track = getPlayerTrackSelection(player.getUniqueId());
            } else {
                return Error.TRACK_NOT_FOUND_FOR_EDIT;
            }
        }
        track.setBoatUtilsMode(mode);
        return Success.SAVED;
    }

    public static Message setSpawn(Player player, Track track) {
        if (track == null) {
            if (hasTrackSelected(player.getUniqueId())) {
                track = getPlayerTrackSelection(player.getUniqueId());
            } else {
                return Error.TRACK_NOT_FOUND_FOR_EDIT;
            }
        }
        track.setSpawnLocation(player.getLocation());
        return Success.SAVED;
    }

    public static Component createOrUpdateLocation(Player player, TrackLocation.Type trackLocationType, String index) {
        Track track = getPlayerTrackSelection(player.getUniqueId());
        if (track == null) {
            return Text.get(player, Error.TRACK_NOT_FOUND_FOR_EDIT);
        }
        return LocationEditor.createOrUpdateLocation(player, track, trackLocationType, index);

    }

    public static Component createOrUpdateRegion(Player player, TrackRegion.RegionType regionType, String index, boolean overload) {
        Track track = getPlayerTrackSelection(player.getUniqueId());
        if (track == null) {
            return Text.get(player, Error.TRACK_NOT_FOUND_FOR_EDIT);
        }
        return RegionEditor.createOrUpdateRegion(player, track, regionType, index, overload);
    }

    static Integer getParsedIndex(String index) throws NumberFormatException{
        if (index.startsWith("-")) {
            index = index.substring(1);
        } else if (index.startsWith("+")) {
            index = index.substring(1);
        }
        return Integer.parseInt(index);
    }

    static boolean getParsedRemoveFlag(String index) {
        return index.startsWith("-");
    }

    public static Message setOwner(Player player, String name, Track track) {
        if (track == null) {
            if (hasTrackSelected(player.getUniqueId())) {
                track = getPlayerTrackSelection(player.getUniqueId());
            } else {
                return Error.TRACK_NOT_FOUND_FOR_EDIT;
            }
        }
        TPlayer tPlayer = TSDatabase.getPlayer(name);
        if (tPlayer == null) {
            return Error.PLAYER_NOT_FOUND;
        }
        track.setOwner(tPlayer);
        return Success.SAVED;
    }

    public static Component handleContributor(Player player, String names) {
        Theme theme = Theme.getTheme(player);

        Track track;
        if (hasTrackSelected(player.getUniqueId()))
            track = getPlayerTrackSelection(player.getUniqueId());
        else
            return Text.get(player, Error.TRACK_NOT_FOUND_FOR_EDIT);

        String[] playerNames = names.split(" ");
        List<TPlayer> tPlayers = Arrays.stream(playerNames).map(TSDatabase::getPlayer).toList();
        List<Component> results = new ArrayList<>();

        int tPlayerIndex = -1;
        for(TPlayer tPlayer : tPlayers) {
            tPlayerIndex++;
            if(tPlayer == null) {
                results.add(Component.text(playerNames[tPlayerIndex], theme.getWarning(), TextDecoration.STRIKETHROUGH));
                continue;
            }

            if(track.getContributors().contains(tPlayer)) {
                track.removeContributor(tPlayer);
                results.add(Component.text(tPlayer.getName(), theme.getError()));
                continue;
            }

            track.addContributor(tPlayer);
            results.add(Component.text(tPlayer.getName(), theme.getSuccess()));
        }

        Component resultsText = results.get(0);
        for(Component result : results.subList(1, results.size())) {
            resultsText = resultsText.append(Component.text(", ", theme.getPrimary(), new HashSet<>())).append(result);
        }

        return Text.get(player, Success.UPDATED_TOGGLEABLE_VALUES, "%value%", "Contributors").append(resultsText);
    }

    public static Message setItem(Player player, Track track) {
        if (track == null) {
            if (hasTrackSelected(player.getUniqueId())) {
                track = getPlayerTrackSelection(player.getUniqueId());
            } else {
                return Error.TRACK_NOT_FOUND_FOR_EDIT;
            }
        }
        var item = player.getInventory().getItemInMainHand();
        if (item.getItemMeta() == null) {
            return Error.ITEM_NOT_FOUND;
        }
        track.setItem(item);
        return Success.SAVED;
    }

    public static Component deleteTrack(Player player, Track track) {
        if (track == null) {
            if (hasTrackSelected(player.getUniqueId())) {
                track = getPlayerTrackSelection(player.getUniqueId());
            } else {
                return Text.get(player, Error.TRACK_NOT_FOUND_FOR_EDIT);
            }
        }
        TrackDatabase.removeTrack(track);
        LogEntryBuilder.start(ApiUtilities.getTimestamp(), "track").setAction("remove").setUUID(player.getUniqueId()).setObjectId(track.getId()).build();
        return Text.get(player, Success.REMOVED_TRACK, "%track%", track.getDisplayName());
    }

    public static Component createTrack(Player player, Track.TrackType trackType, String name) {
        var response = validateTrackName(player, name);
        if (response != null) {
            return response;
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
            return Text.get(player, Error.GENERIC);
        }
        if (trackType.equals(Track.TrackType.BOAT)) {
            track.getTrackOptions().create(TrackOption.FORCE_BOAT);
        } else if (trackType.equals(Track.TrackType.PARKOUR)) {
            track.getTrackOptions().create(TrackOption.NO_ELYTRA);
            track.getTrackOptions().create(TrackOption.NO_CREATIVE);
        }

        LeaderboardManager.updateFastestTimeLeaderboard(track);
        return Text.get(player, Success.CREATED_TRACK, "%track%", name);
    }

    public static Component moveTrack(Player player, Track track) {
        if (track == null) {
            if (hasTrackSelected(player.getUniqueId())) {
                track = getPlayerTrackSelection(player.getUniqueId());
            } else {
                return Text.get(player, Error.TRACK_NOT_FOUND_FOR_EDIT);
            }
        }
        return TrackMove.move(player, track);
    }

    private static Component validateTrackName(Player player, String name) {
        int maxLength = 25;
        if (name.length() > maxLength) {
            return Text.get(player, Error.LENGTH_EXCEEDED, "%length%", String.valueOf(maxLength));
        }

        if (!name.matches("[A-Za-z0-9 ]+")) {
            return Text.get(player, Error.NAME_FORMAT);
        }

        if (ApiUtilities.checkTrackName(name)) {
            return Text.get(player, Error.INVALID_TRACK_NAME);
        }

        if (TrackDatabase.trackNameNotAvailable(name)) {
            return Text.get(player, Error.TRACK_EXISTS);
        }
        return null;
    }

    public static Component setView(Player player, Track track) {
        if (track == null) {
            if (hasTrackSelected(player.getUniqueId())) {
                track = getPlayerTrackSelection(player.getUniqueId());
            } else {
                return Text.get(player, Error.TRACK_NOT_FOUND_FOR_EDIT);
            }
        }
        if (track != getPlayerTrackSelection(player.getUniqueId())) {
            setPlayerTrackSelection(player.getUniqueId(), track);
            return TrackEditor.setVisualisation(player, true, track);
        }
        return TrackEditor.toggleVisualisation(player, track);
    }

    public static Message setTimeTrial(Player player, boolean enable, Track track) {
        if (track == null) {
            if (hasTrackSelected(player.getUniqueId())) {
                track = getPlayerTrackSelection(player.getUniqueId());
            } else {
                return Error.TRACK_NOT_FOUND_FOR_EDIT;
            }
        }
        track.setTimeTrial(enable);
        if (track.isTimeTrial()) {
            return Success.TRACK_TIMETRIAL_ENABLED;
        } else {
            return Success.TRACK_TIMETRIAL_DISABLED;
        }
    }
}
