package me.makkuusen.timing.system.event;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;
import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import lombok.Getter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.heat.FinalHeat;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.heat.Lap;
import me.makkuusen.timing.system.heat.QualifyHeat;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.FinalDriver;
import me.makkuusen.timing.system.participant.QualyDriver;

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

    public static TimingSystem plugin;
    private static Set<Event> events = new HashSet<>();
    private static Set<Heat> heats = new HashSet<>();
    private static HashMap<UUID, Event> playerSelectedEvent = new HashMap<>();
    private static HashMap<UUID, Driver> playerInRunningHeat = new HashMap<>();

    public static void initDatabaseSynchronize() throws SQLException {
        var dbRows = DB.getResults("SELECT * FROM `ts_events` WHERE `isRemoved` = 0;");

        for (DbRow dbRow : dbRows) {
            Event event = new Event(dbRow);
            events.add(event);
            EventSchedule es = new EventSchedule();
            EventResults er = new EventResults();

            var heatDbRows = DB.getResults("SELECT * FROM `ts_heats` WHERE `eventId` = " + event.getId() + " AND `isRemoved` = 0;");
            for (DbRow heatData : heatDbRows) {
                Heat heat;
                if (heatData.getString("type").equalsIgnoreCase("Final")) {
                    heat = new FinalHeat(heatData);
                    heats.add(heat);
                    es.addHeat(heat);
                    var driverDbRows = DB.getResults("SELECT * FROM `ts_drivers` WHERE `heatId` = " + heat.getId() + ";");
                    for (DbRow driverData : driverDbRows) {
                        Driver driver = new FinalDriver(driverData);
                        heat.addDriver(driver);
                        List<Lap> laps = new ArrayList<>();
                        var lapsDbRows = DB.getResults("SELECT * FROM `ts_laps` WHERE `heatId` = " + heat.getId() + " AND `uuid` = '" + driverData.getString("uuid") + "';");
                        for (DbRow lapsData : lapsDbRows) {
                            laps.add(new Lap(lapsData));
                        }
                        driver.setLaps(laps);
                    }
                    if (heat.getEndTime() != null && heat.getEndTime().toEpochMilli() > 0) {
                        er.reportFinalResults(heat.getDrivers().values().stream().toList());
                    }

                } else {
                    heat = new QualifyHeat(heatData);
                    es.addHeat(heat);
                    heats.add(heat);
                    var driverDbRows = DB.getResults("SELECT * FROM `ts_drivers` WHERE `heatId` = " + heat.getId() + ";");
                    for (DbRow driverData : driverDbRows) {
                        Driver driver = new QualyDriver(driverData);
                        heat.addDriver(driver);
                        List<Lap> laps = new ArrayList<>();
                        var lapsDbRows = DB.getResults("SELECT * FROM `ts_laps` WHERE `heatId` = " + heat.getId() + " AND `uuid` = '" + driverData.getString("uuid") + "';");
                        for (DbRow lapsData : lapsDbRows) {
                            laps.add(new Lap(lapsData));
                        }
                        driver.setLaps(laps);
                    }
                    if (heat.getEndTime() != null && heat.getEndTime().toEpochMilli() > 0) {
                        er.reportQualyResults(heat.getDrivers().values().stream().toList());
                    }
                }
            }
            event.setEventSchedule(es);
            event.setEventResults(er);
        }
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

    static public Optional<Event> getEvent(String name) {
        return events.stream().filter(event -> event.getDisplayName().equalsIgnoreCase(name)).findFirst();
    }

    static public Optional<Event> getEvent(int id) {
        return events.stream().filter(event -> event.getId() == id).findFirst();
    }

    static public Optional<Heat> getHeat(int id) {
        return heats.stream().filter(heat -> heat.getId() == id).findFirst();
    }

    static public boolean eventNew(UUID uuid, String name) {
        try {
            if (getEvent(name).isPresent()) {
                return false;
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
            return true;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    static public boolean qualifyHeatNew(Event event, String name, int timeLimit) {

        try {
            var heatId = DB.executeInsert("INSERT INTO `ts_heats`(" +
                    "`eventId`, " +
                    "`name`, " +
                    "`type`, " +
                    "`state`, " +
                    "`startTime`, " +
                    "`endTime`, " +
                    "`fastestLapUUID`, " +
                    "`totalLaps`, " +
                    "`totalPitstops`, " +
                    "`timeLimit`, " +
                    "`startDelay`, " +
                    "`isRemoved`) " +
                    "VALUES (" +
                    event.getId() + "," +
                    "'" + name + "'," +
                    "'QUALIFICATION'," +
                    "'" + HeatState.SETUP.name() + "'," +
                    "NULL," +
                    "NULL," +
                    "NULL," +
                    "NULL," +
                    "NULL," +
                    timeLimit + "," +
                    "NULL," +
                    "0)");
            var dbRow = DB.getFirstRow("SELECT * FROM `ts_heats` WHERE `id` = " + heatId + ";");
            var qualifyHeat = new QualifyHeat(dbRow);
            heats.add(qualifyHeat);
            event.getEventSchedule().addHeat(qualifyHeat);
            return true;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    static public boolean finalHeatNew(Event event, String name, int totalLaps, int pitstops) {

        try {
            var heatId = DB.executeInsert("INSERT INTO `ts_heats`(" +
                    "`eventId`, " +
                    "`name`, " +
                    "`type`, " +
                    "`state`, " +
                    "`startTime`, " +
                    "`endTime`, " +
                    "`fastestLapUUID`, " +
                    "`totalLaps`, " +
                    "`totalPitstops`, " +
                    "`timeLimit`, " +
                    "`startDelay`, " +
                    "`isRemoved`) " +
                    "VALUES (" +
                    event.getId() + "," +
                    "'" + name + "'," +
                    "'FINAL'," +
                    "'" + HeatState.SETUP.name() + "'," +
                    "NULL," +
                    "NULL," +
                    "NULL," +
                    totalLaps + "," +
                    pitstops + "," +
                    "NULL," +
                    "NULL," +
                    "0)");
            var dbRow = DB.getFirstRow("SELECT * FROM `ts_heats` WHERE `id` = " + heatId + ";");
            var finalHeat = new FinalHeat(dbRow);
            heats.add(finalHeat);
            event.getEventSchedule().addHeat(finalHeat);
            return true;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public static boolean finalDriverNew(UUID uuid, Heat heat, int startPosition) {
        try {
            var driverDbRow = driverNew(uuid, heat, startPosition);
            var finalDriver = new FinalDriver(driverDbRow);
            heat.addDriver(finalDriver);
            return true;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public static boolean qualyDriverNew(UUID uuid, Heat heat, int startPosition) {
        try {
            var driverDbRow = driverNew(uuid, heat, startPosition);
            var qualyDriver = new QualyDriver(driverDbRow);
            heat.addDriver(qualyDriver);
            return true;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    private static DbRow driverNew(UUID uuid, Heat heat, int startPosition) throws SQLException {
        var driverId = DB.executeInsert("INSERT INTO `ts_drivers`(" +
                "`uuid`, " +
                "`heatId`, " +
                "`isFinished`, " +
                "`position`, " +
                "`startPosition`, " +
                "`startTime`, " +
                "`endTime`, " +
                "`pitstops`) " +
                "VALUES (" +
                "'" + uuid + "'," +
                heat.getId() + "," +
                "0," +
                startPosition + "," +
                startPosition + "," +
                "NULL," +
                "NULL," +
                "0)");
        return DB.getFirstRow("SELECT * FROM `ts_drivers` WHERE `id` = " + driverId + ";");
    }

    public static boolean lapNew(Lap lap) {
        try {
            String lapEnd;
            if (lap.getLapEnd() == null) {
                lapEnd = "NULL";
            } else {
                lapEnd = String.valueOf(lap.getLapEnd().toEpochMilli());
            }
            var driverId = DB.executeInsert("INSERT INTO `ts_laps`(" +
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

        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
        return true;
    }


    static public Set<Event> getEvents() {
        return events;
    }

    static public List<String> getEventsAsStrings() {
        List<String> eventStrings = new ArrayList<>();
        events.stream().forEach(event -> eventStrings.add(event.toString()));
        return eventStrings;
    }

    static public List<String> getHeatsAsStrings(UUID uuid) {
        var maybeEvent = getPlayerSelectedEvent(uuid);
        if (maybeEvent.isEmpty()) {
            return List.of();
        }
        return maybeEvent.get().getRawHeatList();

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

    public static boolean addPlayerToRunningHeat(Driver driver) {
        if (playerInRunningHeat.get(driver.getTPlayer().getUniqueId()) != null) {
            return false;
        }
        playerInRunningHeat.put(driver.getTPlayer().getUniqueId(), driver);
        return true;
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

    public static void unload(){
        events = new HashSet<>();
        heats = new HashSet<>();
        playerSelectedEvent = new HashMap<>();
        playerInRunningHeat = new HashMap<>();
    }
}
