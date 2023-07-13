package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.TrackTagManager;
import me.makkuusen.timing.system.text.Gui;
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
        GuiButton save = new GuiButton(ButtonUtilities.goBack);
        save.setAction(() -> {
            var constructors = oldTrackPage.getClass().getDeclaredConstructors();
            for (var construct : constructors) {
                if (construct.getParameterCount() == 6) {
                    try {
                        var instance = (TrackPageGui) construct.newInstance(oldTrackPage.tPlayer, oldTrackPage.title, oldTrackPage.page, oldTrackPage.trackSort, filter, oldTrackPage.trackType);
                        instance.show(oldTrackPage.tPlayer.getPlayer());
                    } catch (Exception e) {
                        //sadge
                    }
                }
            }
        });
        setItem(save, 26);
    }

    public void setFilterOptions() {

        int count = 0;
        for (TrackTag tag : TrackTagManager.getTrackTags()) {
            if (count > 8) {
                break;
            }

            setItem(filter.getTags().contains(tag) ? ButtonUtilities.getStatusOnButton() : ButtonUtilities.getStatusOffButton(), count);
            var button = new GuiButton(new ItemBuilder(Material.ANVIL).setName(tag.getValue()).build());
            int finalCount = count;
            button.setAction(() -> {
                if (filter.getTags().contains(tag)) {
                    filter.removeTag(tag);
                } else {
                    filter.addTag(tag);
                }
                setItem(filter.getTags().contains(tag) ? ButtonUtilities.getStatusOnButton() : ButtonUtilities.getStatusOffButton(), finalCount);
            });
            setItem(button, count + 9);
            count++;
        }
    }
}
