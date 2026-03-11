package com.humanangel;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class Main extends JavaPlugin implements CommandExecutor, Listener {

    private Map<UUID, Location> pos1 = new HashMap<>(), pos2 = new HashMap<>();
    private Map<UUID, Location> homes = new HashMap<>();
    private Map<String, Region> regions = new HashMap<>();
    private Map<UUID, UUID> tpaRequests = new HashMap<>();
    private List<String> zoeiraList = new ArrayList<>();
    private List<String> avisosServidor = new ArrayList<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        String[] cmds = {"modo", "sc", "ha", "home", "sethome", "spawn", "lista", "set", "paredes", "pos1", "pos2", "angelwand", "clearlag", "rg", "control", "zoeira", "tpa", "tpaccept", "tpadeny", "avisos", "rglista"};
        for (String s : cmds) {
            PluginCommand pc = getCommand(s);
            if (pc != null) pc.setExecutor(this);
        }
        
        new BukkitRunnable() {
            @Override
            public void run() { limparItens(); }
        }.runTaskTimer(this, 18000L, 18000L);

        new BukkitRunnable() {
            int index = 0;
            @Override
            public void run() {
                if (!avisosServidor.isEmpty()) {
                    if (index >= avisosServidor.size()) index = 0;
                    Bukkit.broadcastMessage("§b§l[AVISO] §f" + ChatColor.translateAlternateColorCodes('&', avisosServidor.get(index)));
                    index++;
                }
            }
        }.runTaskTimer(this, 144000L, 144000L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String c = cmd.getName().toLowerCase();

        if (Arrays.asList("modo", "sc", "set", "paredes", "angelwand", "clearlag", "rg", "control", "zoeira", "avisos", "pos1", "pos2").contains(c) && !p.isOp()) {
            p.sendMessage("§cSem permissão!"); return true;
        }

        switch (c) {
            case "sc":
                if (args.length == 0) p.sendMessage("§eEscreva uma mensagem!");
                else Bukkit.broadcast("§d§l[STAFF] §f" + p.getName() + ": §7" + String.join(" ", args), Server.BROADCAST_CHANNEL_ADMINISTRATIVE);
                break;

            case "modo":
                if (args.length > 0) {
                    String m = args[0].toLowerCase();
                    if (m.equals("c")) p.setGameMode(GameMode.CREATIVE);
                    else if (m.equals("s")) p.setGameMode(GameMode.SURVIVAL);
                    else if (m.equals("a")) p.setGameMode(GameMode.ADVENTURE);
                    else if (m.equals("sp")) p.setGameMode(GameMode.SPECTATOR);
                    p.sendMessage("§aModo alterado para " + p.getGameMode().name());
                } break;

            case "pos1": pos1.put(p.getUniqueId(), p.getLocation()); p.sendMessage("§bPos 1 marcada no seu pé!"); break;
            case "pos2": pos2.put(p.getUniqueId(), p.getLocation()); p.sendMessage("§dPos 2 marcada no seu pé!"); break;

            case "set": fill(p, args, false); break;
            case "paredes": fill(p, args, true); break;
            
            case "home":
                if (homes.containsKey(p.getUniqueId())) p.teleport(homes.get(p.getUniqueId()));
                else p.sendMessage("§c§lERRO: §fVocê não tem uma home salva! Use /sethome");
                break;
            
            case "sethome": homes.put(p.getUniqueId(), p.getLocation()); p.sendMessage("§aHome salva!"); break;
            case "spawn": p.teleport(p.getWorld().getSpawnLocation()); break;
            case "tpa":
                if (args.length == 0) return true;
                Player targetTpa = Bukkit.getPlayer(args[0]);
                if (targetTpa != null) {
                    tpaRequests.put(targetTpa.getUniqueId(), p.getUniqueId());
                    targetTpa.sendMessage("§e" + p.getName() + " pediu TPA. /tpaccept");
                } break;
            case "tpaccept":
                if (tpaRequests.containsKey(p.getUniqueId())) {
                    Player r = Bukkit.getPlayer(tpaRequests.get(p.getUniqueId()));
                    if (r != null) r.teleport(p.getLocation());
                    tpaRequests.remove(p.getUniqueId());
                } break;
            case "control": openControlMenu(p); break;
            case "angelwand": p.getInventory().addItem(new ItemStack(Material.WOODEN_AXE)); break;
            case "clearlag": limparItens(); break;
        }
        return true;
    }

    private void fill(Player p, String[] args, boolean walls) {
        Location l1 = pos1.get(p.getUniqueId()), l2 = pos2.get(p.getUniqueId());
        if (l1 == null || l2 == null || args.length == 0) { p.sendMessage("§cMarque as posições!"); return; }
        Material mat = Material.matchMaterial(args[0].toUpperCase());
        if (mat == null) return;
        int minX = Math.min(l1.getBlockX(), l2.getBlockX()), maxX = Math.max(l1.getBlockX(), l2.getBlockX());
        int minY = Math.min(l1.getBlockY(), l2.getBlockY()), maxY = Math.max(l1.getBlockY(), l2.getBlockY());
        int minZ = Math.min(l1.getBlockZ(), l2.getBlockZ()), maxZ = Math.max(l1.getBlockZ(), l2.getBlockZ());
        for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) {
            if (walls && (x > minX && x < maxX && z > minZ && z < maxZ && y > minY && y < maxY)) continue;
            p.getWorld().getBlockAt(x, y, z).setType(mat);
        }
        p.sendMessage("§aPronto!");
    }

    private void openControlMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§0Controle de Jogadores");
        for (Player online : Bukkit.getOnlinePlayers()) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = skull.getItemMeta();
            meta.setDisplayName("§e" + online.getName());
            skull.setItemMeta(meta);
            inv.addItem(skull);
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§0Controle de Jogadores")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            Player target = Bukkit.getPlayer(ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()));
            if (target != null) e.getWhoClicked().openInventory(target.getInventory());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (p.getInventory().getItemInMainHand().getType() == Material.WOODEN_AXE && p.isOp()) {
            if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                e.setCancelled(true); pos1.put(p.getUniqueId(), e.getClickedBlock().getLocation()); p.sendMessage("§bPos 1!");
            } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                e.setCancelled(true); pos2.put(p.getUniqueId(), e.getClickedBlock().getLocation()); p.sendMessage("§dPos 2!");
            }
        }
    }

    private void limparItens() {
        int i = 0;
        for (World w : Bukkit.getWorlds()) for (Entity en : w.getEntities()) if (en instanceof Item) { en.remove(); i++; }
        Bukkit.broadcastMessage("§e§l[ClearLag] §fLimpamos §6" + i + " §fitens.");
    }

    class Region {
        Location min, max; Map<String, Boolean> flags = new HashMap<>();
        Region(Location l1, Location l2) {
            this.min = new Location(l1.getWorld(), Math.min(l1.getX(), l2.getX()), 0, Math.min(l1.getZ(), l2.getZ()));
            this.max = new Location(l1.getWorld(), Math.max(l1.getX(), l2.getX()), 255, Math.max(l1.getZ(), l2.getZ()));
        }
        boolean isInside(Location l) {
            return l.getWorld().equals(min.getWorld()) && l.getX() >= min.getX() && l.getX() <= max.getX() && l.getZ() >= min.getZ() && l.getZ() <= max.getZ();
        }
    }
}
