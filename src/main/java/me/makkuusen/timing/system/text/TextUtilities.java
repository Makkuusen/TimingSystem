package me.makkuusen.timing.system.text;

import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.theme.DefaultTheme;
import me.makkuusen.timing.system.theme.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TextUtilities {

    public static Component getSpacersStart(Theme theme) {
        return Component.text("--- ").color(theme.getPrimary());
    }

    public static Component getSpacersEnd(Theme theme) {
        return Component.text(" ---").color(theme.getPrimary());
    }

    public static Component getTitleLine(String title, Theme theme) {
        return getSpacersStart(theme).append(Component.text(title)).color(theme.getSecondary()).append(getSpacersEnd(theme));
    }

    public static Component error(String message, Theme theme) {
        return Component.text(message).color(theme.getError());
    }

    public static Component highlight(String message, Theme theme) {
        return Component.text(message).color(theme.getSecondary());
    }

    public static Component primary(String message, Theme theme) {
        return Component.text(message).color(theme.getPrimary());
    }

    public static Component success(String message, Theme theme) {
        return Component.text(message).color(theme.getSuccess());
    }

    public static Component warning(String message, Theme theme) {
        return Component.text(message).color(theme.getWarning());
    }

    public static Component space() {
        return Component.text(" ");
    }

    public static Component tab() {
        return Component.text("  ");
    }

    public static Component hyphen(Theme theme) {
        return Component.text(" - ").color(theme.getPrimary());
    }

    public static Component arrow(Theme theme) {
        return Component.text("->").color(theme.getPrimary());
    }

    public static Component getTitleLine(Component title, Theme theme) {
        return getSpacersStart(theme).append(title).append(getSpacersEnd(theme));
    }

    public static Component getTitleLine(String dark, String highlight, Theme theme) {
        return getSpacersStart(theme).append(Component.text(dark).color(theme.getSecondary())).append(space()).append(Component.text(highlight).color(theme.getPrimary())).append(getSpacersEnd(theme));
    }

    public static Component getParenthesized(String text, Theme theme) {
        return Component.text("(").color(theme.getPrimary()).append(Component.text(text).color(theme.getSecondary()).append(Component.text(")")).color(theme.getPrimary()));
    }
    public static Component getParenthesized(String text, TextColor outside, TextColor inside) {
        return Component.text("(").color(outside).append(Component.text(text).color(inside)).append(Component.text(")")).color(outside);
    }

    public static Component getBrackets(String text, Theme theme) {
        return Component.text("[").color(theme.getPrimary()).append(Component.text(text).color(theme.getSecondary()).append(Component.text("]")).color(theme.getPrimary()));
    }

    public static Component getPageSelector(CommandSender sender, Theme theme, Integer pageStart, int pageEnd, String command) {
        var pageText = TextUtilities.getSpacersStart(theme);
        if (pageStart > 1) {
            pageText = pageText.append(Component.text("<<< ").color(theme.getPrimary()).clickEvent(ClickEvent.runCommand(command + " " + (pageStart - 1))));
        }

        pageText = pageText.append(TimingSystem.getPlugin().getText(sender, Info.PAGE_CURRENT_OF_MAX, "%current%", String.valueOf(pageStart), "%max%", String.valueOf(pageEnd)));

        if (pageEnd > pageStart) {
            pageText = pageText.append(Component.text(" >>>").color(theme.getSecondary()).clickEvent(ClickEvent.runCommand(command + " " + (pageStart + 1))));
        }

        pageText = pageText.append(TextUtilities.getSpacersEnd(theme));

        return pageText;
    }

    public static Theme getTheme(CommandSender sender) {
        return sender instanceof Player ? Database.getPlayer(sender).getTheme() : new DefaultTheme();
    }

    public static Component getTimesRow(String row, String first, String second, Theme theme) {
        return Component.text(row + ".").color(theme.getPrimary())
                .append(Component.space())
                .append(Component.text(first).color(theme.getSecondary()))
                .append(Component.space())
                .append(Component.text("|").color(theme.getPrimary()))
                .append(Component.space())
                .append(Component.text(second).color(theme.getSecondary()));
    }
    public static Component getTimesRow(String row, String first, String second, String third, Theme theme) {
        return Component.text(row + ".").color(theme.getPrimary())
                .append(Component.space())
                .append(Component.text(first).color(theme.getSecondary()))
                .append(Component.space())
                .append(Component.text("|").color(theme.getPrimary()))
                .append(Component.space())
                .append(Component.text(second).color(theme.getSecondary()))
                .append(Component.space())
                .append(Component.text("|").color(theme.getPrimary()))
                .append(Component.space())
                .append(Component.text(third).color(theme.getSecondary()));
    }
}
