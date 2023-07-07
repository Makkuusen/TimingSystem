package me.makkuusen.timing.system.event;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;
import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import lombok.Getter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.heat.Lap;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.Subscriber;
import me.makkuusen.timing.system.round.FinalRound;
import me.makkuusen.timing.system.round.QualificationRound;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.round.RoundType;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Getter
public class EventDatabase {

    private static final Set<Event> events = new HashSet<>();
    private static final Set<Heat> heats = new HashSet<>();
    private static final HashMap<UUID, Event> playerSelectedEvent = new HashMap<>();
    private static final HashMap<UUID, Driver> playerInRunningHeat = new HashMap<>();
    public static TimingSystem plugin;

    public static void initDatabaseSynchronize() throws SQLException {
        var dbRows = DB.getResults("SELECT * FROM `ts_events` WHERE `isRemoved` = 0;");

        for (DbRow dbRow : dbRows) {
            if (dbRow.getString("name").equalsIgnoreCase("QuickRace")) {
                continue;
            }
            Event event = new Event(dbRow);
            events.add(event);
            EventSchedule es = new EventSchedule();

            var signsDbRows = DB.getResults("SELECT * FROM `ts_events_signs` WHERE `eventId` = " + event.getId() + ";");
            for (DbRow signsData : signsDbRows) {
                try {
                    var type = Subscriber.Type.valueOf(signsData.getString("type"));
                    if (type == Subscriber.Type.SUBSCRIBER) {
                        event.subscribers.put(UUID.fromString(signsData.get("uuid")), new Subscriber(signsData));
                    } else if (type == Subscriber.Type.RESERVE) {
                        event.reserves.put(UUID.fromString(signsData.get("uuid")), new Subscriber(signsData));
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }

            var roundDbRows = DB.getResults("SELECT * FROM `ts_rounds` WHERE `eventId` = " + event.getId() + " AND `isRemoved` = 0;");
            for (DbRow roundData : roundDbRows) {
                Round round;
                var type = RoundType.valueOf(roundData.getString("type"));
                if (type == RoundType.FINAL) {
                    round = new FinalRound(roundData);
                } else {
                    round = new QualificationRound(roundData);
                }
                var heatDbRows = DB.getResults("SELECT * FROM `ts_heats` WHERE `roundId` = " + round.getId() + " AND `isRemoved` = 0;");
                for (DbRow heatData : heatDbRows) {
                    initHeat(round, heatData);
                }
                es.addRound(round);
            }
            es.setCurrentRound();
            event.setEventSchedule(es);
        }
    }

    private static void initHeat(Round round, DbRow heatData) throws SQLException {
        Heat heat = new Heat(heatData, round);
        heats.add(heat);
        round.addHeat(heat);
        var driverDbRows = DB.getResults("SELECT * FROM `ts_drivers` WHERE `heatId` = " + heat.getId() + " AND `isRemoved` = 0;");
        for (DbRow driverData : driverDbRows) {
            initDriver(heat, driverData);
        }
    }

    private static void initDriver(Heat heat, DbRow driverData) throws SQLException {
        Driver driver = new Driver(driverData);
        heat.addDriver(driver);
        List<Lap> laps = new ArrayList<>();
        var lapsDbRows = DB.getResults("SELECT * FROM `ts_laps` WHERE `heatId` = " + heat.getId() + " AND `uuid` = '" + driverData.getString("uuid") + "' AND `isRemoved` = 0;");
        for (DbRow lapsData : lapsDbRows) {
            laps.add(new Lap(lapsData));
        }
        driver.setLaps(laps);
    }


    static public void setPlayerSelectedEvent(UUID uuid, Event event) {
        playerSelectedEvent.put(uuid, event);
    }

    static public Optional<Event> getPlayerSelectedEvent(UUID uuid) {
        if (playerSelectedEvent.containsKey(uuid)) {
            return Optional.of(playerSelectedEvent.get(uuid));
        }
        return Optional.empty();
    }

    static public Optional<Driver> getClosestDriverForSpectator(Player player) {
        Optional<Driver> closest = Optional.empty();
        double distance = -1;
        for (Driver driver : playerInRunningHeat.values()) {
            if (driver.getTPlayer().getPlayer() != null && driver.getHeat().getEvent().getSpectators().get(player.getUniqueId()) != null) {
                if (player.getLocation().getWorld() != driver.getTPlayer().getPlayer().getWorld()) {
                    continue;
                }
                if (driver.isFinished()) {
                    continue;
                }
                if (closest.isEmpty()) {
                    closest = Optional.of(driver);
                    distance = player.getLocation().distance(driver.getTPlayer().getPlayer().getLocation());
                } else {
                    if (player.getLocation().distance(driver.getTPlayer().getPlayer().getLocation()) < distance) {
                        closest = Optional.of(driver);
                        distance = player.getLocation().distance(driver.getTPlayer().getPlayer().getLocation());
                    }
                }
            }
        }
        return closest;
    }

    static public Optional<Event> getEvent(String name) {
        return events.stream().filter(event -> event.getDisplayName().equalsIgnoreCase(name)).findFirst();
    }

    static public Optional<Event> getEvent(int id) {
        return events.stream().filter(event -> event.getId() == id).findFirst();
    }

    static public Optional<Heat> getHeat(int id) {
        return heats.stream().filter(heat -> heat.getId() == id).findFirst();
    }

    static public List<Heat> getHeats() {
        return heats.stream().toList();
    }

    static public Optional<Event> eventNew(UUID uuid, String name) {
        try {
            if (getEvent(name).isPresent()) {
                return Optional.empty();
            }
            var eventId = DB.executeInsert("INSERT INTO `ts_events`(" +
                    "`name`, " +
                    "`uuid`, " +
                    "`date`, " +
                    "`track`, " +
                    "`state`, " +
                    "`isRemoved`) " +
                    "VALUES (" +
                    "'" + name + "'," +
                    "'" + uuid + "'," +
                    ApiUtilities.getTimestamp() + "," +
                    "NULL," +
                    "'" + Event.EventState.SETUP.name() + "'," +
                    "0)");
            var dbRow = DB.getFirstRow("SELECT * FROM `ts_events` WHERE `id` = " + eventId + ";");
            Event event = new Event(dbRow);
            if (events.add(event)) {
                setPlayerSelectedEvent(uuid, event);
            }
            return Optional.of(event);
        } catch (SQLException exception) {
            exception.printStackTrace();
            return Optional.empty();
        }
    }

    static public boolean roundNew(Event event, RoundType roundType, int roundNumber) {
        try {
            var roundId = DB.executeInsert("INSERT INTO `ts_rounds`(" +
                    "`eventId`, " +
                    "`roundIndex`, " +
                    "`type`, " +
                    "`state`, " +
                    "`isRemoved`) " +
                    "VALUES (" +
                    event.getId() + ", " +
                    roundNumber + ", " +
                    "'" + roundType.name() + "'," +
                    "'" + Round.RoundState.SETUP.name() + "'," +
                    "0)");
            var dbRow = DB.getFirstRow("SELECT * FROM `ts_rounds` WHERE `id` = " + roundId + ";");
            Round round;
            if (roundType == RoundType.QUALIFICATION) {
                round = new QualificationRound(dbRow);
            } else {
                round = new FinalRound(dbRow);
            }
            event.eventSchedule.addRound(round);
            return true;


        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    static public Optional<Heat> heatNew(Round round, int heatNumber) {
        try {
            var heatId = DB.executeInsert("INSERT INTO `ts_heats`(" +
                    "`roundId`, " +
                    "`heatNumber`, " +
                    "`state`, " +
                    "`startTime`, " +
                    "`endTime`, " +
                    "`fastestLapUUID`, " +
                    "`totalLaps`, " +
                    "`totalPitstops`, " +
                    "`timeLimit`, " +
                    "`startDelay`, " +
                    "`maxDrivers`, " +
                    "`isRemoved`) " +
                    "VALUES (" +
                    round.getId() + "," +
                    heatNumber + "," +
                    "'" + HeatState.SETUP.name() + "'," +
                    "NULL," +
                    "NULL," +
                    "NULL," +
                    "NULL," +
                    "NULL," +
                    "NULL," +
                    "NULL," +
                    "NULL," +
                    "0)");
            var dbRow = DB.getFirstRow("SELECT * FROM `ts_heats` WHERE `id` = " + heatId + ";");
            var heat = new Heat(dbRow, round);
            heats.add(heat);
            round.addHeat(heat);
            return Optional.of(heat);
        } catch (SQLException exception) {
            exception.printStackTrace();
            return Optional.empty();
        }
    }

    public static boolean heatDriverNew(UUID uuid, Heat heat, int startPosition) {
        if (heat.getHeatState() != HeatState.SETUP && heat.getHeatState() != HeatState.LOADED) {
            return false;
        }
        try {
            var driverDbRow = driverNew(uuid, heat, startPosition);
            heat.addDriver(new Driver(driverDbRow));
            return true;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public static Subscriber subscriberNew(TPlayer tPlayer, Event event, Subscriber.Type type) {

        DB.executeUpdateAsync("INSERT INTO `ts_events_signs`(" +
                "`eventId`, " +
                "`uuid`, " +
                "`type`) " +
                "VALUES (" +
                event.getId() + ", " +
                "'" + tPlayer.getUniqueId() + "'," +
                "'" + type.name() + "')");

        return new Subscriber(tPlayer);

    }

    private static DbRow driverNew(UUID uuid, Heat heat, int startPosition) throws SQLException {
        var driverId = DB.executeInsert("INSERT INTO `ts_drivers`(" +
                "`uuid`, " +
                "`heatId`, " +
                "`position`, " +
                "`startPosition`, " +
                "`startTime`, " +
                "`endTime`, " +
                "`pitstops`) " +
                "VALUES (" +
                "'" + uuid + "'," +
                heat.getId() + "," +
                startPosition + "," +
                startPosition + "," +
                "NULL," +
                "NULL," +
                "0)");
        return DB.getFirstRow("SELECT * FROM `ts_drivers` WHERE `id` = " + driverId + ";");
    }

    public static void lapNew(Lap lap) {
        String lapEnd;
        if (lap.getLapEnd() == null) {
            lapEnd = "NULL";
        } else {
            lapEnd = String.valueOf(lap.getLapEnd().toEpochMilli());
        }
        DB.executeUpdateAsync("INSERT INTO `ts_laps`(" +
                "`uuid`, " +
                "`heatId`, " +
                "`trackId`, " +
                "`lapStart`, " +
                "`lapEnd`, " +
                "`pitted`) " +
                "VALUES (" +
                "'" + lap.getPlayer().getUniqueId() + "'," +
                lap.getHeatId() + "," +
                lap.getTrack().getId() + "," +
                lap.getLapStart().toEpochMilli() + "," +
                lapEnd + "," +
                lap.isPitted() + ")");
    }


    static public Set<Event> getEvents() {
        return events;
    }

    static public List<String> getEventsAsStrings() {
        List<String> eventStrings = new ArrayList<>();
        events.forEach(event -> eventStrings.add(event.toString()));
        return eventStrings;
    }

    static public List<String> getRoundsAsStrings(UUID uuid) {
        var maybeEvent = getPlayerSelectedEvent(uuid);
        if (maybeEvent.isEmpty()) {
            return List.of();
        }
        List<String> roundList = new ArrayList<>();
        maybeEvent.get().eventSchedule.getRounds().forEach(round -> roundList.add(round.getName()));
        return roundList;

    }

    static public List<String> getHeatsAsStrings(UUID uuid) {
        var maybeEvent = getPlayerSelectedEvent(uuid);
        if (maybeEvent.isEmpty()) {
            return List.of();
        }
        List<String> roundList = new ArrayList<>();
        maybeEvent.get().eventSchedule.getRounds().forEach(round -> roundList.addAll(round.getRawHeats()));
        return roundList;

    }

    public static ContextResolver<Event, BukkitCommandExecutionContext> getEventContextResolver() {
        return (c) -> {
            String first = c.popFirstArg();
            var maybeEvent = getEvent(first);
            if (maybeEvent.isPresent()) {
                return maybeEvent.get();
            } else {
                // User didn't type an Event, show error!
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }

    public static ContextResolver<Round, BukkitCommandExecutionContext> getRoundContextResolver() {
        return (c) -> {
            String roundName = c.popFirstArg();
            var maybeEvent = getPlayerSelectedEvent(c.getPlayer().getUniqueId());
            if (maybeEvent.isPresent()) {
                Event event = maybeEvent.get();
                var maybeRound = event.getEventSchedule().getRound(roundName);
                if (maybeRound.isPresent()) {
                    return maybeRound.get();
                }
            }
            // User didn't type a heat, show error!
            throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
        };
    }

    public static ContextResolver<Heat, BukkitCommandExecutionContext> getHeatContextResolver() {
        return (c) -> {
            String heatName = c.popFirstArg();
            var maybeEvent = getPlayerSelectedEvent(c.getPlayer().getUniqueId());
            if (maybeEvent.isPresent()) {
                Event event = maybeEvent.get();
                var maybeHeat = event.getEventSchedule().getHeat(heatName);
                if (maybeHeat.isPresent()) {
                    return maybeHeat.get();
                }
            }
            // User didn't type a heat, show error!
            throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
        };
    }

    public static void addPlayerToRunningHeat(Driver driver) {
        if (playerInRunningHeat.get(driver.getTPlayer().getUniqueId()) != null) {
            return;
        }
        playerInRunningHeat.put(driver.getTPlayer().getUniqueId(), driver);
    }

    public static boolean removeEvent(Event event) {
        if (event.hasRunningHeat()) {
            return false;
        }
        List<UUID> uuids = new ArrayList<>(playerSelectedEvent.keySet());
        for (UUID uuid : uuids) {
            if (playerSelectedEvent.get(uuid).equals(event)) {
                playerSelectedEvent.remove(uuid);
            }
        }
        events.remove(event);
        DB.executeUpdateAsync("UPDATE `ts_events` SET `isRemoved` = 1 WHERE `id` = " + event.getId() + ";");
        return true;
    }

    public static void removeEventHard(Event event) {
        if (event.hasRunningHeat()) {
            event.getRunningHeat().get().finishHeat();
        }
        List<UUID> uuids = new ArrayList<>(playerSelectedEvent.keySet());
        for (UUID uuid : uuids) {
            if (playerSelectedEvent.get(uuid).equals(event)) {
                playerSelectedEvent.remove(uuid);
            }
        }
        events.remove(event);
        DB.executeUpdateAsync("UPDATE `ts_events` SET `isRemoved` = 1 WHERE `id` = " + event.getId() + ";");
    }

    public static boolean removeHeat(Heat heat) {
        if (heat.getRound().removeHeat(heat)) {
            heats.remove(heat);
            DB.executeUpdateAsync("UPDATE `ts_heats` SET `isRemoved` = 1 WHERE `id` = " + heat.getId() + ";");
            return true;
        }
        return false;
    }

    public static boolean removeRound(Round round) {
        if (round.getEvent().getEventSchedule().removeRound(round)) {
            DB.executeUpdateAsync("UPDATE `ts_rounds` SET `isRemoved` = 1 WHERE `id` = " + round.getId() + ";");
            return true;
        }
        return false;
    }

    public static void removePlayerFromRunningHeat(UUID uuid) {
        playerInRunningHeat.remove(uuid);
    }

    public static Optional<Driver> getDriverFromRunningHeat(UUID uuid) {
        if (playerInRunningHeat.get(uuid) != null) {
            return Optional.of(playerInRunningHeat.get(uuid));
        }
        return Optional.empty();
    }

}
