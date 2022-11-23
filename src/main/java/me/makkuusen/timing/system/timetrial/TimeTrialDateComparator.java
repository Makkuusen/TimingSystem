package me.makkuusen.timing.system.timetrial;

import java.util.Comparator;

public class TimeTrialDateComparator implements Comparator<TimeTrialFinish> {
    @Override
    public int compare(TimeTrialFinish e1, TimeTrialFinish e2) {
        return e2.compareDate(e1);
    }
}
