package me.makkuusen.timing.system.gui;

import org.bukkit.inventory.ItemStack;

public class GuiButton {

    private ItemStack stack;
    private Runnable action;

    public GuiButton(ItemStack stack) {
        this.stack = stack;
    }

    public void setAction(Runnable runnable) {
        this.action = runnable;
    }

    public ItemStack getStack() {
        return stack;
    }

    public Runnable getAction() {
        return action;
    }
}
