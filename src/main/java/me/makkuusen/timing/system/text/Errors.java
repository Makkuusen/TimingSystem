package me.makkuusen.timing.system.text;


import net.kyori.adventure.text.Component;

public enum Errors {


    GENERIC("error.generic"),
    NOT_NOW("error.not_now"),
    FAILED_TELEPORT("error.failed_to_teleport"),
    FAILED_TO_REMOVE("error.failed_to_remove"),
    FAILED_TO_REMOVE_TAG("error.failed_to_remove_tag"),
    FAILED_TO_ADD_TAG("error.failed_to_add_tag"),
    ONLY_PLAYERS("error.only_players"),
    WORLD_NOT_LOADED("error.world_not_loaded"),
    SYNTAX("error.syntax"),
    NUMBER_FORMAT("error.number_format"),
    LENGTH_EXCEEDED("error.length_exceeded"),
    NAME_FORMAT("error.name_format"),
    SELECTION("error.selection"),
    NO_ZERO_INDEX("error.no_zero_index"),
    NOTHING_TO_REMOVE("error.nothing_to_remove"),
    NOTHING_TO_RESTORE("error.nothing_to_restore"),
    PLAYER_NOT_FOUND("error.player_not_found"),
    TRACKS_NOT_FOUND("error.tracks_not_found"),
    REGIONS_NOT_FOUND("error.regions_not_found"),
    PAGE_NOT_FOUND("error.page_not_found"),
    ITEM_NOT_FOUND("error.item_not_found"),
    TAG_NOT_FOUND("error.tag_not_found"),
    TRACK_IS_CLOSED("error.track_is_closed"),
    TRACK_EXISTS("error.track_exists"),
    RACE_EXISTS("error.race_exists"),
    INVALID_TRACK_TYPE("error.invalid_track_type"),
    INVALID_TRACK_NAME("error.invalid_track_name"),
    INVALID_TRACK_MODE("error.invalid_track_mode"),
    NO_EVENT_SELECTED("error.no_event_selected"),
    PERMISSION_DENIED("error.permission_denied"),


    public final String value;

    Errors(String value) {
        this.value = value;
    }

    public Component message() {
        return Component.text(value).color(TextUtilities.textError);
    }
}