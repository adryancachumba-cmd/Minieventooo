package com.semianarquia.minieventos;

/**
 * Os dois modos de jogo sorteados aleatoriamente a cada rodada.
 */
public enum GameModeType {
    SURVIVAL("Survival"),
    PVP_KITS("PvP com Kits");

    private final String nomeExibicao;

    GameModeType(String nomeExibicao) {
        this.nomeExibicao = nomeExibicao;
    }

    public String getNomeExibicao() {
        return nomeExibicao;
    }
}
