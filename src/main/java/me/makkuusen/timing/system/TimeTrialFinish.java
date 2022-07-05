package me.makkuusen.timing.system;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.UUID;

public class TimeTrialFinish implements Comparator<TimeTrialFinish>
{

    private final int id;
    private final int trackId;
    private final TSPlayer player;
    private final long date;
    private final long time;
    private final boolean isRemoved;

    public TimeTrialFinish(ResultSet data) throws SQLException
    {
        this.id = data.getInt("id");
        this.trackId = data.getInt("trackId");
        this.player = data.getString("uuid") == null ? null : ApiDatabase.getPlayer(UUID.fromString(data.getString("uuid")));
        this.date = data.getInt("date");
        this.time = data.getInt("time");
        this.isRemoved = data.getBoolean("isRemoved");
    }

    public int getId()
    {
        return id;
    }

    public TSPlayer getPlayer()
    {
        return player;
    }

    public long getTime()
    {
        return time;
    }

    public long getDate()
    {
        return date;
    }

    @Override
    public int compare(TimeTrialFinish f1, TimeTrialFinish f2)
    {
        int result = Long.compare(f1.getTime(), f2.getTime());
        if (result == 0)
        {
            return Long.compare(f1.getDate(), f2.getDate());
        }
        return result;
    }

    public int compareTo(TimeTrialFinish rf)
    {
        int result = Long.compare(getTime(), rf.getTime());
        if (result == 0)
        {
            return Long.compare(getDate(), rf.getDate());
        }
        return result;
    }

    @Override
    public boolean equals(Object rf)
    {
        if (rf instanceof TimeTrialFinish timeTrialFinish)
        {
            return timeTrialFinish.getDate() == getDate() && timeTrialFinish.getPlayer() == getPlayer();
        }
        return false;
    }
}
