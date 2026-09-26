package com.semianarquia.minieventos;

/**
 * Estados possiveis do ciclo do mini evento.
 */
public enum EventState {
    /** Esperando a proxima rodada (contagem de 1h em andamento). */
    AGUARDANDO,
    /** Janela de 3 minutos em que /evento esta liberado. */
    INSCRICOES_ABERTAS,
    /** Partida rolando em um dos dois modos, no mundo do evento. */
    EM_ANDAMENTO
}
