package com.semianarquia.minieventos;

import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Uma "foto" completa do estado do jogador antes de entrar no evento,
 * para que possa ser devolvido exatamente como estava.
 */
public class PlayerSnapshot {

    public ItemStack[] inventario;
    public ItemStack[] armadura;
    public ItemStack itemMaoSecundaria;
    public double vida;
    public int fome;
    public float saturacao;
    public float experiencia;
    public int nivel;
    public GameMode modoJogo;

    public static PlayerSnapshot capturar(Player player) {
        PlayerSnapshot snap = new PlayerSnapshot();
        snap.inventario = player.getInventory().getContents().clone();
        snap.armadura = player.getInventory().getArmorContents().clone();
        snap.itemMaoSecundaria = player.getInventory().getItemInOffHand().clone();
        snap.vida = player.getHealth();
        snap.fome = player.getFoodLevel();
        snap.saturacao = player.getSaturation();
        snap.experiencia = player.getExp();
        snap.nivel = player.getLevel();
        snap.modoJogo = player.getGameMode();
        return snap;
    }

    /** Limpa o jogador e aplica o kit/estado do evento (chamado apos capturar()). */
    public static void prepararParaEvento(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setHealth(Math.min(20.0, player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) != null
                ? player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue() : 20.0));
        player.setExp(0f);
        player.setLevel(0);
        if (player.getGameMode() != GameMode.SURVIVAL) {
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    /** Restaura o jogador para exatamente como estava antes do evento. */
    public void restaurar(Player player) {
        player.getInventory().clear();
        player.getInventory().setContents(inventario);
        player.getInventory().setArmorContents(armadura);
        player.getInventory().setItemInOffHand(itemMaoSecundaria);
        player.setGameMode(modoJogo);
        double maxVida = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) != null
                ? player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue() : 20.0;
        player.setHealth(Math.max(1.0, Math.min(vida, maxVida)));
        player.setFoodLevel(fome);
        player.setSaturation(saturacao);
        player.setExp(Math.max(0f, Math.min(experiencia, 0.999f)));
        player.setLevel(Math.max(0, nivel));
    }

    // -----------------------------------------------------------
    // Persistencia em disco (rede de seguranca contra queda do servidor
    // durante um evento - ver PlayerDataManager)
    // -----------------------------------------------------------

    public void salvarEm(ConfigurationSection sec) {
        sec.set("inventario", toSerializableList(inventario));
        sec.set("armadura", toSerializableList(armadura));
        sec.set("offhand", itemMaoSecundaria);
        sec.set("vida", vida);
        sec.set("fome", fome);
        sec.set("saturacao", (double) saturacao);
        sec.set("experiencia", (double) experiencia);
        sec.set("nivel", nivel);
        sec.set("modo-jogo", modoJogo.name());
    }

    public static PlayerSnapshot carregarDe(ConfigurationSection sec) {
        PlayerSnapshot snap = new PlayerSnapshot();
        snap.inventario = fromSerializableList(sec.getList("inventario"));
        snap.armadura = fromSerializableList(sec.getList("armadura"));
        Object off = sec.get("offhand");
        snap.itemMaoSecundaria = (off instanceof ItemStack) ? (ItemStack) off : new ItemStack(org.bukkit.Material.AIR);
        snap.vida = sec.getDouble("vida", 20.0);
        snap.fome = sec.getInt("fome", 20);
        snap.saturacao = (float) sec.getDouble("saturacao", 20.0);
        snap.experiencia = (float) sec.getDouble("experiencia", 0.0);
        snap.nivel = sec.getInt("nivel", 0);
        String modo = sec.getString("modo-jogo", "SURVIVAL");
        try {
            snap.modoJogo = GameMode.valueOf(modo);
        } catch (IllegalArgumentException ex) {
            snap.modoJogo = GameMode.SURVIVAL;
        }
        return snap;
    }

    private static List<ItemStack> toSerializableList(ItemStack[] itens) {
        List<ItemStack> lista = new ArrayList<>();
        for (ItemStack item : itens) {
            lista.add(item != null ? item : new ItemStack(org.bukkit.Material.AIR));
        }
        return lista;
    }

    private static ItemStack[] fromSerializableList(List<?> lista) {
        if (lista == null) return new ItemStack[0];
        ItemStack[] itens = new ItemStack[lista.size()];
        for (int i = 0; i < lista.size(); i++) {
            Object o = lista.get(i);
            itens[i] = (o instanceof ItemStack) ? (ItemStack) o : new ItemStack(org.bukkit.Material.AIR);
        }
        return itens;
    }
}
