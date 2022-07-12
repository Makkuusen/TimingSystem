package me.makkuusen.timing.system.participant;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.heat.FinalHeat;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.Lap;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

@Getter
@Setter
public class FinalDriver extends Driver {

    private int pits;

    public FinalDriver(TPlayer tPlayer, Heat heat, int startPosition){
        super(tPlayer, heat);
        setStartPosition(startPosition);
    }

    public void passPit() {
        if (!getCurrentLap().isPitted()) {
            EventAnnouncements.broadcastPit(getHeat(), this, ++pits);
            getCurrentLap().setPitted(true);
        }
    }

    @Override
    public void reset(){
        super.reset();
        pits = 0;
    }

    public Instant getTimeStamp(int lap, int checkpoint){
        var finalHeat = (FinalHeat) getHeat();
        if (lap > finalHeat.getTotalLaps()){
            return getLaps().get(finalHeat.getTotalLaps() - 1).getLapEnd();
        }

        return getLaps().get(lap - 1).getCheckpointTime(checkpoint);
    }

    @Override
    public int compareTo(@NotNull Driver o) {
        if (!(o instanceof FinalDriver)){
            return 0;
        }

        if (isFinished() && !o.isFinished()) { return -1; }
        else if (!isFinished() && o.isFinished()) { return 1; }
        else if (isFinished() && o.isFinished()) { return getEndTime().compareTo(o.getEndTime()); }

        if (getLaps().size() > o.getLaps().size()) { return -1; }
        else if (getLaps().size() < o.getLaps().size()) { return 1; }

        if (getLaps().size() == 0){
            return 0;
        }

        Lap lap = getCurrentLap();
        Lap oLap = o.getCurrentLap();

        if (lap.getLatestCheckpoint() > oLap.getLatestCheckpoint()) { return -1; }
        else if (lap.getLatestCheckpoint() < oLap.getLatestCheckpoint()) { return 1;}

        if (lap.getLatestCheckpoint() == 0){
            return 0;
        }
        else if (lap.getLatestCheckpoint() == 0){
            return lap.getLapStart().compareTo(oLap.getLapStart());
        }

        Instant last = lap.getCheckpointTime(lap.getLatestCheckpoint());
        Instant oLast = oLap.getCheckpointTime(lap.getLatestCheckpoint());
        return last.compareTo(oLast);
    }
}
