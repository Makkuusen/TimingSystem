package me.makkuusen.timing.system;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RaceCommandRace implements CommandExecutor
{
    static TimingSystem plugin;

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
        else if (arguments[0].equalsIgnoreCase("create"))
        {
            if (!player.isOp() && !player.hasPermission("race.command.create") && !player.isOp())
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            cmdCreate(player, arguments);
            return true;
        }
        else if (arguments[0].equalsIgnoreCase("start"))
        {
            if (!player.isOp() && !player.hasPermission("race.command.start") && !player.isOp())
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            cmdStart(player, arguments);
            return true;
        }
        else if (arguments[0].equalsIgnoreCase("reset"))
        {
            if (!player.isOp() && !player.hasPermission("race.command.create") && !player.isOp())
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            cmdReset(player, arguments);
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
        else if (arguments[0].equalsIgnoreCase("set"))
        {
            if (!player.isOp() && !player.hasPermission("race.command.set") && !player.isOp())
            {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return true;
            }
            if (arguments.length < 3)
            {
                player.sendMessage("§7Syntax /race set laps §nlaps§r§7 §nname§r§7");
                player.sendMessage("§7Syntax /race set pits §npits§r§7 §nname§r§7");
                player.sendMessage("§7Syntax /race set driver +§ndriver§r§7 §nname§r§7");
                player.sendMessage("§7Syntax /race set driver -§ndriver§r§7 §nname§r§7");
                return true;
            }
            cmdSet(player, arguments);

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
        if (player.isOp() || player.hasPermission("race.command.create"))
        {
            player.sendMessage("§2/race create");
        }
        if (player.isOp() || player.hasPermission("race.command.start"))
        {
            player.sendMessage("§2/race start");
        }
        if (player.isOp() || player.hasPermission("race.command.reset"))
        {
            player.sendMessage("§2/race reset");
        }
        if (player.isOp() || player.hasPermission("race.command.list"))
        {
            player.sendMessage("§2/race list [§apage§2]");
        }
        if (player.isOp() || player.hasPermission("race.command.info"))
        {
            player.sendMessage("§2/race info §aname");
        }
        if (player.isOp() || player.hasPermission("race.command.set"))
        {
            player.sendMessage("§2/race set laps §alaps §aname");
            player.sendMessage("§2/race set pits §apits §aname");
            player.sendMessage("§2/race set driver +§adriver §aname");
            player.sendMessage("§2/race set driver -§adriver §aname");

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

        Race race = RaceController.races.get(track.getId());
        if (race == null) {
            plugin.sendMessage(player, "messages.error.missing.race");
        }
        RPlayer rPlayer = ApiDatabase.getPlayer(player.getUniqueId());

        player.sendMessage("");
        plugin.sendMessage(player, "messages.info.race.name", "%name%", track.getName());
        plugin.sendMessage(player, "messages.info.race.type",  "%type%", track.getTypeAsString());
        plugin.sendMessage(player, "messages.info.race.laps", "%laps%", String.valueOf(race.getTotalLaps()));
        plugin.sendMessage(player, "messages.info.race.pits", "%pits%", String.valueOf(race.getTotalPitstops()));
        plugin.sendMessage(player, "messages.info.race.drivers", "%drivers%", race.getDriversAsString());
    }
    static void cmdToggle(Player player, String[] arguments)
    {
        if (TimingSystem.getPlugin().verbose.contains(player.getUniqueId()))
        {
            TimingSystem.getPlugin().verbose.remove(player.getUniqueId());
            plugin.sendMessage(player, "messages.toggle.race.checkpointsOff");
        }
        else
        {
            TimingSystem.getPlugin().verbose.add(player.getUniqueId());
            plugin.sendMessage(player, "messages.toggle.race.checkpointsOn");
        }
    }

    static void cmdCreate(Player player, String[] arguments)
    {
        String name = ApiUtilities.concat(arguments, 1);
        var maybeTrack = RaceDatabase.getRaceTrack(name);
        if (maybeTrack.isEmpty())
        {
            plugin.sendMessage(player,"messages.error.missing.track.name");
            return;
        }
        var track = maybeTrack.get();
        if (RaceController.races.containsKey(track.getId()))
        {
            plugin.sendMessage(player, "messages.error.raceExists");
        }
        Race race = new Race(5, 2, track);
        race.addRaceDriver(ApiDatabase.getPlayer(player.getUniqueId()));
        RaceController.races.put(track.getId(), race);

        player.sendMessage("§aRace has been created.");
    }

    static void cmdStart(Player player, String[] arguments)
    {
        String name = ApiUtilities.concat(arguments, 1);
        var maybeTrack = RaceDatabase.getRaceTrack(name);
        if (maybeTrack.isEmpty())
        {
            plugin.sendMessage(player,"messages.error.missing.track.name");
            return;
        }
        var track = maybeTrack.get();
        Race race = RaceController.races.get(track.getId());
        race.startRace();
        player.sendMessage("§aRace has been started.");
    }

    static void cmdReset(Player player, String[] arguments)
    {
        String name = ApiUtilities.concat(arguments, 1);
        var maybeTrack = RaceDatabase.getRaceTrack(name);
        if (maybeTrack.isEmpty())
        {
            plugin.sendMessage(player,"messages.error.missing.track.name");
            return;
        }
        var track = maybeTrack.get();
        Race race = RaceController.races.get(track.getId());
        race.resetRace();
        player.sendMessage("§aRace has been reset");
    }

    static void cmdSet(Player player, String[] arguments)
    {
        String command = arguments[1];

        if (command.equalsIgnoreCase("laps"))
        {
            if (arguments.length < 4)
            {
                player.sendMessage("§7Syntax /track set laps §nlaps§r§7 §nname§r§7");
                return;
            }
            String name = ApiUtilities.concat(arguments, 3);
            var maybeTrack = RaceDatabase.getRaceTrack(name);
            if (maybeTrack.isEmpty())
            {
                plugin.sendMessage(player,"messages.error.missing.track");
                return;
            }
            var race = RaceController.races.get(maybeTrack.get().getId());
            if (race == null)
            {
                plugin.sendMessage(player, "messages.error.missing.race");
                return;
            }
            cmdSetLaps(player, race, arguments[2]);

        }
        else if (command.equalsIgnoreCase("pits"))
        {
            if (arguments.length < 4)
            {
                player.sendMessage("§7Syntax /track set pits §npits§r§7 §nname§r§7");
                return;
            }
            String name = ApiUtilities.concat(arguments, 3);
            var maybeTrack = RaceDatabase.getRaceTrack(name);
            if (maybeTrack.isEmpty())
            {
                plugin.sendMessage(player,"messages.error.missing.track");
                return;
            }
            var race = RaceController.races.get(maybeTrack.get().getId());
            if (race == null)
            {
                plugin.sendMessage(player, "messages.error.missing.race");
                return;
            }
            cmdSetPits(player, race, arguments[2]);

        }
        else if (command.equalsIgnoreCase("driver"))
        {
            if (arguments.length < 4)
            {
                player.sendMessage("§7Syntax /race set driver +§ndriver§r§7 §nname§r§7");
                player.sendMessage("§7Syntax /race set driver -§ndriver§r§7 §nname§r§7");
                return;
            }
            String name = ApiUtilities.concat(arguments, 3);
            var maybeTrack = RaceDatabase.getRaceTrack(name);
            if (maybeTrack.isEmpty())
            {
                plugin.sendMessage(player,"messages.error.missing.track");
                return;
            }
            var race = RaceController.races.get(maybeTrack.get().getId());
            if (race == null)
            {
                plugin.sendMessage(player, "messages.error.missing.race");
                return;
            }
            cmdSetDriver(player, race, arguments[2]);

        }
        else
        {
            player.sendMessage("§2/race set laps §alaps §aname");
            player.sendMessage("§2/race set pits §apits §aname");
            player.sendMessage("§2/race set driver +§adriver §aname");
            player.sendMessage("§2/race set driver -§adriver §aname");

        }
    }
    static void cmdSetLaps(Player player, Race race, String laps)
    {
        int totalLaps;
        try {
            totalLaps = Integer.parseInt(laps);
        }
        catch (NumberFormatException e){
            plugin.sendMessage(player,"messages.error.numberException");
            return;
        }

        race.setTotalLaps(totalLaps);
        plugin.sendMessage(player,"messages.save.generic");
    }

    static void cmdSetPits(Player player, Race race, String pits)
    {
        int totalPits;
        try {
            totalPits = Integer.parseInt(pits);
        }
        catch (NumberFormatException e){
            plugin.sendMessage(player,"messages.error.numberException");
            return;
        }

        race.setTotalPitstops(totalPits);
        plugin.sendMessage(player,"messages.save.generic");
    }

    static void cmdSetDriver(Player player, Race race, String driverName)
    {
        int regionIndex;
        boolean remove = false;
        if (driverName.startsWith("-"))
        {
            driverName = driverName.substring(1);
            remove = true;
        }
        else if (driverName.startsWith("+"))
        {
            driverName = driverName.substring(1);
        }


        RPlayer rPlayer = ApiDatabase.getPlayer(driverName);
        if (rPlayer == null)
        {
            plugin.sendMessage(player, "messages.error.missing.player");
        }
        if (remove)
        {
            if (race.raceDrivers.containsKey(rPlayer.getUniqueId()))
            {
                race.raceDrivers.remove(rPlayer.getUniqueId());
                plugin.sendMessage(player, "messages.save.generic");
            }
            else
            {
                plugin.sendMessage(player, "messages.error.generic");
            }
        }
        else
        {
            race.addRaceDriver(rPlayer);
            plugin.sendMessage(player,"messages.save.generic");
        }
    }
}
