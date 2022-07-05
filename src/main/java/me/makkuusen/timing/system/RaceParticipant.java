package me.makkuusen.timing.system;

public abstract class RaceParticipant {

    private TSPlayer tsPlayer;
    protected Race race;

    public RaceParticipant(TSPlayer tsPlayer, Race race){
        this.tsPlayer = tsPlayer;
        this.race = race;
    }

    public TSPlayer getTSPlayer() {
        return tsPlayer;
    }

    public void setTSPlayer(TSPlayer tsPlayer) {
        this.tsPlayer = tsPlayer;
    }
}
