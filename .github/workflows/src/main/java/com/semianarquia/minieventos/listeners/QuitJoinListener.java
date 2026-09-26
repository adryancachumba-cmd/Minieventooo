package com.semianarquia.minieventos.listeners;

import com.semianarquia.minieventos.EventManager;
import com.semianarquia.minieventos.MiniEventosPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Bug 6 da especificacao: se um participante desconectar durante a partida,
 * ele deve ser removido dela e, ao voltar, nunca reaparecer no mundo do
 * evento nem receber o kit de novo. Tambem cobre a recuperacao de
 * inventarios presos por uma queda do servidor em pleno evento.
 */
public class QuitJoinListener implements Listener {

    private final MiniEventosPlugin plugin;
    private final EventManager manager;

    public QuitJoinListener(MiniEventosPlugin plugin, EventManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onSair(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (manager.isParticipanteAtivo(player.getUniqueId())) {
            manager.removerPorSaida(player);
        }
    }

    @EventHandler
    public void onEntrar(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Rede de seguranca: inventario ficou pendente por causa de uma
        // queda de servidor durante um evento anterior.
        if (plugin.getDataManager().temPendente(uuid)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Player online = Bukkit.getPlayer(uuid);
                if (online == null) return;
                plugin.getDataManager().restaurar(online);
                online.teleport(manager.obterSpawnPrincipal());
                online.sendMessage(org.bukkit.ChatColor.YELLOW
                        + "[EVENTO] Seus itens de antes do ultimo evento foram recuperados.");
            }, 5L);
            return;
        }

        // Rede de seguranca extra: por algum motivo o jogador esta logando
        // dentro do mundo do evento sem estar marcado como participante ativo
        // (ex.: bind de cama configurada la por engano). Nunca deixa isso acontecer.
        String mundoEvento = manager.getConfigManager().getMundoEventoNome();
        if (player.getWorld().getName().equalsIgnoreCase(mundoEvento) && !manager.isParticipanteAtivo(uuid)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Player online = Bukkit.getPlayer(uuid);
                if (online != null) {
                    online.teleport(manager.obterSpawnPrincipal());
                }
            }, 5L);
        }
    }
}
