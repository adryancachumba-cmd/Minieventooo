package com.semianarquia.minieventos.modes;

import com.semianarquia.minieventos.EventManager;
import com.semianarquia.minieventos.MiniEventosPlugin;
import com.semianarquia.minieventos.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class PvpKitsMode implements GameModeRunner {

    private EventManager manager;
    private MiniEventosPlugin plugin;
    private volatile boolean pvpPermitido = false;
    private BukkitTask taskIniciarBatalha;

    @Override
    public void iniciar(EventManager manager, List<Player> participantes) {
        this.manager = manager;
        this.plugin = manager.getPlugin();
        this.pvpPermitido = false;

        // Entrega o kit para todos antes de qualquer coisa (evita o "Bug 2" da spec:
        // ninguem comeca a lutar sem ter recebido o kit). O inventario ja chega
        // vazio aqui, pois o EventManager preparou cada jogador antes de chamar iniciar().
        List<ItemStack> kit = manager.getConfigManager().getKitPvpKits();
        for (Player p : participantes) {
            for (ItemStack item : kit) {
                p.getInventory().addItem(item.clone());
            }
        }

        configurarBorda(participantes);

        long preparoTicks = 20L * manager.getConfigManager().getPvpKitsPreparoSegundos();
        taskIniciarBatalha = Bukkit.getScheduler().runTaskLater(plugin, this::iniciarBatalha, preparoTicks);
    }

    private void configurarBorda(List<Player> participantes) {
        World mundo = manager.getWorldEvento();
        if (mundo == null) return;

        Location centro;
        if (!participantes.isEmpty()) {
            // Centro = media das posicoes iniciais dos participantes.
            double x = 0, z = 0;
            for (Player p : participantes) {
                x += p.getLocation().getX();
                z += p.getLocation().getZ();
            }
            centro = new Location(mundo, x / participantes.size(), mundo.getSpawnLocation().getY(), z / participantes.size());
        } else {
            centro = mundo.getSpawnLocation();
        }

        WorldBorder borda = mundo.getWorldBorder();
        borda.setCenter(centro);
        borda.setSize(manager.getConfigManager().getBordaTamanhoInicial());
    }

    private void iniciarBatalha() {
        pvpPermitido = true;
        World mundo = manager.getWorldEvento();
        if (mundo != null) {
            WorldBorder borda = mundo.getWorldBorder();
            borda.setSize(manager.getConfigManager().getBordaTamanhoFinal(),
                    manager.getConfigManager().getBordaTempoEncolhimentoSegundos());
        }
        MessageUtil.enviarPara(manager.getVivosComoJogadores(), manager.getConfigManager().getMensagem("batalha-inicio"));
    }

    @Override
    public boolean isPvpPermitido() {
        return pvpPermitido;
    }

    @Override
    public void parar() {
        if (taskIniciarBatalha != null) {
            try {
                taskIniciarBatalha.cancel();
            } catch (Exception ignored) {
            }
        }
        // Restaura a borda do mundo do evento ao padrao para a proxima rodada.
        World mundo = manager != null ? manager.getWorldEvento() : null;
        if (mundo != null) {
            try {
                mundo.getWorldBorder().reset();
            } catch (Exception ignored) {
            }
        }
    }
}
