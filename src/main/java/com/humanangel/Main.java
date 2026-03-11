package com.humanangel;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public class Main extends JavaPlugin implements CommandExecutor, Listener {

    private Map<UUID, Location> pos1 = new HashMap<>(), pos2 = new HashMap<>();
    private Map<UUID, Location> homes = new HashMap<>();
    private Map<UUID, UUID> tpaRequests = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        // Registro individual para garantir que o Bukkit reconheça todos
        String[] commands = {"modo", "sc", "home", "sethome", "spawn", "set", "paredes", "pos1", "pos2", "angelwand", "control", "tpa", "tpaccept"};
        for (String s : commands) {
            getCommand(s).setExecutor(this);
        }
        Bukkit.getConsoleSender().sendMessage("§a[HumanAngel] Plugin Ativado com Sucesso!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String c = cmd.getName().toLowerCase();

        // Sistema de permissão simplificado
        if (!p.isOp() && !c.equals("home") && !c.equals("sethome") && !c.equals("spawn") && !c.equals("tpa") && !c.equals("tpaccept")) {
            p.sendMessage("§cVocê precisa ser OP para usar este comando.");
            return true;
        }

        switch (c) {
            case "sc":
                if (args.length == 0) {
                    p.sendMessage("§cUse: /sc <mensagem>");
                } else {
                    String msg = String.join(" ", args);
                    Bukkit.broadcast("§d§l[STAFF] §f" + p.getName() + ": §7" + msg, Server.BROADCAST_CHANNEL_ADMINISTRATIVE);
                }
                break;

            case "modo":
                if (args.length == 0) {
                    p.sendMessage("§eUse: /modo <c/s/a/sp>");
                    return true;
                }
                String m = args[0].toLowerCase();
                if (m.equals("c")) p.setGameMode(GameMode.CREATIVE);
                else if (m.equals("s")) p.setGameMode(GameMode.SURVIVAL);
                else if (m.equals("a")) p.setGameMode(GameMode.ADVENTURE);
                else if (m.equals("sp")) p.setGameMode(GameMode.SPECTATOR);
                p.sendMessage("§aModo alterado!");
                break;

            case "pos1":
                pos1.put(p.getUniqueId(), p.getLocation());
                p.sendMessage("§bPosição 1 definida!");
                break;

            case "pos2":
                pos2.put(p.getUniqueId(), p.getLocation());
                p.sendMessage("§dPosição 2 definida!");
                break;

            case "set":
                fill(p, args, false);
                break;

            case "paredes":
                fill(p, args, true);
                break;

            case "sethome":
                homes.put(p.getUniqueId(), p.getLocation());
                p.sendMessage("§aHome definida!");
                break;

            case "home":
                if (homes.containsKey(p.getUniqueId())) p.teleport(homes.get(p.getUniqueId()));
                else p.sendMessage("§cDefina uma home com /sethome");
                break;

            case "spawn":
                p.teleport(p.getWorld().getSpawnLocation());
                p.sendMessage("§eTeleportado ao Spawn!");
                break;

            case "angelwand":
                ItemStack wand = new ItemStack(Material.WOODEN_AXE);
                ItemMeta meta = wand.getItemMeta();
                meta.setDisplayName("§b§lAngel Wand");
                wand.setItemMeta(meta);
                p.getInventory().addItem(wand);
                p.sendMessage("§aVocê recebeu o machado de seleção!");
                break;

            case "control":
                openControlMenu(p);
                break;
        }
        return true;
    }

    private void fill(Player p, String[] args, boolean wallsOnly) {
        if (!pos1.containsKey(p.getUniqueId()) || !pos2.containsKey(p.getUniqueId())) {
            p.sendMessage("§cDefina pos1 e pos2 primeiro!");
            return;
        }
        if (args.length == 0) {
            p.sendMessage("§cEspecifique um bloco! Ex: /set stone");
            return;
        }
        Material mat = Material.matchMaterial(args[0].toUpperCase());
        if (mat == null) {
            p.sendMessage("§cBloco inválido!");
            return;
        }

        Location l1 = pos1.get(p.getUniqueId());
        Location l2 = pos2.get(p.getUniqueId());
        
        int minX = Math.min(l1.getBlockX(), l2.getBlockX()), maxX = Math.max(l1.getBlockX(), l2.getBlockX());
        int minY = Math.min(l1.getBlockY(), l2.getBlockY()), maxY = Math.max(l1.getBlockY(), l2.getBlockY());
        int minZ = Math.min(l1.getBlockZ(), l2.getBlockZ()), maxZ = Math.max(l1.getBlockZ(), l2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (wallsOnly) {
                        if (x != minX && x != maxX && z != minZ && z != maxZ) continue;
                    }
                    p.getWorld().getBlockAt(x, y, z).setType(mat);
                }
            }
        }
        p.sendMessage("§aOperação concluída!");
    }

    private void openControlMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§0Controle de Jogadores");
        for (Player online : Bukkit.getOnlinePlayers()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta m = head.getItemMeta();
            m.setDisplayName("§e" + online.getName());
            head.setItemMeta(m);
            inv.addItem(head);
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§0Controle de Jogadores")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;
            Player target = Bukkit.getPlayer(ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()));
            if (target != null) e.getWhoClicked().teleport(target.getLocation());
        }
    }

    @EventHandler
    public void onWand(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (p.getInventory().getItemInMainHand().getType() == Material.WOODEN_AXE && p.isOp()) {
            if (e.getClickedBlock() == null) return;
            e.setCancelled(true);
            if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                pos1.put(p.getUniqueId(), e.getClickedBlock().getLocation());
                p.sendMessage("§bPosição 1 (Wand)!");
            } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                pos2.put(p.getUniqueId(), e.getClickedBlock().getLocation());
                p.sendMessage("§dPosição 2 (Wand)!");
            }
        }
    }
}
