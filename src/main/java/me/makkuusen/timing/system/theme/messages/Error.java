package me.makkuusen.timing.system.theme.messages;


public enum Error implements Message {
    GENERIC,
    NOT_NOW,
    CAN_NOT,
    FAILED_TO_ADD_DRIVER,
    FAILED_TELEPORT,
    FAILED_TO_REMOVE,
    FAILED_TO_REMOVE_TAG,
    FAILED_TO_REMOVE_OPTION,
    FAILED_TO_REMOVE_ROUND,
    FAILED_TO_REMOVE_HEAT,
    FAILED_TO_REMOVE_EVENT,
    FAILED_TO_REMOVE_DRIVERS,
    FAILED_TO_REMOVE_DRIVER,
    FAILED_TO_DISQUALIFY_DRIVER,
    FAILED_TO_ADD_TAG,
    FAILED_TO_ADD_OPTION,
    FAILED_TO_CREATE_TAG,
    FAILED_TO_CREATE_ROUND,
    FAILED_TO_CREATE_HEAT,
    FAILED_TO_CREATE_EVENT,
    FAILED_TO_FINISH_ROUND,
    FAILED_TO_FINISH_HEAT,
    FAILED_TO_FINISH_EVENT,
    FAILED_TO_START_EVENT,
    FAILED_TO_START_HEAT,
    FAILED_TO_LOAD_HEAT,
    FAILED_TO_RESET_HEAT,
    FAILED_TO_ABORT_HEAT,
    ONLY_PLAYERS,
    WORLD_NOT_LOADED,
    SYNTAX,
    NUMBER_FORMAT,
    LENGTH_EXCEEDED,
    NAME_FORMAT,
    COLOR_FORMAT,
    TIME_FORMAT,
    SELECTION,
    NO_ZERO_INDEX,
    NOTHING_TO_REMOVE,
    NOTHING_TO_RESTORE,
    PLAYER_NOT_FOUND,
    TRACKS_NOT_FOUND,
    REGIONS_NOT_FOUND,
    PAGE_NOT_FOUND,
    ITEM_NOT_FOUND,
    TAG_NOT_FOUND,
    ROUND_NOT_FOUND,
    HEAT_NOT_FOUND,
    EVENT_NOT_FOUND,
    ALREADY_SIGNED,
    ALREADY_SIGNED_RACE,
    PLAYER_ALREADY_IN_ROUND,
    TRACK_NOT_FOUND_FOR_EVENT,
    TRACK_NOT_FOUND_FOR_EDIT,
    RACE_NOT_FOUND,
    TRACK_IS_CLOSED,
    TRACK_EXISTS,
    ROUND_NOT_FINISHED,
    QUALIFICATION_NOT_SUPPORTED,
    EVENT_ALREADY_STARTED,
    HEAT_ALREADY_STARTED,
    RACE_EXISTS,
    RACE_FULL,
    HEAT_FULL,
    ADD_DRIVER_FUTURE_ROUND,
    SORT_DRIVERS_FUTURE_ROUND,
    INVALID_TRACK_TYPE,
    INVALID_TRACK_NAME,
    INVALID_TRACK_MODE,
    INVALID_NAME,
    NO_EVENT_SELECTED,
    LEFT_BOAT,
    RACE_IN_PROGRESS,
    NO_HOOKING_OTHERS,
    NO_FISHING_OTHERS,
    NO_ELYTRA,
    NO_CREATIVE,
    STOPPED_FLYING,
    NO_POTION_EFFECTS,
    NO_RIPTIDE,
    NO_SOUL_SPEED,
    WRONG_BOAT_UTILS_MODE,
    LAG_DETECTED,
    MISSED_CHECKPOINTS,
    PERMISSION_DENIED;

    Error() {}

    @Override
    public String getKey() {
        return "error." + this.name().toLowerCase();
    }
}