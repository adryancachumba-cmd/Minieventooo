package com.semianarquia.minieventos.listeners;

import com.semianarquia.minieventos.EventManager;
import com.semianarquia.minieventos.EventState;
import com.semianarquia.minieventos.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Bloqueia comandos para quem esta participando ativamente do evento
 * (inclusive OPs, a nao ser que tenham a permissao "eventos.admin.bypass").
 * Isso evita que jogadores usem /home, /rtp, /spawn, /tpa etc. para escapar
 * da partida (Bug 7 da especificacao).
 */
public class CommandBlockListener implements Listener {

    private final EventManager manager;

    public CommandBlockListener(EventManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onComando(PlayerCommandPreprocessEvent event) {
        if (manager.getState() != EventState.EM_ANDAMENTO) return;

        Player player = event.getPlayer();
        if (!manager.isParticipanteAtivo(player.getUniqueId())) return;
        if (player.hasPermission("eventos.admin.bypass")) return;

        String mensagem = event.getMessage(); // ja vem com a barra "/" na frente
        String semBarra = mensagem.startsWith("/") ? mensagem.substring(1) : mensagem;
        String comandoBase = semBarra.split(" ")[0].toLowerCase(Locale.ROOT);
        // Remove um eventual prefixo de plugin, ex: "minieventos:evento" -> "evento"
        int doisPontos = comandoBase.indexOf(':');
        if (doisPontos >= 0) {
            comandoBase = comandoBase.substring(doisPontos + 1);
        }

        Set<String> permitidos = new HashSet<>();
        permitidos.add("evento");
        List<String> extras = manager.getConfigManager().getComandosPermitidosDuranteEvento();
        for (String extra : extras) {
            permitidos.add(extra.toLowerCase(Locale.ROOT).replace("/", ""));
        }

        if (!permitidos.contains(comandoBase)) {
            event.setCancelled(true);
            MessageUtil.enviar(player, manager.getConfigManager().getMensagem("comando-bloqueado"));
        }
    }
}
