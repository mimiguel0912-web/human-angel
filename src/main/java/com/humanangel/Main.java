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

    private Map<UUID, Boolean> pvpState = new HashMap<>();
    private Map<UUID, Location> pos1 = new HashMap<>(), pos2 = new HashMap<>();
    private Map<UUID, Location> homes = new HashMap<>();
    private List<UUID> scActive = new ArrayList<>();
    private List<String> zoeiraMessages = new ArrayList<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        String[] cmds = {"mode", "control", "sc", "ha", "angelwand", "set", "paredes", "zoeira", "home", "sethome", "pvp", "spawn", "tamanho", "tpa", "tpaccept"};
        for (String s : cmds) {
            PluginCommand pc = getCommand(s);
            if (pc != null) pc.setExecutor(this);
        }
        
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!zoeiraMessages.isEmpty()) {
                String msg = zoeiraMessages.get(new Random().nextInt(zoeiraMessages.size()));
                Bukkit.broadcastMessage("§6§l[AVISO] §f" + msg.replace("&", "§"));
            }
        }, 144000L, 144000L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String c = cmd.getName().toLowerCase();

        if (Arrays.asList("mode", "control", "sc", "ha", "angelwand", "set", "paredes", "zoeira").contains(c)) {
            if (!p.isOp()) { p.sendMessage("§cSem permissão!"); return true; }
        }

        switch (c) {
            case "ha": p.sendMessage("§b§lHUMAN ANGEL §fv1.4"); break;
            case "home":
                if (!homes.containsKey(p.getUniqueId())) p.sendMessage("§c§lERRO: §fvc não tem uma home!!");
                else p.teleport(homes.get(p.getUniqueId()));
                break;
            case "sethome":
                homes.put(p.getUniqueId(), p.getLocation());
                p.sendMessage("§aHome definida!");
                break;
            case "pvp":
                boolean estado = !pvpState.getOrDefault(p.getUniqueId(), true);
                pvpState.put(p.getUniqueId(), estado);
                p.sendMessage(estado ? "§aSeu PvP agora está LIGADO!" : "§cSeu PvP agora está DESLIGADO!");
                break;
            case "angelwand":
                ItemStack t = new ItemStack(Material.TRIDENT);
                ItemMeta m = t.getItemMeta(); m.setDisplayName("§b§lAngel Wand"); t.setItemMeta(m);
                p.getInventory().addItem(t);
                break;
            case "set": fillArea(p, (args.length > 0 ? Material.matchMaterial(args[0].toUpperCase()) : null), false); break;
            case "paredes": fillArea(p, (args.length > 0 ? Material.matchMaterial(args[0].toUpperCase()) : null), true); break;
            case "control":
                Inventory inv = Bukkit.createInventory(null, 54, "§0Controle");
                for (Player o : Bukkit.getOnlinePlayers()) {
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    ItemMeta im = head.getItemMeta(); im.setDisplayName("§e" + o.getName()); head.setItemMeta(im);
                    inv.addItem(head);
                }
                p.openInventory(inv);
                break;
            case "sc":
                if (scActive.contains(p.getUniqueId())) scActive.remove(p.getUniqueId());
                else scActive.add(p.getUniqueId());
                p.sendMessage("§dStaffChat: " + (scActive.contains(p.getUniqueId()) ? "ON" : "OFF"));
                break;
            case "zoeira":
                if (args.length > 0) { zoeiraMessages.add(String.join(" ", args)); p.sendMessage("§aAviso salvo!"); }
                break;
        }
        return true;
    }

    private void fillArea(Player p, Material mat, boolean walls) {
        Location l1 = pos1.get(p.getUniqueId()), l2 = pos2.get(p.getUniqueId());
        if (l1 == null || l2 == null || mat == null) { p.sendMessage("§cUse o tridente primeiro e o nome do bloco em inglês!"); return; }
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

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
            if (!pvpState.getOrDefault(e.getEntity().getUniqueId(), true) || !pvpState.getOrDefault(e.getDamager().getUniqueId(), true)) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§0Controle") && e.getCurrentItem() != null) {
            e.setCancelled(true);
            Player target = Bukkit.getPlayer(ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()));
            if (target != null) e.getWhoClicked().openInventory(target.getInventory());
        }
    }
}
