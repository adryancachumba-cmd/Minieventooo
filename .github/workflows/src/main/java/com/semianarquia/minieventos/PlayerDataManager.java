package com.semianarquia.minieventos;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda o inventario/estado original de cada participante enquanto ele
 * esta dentro do evento, e devolve tudo corretamente ao sair/terminar.
 *
 * Tambem grava uma copia em disco (pendentes.yml) assim que a partida
 * comeca: se o servidor cair no meio do evento, ao reiniciar o plugin
 * consegue devolver os itens de qualquer jogador que ainda esteja com
 * um snapshot pendente (ver MiniEventosPlugin#onEnable e QuitJoinListener).
 */
public class PlayerDataManager {

    private final MiniEventosPlugin plugin;
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();
    private final File arquivo;

    public PlayerDataManager(MiniEventosPlugin plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "pendentes.yml");
    }

    public boolean temPendente(UUID uuid) {
        return snapshots.containsKey(uuid);
    }

    /** Captura o estado atual do jogador, limpa o inventario e o prepara para o evento. */
    public void salvarEPrepararParaEvento(Player player) {
        PlayerSnapshot snap = PlayerSnapshot.capturar(player);
        snapshots.put(player.getUniqueId(), snap);
        persistirEmDisco();
        PlayerSnapshot.prepararParaEvento(player);
    }

    /** Restaura o jogador (se houver snapshot) e remove o registro pendente. */
    public boolean restaurar(Player player) {
        PlayerSnapshot snap = snapshots.remove(player.getUniqueId());
        if (snap == null) {
            return false;
        }
        snap.restaurar(player);
        persistirEmDisco();
        return true;
    }

    /** Descarta um snapshot sem restaurar (uso interno/administrativo). */
    public void descartar(UUID uuid) {
        if (snapshots.remove(uuid) != null) {
            persistirEmDisco();
        }
    }

    // -----------------------------------------------------------
    // Persistencia em disco
    // -----------------------------------------------------------

    private void persistirEmDisco() {
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            for (Map.Entry<UUID, PlayerSnapshot> entrada : snapshots.entrySet()) {
                ConfigurationSection sec = yaml.createSection(entrada.getKey().toString());
                entrada.getValue().salvarEm(sec);
            }
            yaml.save(arquivo);
        } catch (IOException ex) {
            plugin.getLogger().warning("Nao foi possivel salvar pendentes.yml: " + ex.getMessage());
        }
    }

    /**
     * Chamado em onEnable(). Se o arquivo pendentes.yml nao estiver vazio,
     * significa que o servidor foi derrubado com um evento em andamento.
     * Os snapshots sao recarregados para a memoria para que sejam
     * devolvidos assim que cada jogador entrar novamente (ver QuitJoinListener).
     */
    public void carregarPendentesDoDisco() {
        if (!arquivo.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(arquivo);
        int recuperados = 0;
        for (String chave : yaml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(chave);
                ConfigurationSection sec = yaml.getConfigurationSection(chave);
                if (sec == null) continue;
                snapshots.put(uuid, PlayerSnapshot.carregarDe(sec));
                recuperados++;
            } catch (IllegalArgumentException ignored) {
                // chave invalida, ignora
            }
        }
        if (recuperados > 0) {
            plugin.getLogger().warning("Encontrados " + recuperados + " inventario(s) pendente(s) de um evento "
                    + "interrompido. Serao devolvidos assim que os jogadores entrarem no servidor.");
        }
    }
}
