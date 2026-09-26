package com.semianarquia.minieventos;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.NumberConversions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acesso tipado ao config.yml. As listas de spawns e o spawn principal
 * sao gravados de volta no arquivo quando alterados por comandos em jogo.
 */
public class ConfigManager {

    private final MiniEventosPlugin plugin;

    public ConfigManager(MiniEventosPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
    }

    // ---------------------------------------------------------------
    // Gerais
    // ---------------------------------------------------------------

    public String getMundoEventoNome() {
        return plugin.getConfig().getString("mundo-evento", "mundo_eventos");
    }

    public void setMundoEventoNome(String nome) {
        plugin.getConfig().set("mundo-evento", nome);
        plugin.saveConfig();
    }

    public World getMundoEvento() {
        return Bukkit.getWorld(getMundoEventoNome());
    }

    public int getIntervaloEventoMinutos() {
        return Math.max(1, plugin.getConfig().getInt("intervalo-evento-minutos", 60));
    }

    public int getTempoInscricaoMinutos() {
        return Math.max(1, plugin.getConfig().getInt("tempo-inscricao-minutos", 3));
    }

    public int getMinimoJogadores() {
        return Math.max(2, plugin.getConfig().getInt("minimo-jogadores", 2));
    }

    public boolean isSpawnPrincipalDefinido() {
        return plugin.getConfig().getBoolean("spawn-principal.definido", false);
    }

    public Location getSpawnPrincipal() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("spawn-principal");
        if (sec == null || !sec.getBoolean("definido", false)) {
            return null;
        }
        return locationFromSection(sec);
    }

    public void setSpawnPrincipal(Location local) {
        ConfigurationSection sec = plugin.getConfig().createSection("spawn-principal");
        sec.set("definido", true);
        locationToSection(sec, local);
        plugin.saveConfig();
    }

    // ---------------------------------------------------------------
    // Modo Survival
    // ---------------------------------------------------------------

    public int getSurvivalPvpOffMinutos() {
        return plugin.getConfig().getInt("modo-survival.tempo-pvp-off-minutos", 10);
    }

    public int getSurvivalBussolaMinutos() {
        return plugin.getConfig().getInt("modo-survival.tempo-bussola-minutos", 20);
    }

    public int getSurvivalArenaFinalMinutos() {
        return plugin.getConfig().getInt("modo-survival.tempo-arena-final-minutos", 30);
    }

    public List<Location> getSurvivalSpawns() {
        return loadLocationList("modo-survival.spawns");
    }

    public void addSurvivalSpawn(Location local) {
        addLocationToList("modo-survival.spawns", local);
    }

    public List<Location> getSurvivalArenaSpawns() {
        return loadLocationList("modo-survival.spawns-arena-final");
    }

    public void addSurvivalArenaSpawn(Location local) {
        addLocationToList("modo-survival.spawns-arena-final", local);
    }

    // ---------------------------------------------------------------
    // Modo PvP com Kits
    // ---------------------------------------------------------------

    public int getPvpKitsPreparoSegundos() {
        return plugin.getConfig().getInt("modo-pvpkits.tempo-preparo-segundos", 10);
    }

    public double getBordaTamanhoInicial() {
        return plugin.getConfig().getDouble("modo-pvpkits.borda-tamanho-inicial", 500);
    }

    public double getBordaTamanhoFinal() {
        return plugin.getConfig().getDouble("modo-pvpkits.borda-tamanho-final", 20);
    }

    public long getBordaTempoEncolhimentoSegundos() {
        return plugin.getConfig().getLong("modo-pvpkits.borda-tempo-encolhimento-segundos", 1200);
    }

    public List<Location> getPvpKitsSpawns() {
        return loadLocationList("modo-pvpkits.spawns");
    }

    public void addPvpKitsSpawn(Location local) {
        addLocationToList("modo-pvpkits.spawns", local);
    }

    public List<ItemStack> getKitPvpKits() {
        List<ItemStack> itens = new ArrayList<>();
        List<Map<?, ?>> lista = plugin.getConfig().getMapList("modo-pvpkits.kit");
        for (Map<?, ?> mapa : lista) {
            try {
                String materialNome = String.valueOf(mapa.get("material"));
                Material material = Material.matchMaterial(materialNome);
                if (material == null) {
                    plugin.getLogger().warning("Material invalido no kit: " + materialNome);
                    continue;
                }
                int quantidade = mapa.get("quantidade") != null ? NumberConversions.toInt(mapa.get("quantidade")) : 1;
                ItemStack item = new ItemStack(material, Math.max(1, quantidade));

                Object nomeObj = mapa.get("nome");
                Object encantObj = mapa.get("encantamentos");
                if (nomeObj != null || encantObj != null) {
                    ItemMeta meta = item.getItemMeta();
                    if (nomeObj != null && meta != null) {
                        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', String.valueOf(nomeObj)));
                    }
                    item.setItemMeta(meta);
                }
                if (encantObj instanceof Map<?, ?> encantamentos) {
                    for (Map.Entry<?, ?> entrada : encantamentos.entrySet()) {
                        Enchantment ench = Enchantment.getByName(String.valueOf(entrada.getKey()));
                        if (ench == null) {
                            plugin.getLogger().warning("Encantamento invalido no kit: " + entrada.getKey());
                            continue;
                        }
                        int nivel = NumberConversions.toInt(entrada.getValue());
                        item.addUnsafeEnchantment(ench, nivel);
                    }
                }
                itens.add(item);
            } catch (Exception ex) {
                plugin.getLogger().warning("Falha ao ler um item do kit de PvP: " + ex.getMessage());
            }
        }
        return itens;
    }

    // ---------------------------------------------------------------
    // Recompensa / mensagens / comandos permitidos
    // ---------------------------------------------------------------

    public String getComandoRecompensa() {
        return plugin.getConfig().getString("recompensa.comando", "crates give %player% Eventos 1");
    }

    public String getMensagem(String chave) {
        return plugin.getConfig().getString("mensagens." + chave, "");
    }

    public List<String> getComandosPermitidosDuranteEvento() {
        return plugin.getConfig().getStringList("comandos-permitidos-durante-evento");
    }

    // ---------------------------------------------------------------
    // Helpers de (des)serializacao de Location
    // ---------------------------------------------------------------

    private Location locationFromSection(ConfigurationSection sec) {
        String mundoNome = sec.getString("mundo");
        World mundo = mundoNome != null ? Bukkit.getWorld(mundoNome) : null;
        if (mundo == null) {
            // Mundo pode ainda nao estar carregado; guarda o nome mesmo assim
            // e retorna null para quem chamou tratar com cuidado.
            return null;
        }
        double x = sec.getDouble("x");
        double y = sec.getDouble("y");
        double z = sec.getDouble("z");
        float yaw = (float) sec.getDouble("yaw");
        float pitch = (float) sec.getDouble("pitch");
        return new Location(mundo, x, y, z, yaw, pitch);
    }

    private void locationToSection(ConfigurationSection sec, Location local) {
        sec.set("mundo", local.getWorld() != null ? local.getWorld().getName() : "world");
        sec.set("x", local.getX());
        sec.set("y", local.getY());
        sec.set("z", local.getZ());
        sec.set("yaw", (double) local.getYaw());
        sec.set("pitch", (double) local.getPitch());
    }

    private List<Location> loadLocationList(String caminho) {
        List<Location> resultado = new ArrayList<>();
        List<Map<?, ?>> lista = plugin.getConfig().getMapList(caminho);
        for (Map<?, ?> mapa : lista) {
            try {
                String mundoNome = String.valueOf(mapa.get("mundo"));
                World mundo = Bukkit.getWorld(mundoNome);
                if (mundo == null) continue;
                double x = NumberConversions.toDouble(mapa.get("x"));
                double y = NumberConversions.toDouble(mapa.get("y"));
                double z = NumberConversions.toDouble(mapa.get("z"));
                float yaw = mapa.get("yaw") != null ? (float) NumberConversions.toDouble(mapa.get("yaw")) : 0f;
                float pitch = mapa.get("pitch") != null ? (float) NumberConversions.toDouble(mapa.get("pitch")) : 0f;
                resultado.add(new Location(mundo, x, y, z, yaw, pitch));
            } catch (Exception ignored) {
                // entrada invalida, ignora
            }
        }
        return resultado;
    }

    private void addLocationToList(String caminho, Location local) {
        List<Map<?, ?>> lista = new ArrayList<>(plugin.getConfig().getMapList(caminho));
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("mundo", local.getWorld() != null ? local.getWorld().getName() : "world");
        mapa.put("x", local.getX());
        mapa.put("y", local.getY());
        mapa.put("z", local.getZ());
        mapa.put("yaw", (double) local.getYaw());
        mapa.put("pitch", (double) local.getPitch());
        lista.add(mapa);
        plugin.getConfig().set(caminho, lista);
        plugin.saveConfig();
    }
}
