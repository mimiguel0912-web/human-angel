package com.humanangel;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.util.*;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        setupStorage();
        getServer().getPluginManager().registerEvents(this, this);
        
        // Comandos que aparecem no seu log de Bytecode
        String[] comandos = {"home", "sethome", "spawn", "control", "lista", "zueira", "luz", "morte", "compactar"};
        for (String cmd : comandos) {
            getCommand(cmd).setExecutor(this);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String c = cmd.getName().toLowerCase();

        // --- HOMES INFINITAS ---
        if (c.equals("sethome")) {
            String nome = (args.length > 0) ? args[0].toLowerCase() : "home";
            dataConfig.set("homes." + p.getUniqueId() + "." + nome, p.getLocation());
            saveData();
            p.sendMessage("§a[!] Home '§e" + nome + "§a' salva no dados.yml!");
            return true;
        }

        if (c.equals("home")) {
            String nome = (args.length > 0) ? args[0].toLowerCase() : "home";
            Location loc = dataConfig.getLocation("homes." + p.getUniqueId() + "." + nome);
            if (loc != null) {
                p.teleport(loc);
                p.sendMessage("§a[!] Teleportado!");
            } else {
                p.sendMessage("§c[!] Home nao encontrada.");
            }
            return true;
        }

        // --- ZUEIRA (ADICIONAR PALAVRA) ---
        if (c.equals("zueira") && p.hasPermission("humanangel.admin")) {
            if (args.length == 0) {
                p.sendMessage("§cUse: /zueira (palavra)");
                return true;
            }
            List<String> filtro = dataConfig.getStringList("filtroZueira");
            String palavra = args[0].toLowerCase();
            
            if (!filtro.contains(palavra)) {
                filtro.add(palavra);
                dataConfig.set("filtroZueira", filtro);
                saveData();
                p.sendMessage("§a[!] Palavra '§e" + palavra + "§a' adicionada ao dados.yml!");
            } else {
                p.sendMessage("§e[!] Essa palavra ja esta no filtro.");
            }
            return true;
        }

        // --- CONTROL MENU ---
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

        // Comandos Simples
        if (c.equals("spawn")) { p.teleport(p.getWorld().getSpawnLocation()); return true; }
        if (c.equals("morte")) { p.setHealth(0); return true; }

        return true;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        List<String> filtro = dataConfig.getStringList("filtroZueira");
        String msg = e.getMessage().toLowerCase();
        
        for (String palavra : filtro) {
            if (msg.contains(palavra)) {
                e.setMessage("§dEu amo esse servidor! ❤");
                break;
            }
        }
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§8Controle de Jogadores")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            Player alvo = Bukkit.getPlayer(ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()));
            if (alvo != null) {
                Player adm = (Player) e.getWhoClicked();
                adm.teleport(alvo);
                adm.sendMessage("§a[!] Teleportado para " + alvo.getName());
            }
        }
    }

    private void setupStorage() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        dataFile = new File(getDataFolder(), "dados.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveData() {
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }
}
