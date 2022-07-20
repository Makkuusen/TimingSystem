package me.makkuusen.timing.system.event;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.DatabaseTrack;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.Participant;
import me.makkuusen.timing.system.participant.Spectator;
import me.makkuusen.timing.system.track.Track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
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
    private EventSchedule eventSchedule;
    private EventResults eventResults = new EventResults();
    private EventState state;
    Track track;

    public enum EventState {
        SETUP, QUALIFICATION, FINAL, FINISHED
    }

    public Event(DbRow data){
        id = data.getInt("id");
        displayName = data.getString("name");
        uuid = UUID.fromString(data.getString("uuid"));
        date = data.getLong("date");
        track = data.get("track") == null ? null : DatabaseTrack.getTrackById(data.getInt("track")).get();
        state = EventState.valueOf(data.getString("state"));
        eventSchedule = new EventSchedule();
    }

    public boolean start(){
        if (state != EventState.SETUP){
            return false;
        }
        if (track == null){
            return false;
        }
        if (eventSchedule.getHeats().size() == 0){
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

    public boolean finishQualification(){
        if (state != EventState.QUALIFICATION) {
            return false;
        }
        List<Driver> drivers = eventResults.generateFinalStartingPositions();
        // Ugly way to get finalheat
        Heat finalHeat = eventSchedule.getFinalHeatList().get(0);
        int startPos = 1;
        for (Driver driver : drivers){
            EventDatabase.finalDriverNew(driver.getTPlayer().getUniqueId(), finalHeat, startPos++);
        }
        setState(EventState.FINAL);
        return true;
    }

    public boolean finishFinals(){
        if (state != EventState.FINAL) {
            return false;
        }
        ApiUtilities.clearScoreboards();
        setState(EventState.FINISHED);
        return true;
    }

    /*public boolean quickSetup(List<TPlayer> tDrivers, long qualyTime, int laps, int pitstops) {
        if (track == null) {
            return false;
        }
        String heatNameQ = "Qualy";
        QualifyHeat qualifyHeat = new QualifyHeat(this, "Qualy1", qualyTime);
        QualifyHeat qualifyHeat2 = new QualifyHeat(this, "Qualy2", qualyTime);
        String heatName = "Final";
        FinalHeat finalHeat = new FinalHeat(this, heatName, laps, pitstops);
        int startPos = 1;
        int startPos2 = 1;
        for (int i = 0; i < tDrivers.size(); i++){
            if (i%2 == 0) {
                qualifyHeat.addDriver(new QualyDriver(tDrivers.get(i), qualifyHeat, startPos++));
            } else {
                qualifyHeat2.addDriver(new QualyDriver(tDrivers.get(i), qualifyHeat2, startPos2++));
            }
            participants.put(tDrivers.get(i).getUniqueId(), new Spectator(tDrivers.get(i)));
        }
        eventSchedule.createQuickSchedule(List.of(qualifyHeat, qualifyHeat2),List.of(finalHeat));
        return true;
    }*/

    public void addParticipant(UUID uuid){
        participants.put(uuid, new Spectator(Database.getPlayer(uuid)));
    }

    public List<Participant> getParticipants(){
        return participants.values().stream().toList();
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

    public void setTrack(Track track){
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
    public String toString(){
        return displayName;
    }
}
