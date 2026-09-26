package com.semianarquia.minieventos.util;

import com.semianarquia.minieventos.MiniEventosPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class RewardManager {

    private final MiniEventosPlugin plugin;

    public RewardManager(MiniEventosPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Entrega 1x chave da Eventos Crate ao vencedor, via console, usando
     * o comando configurado em config.yml (recompensa.comando).
     * Nunca falha silenciosamente: qualquer erro fica registrado no console.
     */
    public void entregarChaveVencedor(Player vencedor) {
        String template = plugin.getConfigManager().getComandoRecompensa();
        if (template == null || template.isBlank()) {
            plugin.getLogger().severe("Nenhum comando de recompensa configurado (recompensa.comando)! "
                    + "A chave NAO foi entregue a " + vencedor.getName() + ".");
            return;
        }
        String comando = template.replace("%player%", vencedor.getName());
        try {
            boolean sucesso = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comando);
            if (!sucesso) {
                plugin.getLogger().severe("O comando de recompensa retornou falha para "
                        + vencedor.getName() + ": /" + comando);
            }
        } catch (Exception ex) {
            plugin.getLogger().severe("Erro ao entregar a chave da Eventos Crate para "
                    + vencedor.getName() + ": " + ex.getMessage());
        }
    }
}
