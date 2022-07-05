package me.makkuusen.timing.system;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class RaceController {

    // Track ID key
    public static HashMap<Integer, Race> races = new HashMap<>();


    public static Optional<Race> getDriverFromRace(TSPlayer TSPlayer)
    {
        for (Race race : races.values()) {

            if(!race.isRunning())
            {
                continue;
            }
            List<RaceDriver> drivers = race.getDrivers();

            for (RaceDriver rd : drivers)
            {
                if (rd.getTSPlayer().equals(TSPlayer))
                {
                    return Optional.of(race);
                }
            }
        }
        return Optional.empty();
    }
}
