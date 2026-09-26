package com.semianarquia.minieventos.commands;

import com.semianarquia.minieventos.ConfigManager;
import com.semianarquia.minieventos.EventManager;
import com.semianarquia.minieventos.GameModeType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class EventoAdminCommand implements CommandExecutor {

    private final EventManager manager;

    public EventoAdminCommand(EventManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            enviarAjuda(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "abrir" -> {
                boolean ok = manager.abrirInscricoes();
                sender.sendMessage(cor(ok
                        ? "&aInscricoes abertas manualmente."
                        : "&cNao foi possivel abrir: ja existe uma rodada em inscricao/andamento."));
            }
            case "iniciar" -> {
                GameModeType modo = null;
                if (args.length >= 2) {
                    if (args[1].equalsIgnoreCase("survival")) modo = GameModeType.SURVIVAL;
                    else if (args[1].equalsIgnoreCase("pvpkits")) modo = GameModeType.PVP_KITS;
                    else {
                        sender.sendMessage(cor("&cModo invalido. Use: survival ou pvpkits."));
                        return true;
                    }
                }
                boolean ok = manager.forcarInicio(modo);
                sender.sendMessage(cor(ok
                        ? "&aEvento iniciado a forca."
                        : "&cNao foi possivel iniciar: partida ja em andamento ou nenhum jogador online/inscrito."));
            }
            case "encerrar" -> {
                manager.encerrarEmergencia(cor("&c[EVENTO] A rodada foi encerrada manualmente por um administrador."));
                sender.sendMessage(cor("&aEvento encerrado. Todos os participantes foram restaurados."));
            }
            case "status" -> enviarStatus(sender);
            case "reload" -> {
                manager.getConfigManager().reload();
                sender.sendMessage(cor("&aconfig.yml recarregado."));
            }
            case "setmundo" -> {
                if (args.length < 2) {
                    sender.sendMessage(cor("&cUso: /eventoadmin setmundo <nome-do-mundo>"));
                    return true;
                }
                manager.getConfigManager().setMundoEventoNome(args[1]);
                sender.sendMessage(cor("&aMundo do evento definido como: &e" + args[1]
                        + (org.bukkit.Bukkit.getWorld(args[1]) == null ? " &c(atencao: esse mundo nao esta carregado agora)" : "")));
            }
            case "setspawnprincipal" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(cor("&cSo um jogador pode usar este comando (usa a localizacao atual)."));
                    return true;
                }
                manager.getConfigManager().setSpawnPrincipal(player.getLocation());
                sender.sendMessage(cor("&aSpawn principal definido na sua localizacao atual."));
            }
            case "addspawn" -> tratarAddSpawn(sender, args);
            default -> enviarAjuda(sender);
        }
        return true;
    }

    private void tratarAddSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(cor("&cSo um jogador pode usar este comando (usa a localizacao atual)."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(cor("&cUso: /eventoadmin addspawn <survival|survival_arena|pvpkits>"));
            return;
        }
        Location loc = player.getLocation();
        ConfigManager cfg = manager.getConfigManager();
        String tipo = args[1].toLowerCase(Locale.ROOT);
        switch (tipo) {
            case "survival" -> {
                cfg.addSurvivalSpawn(loc);
                sender.sendMessage(cor("&aSpawn de entrada do modo Survival adicionado (total: " + cfg.getSurvivalSpawns().size() + ")."));
            }
            case "survival_arena" -> {
                cfg.addSurvivalArenaSpawn(loc);
                sender.sendMessage(cor("&aSpawn da arena final do Survival adicionado (total: " + cfg.getSurvivalArenaSpawns().size() + ")."));
            }
            case "pvpkits" -> {
                cfg.addPvpKitsSpawn(loc);
                sender.sendMessage(cor("&aSpawn de entrada do modo PvP com Kits adicionado (total: " + cfg.getPvpKitsSpawns().size() + ")."));
            }
            default -> sender.sendMessage(cor("&cTipo invalido. Use: survival, survival_arena ou pvpkits."));
        }
    }

    private void enviarStatus(CommandSender sender) {
        sender.sendMessage(cor("&6&l--- MiniEventos: status ---"));
        sender.sendMessage(cor("&fEstado: &e" + manager.getState()));
        sender.sendMessage(cor("&fModo atual: &e" + (manager.getModoAtual() != null ? manager.getModoAtual().getNomeExibicao() : "-")));
        sender.sendMessage(cor("&fInscritos nesta rodada: &e" + manager.getQuantidadeInscritos()));
        sender.sendMessage(cor("&fVivos na partida atual: &e" + manager.getQuantidadeVivos()));
        sender.sendMessage(cor("&fMundo do evento configurado: &e" + manager.getConfigManager().getMundoEventoNome()
                + (manager.getConfigManager().getMundoEvento() == null ? " &c(nao carregado!)" : " &a(carregado)")));
        sender.sendMessage(cor("&fSpawns Survival: &e" + manager.getConfigManager().getSurvivalSpawns().size()
                + " &f| Arena final: &e" + manager.getConfigManager().getSurvivalArenaSpawns().size()
                + " &f| PvP Kits: &e" + manager.getConfigManager().getPvpKitsSpawns().size()));
    }

    private void enviarAjuda(CommandSender sender) {
        sender.sendMessage(cor("&6&l--- MiniEventos: comandos ---"));
        sender.sendMessage(cor("&e/eventoadmin abrir &f- abre as inscricoes agora"));
        sender.sendMessage(cor("&e/eventoadmin iniciar [survival|pvpkits] &f- forca o inicio imediato"));
        sender.sendMessage(cor("&e/eventoadmin encerrar &f- encerra a rodada atual e restaura todos"));
        sender.sendMessage(cor("&e/eventoadmin status &f- mostra o estado atual do sistema"));
        sender.sendMessage(cor("&e/eventoadmin reload &f- recarrega o config.yml"));
        sender.sendMessage(cor("&e/eventoadmin setmundo <nome> &f- define o mundo do evento"));
        sender.sendMessage(cor("&e/eventoadmin setspawnprincipal &f- define o spawn principal (sua localizacao)"));
        sender.sendMessage(cor("&e/eventoadmin addspawn <survival|survival_arena|pvpkits> &f- adiciona sua localizacao a lista de spawns"));
    }

    private String cor(String texto) {
        return ChatColor.translateAlternateColorCodes('&', texto);
    }
}
