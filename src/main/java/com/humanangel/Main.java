package com.humanangel;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public class Main extends JavaPlugin implements CommandExecutor, Listener {

    private Map<UUID, Location> pos1 = new HashMap<>(), pos2 = new HashMap<>();
    private Map<UUID, Location> homes = new HashMap<>();
    private Map<UUID, UUID> tpaRequests = new HashMap<>();
    private List<UUID> scActive = new ArrayList<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        String[] cmds = {"modo", "escala", "sc", "ha", "home", "sethome", "pvp", "spawn", "tpa", "tpaccept", "tpadeny", "lista", "set", "paredes", "pos1", "pos2", "angelwand"};
        for (String s : cmds) {
            PluginCommand pc = getCommand(s);
            if (pc != null) pc.setExecutor(this);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String c = cmd.getName().toLowerCase();

        if (Arrays.asList("modo", "escala", "sc", "ha", "set", "paredes", "pos1", "pos2", "angelwand").contains(c)) {
            if (!p.isOp()) { p.sendMessage("§cSem permissão!"); return true; }
        }

        switch (c) {
            case "ha": p.sendMessage("§b§lHUMAN ANGEL §fv1.3.5"); break;
            case "pos1": 
                pos1.put(p.getUniqueId(), p.getLocation()); 
                p.sendMessage("§b§l[WAND] §fPosição 1 marcada!");
                break;
            case "pos2": 
                pos2.put(p.getUniqueId(), p.getLocation()); 
                p.sendMessage("§d§l[WAND] §fPosição 2 marcada!");
                break;
            case "angelwand":
                p.getInventory().addItem(new ItemStack(Material.TRIDENT));
                p.sendMessage("§bTridente entregue!"); break;
            case "set": fill(p, args, false); break;
            case "paredes": fill(p, args, true); break;
            case "lista":
                p.sendMessage("§b§l--- LISTA ---");
                p.sendMessage("§f/home, /sethome, /spawn, /pvp, /tpa, /tpaccept, /tpadeny");
                if (p.isOp()) p.sendMessage("§e/modo, /escala, /sc, /set, /paredes, /pos1, /pos2");
                break;
            case "modo":
                if (args.length > 0) {
                    p.setGameMode(args[0].equalsIgnoreCase("c") ? GameMode.CREATIVE : GameMode.SURVIVAL);
                    p.sendMessage("§aModo alterado!");
                } break;
        }
        return true;
    }

    private void fill(Player p, String[] args, boolean walls) {
        Location l1 = pos1.get(p.getUniqueId()), l2 = pos2.get(p.getUniqueId());
        if (l1 == null || l2 == null || args.length == 0) {
            p.sendMessage("§cUse /pos1 e /pos2 primeiro!");
            return;
        }
        Material mat = Material.matchMaterial(args[0].toUpperCase());
        if (mat == null) { p.sendMessage("§cBloco inválido!"); return; }

        int minX = Math.min(l1.getBlockX(), l2.getBlockX()), maxX = Math.max(l1.getBlockX(), l2.getBlockX());
        int minY = Math.min(l1.getBlockY(), l2.getBlockY()), maxY = Math.max(l1.getBlockY(), l2.getBlockY());
        int minZ = Math.min(l1.getBlockZ(), l2.getBlockZ()), maxZ = Math.max(l1.getBlockZ(), l2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (walls && (x > minX && x < maxX && z > minZ && z < maxZ)) continue;
                    p.getWorld().getBlockAt(x, y, z).setType(mat);
                }
            }
        }
        p.sendMessage("§aFeito!");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (p.getInventory().getItemInMainHand().getType() == Material.TRIDENT && p.isOp()) {
            e.setCancelled(true);
            if (e.getAction().name().contains("LEFT")) {
                pos1.put(p.getUniqueId(), e.getClickedBlock() != null ? e.getClickedBlock().getLocation() : p.getLocation());
                p.sendMessage("§b§l[WAND] §fPosição 1 marcada!");
            } else if (e.getAction().name().contains("RIGHT")) {
                pos2.put(p.getUniqueId(), e.getClickedBlock() != null ? e.getClickedBlock().getLocation() : p.getLocation());
                p.sendMessage("§d§l[WAND] §fPosição 2 marcada!");
            }
        }
    }
}
