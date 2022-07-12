package me.makkuusen.timing.system.participant;

import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.TPlayer;
import org.jetbrains.annotations.NotNull;

public class QualyDriver extends Driver {

    public QualyDriver(TPlayer tPlayer, Heat heat){
        super(tPlayer, heat);
    }

    @Override
    public int compareTo(@NotNull Driver o) {
        if (!(o instanceof QualyDriver)){
            return 0;
        }
        var bestLap = getBestLap();
        var oBestLap = o.getBestLap();
        if (bestLap.isEmpty() && oBestLap.isEmpty()) {
            return 0;
        } else if (bestLap.isPresent() && oBestLap.isEmpty()) {
            return -1;
        } else if (bestLap.isEmpty() && oBestLap.isPresent()) {
            return 1;
        }

        var lapTime = bestLap.get().getLapTime();
        var oLapTime = oBestLap.get().getLapTime();
        if (lapTime < oLapTime) {
            return -1;
        } else if (lapTime > oLapTime) {
            return 1;
        }

        return 0;
    }
}
