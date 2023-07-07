package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.api.events.GuiOpenEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BaseGui {

    private final Inventory inventory;
    private final List<GuiButton> buttons = new ArrayList<>();

    public BaseGui(String title, int rows) {
        this.inventory = Bukkit.createInventory(null, rows * 9, Component.text(title));
    }

    public void setItem(GuiButton button, int slot) {
        buttons.add(button);
        inventory.setItem(slot, button.getStack());
    }

    public void removeItem(int slot) {
        var item = inventory.getItem(slot);
        if (item == null) {
            return;
        }
        var maybeButton = buttons.stream().filter(b -> b.getStack().isSimilar(item)).findFirst();
        if (maybeButton.isEmpty()) {
            return;
        }
        buttons.remove(maybeButton.get());
        inventory.remove(item);
    }

    public boolean handleButton(ItemStack stack) {
        var maybeButton = buttons.stream().filter(b -> b.getStack().isSimilar(stack)).findFirst();
        if (maybeButton.isPresent()) {
            maybeButton.get().getAction().run();
            return true;
        }
        return false;
    }

    public boolean hasButton(GuiButton button) {
        return buttons.contains(button);
    }

    public void show(Player player) {
        var tPlayer = Database.getPlayer(player.getUniqueId());
        GuiOpenEvent guiOpenEvent = new GuiOpenEvent(player, this);
        Bukkit.getServer().getPluginManager().callEvent(guiOpenEvent);
        if (guiOpenEvent.isCancelled()) {
            return;
        }
        tPlayer.setOpenGui(this);
        player.openInventory(inventory);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public boolean equalsInv(Inventory inv) {
        return inv.equals(inventory);
    }
}
