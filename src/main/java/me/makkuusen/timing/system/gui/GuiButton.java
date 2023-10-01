package me.makkuusen.timing.system.gui;

import org.bukkit.inventory.ItemStack;

public class GuiButton {

    private final ItemStack stack;
    private Runnable action;

    public GuiButton(ItemStack stack) {
        this.stack = stack;
    }

    public ItemStack getStack() {
        return stack;
    }

    public Runnable getAction() {
        return action;
    }

    public void setAction(Runnable runnable) {
        this.action = runnable;
    }
}
