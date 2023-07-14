package me.makkuusen.timing.system.theme.messages;

public enum ActionBar implements Message {
    RACE,
    RACE_SPECTATOR;

    @Override
    public String getKey() {
        return "actionbar." + this.name().toLowerCase();
    }
}
