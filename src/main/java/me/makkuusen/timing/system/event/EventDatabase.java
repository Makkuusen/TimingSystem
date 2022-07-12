package me.makkuusen.timing.system.event;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;
import lombok.Getter;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.participant.Driver;

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
    private static final HashMap<UUID, Event> playerSelectedEvent = new HashMap<>();


    static public void setPlayerSelectedEvent(UUID uuid, Event event) {
        playerSelectedEvent.put(uuid, event);
    }
    static public Optional<Event> getPlayerSelectedEvent(UUID uuid) {
        if (playerSelectedEvent.containsKey(uuid)){
            return Optional.of(playerSelectedEvent.get(uuid));
        }
        return Optional.empty();
    }

    static public Optional<Event> getEvent(String name) {
        return events.stream().filter(event -> event.getId().equalsIgnoreCase(name)).findFirst();
    }

    static public boolean eventNew(UUID uuid, String name) {
        Event event = new Event(name);
        if (events.add(event)) {
            setPlayerSelectedEvent(uuid, event);
            return true;
        }
        return false;
    }

    static public Set<Event> getEvents() {
        return events;
    }

    static public List<String> getEventsAsStrings() {
        List<String> eventStrings = new ArrayList<>();
        events.stream().forEach(event -> eventStrings.add(event.toString()));
        return eventStrings;
    }

    static public List<String> getHeatsAsStrings(UUID uuid){
        var maybeEvent = getPlayerSelectedEvent(uuid);
        if (maybeEvent.isEmpty()){
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
                if (maybeHeat.isPresent()){
                    return maybeHeat.get();
                }
            }
            // User didn't type a heat, show error!
            throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
        };
    }

    public static Optional<Driver> getDriverFromRunningHeat(UUID uuid){
        for (Event event : EventDatabase.getEvents()) {
            var maybeHeat = event.getEventSchedule().getHeats().stream()
                    .filter(heat -> heat.getHeatState().equals(HeatState.RACING))
                    .filter(heat -> heat.getDrivers().containsKey(uuid))
                    .findFirst();

            if (maybeHeat.isPresent()) {
                var driver = maybeHeat.get().getDrivers().get(uuid);
                return Optional.of(maybeHeat.get().getDrivers().get(uuid));
            }
        }
        return Optional.empty();
    }
}
