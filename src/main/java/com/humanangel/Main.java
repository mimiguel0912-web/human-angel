package com.humanangel;

import org.bukkit.*;
import org.bukkit.block.Block;
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
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        String[] adminCmds = {"mode", "control", "sc", "ha", "angelwand", "set", "paredes", "zoeira"};
        String[] playerCmds = {"home", "sethome", "tpa", "tpaccept", "pvp", "spawn", "tamanho"};
        
        for (String s : adminCmds) getCommand(s).setExecutor(this);
        for (String s : playerCmds) getCommand(s).setExecutor(this);

        // Sistema /zoeira: Avisos Automáticos a cada 2 horas (144000 ticks)
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

        // --- COMANDOS DE ADM (Somente OP) ---
        if (Arrays.asList("mode", "control", "sc", "ha", "angelwand", "set", "paredes", "zoeira").contains(c)) {
            if (!p.isOp()) { p.sendMessage("§cSem permissão!"); return true; }
        }

        switch (c) {
            case "zoeira":
                if (args.length == 0) p.sendMessage("§eUse: /zoeira [mensagem] para adicionar um aviso.");
                else {
                    zoeiraMessages.add(String.join(" ", args));
                    p.sendMessage("§aAviso adicionado ao sistema de 2 horas!");
                }
                break;
            case "set": // WorldEdit Integrado
                if (args.length == 0) return false;
                fillArea(p, Material.matchMaterial(args[0]), false);
                break;
            case "paredes":
                if (args.length == 0) return false;
                fillArea(p, Material.matchMaterial(args[0]), true);
                break;
            case "home": p.teleport(homes.getOrDefault(p.getUniqueId(), p.getWorld().getSpawnLocation())); break;
            case "sethome": homes.put(p.getUniqueId(), p.getLocation()); p.sendMessage("§aHome definida!"); break;
            case "tpa":
                if (args.length == 0) return false;
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null) {
                    tpaRequests.put(target.getUniqueId(), p.getUniqueId());
                    target.sendMessage("§e" + p.getName() + " quer ir até você. /tpaccept");
                }
                break;
            case "tpaccept":
                UUID requesterId = tpaRequests.get(p.getUniqueId());
                if (requesterId != null) {
                    Bukkit.getPlayer(requesterId).teleport(p.getLocation());
                    tpaRequests.remove(p.getUniqueId());
                }
                break;
            case "control":
                Inventory inv = Bukkit.createInventory(null, 54, "§0Controle");
                for (Player online : Bukkit.getOnlinePlayers()) {
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    ItemMeta m = head.getItemMeta(); m.setDisplayName("§e" + online.getName()); head.setItemMeta(m);
                    inv.addItem(head);
                }
                p.openInventory(inv);
                break;
            case "angelwand":
                ItemStack tridente = new ItemStack(Material.TRIDENT);
                ItemMeta tm = tridente.getItemMeta(); tm.setDisplayName("§b§lAngel Wand"); tridente.setItemMeta(tm);
                p.getInventory().addItem(tridente);
                break;
        }
        return true;
    }

    // Lógica do WorldEdit (Set e Paredes)
    private void fillArea(Player p, Material mat, boolean apenasParedes) {
        Location l1 = pos1.get(p.getUniqueId()), l2 = pos2.get(p.getUniqueId());
        if (l1 == null || l2 == null || mat == null) return;
        int minX = Math.min(l1.getBlockX(), l2.getBlockX()), maxX = Math.max(l1.getBlockX(), l2.getBlockX());
        int minY = Math.min(l1.getBlockY(), l2.getBlockY()), maxY = Math.max(l1.getBlockY(), l2.getBlockY());
        int minZ = Math.min(l1.getBlockZ(), l2.getBlockZ()), maxZ = Math.max(l1.getBlockZ(), l2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (apenasParedes && (x > minX && x < maxX && z > minZ && z < maxZ)) continue;
                    p.getWorld().getBlockAt(x, y, z).setType(mat);
                }
            }
        }
        p.sendMessage("§aOperação concluída!");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getItem() != null && e.getItem().getType() == Material.TRIDENT && e.getItem().getItemMeta().getDisplayName().contains("Angel Wand")) {
            e.setCancelled(true);
            if (e.getAction().name().contains("LEFT")) pos1.put(e.getPlayer().getUniqueId(), e.getPlayer().getLocation());
            else pos2.put(e.getPlayer().getUniqueId(), e.getPlayer().getLocation());
            e.getPlayer().sendMessage("§bPosição marcada!");
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
            if (!pvpEnabled) e.setCancelled(true);
            else { inCombat.put(e.getEntity().getUniqueId(), System.currentTimeMillis()); inCombat.put(e.getDamager().getUniqueId(), System.currentTimeMillis()); }
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
