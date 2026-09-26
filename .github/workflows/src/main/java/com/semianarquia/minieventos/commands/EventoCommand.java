package com.semianarquia.minieventos.commands;

import com.semianarquia.minieventos.EventManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EventoCommand implements CommandExecutor {

    private final EventManager manager;

    public EventoCommand(EventManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Somente jogadores podem usar /evento.");
            return true;
        }
        manager.inscrever(player);
        return true;
    }
}
