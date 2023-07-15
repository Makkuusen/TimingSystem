package me.makkuusen.timing.system.theme;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.theme.messages.Info;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        return sender instanceof Player ? Database.getPlayer(sender).getTheme() : new Theme();
    }

    public Component getViewButton() {
        return Component.text("[»]").color(button).hoverEvent(getClickToViewHoverEvent());
    }

    public Component getEditButton() {
        return Component.text("[Edit]").color(button).hoverEvent(getClickToEditHoverEvent());
    }

    public Component getEditButton(String value, Theme theme) {
        return theme.getBrackets(value).hoverEvent(getClickToEditHoverEvent());
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

    public Component getAddButton(String extra) {
        return Component.text("[Add " + extra + "]").color(buttonAdd);
    }

    public static HoverEvent<Component> getClickToViewHoverEvent() {
        return HoverEvent.showText(Component.text("Click to view"));
    }

    public static HoverEvent<Component> getClickToEditHoverEvent() {
        return HoverEvent.showText(Component.text("Click to edit"));
    }

    public static HoverEvent<Component> getClickToAddHoverEvent() {
        return HoverEvent.showText(Component.text("Click to add"));
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

    public Component getTitleLine(String title) {
        return getSpacersStart().append(Component.text(title)).color(getSecondary()).append(getSpacersEnd());
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
        return Component.text("(").color(getPrimary()).append(Component.text(text).color(getSecondary()).append(Component.text(")").color(getPrimary())));
    }

    public Component getBrackets(String text) {
        return Component.text("[").color(getPrimary()).append(Component.text(text).color(getSecondary()).append(Component.text("]").color(getPrimary())));
    }

    public Component getPageSelector(CommandSender sender, Integer pageStart, int pageEnd, String command) {
        var pageText = getSpacersStart();
        if (pageStart > 1) {
            pageText = pageText.append(Component.text("<<< ").color(getSecondary()).clickEvent(ClickEvent.runCommand(command + " " + (pageStart - 1))));
        }

        pageText = pageText.append(TimingSystem.getPlugin().getText(sender, Info.PAGE_CURRENT_OF_MAX, "%current%", String.valueOf(pageStart), "%max%", String.valueOf(pageEnd)));

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

}
