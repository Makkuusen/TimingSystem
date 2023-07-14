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
        GuiButton save = new GuiButton(GuiCommon.goBack);
        save.setAction(() -> {
            TrackPageGui.openNewTrackPage(oldTrackPage, oldTrackPage.tPlayer, oldTrackPage.title, oldTrackPage.page, oldTrackPage.trackSort, filter, oldTrackPage.trackType);
        });
        setItem(save, 26);
    }

    public void setFilterOptions() {

        int count = 0;
        for (TrackTag tag : TrackTagManager.getTrackTags()) {
            if (count > 8) {
                break;
            }

            setItem(filter.getTags().contains(tag) ? GuiCommon.getStatusOnButton() : GuiCommon.getStatusOffButton(), count);
            var button = new GuiButton(new ItemBuilder(Material.ANVIL).setName(tag.getValue()).build());
            int finalCount = count;
            button.setAction(() -> {
                if (filter.getTags().contains(tag)) {
                    filter.removeTag(tag);
                } else {
                    filter.addTag(tag);
                }
                setItem(filter.getTags().contains(tag) ? GuiCommon.getStatusOnButton() : GuiCommon.getStatusOffButton(), finalCount);
            });
            setItem(button, count + 9);
            count++;
        }
    }
}
