package me.makkuusen.timing.system.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import org.bukkit.OfflinePlayer;

public class TimingSystemPlaceholder extends PlaceholderExpansion {

    private final TimingSystem plugin; // The instance is created in the constructor and won't be modified, so it can be final

    public TimingSystemPlaceholder(TimingSystem plugin) {
        this.plugin = plugin;
    }
    @Override
    public String getAuthor() {
        return "Makkuusen";
    }

    @Override
    public String getIdentifier() {
        return "timingsystem";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {

        if (params.equalsIgnoreCase("tracks_total_open")) {
            return String.valueOf(TimingSystemAPI.getOpenTracks().size());
        }

        if (params.equalsIgnoreCase("tracks_total")) {
            return String.valueOf(TimingSystemAPI.getTracks().size());
        }

        return null; // Placeholder is unknown by the Expansion
    }
}
