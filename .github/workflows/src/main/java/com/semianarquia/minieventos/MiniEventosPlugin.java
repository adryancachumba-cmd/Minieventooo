package com.semianarquia.minieventos;

import com.semianarquia.minieventos.commands.EventoAdminCommand;
import com.semianarquia.minieventos.commands.EventoCommand;
import com.semianarquia.minieventos.listeners.CommandBlockListener;
import com.semianarquia.minieventos.listeners.DeathRespawnListener;
import com.semianarquia.minieventos.listeners.PvpListener;
import com.semianarquia.minieventos.listeners.QuitJoinListener;
import com.semianarquia.minieventos.util.RewardManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MiniEventosPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private PlayerDataManager dataManager;
    private RewardManager rewardManager;
    private EventManager eventManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.dataManager = new PlayerDataManager(this);
        this.rewardManager = new RewardManager(this);
        this.eventManager = new EventManager(this);

        // Recupera qualquer inventario preso por uma queda do servidor em
        // pleno evento (ver PlayerDataManager / QuitJoinListener).
        this.dataManager.carregarPendentesDoDisco();

        getCommand("evento").setExecutor(new EventoCommand(eventManager));
        getCommand("eventoadmin").setExecutor(new EventoAdminCommand(eventManager));

        getServer().getPluginManager().registerEvents(new PvpListener(eventManager), this);
        getServer().getPluginManager().registerEvents(new CommandBlockListener(eventManager), this);
        getServer().getPluginManager().registerEvents(new QuitJoinListener(this, eventManager), this);
        getServer().getPluginManager().registerEvents(new DeathRespawnListener(this, eventManager), this);

        eventManager.iniciarCicloAutomatico();

        getLogger().info("MiniEventos habilitado. Proxima rodada em " + configManager.getIntervaloEventoMinutos()
                + " minuto(s) (ou use /eventoadmin abrir / iniciar para testar agora).");
    }

    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.pararCicloAutomatico();
            // Nao faz broadcast (o servidor esta desligando), mas restaura
            // qualquer participante para nao perder itens.
            eventManager.encerrarEmergencia(null);
        }
        getLogger().info("MiniEventos desabilitado.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlayerDataManager getDataManager() {
        return dataManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }
}
