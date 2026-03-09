package com.humanangel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    private HashMap<UUID, Integer> teleportando = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("sc").setExecutor(this);
        getCommand("spawn").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        
        // Loop das frases de zoeira
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            String prefixo = getConfig().getString("Zoeira.prefixo");
            java.util.List<String> frases = getConfig().getStringList("Zoeira.frases");
            String fraseAleatoria = frases.get(new java.util.Random().nextInt(frases.size()));
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', prefixo + fraseAleatoria));
        }, 0L, getConfig().getInt("Zoeira.tempo-segundos") * 20L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("sc")) {
            if (args.length == 0) return false;
            String msg = String.join(" ", args);
            String formato = ChatColor.translateAlternateColorCodes('&', "&d[STAFF] &b" + p.getName() + ": &f" + msg);
            Bukkit.getOnlinePlayers().stream().filter(s -> s.hasPermission("humanangel.staff")).forEach(s -> s.sendMessage(formato));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("spawn")) {
            p.sendMessage(ChatColor.YELLOW + "Aguarde 5 segundos parado...");
            int task = Bukkit.getScheduler().runTaskLater(this, () -> {
                p.teleport(p.getWorld().getSpawnLocation());
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                p.sendMessage(ChatColor.GREEN + "Teleportado!");
                teleportando.remove(p.getUniqueId());
            }, 100L).getTaskId();
            teleportando.put(p.getUniqueId(), task);
            return true;
        }
        return false;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (teleportando.containsKey(e.getPlayer().getUniqueId())) {
            if (e.getFrom().getBlockX() != e.getTo().getBlockX() || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
                Bukkit.getScheduler().cancelTask(teleportando.get(e.getPlayer().getUniqueId()));
                teleportando.remove(e.getPlayer().getUniqueId());
                e.getPlayer().sendMessage(ChatColor.RED + "Você se moveu! Teleporte cancelado.");
            }
        }
    }
}
