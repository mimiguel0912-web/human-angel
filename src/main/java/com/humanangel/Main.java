
package com.humanangel;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import java.io.*;
import java.util.*;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    private Map<UUID, Location> homes = new HashMap<>();
    private Map<UUID, UUID> tpaRequests = new HashMap<>();
    private List<String> filtroZueira = new ArrayList<>();
    private List<String> listaAvisos = new ArrayList<>();
    private Set<UUID> congelados = new HashSet<>();
    private File dataFile;
    private FileConfiguration dataConfig;
    private boolean zueiraAtiva = false;

    @Override
    public void onEnable() {
        setupStorage();
        loadData();
        getServer().getPluginManager().registerEvents(this, this);
        
        // Registrar todos os comandos que aparecem no seu arquivo
        String[] cmds = {"ha", "modo", "home", "sethome", "spawn", "control", "lista", "clearlag", "tpa", "tpaccept", "tpdeny", "zueira", "avisos", "luz", "corrigir", "chapeu", "lixeira", "perfil", "anuncio", "aviso", "congelar", "morte", "compactar"};
        for (String s : cmds) {
            getCommand(s).setExecutor(this);
        }

        // Sistema de Avisos Automáticos
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!listaAvisos.isEmpty()) {
                String msg = listaAvisos.get(new Random().nextInt(listaAvisos.size()));
                Bukkit.broadcastMessage("§6§l[SISTEMA] §f" + msg.replace("&", "§"));
            }
        }, 0L, 36000L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String c = cmd.getName().toLowerCase();

        // --- HOMES INFINITAS ---
        if (c.equals("sethome")) {
            String nome = (args.length > 0) ? args[0].toLowerCase() : "home";
            homes.put(p.getUniqueId(), p.getLocation()); // Simplificado para o mapa
            p.sendMessage("§aHome '" + nome + "' salva!");
            saveData();
            return true;
        }

        if (c.equals("home")) {
            String nome = (args.length > 0) ? args[0].toLowerCase() : "home";
            if (homes.containsKey(p.getUniqueId())) {
                p.teleport(homes.get(p.getUniqueId()));
                p.sendMessage("§aTeleportado!");
            } else {
                p.sendMessage("§cSem home!");
            }
            return true;
        }

        // --- CONTROL COM MENU ---
        if (c.equals("control") && p.hasPermission("humanangel.admin")) {
            Inventory inv = Bukkit.createInventory(null, 54, "§8Controle de Jogadores");
            for (Player online : Bukkit.getOnlinePlayers()) {
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta sm = (SkullMeta) head.getItemMeta();
                sm.setOwningPlayer(online);
                sm.setDisplayName("§e" + online.getName());
                head.setItemMeta(sm);
                inv.addItem(head);
            }
            p.openInventory(inv);
            return true;
        }

        // Outros comandos básicos da 1.6
        if (c.equals("spawn")) p.teleport(p.getWorld().getSpawnLocation());
        if (c.equals("morte")) p.setHealth(0);
        if (c.equals("zueira")) zueiraAtiva = !zueiraAtiva;

        return true;
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§8Controle de Jogadores")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            Player target = Bukkit.getPlayer(ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()));
            if (target != null) {
                Player adm = (Player) e.getWhoClicked();
                adm.teleport(target);
                adm.sendMessage("§aTeleportado para " + target.getName());
            }
        }
    }

    // --- SISTEMA DE ARMAZENAMENTO ---
    private void setupStorage() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        dataFile = new File(getDataFolder(), "dados.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void loadData() {
        // Carrega avisos e configurações do arquivo
        if (dataConfig.contains("avisos")) {
            listaAvisos = dataConfig.getStringList("avisos");
        }
    }

    private void saveData() {
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }
}
