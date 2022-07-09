package me.makkuusen.timing.system;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class Driver extends Participant{

    private boolean finished = false;
    private int position = 0;
    private Instant startTime;

    public Driver(TPlayer tPlayer){
        super(tPlayer);
    }

    public void finishDriver(){

    }

    public void passLap(){

    }

    public long getFinishTime(){

        return 0;
    }
}
