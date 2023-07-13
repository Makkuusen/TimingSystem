package me.makkuusen.timing.system.text;

public enum Gui implements MessageLevel {

    SETTINGS_TITLE,
    MEDALS_TITLE,
    TRACKS_TITLE;

    Gui() {}

    @Override
    public String getKey() {
        return "gui." + this.name().toLowerCase();
    }
}
