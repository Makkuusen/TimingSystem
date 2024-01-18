package me.makkuusen.timing.system.theme.messages;

public enum Gui implements Message {

    SETTINGS_TITLE,
    MEDALS_TITLE,
    FILTER_TITLE,
    RANKED_TITLE,
    TRACKS_TITLE,
    BEST_TIME,
    POSITION,
    TOTAL_FINISHES,
    TOTAL_ATTEMPTS,
    TIME_SPENT,
    CREATED_BY,
    CONTRIBUTORS,
    CREATED_AT,
    WEIGHT,
    TAGS,
    SORTED_BY_DEFAULT,
    SORTED_BY_POSITION,
    SORTED_BY_POPULARITY,
    SORTED_BY_TIME_SPENT,
    SORTED_BY_RECENTLY_ADDED,
    FILTER_BY_NONE,
    FILTER_BY,
    PREVIOUS_PAGE,
    NEXT_PAGE,
    CHANGE_TRACK_TYPE,
    CHANGE_BOAT_TYPE,
    CHANGE_TEAM_COLOR,
    BOAT_TRACKS,
    ELYTRA_TRACKS,
    PARKOUR_TRACKS,
    TOGGLE_SOUND,
    TOGGLE_VERBOSE,
    TOGGLE_TIME_TRIAL,
    TOGGLE_OVERRIDE,
    TOGGLE_MEDALS,
    TOGGLE_FINAL_LAPS,
    TOGGLE_COMPACT_SCOREBOARD,
    COLOR,
    ON,
    OFF,
    RETURN,
    RESET;

    Gui() {}

    @Override
    public String getKey() {
        return "gui." + this.name().toLowerCase();
    }
}
