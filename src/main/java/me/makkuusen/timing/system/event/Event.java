package me.makkuusen.timing.system.event;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.database.Database;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.participant.Spectator;
import me.makkuusen.timing.system.participant.Subscriber;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
public class Event {

    public static TimingSystem plugin;
    public EventSchedule eventSchedule;
    HashMap<UUID, Subscriber> subscribers = new HashMap<>(); // Signed drivers
    HashMap<UUID, Subscriber> reserves = new HashMap<>();
    HashMap<UUID, Spectator> spectators = new HashMap<>();
    Track track;
    private int id;
    private UUID uuid;
    private String displayName;
    private long date;
    private boolean openSign;
    private EventState state;

    public Event(DbRow data) {
        id = data.getInt("id");
        displayName = data.getString("name");
        uuid = UUID.fromString(data.getString("uuid"));
        date = TimingSystem.configuration.useSQLite() ? data.getInt("date").longValue() : data.getLong("date"); // seems weird converting this int to a long maybe hmmmm
        Optional<Track> maybeTrack = data.get("track") == null ? Optional.empty() : TrackDatabase.getTrackById(data.getInt("track"));
        track = maybeTrack.orElse(null);
        state = EventState.valueOf(data.getString("state"));
        openSign = data.get("open").equals(1);
        eventSchedule = new EventSchedule();
    }

    public boolean start() {
        if (state != EventState.SETUP) {
            return false;
        }
        if (track == null) {
            return false;
        }
        return eventSchedule.start(this);
    }

    public boolean finish() {
        if (state == EventState.FINISHED || state == EventState.SETUP) {
            return false;
        }
        if (eventSchedule.isLastRound() && eventSchedule.getRound().isPresent() && eventSchedule.getRound().get().getState() == Round.RoundState.FINISHED) {
            setState(EventState.FINISHED);
            return true;
        }

        return false;
    }

    public boolean hasRunningHeat() {
        if (getState() == Event.EventState.RUNNING) {
            var maybeRound = getEventSchedule().getRound();
            if (maybeRound.isPresent()) {
                return maybeRound.get().getHeats().stream().anyMatch(Heat::isActive);
            }
        }
        return false;
    }

    public Optional<Heat> getRunningHeat() {
        if (getState() != Event.EventState.RUNNING) {
            return Optional.empty();
        }

        var maybeRound = getEventSchedule().getRound();

        return maybeRound.flatMap(round -> round.getHeats().stream().filter(Heat::isActive).findFirst());
    }

    public void addSpectator(UUID uuid) {
        spectators.put(uuid, new Spectator(Database.getPlayer(uuid)));
        var maybeHeat = getRunningHeat();

        maybeHeat.ifPresent(Heat::updateScoreboard);
    }

    public boolean isSpectating(UUID uuid) {
        return spectators.containsKey(uuid);
    }

    public void removeSpectator(UUID uuid) {
        if (spectators.containsKey(uuid)) {
            spectators.remove(uuid);
            if (Database.getPlayer(uuid).getPlayer() != null) {
                var maybeHeat = getRunningHeat();
                if (maybeHeat.isPresent()) {
                    Database.getPlayer(uuid).clearScoreboard();
                }
            }

        }
    }

    public void addSubscriber(TPlayer tPlayer) {
        subscribers.put(tPlayer.getUniqueId(), EventDatabase.subscriberNew(tPlayer, this, Subscriber.Type.SUBSCRIBER));
    }

    public boolean isSubscribing(UUID uuid) {
        return subscribers.containsKey(uuid);
    }

    public void removeSubscriber(UUID uuid) {
        if (subscribers.containsKey(uuid)) {
            DB.executeUpdateAsync("DELETE FROM `ts_events_signs` WHERE `uuid` = '" + uuid.toString() + "' AND `eventId` = " + getId() + " AND `type` = '" + Subscriber.Type.SUBSCRIBER.name() + "';");
            subscribers.remove(uuid);
        }
    }

    public void addReserve(TPlayer tPlayer) {
        reserves.put(tPlayer.getUniqueId(), EventDatabase.subscriberNew(tPlayer, this, Subscriber.Type.RESERVE));
    }

    public boolean isReserving(UUID uuid) {
        return reserves.containsKey(uuid);
    }

    public void removeReserve(UUID uuid) {
        if (reserves.containsKey(uuid)) {
            DB.executeUpdateAsync("DELETE FROM `ts_events_signs` WHERE `uuid` = '" + uuid.toString() + "' AND `eventId` = " + getId() + " AND `type` = '" + Subscriber.Type.RESERVE.name() + "';");
            reserves.remove(uuid);
        }
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

    public boolean isActive() {
        return state != EventState.FINISHED;
    }

    public enum EventState {
        SETUP, RUNNING, FINISHED
    }
}
