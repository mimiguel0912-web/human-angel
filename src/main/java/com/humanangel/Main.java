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
    private String palavraCensurada = ""; // Alterado para o novo sistema de zueira

    @Override
    public void onEnable() {
        saveDefaultConfig();
        dadosFile = new File(getDataFolder(), "dados.yml");
        if (!dadosFile.exists()) saveResource("dados.yml", false);
        dados = YamlConfiguration.loadConfiguration(dadosFile);
        
        getServer().getPluginManager().registerEvents(this, this);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            List<String> avisos = dados.getStringList("avisos");
            if (avisos != null && !avisos.isEmpty()) {
                Bukkit.broadcastMessage("§6§l[SISTEMA] §f" + avisos.get(new Random().nextInt(avisos.size())).replace("&", "§"));
            }
        }, 0L, 36000L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String c = cmd.getName().toLowerCase();

        // COMANDOS GERAIS (NÃO ALTERADOS)
        if (c.equals("lista")) {
            p.sendMessage("§d§lHUMAN ANGEL §7- §fComandos");
            p.sendMessage("§f/home, /sethome, /spawn, /perfil, /luz, /lixeira, /compactar, /chapeu, /morte");
            if (p.hasPermission("humanangel.admin")) p.sendMessage("§c§lSTAFF: §f/control, /modo, /clearlag, /corrigir, /anuncio, /zueira");
            return true;
        }
        if (c.equals("spawn")) { p.teleport(p.getWorld().getSpawnLocation()); return true; }
        if (c.equals("luz")) { p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 1)); return true; }
        if (c.equals("chapeu")) { p.getInventory().setHelmet(p.getInventory().getItemInMainHand()); return true; }
        if (c.equals("lixeira")) { p.openInventory(Bukkit.createInventory(null, 36, "§8Lixeira")); return true; }
        if (c.equals("morte")) { p.setHealth(0); return true; }

        // --- EDITADO: HOMES INFINITAS ---
        if (c.equals("sethome")) {
            String nome = (args.length > 0) ? args[0].toLowerCase() : "home";
            getConfig().set("homes." + p.getUniqueId() + "." + nome, p.getLocation());
            saveConfig();
            p.sendMessage("§a[!] Home '§e" + nome + "§a' definida.");
            return true;
        }
        if (c.equals("home")) {
            String nome = (args.length > 0) ? args[0].toLowerCase() : "home";
            Location loc = getConfig().getLocation("homes." + p.getUniqueId() + "." + nome);
            if (loc != null) p.teleport(loc); else p.sendMessage("§c[!] Home nao encontrada.");
            return true;
        }

        // --- EDITADO: CONTROL COM MENU ---
        if (c.equals("control") && p.hasPermission("humanangel.admin")) {
            Inventory inv = Bukkit.createInventory(null, 54, "§8Controle de Jogadores");
            for (Player online : Bukkit.getOnlinePlayers()) {
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta m = (SkullMeta) head.getItemMeta();
                m.setOwningPlayer(online); m.setDisplayName("§e" + online.getName());
                head.setItemMeta(m); inv.addItem(head);
            }
            p.openInventory(inv);
            return true;
        }

        // --- EDITADO: ZUEIRA DINAMICA ---
        if (c.equals("zueira") && p.hasPermission("humanangel.admin")) {
            if (args.length > 0) {
                palavraCensurada = args[0].toLowerCase();
                p.sendMessage("§e[!] Agora a palavra '§6" + palavraCensurada + "§e' sera censurada.");
            } else {
                palavraCensurada = "";
                p.sendMessage("§c[!] Filtro de zueira desativado.");
            }
            return true;
        }
        return true;
    }

    @EventHandler
    public void aoClicar(InventoryClickEvent e) {
        if (e.getInventory() == null || e.getCurrentItem() == null) return;
        String title = e.getView().getTitle();
        if (title.equals("§8Controle de Jogadores")) {
            e.setCancelled(true);
            Player alvo = Bukkit.getPlayer(ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()));
            if (alvo != null) {
                Inventory inv = Bukkit.createInventory(null, 27, "§8Gerenciar: " + alvo.getName());
                inv.setItem(10, criarItem(Material.COMPASS, "§aTeleportar"));
                inv.setItem(12, criarItem(Material.PAPER, "§bVer IP"));
                inv.setItem(14, criarItem(Material.BARRIER, "§cBanir"));
                inv.setItem(16, criarItem(Material.BEDROCK, "§6Homes"));
                e.getWhoClicked().openInventory(inv);
            }
        } else if (title.contains("§8Gerenciar:")) {
            e.setCancelled(true);
            Player adm = (Player) e.getWhoClicked();
            Player alvo = Bukkit.getPlayer(title.replace("§8Gerenciar: ", ""));
            if (alvo == null) return;
            if (e.getRawSlot() == 10) adm.teleport(alvo);
            if (e.getRawSlot() == 12) adm.sendMessage("§bIP: §f" + alvo.getAddress().getHostString());
            if (e.getRawSlot() == 14) alvo.kickPlayer("§cExpulso.");
            if (e.getRawSlot() == 16) {
                adm.sendMessage("§6Homes:");
                if (getConfig().contains("homes." + alvo.getUniqueId())) {
                    for (String key : getConfig().getConfigurationSection("homes." + alvo.getUniqueId()).getKeys(false))
                        adm.sendMessage("§7- " + key);
                }
            }
        }
    }

    private ItemStack criarItem(Material mat, String nome) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta(); m.setDisplayName(nome); i.setItemMeta(m);
        return i;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (!palavraCensurada.isEmpty() && e.getMessage().toLowerCase().contains(palavraCensurada)) {
            e.setMessage("§dEu amo esse servidor! ❤");
        }
    }
}
