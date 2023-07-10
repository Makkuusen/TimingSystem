package me.makkuusen.timing.system.text;

import me.makkuusen.timing.system.TimingSystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;

public class TextUtilities {

    public static TextColor textDarkColor = TextColor.fromHexString("#7bf200"); //7bf200 //gray
    public static TextColor textHighlightColor = TextColor.color(NamedTextColor.WHITE);
    public static TextColor textError = TextColor.fromHexString("#ff7a75"); //#ff7a75 //red
    public static TextColor textWarn = TextColor.color(NamedTextColor.YELLOW);
    public static TextColor textSuccess = TextColor.fromHexString("#7bf200"); //7bf200 //green
    public static TextColor textBroadcast = TextColor.fromHexString("#ff80ff"); //#ff80ff //Aqua
    public static TextColor textAwardHighlightColor = TextColor.fromHexString("#ff80ff"); //#ff80ff //yellow
    public static TextColor textAwardDarkColor = TextColor.fromHexString("#ce00ce"); //#ce00ce //gold

    public static Component getSpacersStart() {
        return Component.text("--- ").color(textDarkColor);
    }

    public static Component getSpacersEnd() {
        return Component.text(" ---").color(textDarkColor);
    }

    public static Component getTitleLine(String title) {
        return getSpacersStart().append(Component.text(title)).color(textHighlightColor).append(getSpacersEnd());
    }

    public static Component error(String message) {
        return Component.text(message).color(textError);
    }

    public static Component highlight(String message) {
        return Component.text(message).color(textHighlightColor);
    }

    public static Component dark(String message) {
        return Component.text(message).color(textDarkColor);
    }

    public static Component success(String message) {
        return Component.text(message).color(textSuccess);
    }

    public static Component warn(String message) {
        return Component.text(message).color(textWarn);
    }

    public static Component space() {
        return Component.text(" ");
    }

    public static Component tab() {
        return Component.text("  ");
    }

    public static Component hyphen() {
        return Component.text(" - ").color(textDarkColor);
    }

    public static Component arrow() {
        return Component.text("->").color(textDarkColor);
    }

    public static Component getTitleLine(String dark, String highlight) {
        return getSpacersStart().append(Component.text(dark).color(textDarkColor)).append(space()).append(Component.text(highlight).color(textHighlightColor)).append(getSpacersEnd());
    }

    public static Component getTitleLine(Component title) {
        return getSpacersStart().append(title).append(getSpacersEnd());
    }

    public static Component getParenthesized(String text) {
        return Component.text("(").color(textDarkColor).append(Component.text(text).color(textHighlightColor)).append(Component.text(")")).color(textDarkColor);
    }
    public static Component getParenthesized(String text, TextColor outside, TextColor inside) {
        return Component.text("(").color(outside).append(Component.text(text).color(inside)).append(Component.text(")")).color(outside);
    }

    public static Component getBrackets(String text) {
        return Component.text("[").color(textDarkColor).append(Component.text(text).color(textHighlightColor)).append(Component.text("]")).color(textDarkColor);
    }

    public static Component getPageSelector(CommandSender sender, Integer pageStart, int pageEnd, String command) {
        var pageText = TextUtilities.getSpacersStart();
        if (pageStart > 1) {
            pageText = pageText.append(Component.text("<<< ").color(TextUtilities.textHighlightColor).clickEvent(ClickEvent.runCommand(command + " " + (pageStart - 1))));
        }

        pageText = pageText.append(TimingSystem.getPlugin().getText(sender, Info.PAGE_CURRENT_OF_MAX, "%current%", String.valueOf(pageStart), "%max%", String.valueOf(pageEnd)));

        if (pageEnd > pageStart) {
            pageText = pageText.append(Component.text(" >>>").color(TextUtilities.textHighlightColor).clickEvent(ClickEvent.runCommand(command + " " + (pageStart + 1))));
        }

        pageText = pageText.append(TextUtilities.getSpacersEnd());

        return pageText;
    }

    public static Component getTimesRow(String row, String first, String second) {
        return Component.text(row + ".").color(TextUtilities.textDarkColor)
                .append(Component.space())
                .append(Component.text(first).color(TextUtilities.textHighlightColor))
                .append(Component.space())
                .append(Component.text("|").color(TextUtilities.textDarkColor))
                .append(Component.space())
                .append(Component.text(second).color(TextUtilities.textHighlightColor));
    }
    public static Component getTimesRow(String row, String first, String second, String third) {
        return Component.text(row + ".").color(TextUtilities.textDarkColor)
                .append(Component.space())
                .append(Component.text(first).color(TextUtilities.textHighlightColor))
                .append(Component.space())
                .append(Component.text("|").color(TextUtilities.textDarkColor))
                .append(Component.space())
                .append(Component.text(second).color(TextUtilities.textHighlightColor))
                .append(Component.space())
                .append(Component.text("|").color(TextUtilities.textDarkColor))
                .append(Component.space())
                .append(Component.text(third).color(TextUtilities.textHighlightColor));
    }
}
