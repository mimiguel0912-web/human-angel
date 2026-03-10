package com.humanangel;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public class Main extends JavaPlugin implements CommandExecutor, Listener {

    private boolean pvpEnabled = true;
    private long lastPvpToggle = 0;
    private Map<UUID, Long> inCombat = new HashMap<>();
    private List<UUID> scActive = new ArrayList<>();
    private Map<UUID, Location> pos1 = new HashMap<>(), pos2 = new HashMap<>();
    private Map<UUID, Location> homes = new HashMap<>();
    private Map<UUID, UUID> tpaRequests = new HashMap<>();
    private List<String> zoeiraMessages = new ArrayList<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        String[] cmds = {"mode", "control", "sc", "ha", "angelwand", "set", "paredes", "zoeira", "home", "sethome", "tpa", "tpaccept", "pvp", "spawn", "tamanho"};
        for (String s : cmds) {
            PluginCommand pc = getCommand(s);
            if (pc != null) pc.setExecutor(this);
        }
        
        // Loop de Avisos /zoeira a cada 2 horas
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!zoeiraMessages.isEmpty()) {
                String msg = zoeiraMessages.get(new Random().nextInt(zoeiraMessages.size()));
                Bukkit.broadcastMessage("§6§l[AVISO] §f" + ChatColor.translateAlternateColorCodes('&', msg));
            }
        }, 144000L, 144000L);

        startTasks();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String c = cmd.getName().toLowerCase();

        // Bloqueio de ADM
        if (Arrays.asList("mode", "control", "sc", "ha", "angelwand", "set", "paredes", "zoeira").contains(c)) {
            if (!p.isOp()) { p.sendMessage("§cSem permissão!"); return true; }
        }

        switch (c) {
            case "ha": p.sendMessage("§b§lHUMAN ANGEL §fv1.3 - Ativo!"); break;
            case "zoeira":
                if (args.length > 0) {
                    zoeiraMessages.add(String.join(" ", args));
                    p.sendMessage("§aAviso salvo para o sistema de 2h!");
                } break;
            case "set": fillArea(p, (args.length > 0 ? Material.matchMaterial(args[0]) : null), false); break;
            case "paredes": fillArea(p, (args.length > 0 ? Material.matchMaterial(args[0]) : null), true); break;
            case "home": p.teleport(homes.getOrDefault(p.getUniqueId(), p.getWorld().getSpawnLocation())); break;
            case "sethome": homes.put(p.getUniqueId(), p.getLocation()); p.sendMessage("§aHome salva!"); break;
            case "tpa":
                if (args.length > 0) {
                    Player t = Bukkit.getPlayer(args[0]);
                    if (t != null) { tpaRequests.put(t.getUniqueId(), p.getUniqueId()); p.sendMessage("§ePedido enviado!"); }
                } break;
            case "tpaccept":
                UUID req = tpaRequests.remove(p.getUniqueId());
                if (req != null && Bukkit.getPlayer(req) != null) Bukkit.getPlayer(req).teleport(p.getLocation());
                break;
            case "angelwand":
                ItemStack t = new ItemStack(Material.TRIDENT);
                ItemMeta m = t.getItemMeta(); m.setDisplayName("§b§lAngel Wand"); t.setItemMeta(m);
                p.getInventory().addItem(t); break;
        }
        return true;
    }

    private void fillArea(Player p, Material mat, boolean walls) {
        Location l1 = pos1.get(p.getUniqueId()), l2 = pos2.get(p.getUniqueId());
        if (l1 == null || l2 == null || mat == null) { p.sendMessage("§cUse o tridente primeiro!"); return; }
        int minX = Math.min(l1.getBlockX(), l2.getBlockX()), maxX = Math.max(l1.getBlockX(), l2.getBlockX());
        int minY = Math.min(l1.getBlockY(), l2.getBlockY()), maxY = Math.max(l1.getBlockY(), l2.getBlockY());
        int minZ = Math.min(l1.getBlockZ(), l2.getBlockZ()), maxZ = Math.max(l1.getBlockZ(), l2.getBlockZ());
        for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) {
            if (walls && (x > minX && x < maxX && z > minZ && z < maxZ)) continue;
            p.getWorld().getBlockAt(x, y, z).setType(mat);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getItem() != null && e.getItem().getType() == Material.TRIDENT && e.getItem().hasItemMeta() && e.getItem().getItemMeta().getDisplayName().contains("Angel Wand")) {
            e.setCancelled(true);
            if (e.getAction().name().contains("LEFT")) pos1.put(e.getPlayer().getUniqueId(), e.getPlayer().getLocation());
            else pos2.put(e.getPlayer().getUniqueId(), e.getPlayer().getLocation());
            e.getPlayer().sendMessage("§bPosição marcada!");
        }
    }

    private void startTasks() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (World w : Bukkit.getWorlds()) {
                List<Entity> ms = new ArrayList<>();
                for (Entity en : w.getEntities()) if (en instanceof Monster) ms.add(en);
                if (ms.size() > 50) for (int i = 0; i < ms.size()/2; i++) ms.get(i).remove();
            }
        }, 600L, 600L);
    }
}
