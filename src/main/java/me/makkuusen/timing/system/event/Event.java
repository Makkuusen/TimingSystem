package me.makkuusen.timing.system.event;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.DatabaseTrack;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.Participant;
import me.makkuusen.timing.system.participant.Spectator;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackRegion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
public class Event {

    public static TimingSystem plugin;
    private int id;
    private UUID uuid;
    private String displayName;
    private long date;
    HashMap<UUID, Participant> participants = new HashMap<>();
    HashMap<UUID, Spectator> spectators = new HashMap<>();
    private EventSchedule eventSchedule;
    private EventState state;
    Track track;

    public enum EventState {
        SETUP, QUALIFICATION, FINAL, FINISHED
    }

    public Event(DbRow data) {
        id = data.getInt("id");
        displayName = data.getString("name");
        uuid = UUID.fromString(data.getString("uuid"));
        date = data.getLong("date");
        Optional<Track> maybeTrack = data.get("track") == null ? Optional.empty() : DatabaseTrack.getTrackById(data.getInt("track"));
        track = maybeTrack.isEmpty() ? null : maybeTrack.get();
        state = EventState.valueOf(data.getString("state"));
        eventSchedule = new EventSchedule();
    }

    public boolean start() {
        if (state != EventState.SETUP) {
            return false;
        }
        if (track == null) {
            return false;
        }
        if (eventSchedule.getHeats().size() == 0) {
            return false;
        }
        if (eventSchedule.getQualifyHeatList().size() > 0) {
            setState(EventState.QUALIFICATION);
            return true;
        } else if (eventSchedule.getFinalHeatList().size() > 0) {
            setState(EventState.FINAL);
            return true;
        }
        return false;
    }

    public boolean finishQualification() {
        if (state != EventState.QUALIFICATION) {
            return false;
        }
        List<Driver> drivers = EventResults.generateQualificationResults(eventSchedule.getQualifyHeatList());
        if (eventSchedule.getQualifyHeatList().size() > 1) {
            EventAnnouncements.broadcastQualificationResults(this, drivers);
        }


        if (eventSchedule.getFinalHeatList().isEmpty()) {
            int maxDrivers = track.getGridLocations().size();
            int finalHeats = drivers.size() / maxDrivers;
            if (drivers.size() % maxDrivers != 0) {
                finalHeats++;
            }
            for (int i = 0; i < finalHeats; i++) {
                int pits = 0;
                var maybePit = getTrack().getRegion(TrackRegion.RegionType.PIT);
                if (maybePit.isPresent() && maybePit.get().isDefined()) {
                    pits = TimingSystem.configuration.getPits();
                }
                EventDatabase.finalHeatNew(this, i + 1, TimingSystem.configuration.getLaps(), pits);
            }
        }

        int i = 0;
        for (Heat finalHeat : eventSchedule.getFinalHeatList()) {
            int startPos = 1;
            for (; i < drivers.size(); i++) {
                if (startPos > finalHeat.getMaxDrivers()){
                    break;
                }
                EventDatabase.finalDriverNew(drivers.get(i).getTPlayer().getUniqueId(), finalHeat, startPos++);
            }
        }
        setState(EventState.FINAL);
        return true;
    }

    public boolean finishFinals() {
        if (state != EventState.FINAL) {
            return false;
        }
        if (eventSchedule.getFinalHeatList().size() > 1) {
            List<Driver> drivers = EventResults.generateFinalResults(eventSchedule.getFinalHeatList());
            EventAnnouncements.broadcastFinalResults(this, drivers);
        }
        setState(EventState.FINISHED);
        return true;
    }

    public void addSpectator(UUID uuid) {
        spectators.put(uuid, new Spectator(Database.getPlayer(uuid)));
    }

    public List<String> getHeatList() {
        List<String> message = new ArrayList<>();
        message.add("§2--- Heats for §a" + displayName + " §2---");
        message.addAll(eventSchedule.listHeats());
        return message;
    }

    public List<String> getRawHeatList() {
        return eventSchedule.getRawHeats();
    }

    public void setTrack(Track track) {
        this.track = track;
        DB.executeUpdateAsync("UPDATE `ts_events` SET `track` = " + track.getId() + " WHERE `id` = " + id + ";");
    }

    public void setState(EventState state) {
        this.state = state;
        DB.executeUpdateAsync("UPDATE `ts_events` SET `state` = '" + state.name() + "' WHERE `id` = " + id + ";");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return id == event.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
