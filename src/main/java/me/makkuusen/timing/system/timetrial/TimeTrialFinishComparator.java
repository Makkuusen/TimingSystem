package me.makkuusen.timing.system.timetrial;

import java.util.Comparator;

public class TimeTrialFinishComparator implements Comparator<TimeTrialFinish> {
    @Override
    public int compare(TimeTrialFinish e1, TimeTrialFinish e2) {
        return e1.compareTo(e2);
    }
}
