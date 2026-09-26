package com.semianarquia.minieventos.modes;

import com.semianarquia.minieventos.EventManager;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Contrato comum aos dois modos de jogo. O EventManager cuida do que e
 * igual nos dois modos (snapshot de inventario, teleporte de entrada/saida,
 * contagem de eliminados, entrega de recompensa); cada modo cuida apenas
 * das suas regras especificas (PvP timer + bussola + arena final, ou
 * kits + borda).
 */
public interface GameModeRunner {

    /** Chamado uma vez, logo apos todos os participantes serem teleportados para o mundo do evento. */
    void iniciar(EventManager manager, List<Player> participantes);

    /**
     * Deve permitir dano entre dois jogadores neste exato momento?
     * (No modo Survival isso e falso nos primeiros minutos; no modo PvP com Kits, e sempre
     * verdadeiro depois do preparo inicial.)
     */
    boolean isPvpPermitido();

    /** Cancela todas as tarefas agendadas deste modo (chamado ao encerrar a partida). */
    void parar();
}
