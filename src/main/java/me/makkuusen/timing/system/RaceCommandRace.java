package me.makkuusen.timing.system;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RaceCommandRace implements CommandExecutor
{
    static Race plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] arguments)
    {
        if (!(sender instanceof Player player))
        {
            RaceUtilities.msgConsole("§cCommand can only be used by players");
            return true;
        }
        if (arguments.length == 0)
        {
            if (!player.hasPermission("race.command.race") && !player.isOp())
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            GUIManager.openMainGUI(player);
            return true;
        }

        if (arguments[0].equalsIgnoreCase("cancel"))
        {
            if (!player.isOp() && !player.hasPermission("race.command.cancel") && !player.isOp())
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            if(!TimeTrialsController.timeTrials.containsKey(player.getUniqueId())){
                plugin.sendMessage(player, "messages.error.runNotStarted");
                return true;
            }
            TimeTrialsController.playerCancelMap(player);
            plugin.sendMessage(player, "messages.cancel");
            return true;
        }
        else if (arguments[0].equalsIgnoreCase("help"))
        {
            if (!player.isOp() && !player.hasPermission("race.command.help") && !player.isOp())
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            cmdHelp(player);
            return true;
        }
        else if (arguments[0].equalsIgnoreCase("list"))
        {
            if (!player.isOp() && !player.hasPermission("race.command.list") && !player.isOp())
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            if (arguments.length > 2)
            {
                player.sendMessage("§7Syntax: /race list [§npage§r§7]");
                return true;
            }
            cmdList(player, arguments);
            return true;
        }
        else if (arguments[0].equalsIgnoreCase("info"))
        {
            if (!player.isOp() && !player.hasPermission("race.command.info") && !player.isOp())
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            if (arguments.length < 2)
            {
                player.sendMessage("§7Syntax: /race info §nname§r§7");
                return true;
            }
            cmdInfo(player, arguments);
            return true;
        }
        else if (arguments[0].equalsIgnoreCase("toggle"))
        {
            if (!player.isOp() && !player.hasPermission("race.command.toggle") && !player.isOp())
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            if (arguments.length < 2)
            {
                player.sendMessage("§7Syntax: /race toggle checkpointmessages");
                return true;
            }
            cmdToggle(player, arguments);
            return true;
        }
        return true;
    }

    static void cmdHelp(Player player)
    {
        player.sendMessage("");
        plugin.sendMessage(player, "messages.help", "%command%", "race");

        if (player.isOp() || player.hasPermission("race.command.cancel"))
        {
            player.sendMessage("§2/race cancel");
        }
        if (player.isOp() || player.hasPermission("race.command.list"))
        {
            player.sendMessage("§2/race list [§apage§2]");
        }
        if (player.isOp() || player.hasPermission("race.command.info"))
        {
            player.sendMessage("§2/race info §aname");
        }
        if (player.isOp() || player.hasPermission("race.command.toggle"))
        {
            player.sendMessage("§2/race toggle checkpointmessages");
        }

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
            if (pageStart < 1){
                plugin.sendMessage(player, "messages.error.missing.page");
                return;
            }
        } catch (Exception exception)
        {
            plugin.sendMessage(player, "messages.error.missing.page");
            return;
        }
        var publicTracks = RaceDatabase.getAvailableRaceTracks(player);
        if (publicTracks.size() == 0)
        {
            plugin.sendMessage(player,"messages.error.missing.tracks");
            return;
        }

        StringBuilder tmpMessage = new StringBuilder();

        int itemsPerPage = 25;
        int start = (pageStart * itemsPerPage) - itemsPerPage;
        int stop = pageStart * itemsPerPage;

        if (start >= publicTracks.size())
        {
            plugin.sendMessage(player, "messages.error.missing.page");
            return;
        }

        for (int i = start; i < stop; i++)
        {
            if (i == publicTracks.size())
            {
                break;
            }

            RaceTrack track = publicTracks.get(i);
            tmpMessage.append(track.getName()).append(", ");

        }

        plugin.sendMessage(player, "messages.list.tracks", "%startPage%", String.valueOf(pageStart), "%totalPages%", String.valueOf((int) Math.ceil(((double) RaceDatabase.getRaceTracks().size()) / ((double) itemsPerPage))));
        player.sendMessage("§2" + tmpMessage.substring(0, tmpMessage.length() - 2));
    }

    static void cmdInfo(Player player, String[] arguments)
    {
        String name = ApiUtilities.concat(arguments, 1);
        var maybeTrack = RaceDatabase.getRaceTrack(name);
        if (maybeTrack.isEmpty())
        {
            plugin.sendMessage(player,"messages.error.missing.track.name");
            return;
        }
        var track = maybeTrack.get();

        if (!RaceDatabase.getAvailableRaceTracks(player).contains(track))
        {
            plugin.sendMessage(player, "messages.error.trackIsLocked");
            return;
        }
        RPlayer rPlayer = ApiDatabase.getPlayer(player.getUniqueId());
        var bestFinish = track.getBestFinish(rPlayer);

        player.sendMessage("");
        plugin.sendMessage(player, "messages.info.race.name", "%name%", track.getName());
        plugin.sendMessage(player, "messages.info.race.type",  "%type%", track.getTypeAsString());
        plugin.sendMessage(player, "messages.info.track.created", "%date%", ApiUtilities.niceDate(track.getDateCreated()), "%owner%", track.getOwner().getName());
        plugin.sendMessage(player, "messages.info.track.checkpoints", "%size%", String.valueOf(track.getCheckpoints().size()));

        if (bestFinish != null)
        {
            plugin.sendMessage(player, "messages.info.race.bestTime", "%time%", RaceUtilities.formatAsTime(bestFinish.getTime()));
        }

    }
    static void cmdToggle(Player player, String[] arguments)
    {
        if(Race.getPlugin().verbose.contains(player.getUniqueId()))
        {
            Race.getPlugin().verbose.remove(player.getUniqueId());
            plugin.sendMessage(player, "messages.toggle.race.checkpointsOff");
        }
        else
        {
            Race.getPlugin().verbose.add(player.getUniqueId());
            plugin.sendMessage(player, "messages.toggle.race.checkpointsOn");
        }
    }
}
