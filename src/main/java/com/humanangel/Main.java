package com.humanangel;

import me.neznamy.tab.api.TabAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<UUID, UUID> tpaRequests = new HashMap<>(); // Quem pediu, Para quem

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("ha").setExecutor(this);
        getCommand("sethome").setExecutor(this);
        getCommand("home").setExecutor(this);
        getCommand("anuncio").setExecutor(this);
        // ... (Registre os outros comandos aqui conforme o plugin.yml)
        getLogger().info("HumanAngel V2 - MEMORIZADO E ATIVADO!");
    }

    // PROTEÇÃO DE TÍTULO (NÃO DEIXA SUMIR)
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        // Se o cara for um Human Angel (vamos supor que salvamos na config)
        if (getConfig().contains("status." + p.getUniqueId()) && getConfig().getString("status." + p.getUniqueId()).equals("ANGEL")) {
            String tag = "§d§lHUMAN ANGEL §f";
            
            for (int delay : new int[]{40, 100, 200}) {
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (p.isOnline()) {
                        p.setDisplayName(tag + p.getName());
                        p.setPlayerListName(tag + p.getName());
                        if (Bukkit.getPluginManager().isPluginEnabled("TAB")) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player " + p.getName() + " prefix " + tag);
                        }
                    }
                }, delay);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        // SISTEMA DE HOMES INFINITAS COM NOME
        if (cmd.getName().equalsIgnoreCase("sethome")) {
            String nomeHome = (args.length > 0) ? args[0].toLowerCase() : "home";
            String path = "homes." + p.getUniqueId() + "." + nomeHome;
            
            getConfig().set(path + ".world", p.getLocation().getWorld().getName());
            getConfig().set(path + ".x", p.getLocation().getX());
            getConfig().set(path + ".y", p.getLocation().getY());
            getConfig().set(path + ".z", p.getLocation().getZ());
            saveConfig();
            
            p.sendMessage("§aHome '" + nomeHome + "' definida com sucesso!");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("home")) {
            String nomeHome = (args.length > 0) ? args[0].toLowerCase() : "home";
            String path = "homes." + p.getUniqueId() + "." + nomeHome;

            if (!getConfig().contains(path)) {
                p.sendMessage("§cHome '" + nomeHome + "' nao encontrada!");
                return true;
            }

            Location loc = new Location(
                Bukkit.getWorld(getConfig().getString(path + ".world")),
                getConfig().getDouble(path + ".x"),
                getConfig().getDouble(path + ".y"),
                getConfig().getDouble(path + ".z")
            );
            p.teleport(loc);
            p.sendMessage("§aTeleportado para home: " + nomeHome);
            return true;
        }

        // COMANDO DE ANÚNCIO (ADM)
        if (cmd.getName().equalsIgnoreCase("anuncio")) {
            if (!p.hasPermission("humanangel.adm")) return true;
            String msg = String.join(" ", args).replace("&", "§");
            for (Player all : Bukkit.getOnlinePlayers()) {
                all.sendTitle("§6§lAVISO", msg, 10, 70, 20);
            }
            return true;
        }

        // LIXEIRA
        if (cmd.getName().equalsIgnoreCase("lixeira")) {
            Inventory inv = Bukkit.createInventory(null, 36, "§8Lixeira");
            p.openInventory(inv);
            return true;
        }

        // LUZ (VISÃO NOTURNA)
        if (cmd.getName().equalsIgnoreCase("luz")) {
            if (p.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                p.removePotionEffect(PotionEffectType.NIGHT_VISION);
                p.sendMessage("§eLuz desativada!");
            } else {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 99999, 1));
                p.sendMessage("§aLuz ativada!");
            }
            return true;
        }

        return true;
    }
}
