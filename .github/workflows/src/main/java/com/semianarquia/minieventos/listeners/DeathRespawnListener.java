package com.semianarquia.minieventos.listeners;

import com.semianarquia.minieventos.EventManager;
import com.semianarquia.minieventos.MiniEventosPlugin;
import com.semianarquia.minieventos.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

/**
 * Bugs 4 e 5 da especificacao: um jogador eliminado nunca pode reaparecer no
 * mundo do evento, e a cama valida dele (quando existir) deve ser respeitada.
 *
 * A remocao da lista de "vivos" acontece imediatamente na morte (para a
 * bussola/checagem de vencedor reagirem na hora); a devolucao efetiva do
 * inventario e o "desligamento" do status de participante so acontecem no
 * respawn, momento em que tambem redirecionamos o destino do respawn.
 */
public class DeathRespawnListener implements Listener {

    private final MiniEventosPlugin plugin;
    private final EventManager manager;

    public DeathRespawnListener(MiniEventosPlugin plugin, EventManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onMorte(PlayerDeathEvent event) {
        Player jogador = event.getEntity();
        if (!manager.isParticipanteAtivo(jogador.getUniqueId())) return;

        // Os itens que o jogador tinha durante o evento (kit/drops) nao devem
        // ficar largados no mundo do evento.
        event.getDrops().clear();
        event.setDroppedExp(0);

        manager.eliminarPorMorte(jogador);
        MessageUtil.enviar(jogador, manager.getConfigManager().getMensagem("eliminado"));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player jogador = event.getPlayer();
        UUID uuid = jogador.getUniqueId();

        // So mexemos no respawn de quem foi eliminado por morte dentro do
        // evento (vencedores e desistentes ja sao tratados sem depender disso).
        if (!manager.isParticipanteAtivo(uuid)) return;

        Location destino = escolherRespawnValido(jogador);
        event.setRespawnLocation(destino);

        // Espera o respawn terminar de verdade antes de devolver o inventario,
        // senao o proprio processo de respawn pode sobrescrever o que setarmos.
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                plugin.getDataManager().restaurar(online);
            }
            manager.finalizarParticipanteEliminado(uuid);
        });
    }

    private Location escolherRespawnValido(Player jogador) {
        Location cama = jogador.getBedSpawnLocation();
        if (cama != null && cama.getWorld() != null) {
            return cama;
        }
        return manager.obterSpawnPrincipal();
    }
}
