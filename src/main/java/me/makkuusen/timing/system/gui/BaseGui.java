package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.Database;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BaseGui {

    private Inventory inventory;
    private List<GuiButton> buttons = new ArrayList<>();

    public BaseGui(String title, int rows) {
        this.inventory = Bukkit.createInventory(null, rows * 9, title);
    }

    public void setItem(GuiButton button, int slot) {
        buttons.add(button);
        inventory.setItem(slot, button.getStack());
    }

    public boolean handleButton(ItemStack stack) {
        var maybeButton = buttons.stream().filter(b -> b.getStack().isSimilar(stack)).findFirst();
        if (maybeButton.isPresent()) {
            maybeButton.get().getAction().run();
            return true;
        }
        return false;
    }

    public void show(Player player) {
        var tPlayer = Database.getPlayer(player.getUniqueId());
        tPlayer.setOpenGui(this);
        player.openInventory(inventory);
    }

    public boolean equalsInv(Inventory inv) {
        return inv.equals(inventory);
    }
}
