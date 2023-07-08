package me.makkuusen.timing.system.text;

import net.kyori.adventure.text.Component;

public enum Success {

    SAVED("success.saved"),
    CREATED("success.created"),
    ADDED_TAG("success.added_tag"),
    REMOVED("success.removed"),
    REMOVED_TAG("success.removed_tag"),
    REMOVED_LOCATIONS("success.removed_locations"),
    REMOVED_REGIONS("success.removed_regions"),
    REMOVED_BEST_TIME("success.removed_best_time"),
    RESTORED_LOCATIONS("success.restored_locations"),
    RESTORED_REGIONS("success.restored_regions"),
    TIME_TRIAL_CANCELLED("success.time_trial_cancelled"),
    TRACK_NOW_OPEN("success.track_now_open"),
    TRACK_NOW_CLOSED("success.track_now_closed"),
    TRACK_MOVED("success.track_moved"),
    TRACK_OPTIONS_CLEARED("success.track_options_cleared"),
    TRACK_OPTIONS_NEW("success.track_options_new"),

    TELEPORT_TO_TRACK("success.teleport_to_track"),






    public final String value;

    Success(String value) {
        this.value = value;
    }

    public Component message() {
        return Component.text(value).color(TextUtilities.textSuccess);
    }
}