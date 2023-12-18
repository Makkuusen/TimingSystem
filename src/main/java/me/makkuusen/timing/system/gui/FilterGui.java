package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.TrackTagManager;
import me.makkuusen.timing.system.sounds.PlaySound;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Gui;
import me.makkuusen.timing.system.track.tags.TrackTag;
import org.bukkit.Material;

public class FilterGui extends BaseGui{

    TrackPageGui oldTrackPage;
    TrackFilter filter;
    public FilterGui(TrackPageGui trackPage) {
        super(Text.get(trackPage.tPlayer.getPlayer(), Gui.FILTER_TITLE), 4);
        oldTrackPage = trackPage;
        this.filter = trackPage.filter;
        setButtons();
    }

    public void setButtons() {
        setFilterOptions();
        GuiButton save = new GuiButton(new ItemBuilder(Material.ARROW).setName(Text.get(oldTrackPage.tPlayer, Gui.RETURN)).build());
        save.setAction(() -> {
            PlaySound.buttonClick(oldTrackPage.tPlayer);
            oldTrackPage.tPlayer.setFilter(filter);
            TrackPageGui.openNewTrackPage(oldTrackPage, oldTrackPage.tPlayer, oldTrackPage.title);
        });
        setItem(save, 35);
    }

    public void setFilterOptions() {

        int count = 0;
        boolean getZeroWeight = oldTrackPage.tPlayer.getPlayer().hasPermission("timingsystem.packs.trackadmin");
        for (TrackTag tag : TrackTagManager.getSortedTrackTags(getZeroWeight)) {
            if (count > 25) {
                break;
            }

            if (count == 9) {
                count = count + 9;
            }

            setItem(filter.getTags().contains(tag) ? GuiCommon.getStatusOnButton(oldTrackPage.tPlayer) : GuiCommon.getStatusOffButton(oldTrackPage.tPlayer), count);
            var button = new GuiButton(tag.getItem(oldTrackPage.tPlayer.getPlayer()));
            int finalCount = count;
            button.setAction(() -> {
                PlaySound.buttonClick(oldTrackPage.tPlayer);
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
