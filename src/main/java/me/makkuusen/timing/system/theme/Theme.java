package me.makkuusen.timing.system.theme;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.database.Database;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.theme.messages.Hover;
import me.makkuusen.timing.system.theme.messages.Info;
import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class Theme {
    private TextColor primary = TextColor.color(NamedTextColor.GRAY); //7bf200 //gray
    private TextColor secondary = TextColor.color(NamedTextColor.WHITE);
    private TextColor award = TextColor.color(NamedTextColor.GOLD); //#ce00ce //gold
    private TextColor awardSecondary = TextColor.color(NamedTextColor.YELLOW); //#ff80ff //yellow
    private TextColor error = TextColor.color(NamedTextColor.RED); //#ff7a75 //red
    private TextColor warning = TextColor.color(NamedTextColor.YELLOW);
    private TextColor success = TextColor.color(NamedTextColor.GREEN); //7bf200 //green
    private TextColor broadcast = TextColor.color(NamedTextColor.AQUA); //#ff80ff //Aqua
    private TextColor title = TextColor.color(NamedTextColor.DARK_GRAY);
    private TextColor button = TextColor.color(NamedTextColor.YELLOW);
    private TextColor buttonRemove = TextColor.color(NamedTextColor.RED);
    private TextColor buttonAdd = TextColor.color(NamedTextColor.GREEN);

    // Potential buttons to use
    // [»] [≈] [+] [-] [√] [?] [≡] [±]

    public Theme() {
    }

    public static Theme getTheme(CommandSender sender) {
        return sender instanceof Player ? Database.getPlayer(sender).getTheme() : TimingSystem.defaultTheme;
    }

    public Component getViewButton(CommandSender sender) {
        return Component.text("[»]").color(button).hoverEvent(getClickToViewHoverEvent(sender));
    }

    public Component getEditButton(CommandSender sender, String value, Theme theme) {
        return getBrackets(value).hoverEvent(getClickToEditHoverEvent(sender));
    }

    public Component getAddButton() {
        return Component.text("[+]").color(buttonAdd).hoverEvent(HoverEvent.showText(Component.text("Click to add")));
    }

    public Component getRemoveButton() {
        return Component.text("[-]").color(buttonRemove).hoverEvent(HoverEvent.showText(Component.text("Click to delete")));
    }

    public Component getRefreshButton() {
        return Component.text("↻").color(button).hoverEvent(HoverEvent.showText(Component.text("Refresh")));
    }

    public Component getMoveButton() {
        return Component.text("[±]").color(button);
    }


    public Component getAddButton(Component text) {
        return Component.text("[").append(text).append(Component.text("]")).color(buttonAdd);
    }

    public HoverEvent<Component> getClickToViewHoverEvent(CommandSender sender) {
        return HoverEvent.showText(Text.get(sender, Hover.CLICK_TO_VIEW));
    }

    public HoverEvent<Component> getClickToEditHoverEvent(CommandSender sender) {
        return HoverEvent.showText(Text.get(sender, Hover.CLICK_TO_EDIT));
    }

    public HoverEvent<Component> getClickToAddHoverEvent(CommandSender sender) {
        return HoverEvent.showText(Text.get(sender, Hover.CLICK_TO_ADD));
    }

    public Component highlight(String message) {
        return Component.text(message).color(getSecondary());
    }

    public Component getSpacersStart() {
        return Component.text("--- ").color(getPrimary());
    }

    public Component getSpacersEnd() {
        return Component.text(" ---").color(getPrimary());
    }

    public Component error(String message) {
        return Component.text(message).color(getError());
    }

    public Component primary(String message) {
        return Component.text(message).color(getPrimary());
    }

    public Component success(String message) {
        return Component.text(message).color(getSuccess());
    }

    public Component warning(String message) {
        return Component.text(message).color(getWarning());
    }

    public Component hyphen() {
        return Component.text(" - ").color(getPrimary());
    }

    public Component arrow() {
        return Component.text("->").color(getPrimary());
    }

    public Component tab() {
        return Component.text("  ");
    }

    public Component getTitleLine(Component title) {
        return getSpacersStart().append(title).append(getSpacersEnd());
    }

    public Component getTitleLine(String dark, String highlight) {
        return getSpacersStart().append(Component.text(dark).color(getSecondary())).append(Component.space()).append(Component.text(highlight).color(getPrimary())).append(getSpacersEnd());
    }

    public Component getParenthesized(String text) {
        return Component.text("(").color(getPrimary()).append(Component.text(text).color(getSecondary())).append(Component.text(")").color(getPrimary()));
    }

    public Component getBrackets(String text) {
        return Component.text("[").color(getPrimary()).append(Component.text(text).color(getSecondary())).append(Component.text("]").color(getPrimary()));
    }
    public Component getBrackets(Component text) {
        return Component.text("[").color(getPrimary()).append(text.color(getSecondary())).append(Component.text("]").color(getPrimary()));
    }

    public Component getBrackets(Component text, TextColor color) {
        return Component.text(" [").append(text).append(Component.text("]")).color(color);
    }

    public Component getPageSelector(CommandSender sender, Integer pageStart, int pageEnd, String command) {
        var pageText = getSpacersStart();
        if (pageStart > 1) {
            pageText = pageText.append(Component.text("<<< ").color(getSecondary()).clickEvent(ClickEvent.runCommand(command + " " + (pageStart - 1))));
        }

        pageText = pageText.append(Text.get(sender, Info.PAGE_CURRENT_OF_MAX, "%current%", String.valueOf(pageStart), "%max%", String.valueOf(pageEnd)));

        if (pageEnd > pageStart) {
            pageText = pageText.append(Component.text(" >>>").color(getSecondary()).clickEvent(ClickEvent.runCommand(command + " " + (pageStart + 1))));
        }

        pageText = pageText.append(getSpacersEnd());

        return pageText;
    }

    public Component getTimesRow(String row, String first, String second) {
        return Component.text(row + ".").color(getPrimary()).append(Component.space()).append(Component.text(first).color(getSecondary())).append(Component.space()).append(Component.text("|").color(getPrimary())).append(Component.space()).append(Component.text(second).color(getSecondary()));
    }

    public Component getTimesRow(String row, String first, String second, String third) {
        return Component.text(row + ".").color(getPrimary()).append(Component.space()).append(Component.text(first).color(getSecondary())).append(Component.space()).append(Component.text("|").color(getPrimary())).append(Component.space()).append(Component.text(second).color(getSecondary())).append(Component.space()).append(Component.text("|").color(getPrimary())).append(Component.space()).append(Component.text(third).color(getSecondary()));
    }
    @NotNull
    public Component getCheckpointHovers(TimeTrialFinish finish, TimeTrialFinish best, Component appendTo) {
        Component checkpoints = Component.empty();
        for (Integer key : finish.getCheckpointKeys()) {
            if (key != 1) {
                checkpoints = checkpoints.appendNewline();
            }
            checkpoints = checkpoints.append(Component.text(key + ": ").color(getPrimary()).append(Component.text(ApiUtilities.formatAsTime(finish.getCheckpointTime(key))).color(getSecondary())));

            if (best != null && best.getId() != finish.getId()) {
                checkpoints = checkpoints.append(finish.getDeltaToOther(best, finish.getPlayer().getTheme(), key));
            }
        }
        appendTo = appendTo.append(Component.text(" *").color(getWarning()));
        appendTo = appendTo.hoverEvent(HoverEvent.showText(checkpoints));
        return appendTo;
    }

    @NotNull
    public Component getCheckpointHovers(TimeTrialFinish finish, Component appendTo) {
        Component checkpoints = Component.empty();
        for (Integer key : finish.getCheckpointKeys()) {
            if (key != 1) {
                checkpoints = checkpoints.appendNewline();
            }
            checkpoints = checkpoints.append(Component.text(key + ": ").color(getPrimary()).append(Component.text(ApiUtilities.formatAsTime(finish.getCheckpointTime(key))).color(getSecondary())));
        }
        appendTo = appendTo.append(Component.text(" *").color(getWarning()));
        appendTo = appendTo.hoverEvent(HoverEvent.showText(checkpoints));
        return appendTo;
    }

}
