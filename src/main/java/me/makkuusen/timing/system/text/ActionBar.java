package me.makkuusen.timing.system.text;

public enum ActionBar implements MessageLevel {
    RACE,
    RACE_SPECTATOR;

    @Override
    public String getKey() {
        return "actionbar." + this.name().toLowerCase();
    }
}
