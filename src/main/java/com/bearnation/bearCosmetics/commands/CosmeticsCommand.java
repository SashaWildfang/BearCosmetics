package com.bearnation.bearCosmetics.commands;

import com.bearnation.bearCosmetics.gui.CosmeticCategoryMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CosmeticsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        CosmeticCategoryMenu.open(player);
        return true;
    }
}
