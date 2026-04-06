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
import org.bukkit.potion.*;
import java.io.*;
import java.util.*;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    private File dadosFile;
    private FileConfiguration dados;
    private boolean zueiraAtiva = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        // Manutenção do sistema de dados original
        dadosFile = new File(getDataFolder(), "dados.yml");
        if (!dadosFile.exists()) saveResource("dados.yml", false);
        dados = YamlConfiguration.loadConfiguration(dadosFile);
        
        getServer().getPluginManager().registerEvents(this, this);

        // Sistema de mensagens automáticas (Mantido da v1.6)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (dados.contains("avisos")) {
                List<String> avisos = dados.getStringList("avisos");
                if (!avisos.isEmpty()) {
                    String msg = avisos.get(new Random().nextInt(avisos.size()));
                    Bukkit.broadcastMessage("§6§l[SISTEMA] §f" + msg.replace("&", "§"));
                }
            }
        }, 0L, 36000L);
        
        Bukkit.getConsoleSender().sendMessage("§d[HumanAngel] Versao 2.0 carregada com sucesso.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String c = cmd.getName().toLowerCase();

        // --- COMANDOS ORIGINAIS (NÃO ALTERADOS) ---
        if (c.equals("lista")) {
            p.sendMessage(" ");
            p.sendMessage("§d§lHUMAN ANGEL §7- §fLista de Comandos do Servidor");
            p.sendMessage("§f/ha, /sethome, /home, /spawn, /perfil, /luz, /lixeira, /compactar, /chapeu, /morte");
            if (p.hasPermission("humanangel.admin")) {
                p.sendMessage("§c§lSTAFF: §f/control, /modo, /clearlag, /corrigir, /anuncio, /congelar, /zueira");
            }
            p.sendMessage(" ");
            return true;
        }

        if (c.equals("spawn")) { p.teleport(p.getWorld().getSpawnLocation()); p.sendMessage("§a[!] Teleportado ao spawn principal."); return true; }
        if (c.equals("luz")) { p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 1)); p.sendMessage("§a[!] Visao noturna ativada."); return true; }
        if (c.equals("chapeu")) { p.getInventory().setHelmet(p.getInventory().getItemInMainHand()); return true; }
        if (c.equals("lixeira")) { p.openInventory(Bukkit.createInventory(null, 36, "§8Lixeira de Itens")); return true; }
        if (c.equals("morte")) { p.setHealth(0.0D); return true; }
        if (c.equals("modo") && p.hasPermission("humanangel.admin")) { p.setGameMode(p.getGameMode() == GameMode.SURVIVAL ? GameMode.CREATIVE : GameMode.SURVIVAL); return true; }

        // --- HOMES INFINITAS (EDITADO) ---
        if (c.equals("sethome")) {
            String nome = (args.length > 0) ? args[0].toLowerCase() : "home";
            getConfig().set("homes." + p.getUniqueId() + "." + nome, p.getLocation());
            saveConfig();
            p.sendMessage("§a[!] Voce definiu a home: §e" + nome);
            return true;
        }

        if (c.equals("home")) {
            String nome = (args.length > 0) ? args[0].toLowerCase() : "home";
            Location loc = getConfig().getLocation("homes." + p.getUniqueId() + "." + nome);
            if (loc != null) {
                p.teleport(loc);
                p.sendMessage("§a[!] Teleportado para a home: §e" + nome);
            } else {
                p.sendMessage("§c[!] Home '§e" + nome + "§c' nao encontrada.");
            }
            return true;
        }

        // --- CONTROL AVANÇADO (EDITADO) ---
        if (c.equals("control") && p.hasPermission("humanangel.admin")) {
            abrirMenuPrincipal(p);
            return true;
        }

        if (c.equals("zueira") && p.hasPermission("humanangel.admin")) {
            zueiraAtiva = !zueiraAtiva;
            p.sendMessage("§e[!] Filtro Zueira: " + (zueiraAtiva ? "§aLigado" : "§cDesligado"));
            return true;
        }

        return true;
    }

    // --- SISTEMA DE MENUS (CONTROL) ---
    public void abrirMenuPrincipal(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8Controle de Jogadores");
        for (Player online : Bukkit.getOnlinePlayers()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta m = (SkullMeta) head.getItemMeta();
            m.setOwningPlayer(online);
            m.setDisplayName("§e" + online.getName());
            head.setItemMeta(m);
            inv.addItem(head);
        }
        p.openInventory(inv);
    }

    public void abrirMenuAcoes(Player adm, Player alvo) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Gerenciar: " + alvo.getName());
        inv.setItem(10, criarItem(Material.COMPASS, "§aIr ate o jogador"));
        inv.setItem(12, criarItem(Material.PAPER, "§bVer Perfil e IP"));
        inv.setItem(14, criarItem(Material.BARRIER, "§cBanir Jogador"));
        inv.setItem(16, criarItem(Material.BEDROCK, "§6Ver Homes do Player"));
        adm.openInventory(inv);
    }

    private ItemStack criarItem(Material mat, String nome, String lore) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(nome);
        List<String> l = new ArrayList<>(); l.add(lore); m.setLore(l);
        i.setItemMeta(m);
        return i;
    }

    @EventHandler
    public void aoClicar(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§8Controle de Jogadores")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            Player alvo = Bukkit.getPlayer(ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()));
            if (alvo != null) abrirMenuAcoes((Player) e.getWhoClicked(), alvo);
        } else if (e.getView().getTitle().contains("§8Gerenciar:")) {
            e.setCancelled(true);
            Player adm = (Player) e.getWhoClicked();
            Player alvo = Bukkit.getPlayer(e.getView().getTitle().replace("§8Gerenciar: ", ""));
            if (alvo == null) return;

            if (e.getRawSlot() == 10) adm.teleport(alvo);
            if (e.getRawSlot() == 12) adm.sendMessage("§b[INFO] IP de " + alvo.getName() + ": §f" + alvo.getAddress().getHostString());
            if (e.getRawSlot() == 14) alvo.kickPlayer("§cExpulso por um Administrador.");
            if (e.getRawSlot() == 16) {
                adm.sendMessage("§6[Homes] Lista de homes de " + alvo.getName() + ":");
                if (getConfig().contains("homes." + alvo.getUniqueId())) {
                    for (String key : getConfig().getConfigurationSection("homes." + alvo.getUniqueId()).getKeys(false)) {
                        adm.sendMessage("§7- " + key);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (zueiraAtiva && (e.getMessage().toLowerCase().contains("lixo") || e.getMessage().toLowerCase().contains("hack"))) {
            e.setMessage("§dEu amo esse servidor! ❤");
        }
    }
}
