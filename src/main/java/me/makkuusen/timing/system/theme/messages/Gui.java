package me.makkuusen.timing.system.theme.messages;

public enum Gui implements Message {

    SETTINGS_TITLE,
    MEDALS_TITLE,
    FILTER_TITLE,
    RANKED_TITLE,
    TRACKS_TITLE;

    Gui() {}

    @Override
    public String getKey() {
        return "gui." + this.name().toLowerCase();
    }
}
