package me.makkuusen.timing.system.event;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.ApiDatabase;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.heat.FinalHeat;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.QualyHeat;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.FinalDriver;
import me.makkuusen.timing.system.participant.Participant;
import me.makkuusen.timing.system.participant.QualyDriver;
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
    private String id;
    private String displayName;
    private String date;
    HashMap<UUID, Participant> participants = new HashMap<>();
    private EventSchedule eventSchedule;
    private EventResults eventResults = new EventResults();
    private EventState state = EventState.SETUP;
    Track track;

    public enum EventState {
        SETUP, QUALIFICATION, FINAL, FINISHED
    }

    public Event(String name){
        this.id = name;
        this.displayName = name;
        this.eventSchedule = new EventSchedule();
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
        if (eventSchedule.getQualyHeatList().size() > 0) {
            state = EventState.QUALIFICATION;
            return true;
        } else if (eventSchedule.getFinalHeatList().size() > 0) {
            state = EventState.FINAL;
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
            finalHeat.addDriver(new FinalDriver(driver.getTPlayer(), finalHeat, startPos++));
        }
        state = EventState.FINAL;
        return true;
    }

    public boolean finishFinals(){
        if (state != EventState.FINAL) {
            return false;
        }
        ApiUtilities.clearScoreboards();
        state = EventState.FINISHED;
        return true;
    }

    public boolean quickSetup(List<TPlayer> tDrivers, long qualyTime, int laps, int pitstops) {
        if (track == null) {
            return false;
        }
        String heatNameQ = "Qualy";
        QualyHeat qualyHeat = new QualyHeat(this, "Qualy1", qualyTime);
        QualyHeat qualyHeat2 = new QualyHeat(this, "Qualy2", qualyTime);
        String heatName = "Final";
        FinalHeat finalHeat = new FinalHeat(this, heatName, laps, pitstops);
        int startPos = 1;
        int startPos2 = 1;
        for (int i = 0; i < tDrivers.size(); i++){
            if (i%2 == 0) {
                qualyHeat.addDriver(new QualyDriver(tDrivers.get(i), qualyHeat, startPos++));
            } else {
                qualyHeat2.addDriver(new QualyDriver(tDrivers.get(i), qualyHeat2, startPos2++));
            }
            participants.put(tDrivers.get(i).getUniqueId(), new Spectator(tDrivers.get(i)));
        }
        eventSchedule.createQuickSchedule(List.of(qualyHeat, qualyHeat2),List.of(finalHeat));
        return true;
    }

    public void addParticipant(UUID uuid){
        participants.put(uuid, new Spectator(ApiDatabase.getPlayer(uuid)));
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return id.equals(event.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString(){
        return id;
    }
}
