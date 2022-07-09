package me.makkuusen.timing.system;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CommandTrack implements CommandExecutor
{
    static TimingSystem plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] arguments)
    {

        if (!(sender instanceof Player player))
        {
            ApiUtilities.msgConsole("§cCommand can only be used by players");
            return false;
        }
        if (arguments.length == 0)
        {
            if (!player.hasPermission("track.command.race") && !player.isOp())
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            GUITrack.openTrackGUI(player);
            return true;
        }

        if (arguments[0].equalsIgnoreCase("create"))
        {
            if (!player.isOp() && !player.hasPermission("track.command.create") && !player.isOp())
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }

            if (arguments.length < 3)
            {
                sender.sendMessage("§7Syntax: /track create §ntype§r§7 §nname §r§7");
                return true;
            }
            cmdCreate(player, arguments);
        }
        else if (arguments[0].equalsIgnoreCase("info"))
        {
            if (!player.isOp() && !player.hasPermission("track.command.info") && !player.isOp())
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            if (arguments.length < 2)
            {
                sender.sendMessage("§7Syntax: /track info §nname§r§7");
                return true;
            }
            cmdInfo(player, arguments);
        }
        else if (arguments[0].equalsIgnoreCase("list"))
        {
            if (!player.isOp() && !player.hasPermission("track.command.list"))
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            if (arguments.length > 2)
            {
                player.sendMessage("§7Syntax: /track list [§nsida§r§7]");
                return true;
            }
            cmdList(player, arguments);
        }
        else if (arguments[0].equalsIgnoreCase("delete"))
        {
            if (!player.isOp() && !player.hasPermission("track.command.delete") && !player.isOp())
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }

            cmdDelete(player, arguments);

        }
        else if (arguments[0].equalsIgnoreCase("set"))
        {
            if (!player.isOp() && !player.hasPermission("track.command.set") && !player.isOp())
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            if (arguments.length < 3)
            {
                player.sendMessage("§7Syntax /track set name §nid§r§7 §nname§r§7");
                player.sendMessage("§7Syntax /track set owner §nplayer§r§7 §nname§r§7");
                player.sendMessage("§7Syntax /track set spawn §nname§r§7");
                player.sendMessage("§7Syntax /track set leaderboard §nname§r§7");
                player.sendMessage("§7Syntax /track set gui §nname§r§7");
                player.sendMessage("§7Syntax /track set type §ntype§r§7 §nname§r§7");
                player.sendMessage("§7Syntax /track set mode §nmode§r§7 §nname§r§7");
                player.sendMessage("§7Syntax /track set startregion §nname§r§7");
                player.sendMessage("§7Syntax /track set endregion §nname§r§7");
                player.sendMessage("§7Syntax /track set pitregion §nname§r§7");
                player.sendMessage("§7Syntax /track set checkpoint +§nnumber§r§7 §nname§r§7");
                player.sendMessage("§7Syntax /track set checkpoint -§nnumber§r§7 §nname§r§7");
                player.sendMessage("§7Syntax /track set resetregion +§nnumber§r§7 §nname§r§7");
                player.sendMessage("§7Syntax /track set resetregion -§nnumber§r§7 §nname§r§7");
                player.sendMessage("§7Syntax /track set grid +§nnumber§r§7 §nname§r§7");
                player.sendMessage("§7Syntax /track set grid -§nnumber§r§7 §nname§r§7");
                return true;
            }
            cmdSet(player, arguments);
        }
        else if (arguments[0].equalsIgnoreCase("toggle"))
        {
            if (!player.isOp() && !player.hasPermission("track.command.toggle") && !player.isOp())
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            if (arguments.length < 3)
            {
                player.sendMessage("§7Syntax /track toggle open §nname§r§7");
                player.sendMessage("§7Syntax /track toggle government §nname§r§7");
                return true;
            }
            cmdToggle(player, arguments);
        }
        else if (arguments[0].equalsIgnoreCase("options"))
        {
            if (!player.isOp() && !player.hasPermission("track.command.options") && !player.isOp())
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            if (arguments.length < 3)
            {
                player.sendMessage("§7Syntax /track options §nchange§r§7 §ntrack");
                return true;
            }
            cmdOptions(player, arguments);
        }
        else if (arguments[0].equalsIgnoreCase("help"))
        {
            if (!player.hasPermission("track.command.help") && !player.isOp())
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            cmdHelp(player);
        }
        else if (arguments[0].equalsIgnoreCase("updateleaderboards"))
        {

            if (!player.hasPermission("track.command.updateleaderboards") && !player.isOp())
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            if (arguments.length != 1)
            {
                sender.sendMessage("§7Syntax: /track updateleaderboards");
                return true;
            }
            Bukkit.getScheduler().runTaskAsynchronously(TimingSystem.getPlugin(), () -> LeaderboardManager.updateAllFastestTimeLeaderboard(sender));
            plugin.sendMessage(sender, "messages.update.leaderboards");
        }
        else if (arguments[0].equalsIgnoreCase("deletebesttime"))
        {
            if (!player.hasPermission("track.command.deletebesttime") && !player.isOp())
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }

            if (arguments.length < 3)
            {
                sender.sendMessage("§7Syntax: /track deletebesttime §nplayer§r§7 §nname§r§7");
                return true;
            }

            TPlayer TPlayer = ApiDatabase.getPlayer(arguments[1]);
            if (TPlayer == null)
            {
                plugin.sendMessage(player,"messages.error.missing.player");
                return true;
            }

            String potentialMapName = ApiUtilities.concat(arguments, 2);
            var maybeTrack = TrackDatabase.getTrack(potentialMapName);
            if (maybeTrack.isEmpty())
            {
                plugin.sendMessage(player,"messages.error.missing.track.name");
                return true;
            }
            Track track = maybeTrack.get();
            TimeTrialFinish bestFinish = track.getBestFinish(TPlayer);
            if (bestFinish == null)
            {
                plugin.sendMessage(player,"messages.error.missing.bestTime");
                return true;
            }
            track.deleteBestFinish(TPlayer, bestFinish);
            plugin.sendMessage(player, "messages.remove.bestTime", "%player%", TPlayer.getName(), "%map%", potentialMapName);
            LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
            return true;
        }
        else if (arguments[0].equalsIgnoreCase("override"))
        {
            if (!player.isOp() && !player.hasPermission("track.command.override"))
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }

            if (TimingSystem.getPlugin().override.contains(player.getUniqueId()))
            {
                TimingSystem.getPlugin().override.remove(player.getUniqueId());
                plugin.sendMessage(player, "messages.remove.override");
            }

            else
            {
                TimingSystem.getPlugin().override.add(player.getUniqueId());
                plugin.sendMessage(player, "messages.create.override");
            }
            return true;
        }

        else if (arguments[0].equalsIgnoreCase("times"))
        {
            if (!player.isOp() && !player.hasPermission("track.command.times"))
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }

            if (arguments.length < 2)
            {
                sender.sendMessage("§7Syntax: /track times §7[§npage§7]§r§7 §nname§r§7");
                return true;
            }
            cmdTimes(player, arguments);
            return true;
        }

        String name = ApiUtilities.concat(arguments, 0);
        var maybeTrack = TrackDatabase.getTrack(name);
        if (maybeTrack.isEmpty())
        {
            plugin.sendMessage(player,"messages.errror.unknownCommand", "%command%", "race");
            return true;
        }
        var track = maybeTrack.get();

        if (!player.hasPermission("track.command.teleport") && !player.hasPermission("track.admin") && !player.isOp())
        {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return true;
        }

        if(!track.isOpen() && !player.hasPermission("race.override")){
            plugin.sendMessage(player, "messages.error.trackIsClosed");
            return true;
        }

        track.teleportPlayer(player);

        return true;
    }

    static void cmdHelp(Player player)
    {
        player.sendMessage("");
        plugin.sendMessage(player, "messages.help", "%command%" , "track");

        if (player.isOp() || player.hasPermission("track.command.race"))
        {
            player.sendMessage("§2/track");
        }

        if (player.isOp() || player.hasPermission("track.command.teleport"))
        {
            player.sendMessage("§2/track §aname");
        }

        if (player.isOp() || player.hasPermission("track.command.create"))
        {
            player.sendMessage("§2/track create §atype name");
        }

        if (player.isOp() || player.hasPermission("track.command.create"))
        {
            player.sendMessage("§2/track create §atype name");
        }
        if (player.isOp() || player.hasPermission("track.command.delete"))
        {
            player.sendMessage("§2/track delete §aname");
        }
        if (player.isOp() || player.hasPermission("track.command.info"))
        {
            player.sendMessage("§2/track info §aname");
        }
        if (player.isOp() || player.hasPermission("track.command.list"))
        {
            player.sendMessage("§2/track list [§apage§2]");
        }
        if (player.isOp() || player.hasPermission("track.command.set"))
        {
            player.sendMessage("§2/track set name §aid §aname");
            player.sendMessage("§2/track set owner §aplayer §aname");
            player.sendMessage("§2/track set spawn §aname");
            player.sendMessage("§2/track set leaderboard §aname");
            player.sendMessage("§2/track set gui §aname");
            player.sendMessage("§2/track set type §atype §aname");
            player.sendMessage("§2/track set mode §amode §aname");
            player.sendMessage("§2/track set startregion §aname");
            player.sendMessage("§2/track set endregion §aname");
            player.sendMessage("§2/track set pitregion §aname");
            player.sendMessage("§2/track set checkpoint +§anumber §aname");
            player.sendMessage("§2/track set checkpoint -§anumber §aname");
            player.sendMessage("§2/track set resetregion +§anumber §aname");
            player.sendMessage("§2/track set resetregion -§anumber §aname");
            player.sendMessage("§2/track set grid +§anumber §aname");
            player.sendMessage("§2/track set grid -§anumber §aname");
        }
        if (player.isOp() || player.hasPermission("track.command.toggle"))
        {
            player.sendMessage("§2/track toggle open §aname");
            player.sendMessage("§2/track toggle government §aname");
        }
        if (player.isOp() || player.hasPermission("track.command.times"))
        {
            player.sendMessage("§2/track times §aname");
            player.sendMessage("§2/track times [§apage§2] §aname");
        }
        if (player.isOp() || player.hasPermission("track.command.options"))
        {
            player.sendMessage("§2/track options §achange §aname");
        }
        if (player.isOp() || player.hasPermission("track.command.updateleaderboards"))
        {
            player.sendMessage("§2/track updateleaderboards");
        }
        if (player.isOp() || player.hasPermission("track.command.deletebesttime"))
        {
            player.sendMessage("§2/track deletebesttime §aplayer name");
        }
        if (player.isOp() || player.hasPermission("track.command.override"))
        {
            player.sendMessage("§2/track override");
        }
    }

    static void cmdCreate(Player player, String[] arguments)
    {

        String name = ApiUtilities.concat(arguments, 2);

        int maxLength = 25;
        if (name.length() > maxLength)
        {
            plugin.sendMessage(player, "messages.error.nametoLong", "%length%", String.valueOf(maxLength));
            return;
        }

        if (!name.matches("[A-Za-zÅÄÖåäöØÆøæ0-9 ]+"))
        {
            plugin.sendMessage(player, "messages.error.nameRegexException");
            return;
        }

        if (!TrackDatabase.trackNameAvailable(name))
        {
            plugin.sendMessage(player, "messages.error.trackExists");
            return;
        }

        if(player.getInventory().getItemInMainHand().getItemMeta() == null)
        {
            plugin.sendMessage(player, "messages.error.missing.item");
            return;
        }

        String type = arguments[1];
        Track.TrackType t = Track.TrackType.BOAT;
        if (type.equalsIgnoreCase("parkour"))
        {
            t = Track.TrackType.PARKOUR;
        }
        else if (type.equalsIgnoreCase("elytra"))
        {
            t = Track.TrackType.ELYTRA;
        }

        Track track = TrackDatabase.trackNew(name, player.getUniqueId(), player.getLocation(), t, player.getInventory().getItemInMainHand());
        if (track == null)
        {
            plugin.sendMessage(player, "messages.error.generic");
            return;
        }

        plugin.sendMessage(player, "messages.create.name", "%name%", name);
        LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
    }

    static void cmdInfo(Player player, String[] arguments)
    {
        String name = ApiUtilities.concat(arguments, 1);
        var maybeTrack = TrackDatabase.getTrack(name);
        if (maybeTrack.isEmpty())
        {
            plugin.sendMessage(player,"messages.error.missing.track.name");
            return;
        }
        var track = maybeTrack.get();
        player.sendMessage(" ");
        plugin.sendMessage(player, "messages.info.track.name", "%name%", track.getName(), "%id%", String.valueOf(track.getId()));
        if (track.isOpen())
        {
            plugin.sendMessage(player, "messages.info.track.open", "%type%", track.getTypeAsString());
        }
        else
        {
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

    static void cmdList(Player player, String[] arguments)
    {
        String pageStartRaw = "1";

        if (arguments.length == 2)
        {
            pageStartRaw = arguments[1];
        }

        int pageStart;

        try
        {
            pageStart = pageStartRaw == null ? 1 : Integer.parseInt(pageStartRaw);
        } catch (Exception exception)
        {
            return;
        }

        if (TrackDatabase.getTracks().size() == 0)
        {
            plugin.sendMessage(player, "messages.error.missing.tracks");
            return;
        }

        StringBuilder tmpMessage = new StringBuilder();

        int itemsPerPage = 25;
        int start = (pageStart * itemsPerPage) - itemsPerPage;
        int stop = pageStart * itemsPerPage;

        if (start >= TrackDatabase.getTracks().size())
        {
            plugin.sendMessage(player, "messages.error.missing.page");
            return;
        }

        for (int i = start; i < stop; i++)
        {
            if (i == TrackDatabase.getTracks().size())
            {
                break;
            }

            Track track = TrackDatabase.getTracks().get(i);

            tmpMessage.append(track.getName()).append(", ");

        }

        plugin.sendMessage(player, "messages.list.tracks", "%startPage%", String.valueOf(pageStart), "%totalPages%", String.valueOf((int) Math.ceil(((double) TrackDatabase.getTracks().size()) / ((double) itemsPerPage))));
        player.sendMessage("§2" + tmpMessage.substring(0, tmpMessage.length() - 2));
    }

    static void cmdDelete(Player player, String[] arguments)
    {
        String name = ApiUtilities.concat(arguments, 1);
        var maybeTrack = TrackDatabase.getTrack(name);
        if (maybeTrack.isEmpty())
        {
            plugin.sendMessage(player,"messages.error.missing.track.name");
            return;
        }
        TrackDatabase.removeTrack(maybeTrack.get());
        plugin.sendMessage(player,"messages.remove.track");

    }

    static void cmdToggle(Player player, String[] arguments)
    {
        String command = arguments[1];
        String name = ApiUtilities.concat(arguments, 2);
        var maybeTrack = TrackDatabase.getTrack(name);
        if (maybeTrack.isEmpty())
        {
            plugin.sendMessage(player,"messages.error.missing.track.name");
            return;
        }
        Track Track = maybeTrack.get();
        if (command.equalsIgnoreCase("open"))
        {
            Track.setToggleOpen(!Track.isOpen());

            if (Track.isOpen())
            {
                plugin.sendMessage(player, "messages.toggle.track.open");
            }
            else
            {
                plugin.sendMessage(player, "messages.toggle.track.closed");
            }
        }
        else if (command.equalsIgnoreCase("government"))
        {
            Track.setToggleGovernment(!Track.isGovernment());
            if (Track.isGovernment())
            {
                plugin.sendMessage(player, "messages.toggle.track.government");
            }
            else
            {
                plugin.sendMessage(player, "messages.toggle.track.private");
            }

        }
        else
        {
            player.sendMessage("§7Syntax /track toggle open §nname§r§7");
            player.sendMessage("§7Syntax /track toggle government §nname§r§7");
        }
    }

    static void cmdOptions(Player player, String[] arguments)
    {
        String options = arguments[1];

        String name = ApiUtilities.concat(arguments, 2);
        var maybeTrack = TrackDatabase.getTrack(name);
        if (maybeTrack.isEmpty())
        {
            plugin.sendMessage(player,"messages.error.missing.track.name");
            return;
        }
        Track track = maybeTrack.get();

        String newOptions = ApiUtilities.parseFlagChange(track.getOptions(), options);
        if (newOptions == null)
        {
            plugin.sendMessage(player, "messages.save.generic");
            return;
        }

        if (newOptions.length() == 0)
        {
            plugin.sendMessage(player, "messages.options.allRemoved");
        }
        else
        {
            plugin.sendMessage(player, "messages.options.list", "%options%", ApiUtilities.formatPermissions(newOptions.toCharArray()));
        }
        track.setOptions(newOptions);
    }

    static void cmdTimes(Player player, String[] arguments){

        int pageStart;

        String pageStartRaw = arguments[1];

        Optional<Track> maybeTrack;
        try
        {
            pageStart = Integer.parseInt(pageStartRaw);
            String name = ApiUtilities.concat(arguments, 2);
            maybeTrack = TrackDatabase.getTrack(name);
            if (maybeTrack.isEmpty()){
                plugin.sendMessage(player,"messages.error.missing.track.name");
                return;
            }
        } catch (Exception exception)
        {
            pageStart = 1;
            String name = ApiUtilities.concat(arguments, 1);
            maybeTrack = TrackDatabase.getTrack(name);
            if (maybeTrack.isEmpty())
            {
                plugin.sendMessage(player,"messages.error.missing.track.name");
                return;
            }
        }

        var track = maybeTrack.get();

        int itemsPerPage = TimingSystem.configuration.getTimesPageSize();
        int start = (pageStart * itemsPerPage) - itemsPerPage;
        int stop = pageStart * itemsPerPage;

        if (start >= track.getTopList().size())
        {
            plugin.sendMessage(player, "messages.error.missing.page");
            return;
        }

        plugin.sendMessage(player, "messages.list.times", "%track%", track.getName(), "%startPage%", String.valueOf(pageStart), "%totalPages%", String.valueOf((int) Math.ceil(((double) track.getTopList().size()) / ((double) itemsPerPage))));

        for (int i = start; i < stop; i++)
        {
            if (i == track.getTopList().size())
            {
                break;
            }

            TimeTrialFinish finish = track.getTopList().get(i);
            plugin.sendMessage(player, "messages.list.timesrow", "%pos%" , String.valueOf(i+1), "%player%", finish.getPlayer().getName(), "%time%", ApiUtilities.formatAsTime(finish.getTime()));
        }

    }

    static void cmdSet(Player player, String[] arguments)
    {
        String command = arguments[1];

        if (command.equalsIgnoreCase("startregion"))
        {
            String name = ApiUtilities.concat(arguments, 2);
            var maybeTrack = TrackDatabase.getTrack(name);
            if (maybeTrack.isEmpty())
            {
                plugin.sendMessage(player,"messages.error.missing.track.name");
                return;
            }
            cmdSetStartRegion(player, maybeTrack.get());

        }
        else if (command.equalsIgnoreCase("endregion"))
        {
            String name = ApiUtilities.concat(arguments, 2);
            var maybeTrack = TrackDatabase.getTrack(name);
            if (maybeTrack.isEmpty())
            {
                plugin.sendMessage(player,"messages.error.missing.track.name");
                return;
            }
            cmdSetEndRegion(player, maybeTrack.get());

        }
        else if (command.equalsIgnoreCase("pitregion"))
        {
            String name = ApiUtilities.concat(arguments, 2);
            var maybeTrack = TrackDatabase.getTrack(name);
            if (maybeTrack.isEmpty())
            {
                plugin.sendMessage(player,"messages.error.missing.track.name");
                return;
            }
            cmdSetPitRegion(player, maybeTrack.get());

        }
        else if (command.equalsIgnoreCase("spawn"))
        {
            String name = ApiUtilities.concat(arguments, 2);
            var maybeTrack = TrackDatabase.getTrack(name);
            if (maybeTrack.isEmpty())
            {
                plugin.sendMessage(player,"messages.error.missing.track.name");
                return;
            }
            maybeTrack.get().setSpawnLocation(player.getLocation());
            plugin.sendMessage(player,"messages.save.generic");

        }
        else if (command.equalsIgnoreCase("leaderboard"))
        {
            String name = ApiUtilities.concat(arguments, 2);
            var maybeTrack = TrackDatabase.getTrack(name);
            if (maybeTrack.isEmpty())
            {
                plugin.sendMessage(player,"messages.error.missing.track.name");
                return;
            }
            Track track = maybeTrack.get();
            Location loc = player.getLocation();
            loc.setY(loc.getY() + 3);
            track.setLeaderboardLocation(loc);
            LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
            plugin.sendMessage(player,"messages.save.generic");

        }
        else if (command.equalsIgnoreCase("gui"))
        {
            if (arguments.length < 3)
            {
                player.sendMessage("§7Syntax /track set gui §nname§r§7");
                return;
            }
            String name = ApiUtilities.concat(arguments, 2);
            var maybeTrack = TrackDatabase.getTrack(name);
            if (maybeTrack.isEmpty())
            {
                plugin.sendMessage(player,"messages.error.missing.track.name");
                return;
            }

            var item = player.getInventory().getItemInMainHand();

            if(item.getItemMeta() == null)
            {
                plugin.sendMessage(player,"messages.error.missing.item");
                return;
            }
            maybeTrack.get().setGuiItem(item);
            plugin.sendMessage(player,"messages.save.generic");

        }
        else if (command.equalsIgnoreCase("type"))
        {
            if (arguments.length < 4)
            {
                player.sendMessage("§7Syntax /track set type §ntype§r§7 §nname§r§7");
                return;
            }
            String name = ApiUtilities.concat(arguments, 3);
            var maybeTrack = TrackDatabase.getTrack(name);
            if (maybeTrack.isEmpty())
            {
                plugin.sendMessage(player,"messages.error.missing.track.name");
                return;
            }
            cmdSetType(player, maybeTrack.get(), arguments[2]);

        }
        else if (command.equalsIgnoreCase("mode"))
        {
            if (arguments.length < 4)
            {
                player.sendMessage("§7Syntax /track set mode §nmode§r§7 §nname§r§7");
                return;
            }
            String name = ApiUtilities.concat(arguments, 3);
            var maybeTrack = TrackDatabase.getTrack(name);
            if (maybeTrack.isEmpty())
            {
                plugin.sendMessage(player,"messages.error.missing.track.name");
                return;
            }
            cmdSetMode(player, maybeTrack.get(), arguments[2]);

        }
        else if (command.equalsIgnoreCase("name"))
        {
            if (arguments.length < 4)
            {
                player.sendMessage("§7Syntax /track set name §nid§r§7 §nname§r§7");
                return;
            }
            String name = ApiUtilities.concat(arguments, 3);
            cmdSetName(player, arguments[2], name);

        }
        else if (command.equalsIgnoreCase("owner"))
        {
            if (arguments.length < 4)
            {
                player.sendMessage("§7Syntax /track set owner §nplayer§r§7 §nname§r§7");
                return;
            }

            TPlayer TPlayer = ApiDatabase.getPlayer(arguments[2]);
            if (TPlayer == null)
            {
                plugin.sendMessage(player,"messages.error.missing.player");
                return;
            }

            String name = ApiUtilities.concat(arguments, 3);
            var maybeTrack = TrackDatabase.getTrack(name);
            if (maybeTrack.isEmpty())
            {
                plugin.sendMessage(player,"messages.error.missing.track.name");
                return;
            }
            maybeTrack.get().setOwner(TPlayer);
            plugin.sendMessage(player,"messages.save.generic");
        }
        else if (command.equalsIgnoreCase("checkpoint"))
        {
            if (arguments.length < 4)
            {
                player.sendMessage("§7Syntax /track set checkpoint §nnumber§r§7 §nname§r§7");
                return;
            }
            String name = ApiUtilities.concat(arguments, 3);
            var maybeTrack = TrackDatabase.getTrack(name);
            if (maybeTrack.isEmpty())
            {
                plugin.sendMessage(player,"messages.error.missing.track.name");
                return;
            }
            cmdSetCheckpoint(player, maybeTrack.get(), arguments[2]);

        }
        else if (command.equalsIgnoreCase("grid"))
        {
            if (arguments.length < 4)
            {
                player.sendMessage("§7Syntax /track set grid §nnumber§r§7 §nname§r§7");
                return;
            }
            String name = ApiUtilities.concat(arguments, 3);
            var maybeTrack = TrackDatabase.getTrack(name);
            if (maybeTrack.isEmpty())
            {
                plugin.sendMessage(player,"messages.error.missing.track.name");
                return;
            }
            cmdSetGridRegion(player, maybeTrack.get(), arguments[2]);

        }
        else if (command.equalsIgnoreCase("resetregion"))
        {
            if (arguments.length < 4)
            {
                player.sendMessage("§7Syntax /track set resetregion §nnumber§r§7 §nname§r§7");
                return;
            }
            String name = ApiUtilities.concat(arguments, 3);
            var maybeTrack = TrackDatabase.getTrack(name);
            if (maybeTrack.isEmpty())
            {
                plugin.sendMessage(player,"messages.error.missing.track.name");
                return;
            }
            cmdSetResetRegion(player, maybeTrack.get(), arguments[2]);
        }
        else
        {
            player.sendMessage("§2/track set name §aid §aname");
            player.sendMessage("§2/track set owner §aplayer §aname");
            player.sendMessage("§2/track set spawn §aname");
            player.sendMessage("§2/track set leaderboard §aname");
            player.sendMessage("§2/track set gui §aname");
            player.sendMessage("§2/track set type §atype §aname");
            player.sendMessage("§2/track set mode §amode §aname");
            player.sendMessage("§2/track set startregion §aname");
            player.sendMessage("§2/track set endregion §aname");
            player.sendMessage("§2/track set pitregion §aname");
            player.sendMessage("§2/track set checkpoint +§anumber §aname");
            player.sendMessage("§2/track set checkpoint -§anumber §aname");
            player.sendMessage("§2/track set resetregion +§anumber §aname");
            player.sendMessage("§2/track set resetregion -§anumber §aname");
            player.sendMessage("§2/track set grid +§anumber §aname");
            player.sendMessage("§2/track set grid -§anumber §aname");
        }
    }

    static void cmdSetType(Player player, Track track, String type)
    {
        Track.TrackType trackType = track.getTypeFromString(type);

        if (trackType == null)
        {
            plugin.sendMessage(player, "messages.error.trackTypeException");
            return;
        }
        track.setTrackType(trackType);
        plugin.sendMessage(player,"messages.save.generic");
    }

    static void cmdSetName(Player player, String id, String name)
    {
        int trackId;
        try
        {
            trackId = Integer.parseInt(id);
        } catch (NumberFormatException e)
        {
            plugin.sendMessage(player, "messages.error.numberException");
            return;
        }
        var maybeTrack = TrackDatabase.getTrackById(trackId);
        if (maybeTrack.isEmpty())
        {
            plugin.sendMessage(player, "messages.error.missing.track.id");
            return;
        }

        int maxLength = 25;
        if (name.length() > maxLength)
        {
            plugin.sendMessage(player, "messages.error.nametoLong", "%length%", String.valueOf(maxLength));
            return;
        }

        if (!name.matches("[A-Za-zÅÄÖåäöØÆøæ0-9 ]+"))
        {
            plugin.sendMessage(player, "messages.error.nameRegexException");
            return;
        }
        maybeTrack.get().setName(name);
        plugin.sendMessage(player,"messages.save.generic");
        LeaderboardManager.updateFastestTimeLeaderboard(trackId);
    }

    static void cmdSetMode(Player player, Track track, String mode)
    {
        Track.TrackMode trackMode = track.getModeFromString(mode);

        if (mode == null)
        {
            plugin.sendMessage(player, "messages.error.trackTypeException");
            return;
        }
        track.setMode(trackMode);
        plugin.sendMessage(player,"messages.save.generic");

    }

    static void cmdSetStartRegion(Player player, Track track)
    {
        List<Location> positions = getPositions(player);
        if (positions == null)
        {
            return;
        }
        track.setStartRegion(positions.get(0), positions.get(1));
        plugin.sendMessage(player, "messages.create.region");
    }

    static void cmdSetEndRegion(Player player, Track track)
    {
        List<Location> positions = getPositions(player);
        if (positions == null)
        {
            return;
        }
        track.setEndRegion(positions.get(0), positions.get(1));
        plugin.sendMessage(player, "messages.create.region");
    }

    static void cmdSetPitRegion(Player player, Track track)
    {
        List<Location> positions = getPositions(player);
        if (positions == null)
        {
            return;
        }
        track.setPitRegion(positions.get(0), positions.get(1), player.getLocation());
        plugin.sendMessage(player, "messages.create.region");
    }

    static void cmdSetResetRegion(Player player, Track track, String index)
    {

        int regionIndex;
        boolean remove = false;
        if (index.startsWith("-"))
        {
            index = index.substring(1);
            remove = true;
        }
        else if (index.startsWith("+"))
        {
            index = index.substring(1);
        }
        try
        {
            regionIndex = Integer.parseInt(index);
        } catch (NumberFormatException exception)
        {
            plugin.sendMessage(player, "messages.error.numberException");
            return;
        }
        if (remove)
        {
            if (track.removeResetRegion(regionIndex))
            {
                plugin.sendMessage(player, "messages.remove.region");
            }
            else
            {
                plugin.sendMessage(player, "messages.error.remove.region");
            }
        }
        else
        {
            List<Location> positions = getPositions(player);
            if (positions == null)
            {
                return;
            }
            track.setResetRegion(positions.get(0), positions.get(1), player.getLocation(), regionIndex);
            plugin.sendMessage(player, "messages.create.region");
        }
    }

    static void cmdSetGridRegion(Player player, Track track, String index)
    {

        int regionIndex;
        boolean remove = false;
        if (index.startsWith("-"))
        {
            index = index.substring(1);
            remove = true;
        }
        else if (index.startsWith("+"))
        {
            index = index.substring(1);
        }
        try
        {
            regionIndex = Integer.parseInt(index);
        } catch (NumberFormatException exception)
        {
            plugin.sendMessage(player, "messages.error.numberException");
            return;
        }
        if (remove)
        {
            if (track.removeResetRegion(regionIndex))
            {
                plugin.sendMessage(player, "messages.remove.region");
            }
            else
            {
                plugin.sendMessage(player, "messages.error.remove.region");
            }
        }
        else
        {
            List<Location> positions = getPositions(player);
            if (positions == null)
            {
                return;
            }
            track.setGridRegion(positions.get(0), positions.get(1), player.getLocation(), regionIndex);
            plugin.sendMessage(player, "messages.create.region");
        }
    }

    static void cmdSetCheckpoint(Player player, Track track, String index)
    {
        int regionIndex;
        boolean remove = false;
        if (index.startsWith("-"))
        {
            index = index.substring(1);
            remove = true;
        }
        else if (index.startsWith("+"))
        {
            index = index.substring(1);
        }

        try
        {
            regionIndex = Integer.parseInt(index);
        } catch (NumberFormatException exception)
        {
            plugin.sendMessage(player, "messages.error.numberException");
            return;
        }

        if (remove)
        {
            if (track.removeCheckpoint(regionIndex))
            {
                plugin.sendMessage(player, "messages.remove.checkpoint");
            }
            else
            {
                plugin.sendMessage(player, "messages.error.remove.checkpoint");
            }
        }
        else
        {
            List<Location> positions = getPositions(player);
            if (positions == null)
            {
                return;
            }
            track.setCheckpoint(positions.get(0), positions.get(1), player.getLocation(), regionIndex);
            plugin.sendMessage(player,"messages.create.checkpoint");
        }
    }


    private static List<Location> getPositions(Player player)
    {
        BukkitPlayer bPlayer = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(bPlayer);
        Region selection;
        try
        {
            selection = session.getSelection(bPlayer.getWorld());
        } catch (IncompleteRegionException e)
        {
            plugin.sendMessage(player, "messages.error.missing.selection");
            return null;
        }

        if (selection instanceof CuboidRegion)
        {
            List<Location> locations = new ArrayList<>();
            BlockVector3 p1 = selection.getMinimumPoint();
            locations.add(new Location(player.getWorld(), p1.getBlockX(), p1.getBlockY(), p1.getBlockZ()));
            BlockVector3 p2 = selection.getMaximumPoint();
            locations.add(new Location(player.getWorld(), p2.getBlockX(), p2.getBlockY(), p2.getBlockZ()));
            return locations;
        }
        else
        {
            plugin.sendMessage(player, "messages.error.selectionException");
            return null;
        }
    }

}

