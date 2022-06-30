package me.makkuusen.timing.system;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RaceCommandRace implements CommandExecutor
{

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] arguments)
    {
        if (!(sender instanceof Player player))
        {
            RaceUtilities.msgConsole("§cKommandot kan enbart användas av spelare");
            return true;
        }
        if (arguments.length == 0)
        {
            if (!player.hasPermission("race.command.race") && !player.isOp())
            {
                player.sendMessage("§cÅtkomst nekad.");
                return true;
            }
            GUIManager.openMainGUI(player);
            return true;
        }

        if (arguments[0].equalsIgnoreCase("cancel"))
        {
            if (!player.isOp() && !player.hasPermission("race.command.cancel") && !player.isOp())
            {
                player.sendMessage("§cÅtkomst nekad.");
                return true;
            }
            if(!PlayerTimer.isPlayerInMap(player)){
                player.sendMessage("§cDu har inte påbörjat en bana.");
                return true;
            }
            PlayerTimer.playerCancelMap(player);
            player.sendMessage("§aAvbröt tidtagningen.");
            return true;
        }
        else if (arguments[0].equalsIgnoreCase("help"))
        {
            if (!player.isOp() && !player.hasPermission("race.command.help") && !player.isOp())
            {
                player.sendMessage("§cÅtkomst nekad.");
                return true;
            }
            cmdHelp(player);
            return true;
        }
        else if (arguments[0].equalsIgnoreCase("list"))
        {
            if (!player.isOp() && !player.hasPermission("race.command.list") && !player.isOp())
            {
                player.sendMessage("§cÅtkomst nekad.");
                return true;
            }
            if (arguments.length > 2)
            {
                player.sendMessage("§7Syntax: /race list [§nsida§r§7]");
                return true;
            }
            cmdList(player, arguments);
            return true;
        }
        else if (arguments[0].equalsIgnoreCase("info"))
        {
            if (!player.isOp() && !player.hasPermission("race.command.info") && !player.isOp())
            {
                player.sendMessage("§cÅtkomst nekad.");
                return true;
            }
            if (arguments.length < 2)
            {
                player.sendMessage("§7Syntax: /race info §nnamn§r§7");
                return true;
            }
            cmdInfo(player, arguments);
            return true;
        }
        else if (arguments[0].equalsIgnoreCase("toggle"))
        {
            if (!player.isOp() && !player.hasPermission("race.command.toggle") && !player.isOp())
            {
                player.sendMessage("§cÅtkomst nekad.");
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

        String name = ApiUtilities.concat(arguments, 0);
        var maybeTrack = RaceDatabase.getRaceTrack(name);
        if (maybeTrack.isEmpty())
        {
            sender.sendMessage("§7Okänt kommando. Skriv §n/race help§r§7 för en lista på kommandon.");
            return true;
        }
        var track = maybeTrack.get();

        if (!player.hasPermission("race.command.start") && !player.hasPermission("track.admin") && !player.isOp())
        {
            player.sendMessage("§cÅtkomst nekad.");
            return true;
        }

        if(!track.isOpen() && !player.hasPermission("race.override")){
            player.sendMessage("§cBanan är stängd.");
            return true;
        }

        track.startRace(player);

        return true;
    }

    static void cmdHelp(Player player)
    {
        player.sendMessage("");
        player.sendMessage("§2---§a Hjälp: /race §2---");
        if (player.isOp() || player.hasPermission("race.command.start"))
        {
            player.sendMessage("§2/race §anamn");
        }
        if (player.isOp() || player.hasPermission("race.command.cancel"))
        {
            player.sendMessage("§2/race cancel");
        }
        if (player.isOp() || player.hasPermission("race.command.list"))
        {
            player.sendMessage("§2/race list [§asida§2]");
        }
        if (player.isOp() || player.hasPermission("race.command.info"))
        {
            player.sendMessage("§2/race info §anamn");
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
                player.sendMessage("§cFelaktigt sidnummer.");
                return;
            }
        } catch (Exception exception)
        {
            player.sendMessage("§cFelaktigt sidnummer.");
            return;
        }
        var publicTracks = RaceDatabase.getAvailableRaceTracks(player);
        if (publicTracks.size() == 0)
        {
            player.sendMessage("§cDet finns inga banor.");
            return;
        }

        StringBuilder tmpMessage = new StringBuilder();

        int itemsPerPage = 25;
        int start = (pageStart * itemsPerPage) - itemsPerPage;
        int stop = pageStart * itemsPerPage;

        if (start >= publicTracks.size())
        {
            player.sendMessage("§cDet finns inte så många sidor att visa.");
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

        player.sendMessage("§2--- §aBanor (sida §l" + pageStart + " §r§aav §l" + ((int) Math.ceil(((double) RaceDatabase.getAvailableRaceTracks(player).size()) / ((double) itemsPerPage))) + "§r§a) §2---");
        player.sendMessage("§2" + tmpMessage.substring(0, tmpMessage.length() - 2));
    }

    static void cmdInfo(Player player, String[] arguments)
    {
        String name = ApiUtilities.concat(arguments, 1);
        var maybeTrack = RaceDatabase.getRaceTrack(name);
        if (maybeTrack.isEmpty())
        {
            player.sendMessage("§cDet finns ingen bana med det namnet.");
            return;
        }
        var track = maybeTrack.get();

        if (!RaceDatabase.getAvailableRaceTracks(player).contains(track))
        {
            player.sendMessage("§cBanan med det namnet är låst.");
            return;
        }
        RPlayer rPlayer = ApiDatabase.getPlayer(player.getUniqueId());
        var bestFinish = track.getBestFinish(rPlayer);

        player.sendMessage("");
        player.sendMessage("§2--- §aBana: " + track.getName() + " §2---");
        player.sendMessage("§2Bantyp:§a " + track.getTypeAsString());
        player.sendMessage("§2Skapad:§a " + ApiUtilities.niceDate(track.getDateCreated()) + "§2 ägs av §a" + (track.isGovernment() ? "staten" : track.getOwner().getName()));
        player.sendMessage("§2Antal checkpoints:§a " + track.getCheckpoints().size());
        if (bestFinish != null)
        {
            player.sendMessage("§2Ditt bästa varv:§a " + RaceUtilities.formatAsTime(bestFinish.getTime()));
        }

    }
    static void cmdToggle(Player player, String[] arguments)
    {
        if(Race.getPlugin().verbose.contains(player.getUniqueId()))
        {
            Race.getPlugin().verbose.remove(player.getUniqueId());
        }
        else
        {
            Race.getPlugin().verbose.add(player.getUniqueId());
        }
        player.sendMessage("§aKontrollpunktsannonseringar är nu " + (Race.getPlugin().verbose.contains(player.getUniqueId()) ? "påslagna." : "avstängda."));

    }
}
