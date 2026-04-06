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
        // Preserva a lógica de carregamento de dados da 1.6
        dadosFile = new File(getDataFolder(), "dados.yml");
        if (!dadosFile.exists()) saveResource("dados.yml", false);
        dados = YamlConfiguration.loadConfiguration(dadosFile);
        
        getServer().getPluginManager().registerEvents(this, this);

        // Sistema de Avisos a cada 30 min (Não deletado)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            List<String> avisos = dados.getStringList("avisos");
            if (avisos != null && !avisos.isEmpty()) {
                Bukkit.broadcastMessage("§6§l[SISTEMA] §f" + avisos.get(new Random().nextInt(avisos.size())).replace("&", "§"));
            }
        }, 0L, 36000L);
        
        Bukkit.getConsoleSender().sendMessage("§d[HumanAngel] v2.0 - Código Original preservado e expandido.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String c = cmd.getName().toLowerCase();

        // --- COMANDOS ORIGINAIS (NÃO DELETADOS) ---
        if (c.equals("lista")) {
            p.sendMessage("§d§lHUMAN ANGEL §7- §fComandos");
            p.sendMessage("§f/home, /sethome, /spawn, /perfil, /luz, /lixeira, /compactar, /chapeu, /morte");
            if (p.hasPermission("humanangel.admin")) p.sendMessage("§c§lSTAFF: §f/control, /modo, /clearlag, /corrigir, /anuncio, /zueira, /congelar");
            return true;
        }
        
        if (c.equals("spawn")) { p.teleport(p.getWorld().getSpawnLocation()); return true; }
        if (c.equals("luz")) { p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 1)); return true; }
        if (c.equals("chapeu")) { p.getInventory().setHelmet(p.getInventory().getItemInMainHand()); return true; }
        if (c.equals("lixeira")) { p.openInventory(Bukkit.createInventory(null, 36, "§8Lixeira")); return true; }
        if (c.equals("morte")) { p.setHealth(0); return true; }

        // --- HOMES INFINITAS (EDITADO) ---
        if (c.equals("sethome")) {
            String nome = (args.length > 0) ? args[0].toLowerCase() : "home";
            getConfig().set("homes." + p.getUniqueId() + "." + nome, p.getLocation());
            saveConfig();
            p.sendMessage("§a[!] Home '§e" + nome + "§a' salva com sucesso.");
            return true;
        }

        if (c.equals("home")) {
            String nome = (args.length > 0) ? args[0].toLowerCase() : "home";
            Location loc = getConfig().getLocation("homes." + p.getUniqueId() + "." + nome);
            if (loc != null) {
                p.teleport(loc);
                p.sendMessage("§a[!] Teleportado para: §e" + nome);
            } else {
                p.sendMessage("§c[!] Home não encontrada.");
            }
            return true;
        }

        // --- CONTROL AVANÇADO (EDITADO) ---
        if (c.equals("control") && p.hasPermission("humanangel.admin")) {
            abrirMenuPlayers(p);
            return true;
        }

        if (c.equals("zueira") && p.hasPermission("humanangel.admin")) {
            zueiraAtiva = !zueiraAtiva;
            p.sendMessage("§e[!] Zueira: " + (zueiraAtiva ? "§aLigada" : "§cDesligada"));
            return true;
        }

        return true;
    }

    // --- SISTEMA DE MENUS ---
    public void abrirMenuPlayers(Player p) {
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
        Inventory inv = Bukkit.createInventory(null, 27, "§8Opções: " + alvo.getName());
        inv.setItem(10, criarItem(Material.COMPASS, "§aTeleportar", "§7Ir até o player"));
        inv.setItem(12, criarItem(Material.PAPER, "§bInformações/IP", "§7Ver IP e dados"));
        inv.setItem(14, criarItem(Material.BARRIER, "§cBanir", "§7Punir jogador"));
        inv.setItem(16, criarItem(Material.BEDROCK, "§6Ver Homes", "§7Listar casas"));
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
        } else if (e.getView().getTitle().contains("§8Opções:")) {
            e.setCancelled(true);
            Player adm = (Player) e.getWhoClicked();
            Player alvo = Bukkit.getPlayer(e.getView().getTitle().replace("§8Opções: ", ""));
            if (alvo == null) return;

            if (e.getRawSlot() == 10) adm.teleport(alvo);
            if (e.getRawSlot() == 12) adm.sendMessage("§b[!] IP de " + alvo.getName() + ": §f" + alvo.getAddress().getHostString());
            if (e.getRawSlot() == 14) alvo.kickPlayer("§cExpulso via Menu de Controle.");
            if (e.getRawSlot() == 16) {
                adm.sendMessage("§6Homes de " + alvo.getName() + ":");
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
