package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.TrackTagManager;
import me.makkuusen.timing.system.theme.messages.Gui;
import me.makkuusen.timing.system.track.TrackTag;
import org.bukkit.Material;

public class FilterGui extends BaseGui{

    TrackPageGui oldTrackPage;
    TrackFilter filter;
    public FilterGui(TrackPageGui trackPage) {
        super(TimingSystem.getPlugin().getText(trackPage.tPlayer.getPlayer(), Gui.FILTER_TITLE), 3);
        oldTrackPage = trackPage;
        this.filter = trackPage.filter;
        setButtons();
    }

    public void setButtons() {
        setFilterOptions();
        GuiButton save = new GuiButton(new ItemBuilder(Material.ARROW).setName(TimingSystem.getPlugin().getText(oldTrackPage.tPlayer, Gui.RETURN)).build());
        save.setAction(() -> {
            GuiCommon.playConfirm(oldTrackPage.tPlayer);
            oldTrackPage.tPlayer.setFilter(filter);
            TrackPageGui.openNewTrackPage(oldTrackPage, oldTrackPage.tPlayer, oldTrackPage.title);
        });
        setItem(save, 26);
    }

    public void setFilterOptions() {

        int count = 0;
        for (TrackTag tag : TrackTagManager.getTrackTags().values()) {
            if (count > 8) {
                break;
            }

            setItem(filter.getTags().contains(tag) ? GuiCommon.getStatusOnButton(oldTrackPage.tPlayer) : GuiCommon.getStatusOffButton(oldTrackPage.tPlayer), count);
            var button = new GuiButton(tag.getItem());
            int finalCount = count;
            button.setAction(() -> {
                GuiCommon.playConfirm(oldTrackPage.tPlayer);
                if (filter.getTags().contains(tag)) {
                    filter.removeTag(tag);
                } else {
                    filter.addTag(tag);
                }
                setItem(filter.getTags().contains(tag) ? GuiCommon.getStatusOnButton(oldTrackPage.tPlayer) : GuiCommon.getStatusOffButton(oldTrackPage.tPlayer), finalCount);
            });
            setItem(button, count + 9);
            count++;
        }
    }
}
