package com.semianarquia.minieventos;

import com.semianarquia.minieventos.modes.GameModeRunner;
import com.semianarquia.minieventos.modes.PvpKitsMode;
import com.semianarquia.minieventos.modes.SurvivalMode;
import com.semianarquia.minieventos.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Controla todo o ciclo do mini evento: o relogio de 1 em 1 hora, a janela
 * de inscricoes de 3 minutos, o sorteio do modo, e o inicio/fim da partida.
 * As regras especificas de cada modo ficam nas classes em modes/.
 */
public class EventManager {

    private final MiniEventosPlugin plugin;
    private final Random random = new Random();

    private volatile EventState state = EventState.AGUARDANDO;
    private volatile GameModeType modoAtual = null;
    private volatile World worldEvento = null;
    private GameModeRunner runnerAtual = null;

    private final Set<UUID> inscritos = new LinkedHashSet<>();
    private final Set<UUID> participantesAtivos = new HashSet<>();
    private final Set<UUID> vivos = new HashSet<>();

    private BukkitTask taskCicloHorario;
    private BukkitTask taskFecharInscricoes;

    public EventManager(MiniEventosPlugin plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------
    // Ciclo automatico (1x por hora)
    // -----------------------------------------------------------

    public void iniciarCicloAutomatico() {
        long periodoTicks = 20L * 60L * configManager().getIntervaloEventoMinutos();
        taskCicloHorario = Bukkit.getScheduler().runTaskTimer(plugin, this::tentarAbrirInscricoes, periodoTicks, periodoTicks);
        plugin.getLogger().info("Ciclo automatico iniciado: uma rodada a cada " + configManager().getIntervaloEventoMinutos() + " minuto(s).");
    }

    public void pararCicloAutomatico() {
        if (taskCicloHorario != null) {
            taskCicloHorario.cancel();
        }
    }

    private void tentarAbrirInscricoes() {
        if (state != EventState.AGUARDANDO) {
            plugin.getLogger().warning("Chegou a hora de uma nova rodada, mas ainda ha inscricoes/partida em andamento. Este ciclo sera pulado.");
            return;
        }
        abrirInscricoes();
    }

    // -----------------------------------------------------------
    // Inscricoes
    // -----------------------------------------------------------

    /** Abre a janela de inscricoes agora, se o sistema estiver ocioso. */
    public boolean abrirInscricoes() {
        if (state != EventState.AGUARDANDO) {
            return false;
        }
        state = EventState.INSCRICOES_ABERTAS;
        inscritos.clear();
        MessageUtil.broadcast(configManager().getMensagem("inscricoes-abertas"));
        long tempoTicks = 20L * 60L * configManager().getTempoInscricaoMinutos();
        taskFecharInscricoes = Bukkit.getScheduler().runTaskLater(plugin, this::fecharInscricoesEIniciar, tempoTicks);
        return true;
    }

    /** Chamado pelo comando /evento. */
    public void inscrever(Player player) {
        if (state == EventState.EM_ANDAMENTO) {
            MessageUtil.enviar(player, configManager().getMensagem("evento-em-andamento-tentativa"));
            return;
        }
        if (state != EventState.INSCRICOES_ABERTAS) {
            MessageUtil.enviar(player, configManager().getMensagem("inscricoes-encerradas-tentativa"));
            return;
        }
        if (inscritos.contains(player.getUniqueId())) {
            MessageUtil.enviar(player, configManager().getMensagem("ja-inscrito"));
            return;
        }
        inscritos.add(player.getUniqueId());
        MessageUtil.enviar(player, configManager().getMensagem("inscrito-com-sucesso"));
    }

    private void fecharInscricoesEIniciar() {
        // Trava o estado imediatamente para que nenhum /evento seja aceito
        // depois deste ponto (Bug 8 da especificacao).
        state = EventState.EM_ANDAMENTO;
        MessageUtil.broadcast(configManager().getMensagem("inscricoes-fechadas"));

        List<Player> jogadores = new ArrayList<>();
        for (UUID uuid : inscritos) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                jogadores.add(p);
            }
        }
        inscritos.clear();

        if (jogadores.size() < configManager().getMinimoJogadores()) {
            MessageUtil.broadcast(configManager().getMensagem("evento-cancelado"));
            state = EventState.AGUARDANDO;
            return;
        }

        GameModeType modoSorteado = random.nextBoolean() ? GameModeType.SURVIVAL : GameModeType.PVP_KITS;
        iniciarPartida(modoSorteado, jogadores);
    }

    // -----------------------------------------------------------
    // Inicio da partida
    // -----------------------------------------------------------

    private void iniciarPartida(GameModeType modo, List<Player> jogadores) {
        World mundo = configManager().getMundoEvento();
        if (mundo == null) {
            plugin.getLogger().severe("O mundo de evento '" + configManager().getMundoEventoNome()
                    + "' nao existe ou nao esta carregado! Crie-o e/ou ajuste 'mundo-evento' no config.yml. Rodada cancelada.");
            MessageUtil.broadcast(configManager().getMensagem("evento-cancelado"));
            state = EventState.AGUARDANDO;
            return;
        }

        this.worldEvento = mundo;
        this.modoAtual = modo;

        List<Location> spawnsConfigurados = modo == GameModeType.SURVIVAL
                ? configManager().getSurvivalSpawns()
                : configManager().getPvpKitsSpawns();
        if (spawnsConfigurados.isEmpty()) {
            plugin.getLogger().warning("Nenhum spawn configurado para o modo " + modo.getNomeExibicao()
                    + "; todos os participantes irao para o spawn do mundo do evento. Configure com /eventoadmin addspawn.");
        }
        List<Location> spawns = spawnsConfigurados.isEmpty()
                ? List.of(mundo.getSpawnLocation())
                : spawnsConfigurados;

        participantesAtivos.clear();
        vivos.clear();

        List<Player> ordem = new ArrayList<>(jogadores);
        Collections.shuffle(ordem);

        int i = 0;
        for (Player p : ordem) {
            plugin.getDataManager().salvarEPrepararParaEvento(p);
            Location destino = spawns.get(i % spawns.size());
            i++;
            p.teleport(destino);
            participantesAtivos.add(p.getUniqueId());
            vivos.add(p.getUniqueId());
        }

        MessageUtil.broadcast(configManager().getMensagem("sorteio-modo").replace("%modo%", modo.getNomeExibicao()));

        runnerAtual = (modo == GameModeType.SURVIVAL) ? new SurvivalMode() : new PvpKitsMode();
        runnerAtual.iniciar(this, ordem);
    }

    // -----------------------------------------------------------
    // Eliminacao / saida / fim de partida
    // -----------------------------------------------------------

    /** Chamado quando um participante morre dentro do evento. */
    public void eliminarPorMorte(Player jogador) {
        if (!isParticipanteAtivo(jogador.getUniqueId())) return;
        vivos.remove(jogador.getUniqueId());
        verificarFimDePartida();
    }

    /**
     * Chamado quando um participante desconecta durante o evento. Restaura o
     * inventario dele imediatamente e o remove da partida (Bug 6 da spec).
     */
    public void removerPorSaida(Player jogador) {
        if (!isParticipanteAtivo(jogador.getUniqueId())) return;
        vivos.remove(jogador.getUniqueId());
        participantesAtivos.remove(jogador.getUniqueId());
        plugin.getDataManager().restaurar(jogador);
        try {
            jogador.teleport(obterSpawnPrincipal());
        } catch (Exception ignored) {
            // o jogador ja pode estar totalmente desconectado; a restauracao do
            // inventario acima e o que realmente importa aqui.
        }
        verificarFimDePartida();
    }

    /**
     * Chamado pelo DeathRespawnListener depois que o jogador eliminado ja
     * respawnou no lugar certo e ja teve o inventario original devolvido.
     * So agora ele deixa de ser considerado "participante ativo".
     */
    public void finalizarParticipanteEliminado(UUID uuid) {
        participantesAtivos.remove(uuid);
    }

    private void verificarFimDePartida() {
        if (state != EventState.EM_ANDAMENTO) return;
        if (vivos.size() <= 1) {
            Player vencedor = null;
            if (!vivos.isEmpty()) {
                Player candidato = Bukkit.getPlayer(vivos.iterator().next());
                if (candidato != null && candidato.isOnline()) {
                    vencedor = candidato;
                }
            }
            encerrarPartida(vencedor);
        }
    }

    private void encerrarPartida(Player vencedor) {
        if (runnerAtual != null) {
            runnerAtual.parar();
            runnerAtual = null;
        }

        if (vencedor != null) {
            plugin.getDataManager().restaurar(vencedor);
            vencedor.teleport(obterSpawnPrincipal());
            MessageUtil.enviar(vencedor, configManager().getMensagem("venceu"));
            plugin.getRewardManager().entregarChaveVencedor(vencedor);
            MessageUtil.broadcast(configManager().getMensagem("anuncio-vencedor")
                    .replace("%player%", vencedor.getName())
                    .replace("%modo%", modoAtual != null ? modoAtual.getNomeExibicao() : ""));
        }

        // Rede de seguranca: qualquer participante que ainda esteja marcado como
        // ativo (ex.: empate simultaneo) tambem precisa ser restaurado.
        for (UUID uuid : new HashSet<>(participantesAtivos)) {
            if (vencedor != null && uuid.equals(vencedor.getUniqueId())) continue;
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                plugin.getDataManager().restaurar(p);
                p.teleport(obterSpawnPrincipal());
            }
        }

        participantesAtivos.clear();
        vivos.clear();
        modoAtual = null;
        worldEvento = null;
        state = EventState.AGUARDANDO;
    }

    /** Parada de emergencia (comando de admin ou desligamento do plugin). Restaura todo mundo. */
    public void encerrarEmergencia(String motivoBroadcast) {
        if (runnerAtual != null) {
            runnerAtual.parar();
            runnerAtual = null;
        }
        if (taskFecharInscricoes != null) {
            taskFecharInscricoes.cancel();
        }
        for (UUID uuid : new HashSet<>(participantesAtivos)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                plugin.getDataManager().restaurar(p);
                p.teleport(obterSpawnPrincipal());
            }
        }
        participantesAtivos.clear();
        vivos.clear();
        inscritos.clear();
        modoAtual = null;
        worldEvento = null;
        state = EventState.AGUARDANDO;
        if (motivoBroadcast != null && !motivoBroadcast.isBlank()) {
            MessageUtil.broadcast(motivoBroadcast);
        }
    }

    /**
     * Forca o inicio imediato de uma partida (comando de admin), pulando a
     * espera da proxima hora. Usa os jogadores atualmente inscritos se a
     * janela de inscricao estiver aberta; caso contrario, usa todos os
     * jogadores online (util para testes).
     */
    public boolean forcarInicio(GameModeType modoForcado) {
        if (state == EventState.EM_ANDAMENTO) {
            return false;
        }
        List<Player> jogadores;
        if (state == EventState.INSCRICOES_ABERTAS) {
            if (taskFecharInscricoes != null) taskFecharInscricoes.cancel();
            jogadores = new ArrayList<>();
            for (UUID uuid : inscritos) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) jogadores.add(p);
            }
            inscritos.clear();
        } else {
            jogadores = new ArrayList<>(Bukkit.getOnlinePlayers());
        }
        if (jogadores.isEmpty()) {
            state = EventState.AGUARDANDO;
            return false;
        }
        state = EventState.EM_ANDAMENTO;
        GameModeType modo = modoForcado != null ? modoForcado : (random.nextBoolean() ? GameModeType.SURVIVAL : GameModeType.PVP_KITS);
        iniciarPartida(modo, jogadores);
        return true;
    }

    // -----------------------------------------------------------
    // Consultas usadas pelos listeners/comandos
    // -----------------------------------------------------------

    public Location obterSpawnPrincipal() {
        Location loc = configManager().getSpawnPrincipal();
        if (loc != null && loc.getWorld() != null) {
            return loc;
        }
        return Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    public boolean isParticipanteAtivo(UUID uuid) {
        return participantesAtivos.contains(uuid);
    }

    public boolean isVivo(UUID uuid) {
        return vivos.contains(uuid);
    }

    public List<Player> getVivosComoJogadores() {
        List<Player> lista = new ArrayList<>();
        for (UUID uuid : vivos) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) lista.add(p);
        }
        return lista;
    }

    public GameModeRunner getRunnerAtual() {
        return runnerAtual;
    }

    public EventState getState() {
        return state;
    }

    public GameModeType getModoAtual() {
        return modoAtual;
    }

    public World getWorldEvento() {
        return worldEvento;
    }

    public int getQuantidadeInscritos() {
        return inscritos.size();
    }

    public int getQuantidadeVivos() {
        return vivos.size();
    }

    public MiniEventosPlugin getPlugin() {
        return plugin;
    }

    public ConfigManager getConfigManager() {
        return plugin.getConfigManager();
    }

    private ConfigManager configManager() {
        return plugin.getConfigManager();
    }
}
