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

public class RaceCommandTrack implements CommandExecutor
{

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] arguments)
    {

        if (!(sender instanceof Player player))
        {
            RaceUtilities.msgConsole("§cKommandot kan enbart användas av spelare");
            return false;
        }
        if (arguments.length == 0)
        {
            if (!player.hasPermission("track.command.help") && !player.isOp())
            {
                player.sendMessage("§cÅtkomst nekad.");
                return true;
            }
            else
            {
                cmdHelp(player);
            }
            return true;
        }

        if (arguments[0].equalsIgnoreCase("create"))
        {
            if (!player.isOp() && !player.hasPermission("track.command.create") && !player.isOp())
            {
                player.sendMessage("§cÅtkomst nekad.");
                return true;
            }

            if (arguments.length < 3)
            {
                sender.sendMessage("§7Syntax: /track create §ntyp§r§7 §nnamn §r§7");
                return true;
            }
            cmdCreate(player, arguments);
        }
        else if (arguments[0].equalsIgnoreCase("info"))
        {
            if (!player.isOp() && !player.hasPermission("track.command.info") && !player.isOp())
            {
                player.sendMessage("§cÅtkomst nekad.");
                return true;
            }
            if (arguments.length < 2)
            {
                sender.sendMessage("§7Syntax: /track info §nnamn§r§7");
                return true;
            }
            cmdInfo(player, arguments);
        }
        else if (arguments[0].equalsIgnoreCase("list"))
        {
            if (!player.isOp() && !player.hasPermission("track.command.list"))
            {
                player.sendMessage("§cÅtkomst nekad.");
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
                player.sendMessage("§cÅtkomst nekad.");
                return true;
            }

            cmdDelete(player, arguments);

        }
        else if (arguments[0].equalsIgnoreCase("set"))
        {
            if (!player.isOp() && !player.hasPermission("track.command.set") && !player.isOp())
            {
                player.sendMessage("§cÅtkomst nekad.");
                return true;
            }
            if (arguments.length < 3)
            {
                player.sendMessage("§7Syntax /track set name §nid§r§7 §nnamn§r§7");
                player.sendMessage("§7Syntax /track set owner §nspelare§r§7 §nnamn§r§7");
                player.sendMessage("§7Syntax /track set spawn §nnamn§r§7");
                player.sendMessage("§7Syntax /track set leaderboard §nnamn§r§7");
                player.sendMessage("§7Syntax /track set gui §nnamn§r§7");
                player.sendMessage("§7Syntax /track set type §ntyp§r§7 §nnamn§r§7");
                player.sendMessage("§7Syntax /track set startregion §nnamn§r§7");
                player.sendMessage("§7Syntax /track set endregion §nnamn§r§7");
                player.sendMessage("§7Syntax /track set checkpoint +§nnummer§r§7 §nnamn§r§7");
                player.sendMessage("§7Syntax /track set checkpoint -§nnummer§r§7 §nnamn§r§7");
                player.sendMessage("§7Syntax /track set resetregion +§nnummer§r§7 §nnamn§r§7");
                player.sendMessage("§7Syntax /track set resetregion -§nnummer§r§7 §nnamn§r§7");
                return true;
            }
            cmdSet(player, arguments);
        }
        else if (arguments[0].equalsIgnoreCase("toggle"))
        {
            if (!player.isOp() && !player.hasPermission("track.command.toggle") && !player.isOp())
            {
                player.sendMessage("§cÅtkomst nekad.");
                return true;
            }
            if (arguments.length < 3)
            {
                player.sendMessage("§7Syntax /track toggle open §nnamn§r§7");
                return true;
            }
            cmdToggle(player, arguments);
        }
        else if (arguments[0].equalsIgnoreCase("options"))
        {
            if (!player.isOp() && !player.hasPermission("track.command.options") && !player.isOp())
            {
                player.sendMessage("§cÅtkomst nekad.");
                return true;
            }
            if (arguments.length < 3)
            {
                player.sendMessage("§7Syntax /track options §nändring§r§7 §nbana");
                return true;
            }
            cmdOptions(player, arguments);
        }
        else if (arguments[0].equalsIgnoreCase("help"))
        {
            if (!player.hasPermission("track.command.help") && !player.isOp())
            {
                player.sendMessage("§cÅtkomst nekad.");
                return true;
            }
            cmdHelp(player);
        }
        else if (arguments[0].equalsIgnoreCase("updateleaderboards"))
        {

            if (!player.hasPermission("track.command.updateleaderboards") && !player.isOp())
            {
                player.sendMessage("§cÅtkomst nekad.");
                return true;
            }
            if (arguments.length != 1)
            {
                sender.sendMessage("§7Syntax: /track updateleaderboards");
                return true;
            }
            Bukkit.getScheduler().runTaskAsynchronously(Race.getPlugin(), () -> LeaderboardManager.updateAllFastestTimeLeaderboard(sender));
            sender.sendMessage("§aFörsöker uppdatera topplistorna.");
        }
        else if (arguments[0].equalsIgnoreCase("deletebesttime"))
        {
            if (!player.hasPermission("track.command.deletebesttime") && !player.isOp())
            {
                player.sendMessage("§cÅtkomst nekad.");
                return true;
            }
            // ISSUE #1
            // /parkouradmin resetbesttime <playerUuid> <map>
            if (arguments.length < 3)
            {
                sender.sendMessage("§7Syntax: /track deletebesttime §nspelare§r§7 §nnamn§r§7");
                return true;
            }

            RPlayer rPlayer = ApiDatabase.getPlayer(arguments[1]);
            if (rPlayer == null)
            {
                player.sendMessage("§cSpelaren kunde inte hittas.");
                return true;
            }

            String potentialMapName = ApiUtilities.concat(arguments, 2);
            var maybeTrack = RaceDatabase.getRaceTrack(potentialMapName);
            if (maybeTrack.isEmpty())
            {
                sender.sendMessage("§cKan inte hitta banan");
                return true;
            }
            RaceTrack track = maybeTrack.get();
            RaceFinish bestFinish = track.getBestFinish(rPlayer);
            if (bestFinish == null)
            {
                sender.sendMessage("§cBästatiden kunde inte hittas");
                return true;
            }
            track.deleteBestFinish(rPlayer, bestFinish);
            sender.sendMessage("§aÅterställde bästa varvtid för " + rPlayer.getName() + " på " + potentialMapName + ".");
            LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
            return true;
        }
        else if (arguments[0].equalsIgnoreCase("override"))
        {
            if (!player.isOp() && !player.hasPermission("track.command.override"))
            {
                player.sendMessage("§cÅtkomst nekad.");
                return true;
            }

            if (Race.getPlugin().override.contains(player.getUniqueId()))
            {
                Race.getPlugin().override.remove(player.getUniqueId());
                player.sendMessage("§aÖverskridningsläget har stängts av.");
            }

            else
            {
                Race.getPlugin().override.add(player.getUniqueId());
                player.sendMessage("§aÖverskridningsläget har aktiverats.");
            }
            return true;
        }
        else
        {
            sender.sendMessage("§7Okänt kommando. Skriv §n/track help§r§7 för en lista på kommandon.");
        }
        return false;
    }

    static void cmdHelp(Player player)
    {
        player.sendMessage("");
        player.sendMessage("§2---§a Hjälp: /track §2---");
        if (player.isOp() || player.hasPermission("track.command.create"))
        {
            player.sendMessage("§2/track create §atyp namn");
        }
        if (player.isOp() || player.hasPermission("track.command.delete"))
        {
            player.sendMessage("§2/track delete §anamn");
        }
        if (player.isOp() || player.hasPermission("track.command.info"))
        {
            player.sendMessage("§2/track info §anamn");
        }
        if (player.isOp() || player.hasPermission("track.command.list"))
        {
            player.sendMessage("§2/track list [§asida§2]");
        }
        if (player.isOp() || player.hasPermission("track.command.set"))
        {
            player.sendMessage("§2/track set name §aid §anamn");
            player.sendMessage("§2/track set owner §aspelare namn");
            player.sendMessage("§2/track set spawn §anamn");
            player.sendMessage("§2/track set leaderboard §anamn");
            player.sendMessage("§2/track set gui §anamn");
            player.sendMessage("§2/track set type §atyp namn");
            player.sendMessage("§2/track set startregion §anamn");
            player.sendMessage("§2/track set endregion §anamn");
            player.sendMessage("§2/track set checkpoint +§anummer §anamn");
            player.sendMessage("§2/track set checkpoint -§anummer §anamn");
            player.sendMessage("§2/track set resetregion +§anummer §anamn");
            player.sendMessage("§2/track set resetregion -§anummer §anamn");
        }
        if (player.isOp() || player.hasPermission("track.command.toggle"))
        {
            player.sendMessage("§2/track toggle open §anamn");
            player.sendMessage("§2/track toggle government §anamn");
        }
        if (player.isOp() || player.hasPermission("track.command.options"))
        {
            player.sendMessage("§2/track options §aändring §anamn");
        }
        if (player.isOp() || player.hasPermission("track.command.updateleaderboards"))
        {
            player.sendMessage("§2/track updateleaderboards");
        }
        if (player.isOp() || player.hasPermission("track.command.deletebesttime"))
        {
            player.sendMessage("§2/track deletebesttime §aspelare namn");
        }
        if (player.isOp() || player.hasPermission("track.command.override"))
        {
            player.sendMessage("§2/track override");
        }
    }

    static void cmdCreate(Player player, String[] arguments)
    {

        String name = ApiUtilities.concat(arguments, 2);

        if (name.length() > 25)
        {
            player.sendMessage("§cNamnets längd får inte överstiga 25 tecken.");
            return;
        }

        if (!name.matches("[A-Za-zÅÄÖåäöØÆøæ0-9 ]+"))
        {
            player.sendMessage("§cNamnet kan endast innehålla alfabetiska tecken, mellanslag och siffror.");
            return;
        }

        if (!RaceDatabase.trackNameAvailable(name))
        {
            player.sendMessage("§cDet finns redan en bana med det givna namnet.");
            return;
        }

        if(player.getInventory().getItemInMainHand().getItemMeta() == null)
        {
            player.sendMessage("§cDu behöver hålla ett föremål i handen.");
            return;
        }

        String type = arguments[1];
        RaceTrack.TrackType t = RaceTrack.TrackType.BOAT;
        if (type.equalsIgnoreCase("parkour"))
        {
            t = RaceTrack.TrackType.PARKOUR;
        }
        else if (type.equalsIgnoreCase("elytra"))
        {
            t = RaceTrack.TrackType.ELYTRA;
        }

        RaceTrack track = RaceDatabase.trackNew(name, player.getUniqueId(), player.getLocation(), t, player.getInventory().getItemInMainHand());
        if (track == null)
        {
            player.sendMessage("§cNågot gick fel.");
            return;
        }

        player.sendMessage("§aDu skapade " + name + ".");
        LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
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
        player.sendMessage("");
        player.sendMessage("§2--- §aBana: " + track.getName() + " (" + track.getId() + ") §2---");
        player.sendMessage("§2Öppen: §a" + (track.isOpen() ? "Ja" : "Nej") + "    §2Bantyp:§a " + track.getTypeAsString());
        player.sendMessage("§2Skapad:§a " + ApiUtilities.niceDate(track.getDateCreated()) + "§2 ägs av §a" + track.getOwner().getName());
        player.sendMessage("§2Alternativ: §a+" + RaceUtilities.formatPermissions(track.getOptions()));
        player.sendMessage("§2Checkpoint teleportering:§a " + (track.hasOption('c') ? "Ja" : "Nej"));
        player.sendMessage("§2Antal checkpoints:§a " + track.getCheckpoints().size());
        player.sendMessage("§2Antal resetregioner:§a " + track.getResetRegions().size());
        player.sendMessage("§2Startpunkt:§a " + ApiUtilities.niceLocation(track.getSpawnLocation()));
        player.sendMessage("§2Topplista:§a " + ApiUtilities.niceLocation(track.getLeaderboardLocation()));
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

        if (RaceDatabase.getRaceTracks().size() == 0)
        {
            player.sendMessage("§cDet finns inga banor.");
            return;
        }

        StringBuilder tmpMessage = new StringBuilder();

        int itemsPerPage = 25;
        int start = (pageStart * itemsPerPage) - itemsPerPage;
        int stop = pageStart * itemsPerPage;

        if (start >= RaceDatabase.getRaceTracks().size())
        {
            player.sendMessage("§cDet finns inte så många sidor att visa.");
            return;
        }

        for (int i = start; i < stop; i++)
        {
            if (i == RaceDatabase.getRaceTracks().size())
            {
                break;
            }

            RaceTrack track = RaceDatabase.getRaceTracks().get(i);

            tmpMessage.append(track.getName()).append(", ");

        }

        player.sendMessage("§2--- §aBanor (sida §l" + pageStart + " §r§aav §l" + ((int) Math.ceil(((double) RaceDatabase.getRaceTracks().size()) / ((double) itemsPerPage))) + "§r§a) §2---");
        player.sendMessage("§2" + tmpMessage.substring(0, tmpMessage.length() - 2));
    }

    static void cmdDelete(Player player, String[] arguments)
    {
        String name = ApiUtilities.concat(arguments, 1);
        var maybeTrack = RaceDatabase.getRaceTrack(name);
        if (maybeTrack.isEmpty())
        {
            player.sendMessage("§cDet finns ingen bana med det namnet.");
            return;
        }
        RaceDatabase.removeRaceTrack(maybeTrack.get());
        player.sendMessage("§aBanan har tagits bort.");

    }

    static void cmdToggle(Player player, String[] arguments)
    {
        String command = arguments[1];
        String name = ApiUtilities.concat(arguments, 2);
        var maybeTrack = RaceDatabase.getRaceTrack(name);
        if (maybeTrack.isEmpty())
        {
            player.sendMessage("§cDet finns ingen bana med det namnet.");
            return;
        }
        RaceTrack raceTrack = maybeTrack.get();
        if (command.equalsIgnoreCase("open"))
        {
            raceTrack.setToggleOpen(!raceTrack.isOpen());
            player.sendMessage("§aBanan är nu " + (raceTrack.isOpen() ? "öppen." : "stängd."));
        }
        else if (command.equalsIgnoreCase("government"))
        {
            raceTrack.setToggleGovernment(!raceTrack.isGovernment());
            player.sendMessage("§aBanan är nu " + (raceTrack.isGovernment() ? "statlig." : "privat."));

        }
        else
        {
            player.sendMessage("§7Syntax /track toggle open §nnamn§r§7");
            player.sendMessage("§7Syntax /track toggle government §nnamn§r§7");
        }
    }

    static void cmdOptions(Player player, String[] arguments)
    {
        String options = arguments[1];

        String name = ApiUtilities.concat(arguments, 2);
        var maybeTrack = RaceDatabase.getRaceTrack(name);
        if (maybeTrack.isEmpty())
        {
            player.sendMessage("§cDet finns ingen bana med det namnet.");
            return;
        }
        RaceTrack track = maybeTrack.get();

        String newOptions = RaceUtilities.parseFlagChange(track.getOptions(), options);
        if (newOptions == null)
        {
            player.sendMessage("§cInga rättighetsflaggor ändrades.");
            return;
        }

        if (newOptions.length() == 0)
        {
            player.sendMessage("§aAlla alternativen har tagits bort.");
        }
        else
        {
            player.sendMessage("§aAlternativen är nu: " + RaceUtilities.formatPermissions(newOptions.toCharArray()));
        }
        track.setOptions(newOptions);
    }

    static void cmdSet(Player player, String[] arguments)
    {
        String command = arguments[1];

        if (command.equalsIgnoreCase("startregion"))
        {
            String name = ApiUtilities.concat(arguments, 2);
            var maybeTrack = RaceDatabase.getRaceTrack(name);
            if (maybeTrack.isEmpty())
            {
                player.sendMessage("§cDet finns ingen bana med det namnet.");
                return;
            }
            cmdSetStartRegion(player, maybeTrack.get());

        }
        else if (command.equalsIgnoreCase("endregion"))
        {
            String name = ApiUtilities.concat(arguments, 2);
            var maybeTrack = RaceDatabase.getRaceTrack(name);
            if (maybeTrack.isEmpty())
            {
                player.sendMessage("§cDet finns ingen bana med det namnet.");
                return;
            }
            cmdSetEndRegion(player, maybeTrack.get());

        }
        else if (command.equalsIgnoreCase("spawn"))
        {
            String name = ApiUtilities.concat(arguments, 2);
            var maybeTrack = RaceDatabase.getRaceTrack(name);
            if (maybeTrack.isEmpty())
            {
                player.sendMessage("§cDet finns ingen bana med det namnet.");
                return;
            }
            maybeTrack.get().setSpawnLocation(player.getLocation());
            player.sendMessage("§aStartpunkten har sparats ner.");

        }
        else if (command.equalsIgnoreCase("leaderboard"))
        {
            String name = ApiUtilities.concat(arguments, 2);
            var maybeTrack = RaceDatabase.getRaceTrack(name);
            if (maybeTrack.isEmpty())
            {
                player.sendMessage("§cDet finns ingen bana med det namnet.");
                return;
            }
            RaceTrack track = maybeTrack.get();
            Location loc = player.getLocation();
            loc.setY(loc.getY() + 3);
            track.setLeaderboardLocation(loc);
            LeaderboardManager.updateFastestTimeLeaderboard(track.getId());
            player.sendMessage("§aKoordinater för topplistan har sparats ner.");

        }
        else if (command.equalsIgnoreCase("gui"))
        {
            if (arguments.length < 3)
            {
                player.sendMessage("§7Syntax /track set gui §nnamn§r§7");
                return;
            }
            String name = ApiUtilities.concat(arguments, 2);
            var maybeTrack = RaceDatabase.getRaceTrack(name);
            if (maybeTrack.isEmpty())
            {
                player.sendMessage("§cDet finns ingen bana med det namnet.");
                return;
            }

            var item = player.getInventory().getItemInMainHand();

            if(item.getItemMeta() == null)
            {
                player.sendMessage("§cDu behöver ha ett föremål i handen.");
                return;
            }
            maybeTrack.get().setGuiItem(item);
            player.sendMessage("§aFöremålet har sparats.");

        }
        else if (command.equalsIgnoreCase("type"))
        {
            if (arguments.length < 4)
            {
                player.sendMessage("§7Syntax /track set type §ntyp§r§7 §nnamn§r§7");
                return;
            }
            String name = ApiUtilities.concat(arguments, 3);
            var maybeTrack = RaceDatabase.getRaceTrack(name);
            if (maybeTrack.isEmpty())
            {
                player.sendMessage("§cDet finns ingen bana med det namnet.");
                return;
            }
            cmdSetType(player, maybeTrack.get(), arguments[2]);

        }
        else if (command.equalsIgnoreCase("name"))
        {
            if (arguments.length < 4)
            {
                player.sendMessage("§7Syntax /track set name §nid§r§7 §nnamn§r§7");
                return;
            }
            String name = ApiUtilities.concat(arguments, 3);
            cmdSetName(player, arguments[2], name);

        }
        else if (command.equalsIgnoreCase("owner"))
        {
            if (arguments.length < 4)
            {
                player.sendMessage("§7Syntax /track set owner §nspelare§r§7 §nnamn§r§7");
                return;
            }

            RPlayer rPlayer = ApiDatabase.getPlayer(arguments[2]);
            if (rPlayer == null)
            {
                player.sendMessage("§cSpelaren kunde inte hittas.");
                return;
            }

            String name = ApiUtilities.concat(arguments, 3);
            var maybeTrack = RaceDatabase.getRaceTrack(name);
            if (maybeTrack.isEmpty())
            {
                player.sendMessage("§cDet finns ingen bana med det namnet.");
                return;
            }
            maybeTrack.get().setOwner(rPlayer);
            player.sendMessage("§aTypen har sparats ner.");
        }
        else if (command.equalsIgnoreCase("checkpoint"))
        {
            if (arguments.length < 4)
            {
                player.sendMessage("§7Syntax /track set checkpoint §nnummer§r§7 §nnamn§r§7");
                return;
            }
            String name = ApiUtilities.concat(arguments, 3);
            var maybeTrack = RaceDatabase.getRaceTrack(name);
            if (maybeTrack.isEmpty())
            {
                player.sendMessage("§cDet finns ingen bana med det namnet.");
                return;
            }
            cmdSetCheckpoint(player, maybeTrack.get(), arguments[2]);

        }
        else if (command.equalsIgnoreCase("resetregion"))
        {
            if (arguments.length < 4)
            {
                player.sendMessage("§7Syntax /track set resetregion §nnummer§r§7 §nnamn§r§7");
                return;
            }
            String name = ApiUtilities.concat(arguments, 3);
            var maybeTrack = RaceDatabase.getRaceTrack(name);
            if (maybeTrack.isEmpty())
            {
                player.sendMessage("§cDet finns ingen bana med det namnet.");
                return;
            }
            cmdSetResetRegion(player, maybeTrack.get(), arguments[2]);
        }
        else
        {
            player.sendMessage("§2/track set name §aid anamn");
            player.sendMessage("§2/track set owner §aspelare namn");
            player.sendMessage("§2/track set spawn §anamn");
            player.sendMessage("§2/track set leaderboard §anamn");
            player.sendMessage("§2/track set gui §anamn");
            player.sendMessage("§2/track set type §atyp namn");
            player.sendMessage("§2/track set startregion §anamn");
            player.sendMessage("§2/track set endregion §anamn");
            player.sendMessage("§2/track set checkpoint +§anummer §anamn");
            player.sendMessage("§2/track set checkpoint -§anummer §anamn");
            player.sendMessage("§2/track set resetregion +§anummer §anamn");
            player.sendMessage("§2/track set resetregion -§anummer §anamn");
        }
    }

    static void cmdSetType(Player player, RaceTrack track, String type)
    {
        RaceTrack.TrackType trackType = track.getTypeFromString(type);

        if (trackType == null)
        {
            player.sendMessage("§cFelaktig bantyp.");
            return;
        }
        track.setTrackType(trackType);
        player.sendMessage("§aTypen har sparats.");
    }

    static void cmdSetName(Player player, String id, String name)
    {
        int trackId;
        try
        {
            trackId = Integer.parseInt(id);
        } catch (NumberFormatException e)
        {
            player.sendMessage("§cID måste vara en siffror.");
            return;
        }
        var maybeTrack = RaceDatabase.getTrackById(trackId);
        if (maybeTrack.isEmpty())
        {
            player.sendMessage("§cDet finns ingen bana med det id:t");
            return;
        }

        if (name.length() > 25)
        {
            player.sendMessage("§cNamnets längd får inte överstiga 25 tecken.");
            return;
        }

        if (!name.matches("[A-Za-zÅÄÖåäöØÆøæ0-9 ]+"))
        {
            player.sendMessage("§cNamnet kan endast innehålla alfabetiska tecken, mellanslag och siffror.");
            return;
        }
        maybeTrack.get().setName(name);
        player.sendMessage("§aDet nya namnet är sparat.");
        LeaderboardManager.updateFastestTimeLeaderboard(trackId);
    }

    static void cmdSetStartRegion(Player player, RaceTrack track)
    {
        List<Location> positions = getPositions(player);
        if (positions == null)
        {
            return;
        }
        track.setStartRegion(positions.get(0), positions.get(1));
        player.sendMessage("§aDen nya startregionen har skapats.");
    }

    static void cmdSetEndRegion(Player player, RaceTrack track)
    {
        List<Location> positions = getPositions(player);
        if (positions == null)
        {
            return;
        }
        track.setEndRegion(positions.get(0), positions.get(1));
        player.sendMessage("§aDen nya slutregionen har skapats.");
    }

    static void cmdSetResetRegion(Player player, RaceTrack track, String index)
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
            player.sendMessage("§cIndex kunde inte processas. Är det en siffra?.");
            return;
        }
        if (remove)
        {
            if (track.removeResetRegion(regionIndex))
            {
                player.sendMessage("§aResetregionen har tagits bort.");
            }
            else
            {
                player.sendMessage("§aResetregionen kunde inte tas bort.");
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
            player.sendMessage("§aDen nya resetregionen har skapats.");
        }
    }

    static void cmdSetCheckpoint(Player player, RaceTrack track, String index)
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
            player.sendMessage("§cIndex kunde inte processas. Är det en siffra?.");
            return;
        }

        if (remove)
        {
            if (track.removeCheckpoint(regionIndex))
            {
                player.sendMessage("§aCheckpointen har tagits bort.");
            }
            else
            {
                player.sendMessage("§aCheckpointen kunde inte tas bort.");
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
            player.sendMessage("§aDen nya checkpointen har skapats.");
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
            player.sendMessage("§cDu måste först göra en markering.");
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
            player.sendMessage("§cDin markering kunde inte användas.");
            return null;
        }
    }

}

