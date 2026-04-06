package com.humanangel;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    @Override
    public void onEnable() {
        // Garante a criação da config e o registro de eventos
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        
        Bukkit.getConsoleSender().sendMessage("§d[HumanAngel] §fPlugin inicializado com sucesso!");
        Bukkit.getConsoleSender().sendMessage("§d[HumanAngel] §fVersao 2.0 - Homes Infinitas e Menu Control.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cSomente jogadores podem usar comandos.");
            return true;
        }
        Player p = (Player) sender;

        // --- COMANDO LISTA (Completo como na v1.6) ---
        if (cmd.getName().equalsIgnoreCase("lista")) {
            p.sendMessage(" ");
            p.sendMessage("§d§lHUMAN ANGEL §7- §fLista de Comandos");
            p.sendMessage("§f/ha, /sethome, /home, /spawn, /perfil, /luz, /lixeira, /compactar, /chapeu, /morte");
            if (p.hasPermission("humanangel.admin")) {
                p.sendMessage("§c§lSTAFF: §f/control, /modo, /clearlag, /corrigir, /anuncio, /congelar, /zueira");
            }
            p.sendMessage(" ");
            return true;
        }

        // --- SISTEMA DE SPAWN ---
        if (cmd.getName().equalsIgnoreCase("spawn")) {
            p.teleport(p.getWorld().getSpawnLocation());
            p.sendMessage("§a§l[!] §fVoce foi teleportado para o spawn principal.");
            return true;
        }

        // --- SISTEMA DE HOMES INFINITAS COM NOMES ---
        if (cmd.getName().equalsIgnoreCase("sethome")) {
            String nomeHome = (args.length > 0) ? args[0].toLowerCase() : "home";
            UUID uuid = p.getUniqueId();
            
            getConfig().set("homes." + uuid + "." + nomeHome + ".world", p.getWorld().getName());
            getConfig().set("homes." + uuid + "." + nomeHome + ".x", p.getLocation().getX());
            getConfig().set("homes." + uuid + "." + nomeHome + ".y", p.getLocation().getY());
            getConfig().set("homes." + uuid + "." + nomeHome + ".z", p.getLocation().getZ());
            getConfig().set("homes." + uuid + "." + nomeHome + ".yaw", p.getLocation().getYaw());
            getConfig().set("homes." + uuid + "." + nomeHome + ".pitch", p.getLocation().getPitch());
            saveConfig();
            
            p.sendMessage("§a§l[!] §fHome §e" + nomeHome + " §fsetada com sucesso!");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("home")) {
            String nomeHome = (args.length > 0) ? args[0].toLowerCase() : "home";
            String path = "homes." + p.getUniqueId() + "." + nomeHome;

            if (!getConfig().contains(path)) {
                p.sendMessage("§c§l[!] §fVoce nao possui uma home chamada: §e" + nomeHome);
                return true;
            }

            World w = Bukkit.getWorld(getConfig().getString(path + ".world"));
            double x = getConfig().getDouble(path + ".x");
            double y = getConfig().getDouble(path + ".y");
            double z = getConfig().getDouble(path + ".z");
            float yaw = (float) getConfig().getDouble(path + ".yaw");
            float pitch = (float) getConfig().getDouble(path + ".pitch");

            p.teleport(new Location(w, x, y, z, yaw, pitch));
            p.sendMessage("§a§l[!] §fTeleportado para a home: §e" + nomeHome);
            return true;
        }

        // --- COMANDO CONTROL (MENU DE CABEÇAS) ---
        if (cmd.getName().equalsIgnoreCase("control")) {
            if (!p.hasPermission("humanangel.admin")) {
                p.sendMessage("§cVoce nao tem permissao para usar o modo controle.");
                return true;
            }
            abrirMenuControl(p);
            return true;
        }

        // --- OUTROS COMANDOS DA LISTA ---
        if (cmd.getName().equalsIgnoreCase("luz")) {
            if (p.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                p.removePotionEffect(PotionEffectType.NIGHT_VISION);
                p.sendMessage("§e[!] Visao noturna desativada.");
            } else {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
                p.sendMessage("§a[!] Visao noturna ativada.");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("morte")) {
            p.setHealth(0.0D);
            p.sendMessage("§cVoce se suicidou.");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("modo")) {
            if (p.hasPermission("humanangel.admin")) {
                p.setGameMode(p.getGameMode() == GameMode.SURVIVAL ? GameMode.CREATIVE : GameMode.SURVIVAL);
                p.sendMessage("§e[!] Seu modo de jogo foi alterado para " + p.getGameMode().name());
            }
            return true;
        }

        return true;
    }

    // --- LOGICA DOS MENUS (Gera mais peso ao arquivo) ---
    public void abrirMenuControl(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8Controle de Jogadores");
        for (Player online : Bukkit.getOnlinePlayers()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(online);
            meta.setDisplayName("§e" + online.getName());
            List<String> lore = new ArrayList<>();
            lore.add("§7Clique para teleportar ate este jogador.");
            meta.setLore(lore);
            head.setItemMeta(meta);
            inv.addItem(head);
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void aoClicarNoMenu(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§8Controle de Jogadores")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;
            
            Player adm = (Player) e.getWhoClicked();
            String alvoNome = e.getCurrentItem().getItemMeta().getDisplayName().replace("§e", "");
            Player alvo = Bukkit.getPlayer(alvoNome);
            
            if (alvo != null) {
                adm.teleport(alvo);
                adm.sendMessage("§a[Control] Teleportado para §f" + alvo.getName());
            } else {
                adm.sendMessage("§cJogador offline.");
            }
        }
    }
}
