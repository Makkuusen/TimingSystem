package me.makkuusen.timing.system;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

@Getter
@Setter
public class FinalDriver extends Driver {

    private int pits;

    public FinalDriver(TPlayer tPlayer, Heat heat){
        super(tPlayer, heat);
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

        Lap lap = getCurrentLap();
        Lap oLap = o.getCurrentLap();

        if (lap.getLatestCheckpoint() > oLap.getLatestCheckpoint()) { return -1; }
        else if (lap.getLatestCheckpoint() < oLap.getLatestCheckpoint()) { return 1;}

        if (getLaps().size() == 0 && lap.getLatestCheckpoint() == 0){
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
