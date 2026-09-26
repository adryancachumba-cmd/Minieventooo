package com.semianarquia.minieventos.modes;

import com.semianarquia.minieventos.EventManager;
import com.semianarquia.minieventos.MiniEventosPlugin;
import com.semianarquia.minieventos.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class SurvivalMode implements GameModeRunner {

    private EventManager manager;
    private MiniEventosPlugin plugin;
    private volatile boolean pvpPermitido = false;

    private BukkitTask taskAtivarPvp;
    private BukkitTask taskBussola;
    private BukkitTask taskAtualizarBussola;
    private BukkitTask taskArenaFinal;

    private int proximoSpawnArena = 0;

    @Override
    public void iniciar(EventManager manager, List<Player> participantes) {
        this.manager = manager;
        this.plugin = manager.getPlugin();
        this.pvpPermitido = false;

        long ticksPorMinuto = 20L * 60L;
        long tPvp = manager.getConfigManager().getSurvivalPvpOffMinutos() * ticksPorMinuto;
        long tBussola = manager.getConfigManager().getSurvivalBussolaMinutos() * ticksPorMinuto;
        long tArena = manager.getConfigManager().getSurvivalArenaFinalMinutos() * ticksPorMinuto;

        taskAtivarPvp = Bukkit.getScheduler().runTaskLater(plugin, this::ativarPvp, tPvp);
        taskBussola = Bukkit.getScheduler().runTaskLater(plugin, this::entregarBussolas, tBussola);
        taskArenaFinal = Bukkit.getScheduler().runTaskLater(plugin, this::irParaArenaFinal, tArena);
    }

    private void ativarPvp() {
        pvpPermitido = true;
        MessageUtil.enviarPara(manager.getVivosComoJogadores(), manager.getConfigManager().getMensagem("pvp-ativado"));
    }

    private void entregarBussolas() {
        List<Player> vivos = manager.getVivosComoJogadores();
        for (Player p : vivos) {
            p.getInventory().addItem(new ItemStack(Material.COMPASS));
            MessageUtil.enviar(p, manager.getConfigManager().getMensagem("bussola-entregue"));
        }
        // Atualiza o alvo da bussola a cada segundo enquanto houver 2+ jogadores vivos.
        taskAtualizarBussola = Bukkit.getScheduler().runTaskTimer(plugin, this::atualizarBussolas, 0L, 20L);
    }

    private void atualizarBussolas() {
        List<Player> vivos = manager.getVivosComoJogadores();
        if (vivos.size() < 2) return;
        for (Player p : vivos) {
            Player maisProximo = null;
            double menorDistanciaQuadrada = Double.MAX_VALUE;
            for (Player outro : vivos) {
                if (outro.equals(p)) continue;
                if (!outro.getWorld().equals(p.getWorld())) continue;
                double distSq = outro.getLocation().distanceSquared(p.getLocation());
                if (distSq < menorDistanciaQuadrada) {
                    menorDistanciaQuadrada = distSq;
                    maisProximo = outro;
                }
            }
            if (maisProximo != null) {
                p.setCompassTarget(maisProximo.getLocation());
            }
        }
    }

    private void irParaArenaFinal() {
        List<Location> spawns = manager.getConfigManager().getSurvivalArenaSpawns();
        List<Player> vivos = manager.getVivosComoJogadores();
        if (spawns.isEmpty()) {
            plugin.getLogger().warning("Nenhum spawn de arena final configurado para o modo Survival! "
                    + "Use /eventoadmin addspawn survival_arena. Os jogadores NAO serao teleportados.");
            return;
        }
        List<Player> ordemAleatoria = new ArrayList<>(vivos);
        java.util.Collections.shuffle(ordemAleatoria);
        for (int i = 0; i < ordemAleatoria.size(); i++) {
            Location destino = spawns.get(proximoSpawnArena % spawns.size());
            proximoSpawnArena++;
            ordemAleatoria.get(i).teleport(destino);
            MessageUtil.enviar(ordemAleatoria.get(i), manager.getConfigManager().getMensagem("arena-final"));
        }
    }

    @Override
    public boolean isPvpPermitido() {
        return pvpPermitido;
    }

    @Override
    public void parar() {
        cancelar(taskAtivarPvp);
        cancelar(taskBussola);
        cancelar(taskAtualizarBussola);
        cancelar(taskArenaFinal);
    }

    private void cancelar(BukkitTask task) {
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
        }
    }
}
