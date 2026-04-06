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

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("§a[HumanAngel] Versão 2.0 - Sistema Completo Ativado!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        switch (cmd.getName().toLowerCase()) {
            case "lista": //
                p.sendMessage("§b--- COMANDOS HUMANANGEL ---");
                p.sendMessage("§fGeral: /home, /sethome, /tpa, /tpaccept, /spawn, /chapeu, /lixeira, /perfil, /morte, /compactar, /luz");
                if (p.hasPermission("humanangel.admin")) {
                    p.sendMessage("§eAdmin: /modo, /control, /clearlag, /corrigir, /mudarip, /anuncio, /aviso, /congelar, /zueira, /avisos");
                }
                break;

            case "spawn": //
                p.teleport(p.getWorld().getSpawnLocation());
                p.sendMessage("§aTeleportado ao Spawn!");
                break;

            case "sethome": //
                String nomeSet = (args.length > 0) ? args[0].toLowerCase() : "home";
                salvarHome(p, nomeSet);
                p.sendMessage("§aHome '" + nomeSet + "' definida!");
                break;

            case "home": //
                String nomeIr = (args.length > 0) ? args[0].toLowerCase() : "home";
                irHome(p, nomeIr);
                break;

            case "luz": //
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 1));
                p.sendMessage("§aVisão noturna ativada!");
                break;

            case "control": //
                if (p.hasPermission("humanangel.admin")) abrirMenuControl(p);
                break;

            case "anuncio": //
                if (args.length > 0) {
                    String msg = String.join(" ", args).replace("&", "§");
                    for (Player all : Bukkit.getOnlinePlayers()) all.sendTitle("§6§lAVISO", msg, 10, 70, 20);
                }
                break;
                
            case "morte": //
                p.setHealth(0);
                break;
        }
        return true;
    }

    private void salvarHome(Player p, String nome) {
        String path = "homes." + p.getUniqueId() + "." + nome;
        getConfig().set(path + ".world", p.getLocation().getWorld().getName());
        getConfig().set(path + ".x", p.getLocation().getX());
        getConfig().set(path + ".y", p.getLocation().getY());
        getConfig().set(path + ".z", p.getLocation().getZ());
        saveConfig();
    }

    private void irHome(Player p, String nome) {
        String path = "homes." + p.getUniqueId() + "." + nome;
        if (!getConfig().contains(path)) {
            p.sendMessage("§cHome não encontrada!");
            return;
        }
        World w = Bukkit.getWorld(getConfig().getString(path + ".world"));
        p.teleport(new Location(w, getConfig().getDouble(path + ".x"), getConfig().getDouble(path + ".y"), getConfig().getDouble(path + ".z")));
    }

    public void abrirMenuControl(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8Controle de Jogadores");
        for (Player online : Bukkit.getOnlinePlayers()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(online);
            meta.setDisplayName("§e" + online.getName());
            head.setItemMeta(meta);
            inv.addItem(head);
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void aoClicar(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§8Controle de Jogadores")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            Player pAlvo = Bukkit.getPlayer(e.getCurrentItem().getItemMeta().getDisplayName().replace("§e", ""));
            if (pAlvo != null) ((Player)e.getWhoClicked()).teleport(pAlvo);
        }
    }
}
