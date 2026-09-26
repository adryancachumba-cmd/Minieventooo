package com.semianarquia.minieventos.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MessageUtil {

    private MessageUtil() {
    }

    public static String cor(String texto) {
        if (texto == null) return "";
        return ChatColor.translateAlternateColorCodes('&', texto);
    }

    public static void enviar(CommandSender destino, String texto) {
        if (texto == null || texto.isEmpty()) return;
        destino.sendMessage(cor(texto));
    }

    public static void broadcast(String texto) {
        if (texto == null || texto.isEmpty()) return;
        Bukkit.broadcastMessage(cor(texto));
    }

    public static void enviarPara(Iterable<Player> jogadores, String texto) {
        String colorido = cor(texto);
        for (Player p : jogadores) {
            if (p != null && p.isOnline()) {
                p.sendMessage(colorido);
            }
        }
    }
}
