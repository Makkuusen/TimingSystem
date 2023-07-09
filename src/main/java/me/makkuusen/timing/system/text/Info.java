package me.makkuusen.timing.system.text;

public enum Info implements MessageLevel{

    PAGE_CURRENT_OF_MAX,
    TRACK_TITLE,
    TRACK_TIMES_TITLE,
    PLAYER_BEST_TIMES_TITLE,
    PLAYER_RECENT_TIMES_TITLE,
    PLAYER_STATS_TITLE,
    REGIONS_TITLE,
    LOCATIONS_TITLE,
    ROUNDS_TITLE,
    HEATS_TITLE,
    ROUND_RESULT_TITLE,
    HEAT_RESULT_TITLE,
    SIGNS_TITLE,
    RESERVES_TITLE,
    ACTIVE_EVENTS_TITLE,
    TRACK_TYPE,
    TRACK_OPEN,
    TRACK_CLOSED,
    TRACK_DATE_CREATED,
    TRACK_OPTIONS,
    TRACK_MODE,
    TRACK_CHECKPOINTS,
    TRACK_GRIDS,
    TRACK_QUALIFICATION_GRIDS,
    TRACK_RESET_REGIONS,
    TRACK_SPAWN_LOCATION,
    TRACK_WEIGHT,
    TRACK_TAGS,
    PLAYER_STATS_BEST_LAP,
    PLAYER_STATS_POSITION,
    PLAYER_STATS_FINISHES,
    PLAYER_STATS_ATTEMPTS,
    PLAYER_STATS_TIME_SPENT,
    TIME_TRIAL_FIRST_FINISH,
    TIME_TRIAL_NEW_RECORD,
    TIME_TRIAL_FINISH,
    TIME_TRIAL_CHECKPOINT,
    TIME_TRIAL_LAG_START,
    TIME_TRIAL_LAG_END,
    UPDATING_LEADERBOARDS;


    Info() {

    }

    @Override
    public String getKey() {
        return "info." + this.name().toLowerCase();
    }
}
