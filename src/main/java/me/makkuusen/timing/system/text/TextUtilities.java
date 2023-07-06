package me.makkuusen.timing.system.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class TextUtilities {

    public static TextColor textDarkColor = TextColor.color(NamedTextColor.GRAY);
    public static TextColor textHighlightColor = TextColor.color(NamedTextColor.WHITE);
    public static TextColor textError = TextColor.color(NamedTextColor.RED);
    public static TextColor textWarn = TextColor.color(NamedTextColor.YELLOW);
    public static TextColor textSuccess = TextColor.color(NamedTextColor.GREEN);

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

    public static Component getBrackets(String text) {
        return Component.text("[").color(textDarkColor).append(Component.text(text).color(textHighlightColor)).append(Component.text("]")).color(textDarkColor);
    }
}
