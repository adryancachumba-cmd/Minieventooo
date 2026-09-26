package com.semianarquia.minieventos.listeners;

import com.semianarquia.minieventos.EventManager;
import com.semianarquia.minieventos.EventState;
import com.semianarquia.minieventos.modes.GameModeRunner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Cancela dano entre participantes do evento enquanto o PvP nao estiver
 * liberado (imunidade inicial do modo Survival / preparo do modo PvP com Kits).
 * Dano vindo de mobs/ambiente nunca e afetado.
 */
public class PvpListener implements Listener {

    private final EventManager manager;

    public PvpListener(EventManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDanoEntreEntidades(EntityDamageByEntityEvent event) {
        if (manager.getState() != EventState.EM_ANDAMENTO) return;
        if (!(event.getEntity() instanceof Player vitima)) return;

        Entity causador = event.getDamager();
        Player atacante = resolverAtacante(causador);
        if (atacante == null) return;

        if (!manager.isParticipanteAtivo(vitima.getUniqueId()) || !manager.isParticipanteAtivo(atacante.getUniqueId())) {
            return;
        }

        GameModeRunner runner = manager.getRunnerAtual();
        boolean pvpLiberado = runner != null && runner.isPvpPermitido();
        if (!pvpLiberado) {
            event.setCancelled(true);
        }
    }

    private Player resolverAtacante(Entity causador) {
        if (causador instanceof Player p) {
            return p;
        }
        if (causador instanceof Projectile projetil && projetil.getShooter() instanceof Player p) {
            return p;
        }
        return null;
    }
}
