package me.makkuusen.timing.system;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FinalDriver extends Driver{

    private int laps;
    private int pits;

    public FinalDriver(TPlayer tPlayer){
        super(tPlayer);
    }

    public void passLap(){

    }

    public void setFinished(){
        setFinished(true);
    }


}
