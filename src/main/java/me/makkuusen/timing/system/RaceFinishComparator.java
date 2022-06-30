package me.makkuusen.timing.system;

import java.util.Comparator;

public class RaceFinishComparator implements Comparator<RaceFinish>
{
    @Override
    public int compare(RaceFinish e1, RaceFinish e2)
    {
        return e1.compareTo(e2);
    }
}
