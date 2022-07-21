package me.makkuusen.timing.system.race;

import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.heat.HeatState;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class RaceController {

    // Track ID key
    public static HashMap<Integer, Race> races = new HashMap<>();

    public static Optional<Race> getDriverFromActiveRace(TPlayer tPlayer) {
        for (Race race : races.values()) {

            if (!race.getRaceState().equals(HeatState.RACING)) {
                continue;
            }
            List<RaceDriver> drivers = race.getDrivers();

            for (RaceDriver rd : drivers) {
                if (rd.getTSPlayer().equals(tPlayer)) {
                    return Optional.of(race);
                }
            }
        }
        return Optional.empty();
    }
}
