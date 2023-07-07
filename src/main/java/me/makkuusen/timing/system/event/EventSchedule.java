package me.makkuusen.timing.system.event;

import lombok.Getter;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.text.TextUtilities;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
public class EventSchedule {
    private final List<Round> rounds = new ArrayList<>();
    private Integer currentRound = null;

    public EventSchedule() {

    }

    public boolean start(Event event) {
        if (rounds.size() > 0) {
            event.setState(Event.EventState.RUNNING);
            currentRound = 1;
            getRound().get().setState(Round.RoundState.RUNNING);
            return true;
        }
        return false;
    }

    public void addRound(Round round) {
        rounds.add(round);
    }

    public boolean removeRound(Round round) {
        if (round.getState() != Round.RoundState.FINISHED && round.getState() != Round.RoundState.RUNNING) {
            var heats = round.getHeats().stream().toList();
            for (Heat heat : heats) {
                if (!round.removeHeat(heat)) {
                    return false;
                }
            }
            rounds.remove(round);
            for (Round _round : rounds) {
                if (round.getRoundIndex() < _round.getRoundIndex()) {
                    _round.setRoundIndex(_round.getRoundIndex() - 1);
                }
            }
            return true;
        }
        return false;
    }

    public Integer getCurrentRound() {
        if (currentRound != null) {
            return currentRound;
        }
        return null;
    }

    public void nextRound() {
        currentRound++;
    }

    public boolean isLastRound() {
        return currentRound >= rounds.size();
    }

    public Optional<Round> getRound(int index) {
        if (index > 0 && index <= rounds.size()) {
            return Optional.of(rounds.get(index - 1));
        }
        return Optional.empty();
    }

    public Optional<Round> getRound() {
        if (currentRound == null) {
            return Optional.empty();
        }
        if (currentRound - 1 > rounds.size() - 1) {
            return Optional.empty();
        }
        return Optional.of(rounds.get(currentRound - 1));
    }

    public Optional<Round> getNextRound() {
        return Optional.of(rounds.get(currentRound));
    }


    public List<Component> getHeatList(Event event) {
        List<Component> message = new ArrayList<>();
        message.add(TextUtilities.getTitleLine("Heats for", event.getDisplayName()));
        message.addAll(listHeats());
        return message;
    }

    public List<Component> getRoundList(Event event) {
        List<Component> message = new ArrayList<>();
        message.add(TextUtilities.getTitleLine("Rounds for", event.getDisplayName()));
        message.addAll(listRounds());
        return message;
    }

    public Optional<Heat> getHeat(String name) {
        for (Round round : rounds) {
            if (round.getHeat(name).isPresent()) {
                return round.getHeat(name);
            }
        }
        return Optional.empty();
    }

    public Optional<Round> getRound(String name) {
        return rounds.stream().filter(round -> name.equalsIgnoreCase(round.getName())).findFirst();
    }

    public List<Component> listRounds() {
        List<Component> message = new ArrayList<>();
        rounds.forEach(round -> message.add(Component.text(" - " + round.getName()).color(TextUtilities.textHighlightColor)));
        return message;
    }

    public List<Component> listHeats() {
        List<Component> message = new ArrayList<>();
        for (Round round : rounds) {
            round.getHeats().forEach(heat -> message.add(Component.text(" - " + heat.getName()).color(TextUtilities.textHighlightColor)));
        }
        return message;
    }

    public void setCurrentRound() {
        int lastRound = 1;
        for (Round round : rounds) {
            if (round.getState() == Round.RoundState.FINISHED) {
                lastRound++;
            }
        }
        currentRound = lastRound;
    }
}
