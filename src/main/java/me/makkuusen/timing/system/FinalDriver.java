package me.makkuusen.timing.system;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class FinalDriver extends Driver{

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
    public int compareTo(@NotNull Driver driver) {
        return 0;
    }
}
