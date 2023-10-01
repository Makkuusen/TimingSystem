package me.makkuusen.timing.system.theme.messages;

public enum ScoreBoard implements Message{

    BEST_TIME,
    AVERAGE_TIME,
    FINISHES;

    ScoreBoard () {}

    @Override
    public String getKey() {
        return "scoreboard." + this.name().toLowerCase();
    }
}
