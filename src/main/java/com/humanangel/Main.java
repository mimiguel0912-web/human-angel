package com.humanangel;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.InventoryClickEvent;
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
    private List<String> zoeiraList = new ArrayList<>();
    private List<String> avisosServidor = new ArrayList<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        String[] cmds = {"ha", "modo", "sc", "home", "sethome", "spawn", "set", "paredes", "pos1", "pos2", "angelwand", "clearlag", "control", "zoeira", "avisos", "rg", "flag", "rglista", "lista"};
        for (String s : cmds) {
            PluginCommand pc = getCommand(s);
            if (pc != null) pc.setExecutor(this);
        }

        new BukkitRunnable() {
            int idx = 0;
            @Override
            public void run() {
                if (!avisosServidor.isEmpty()) {
                    if (idx >= avisosServidor.size()) idx = 0;
                    Bukkit.broadcastMessage("§b§l[AVISO] §f" + ChatColor.translateAlternateColorCodes('&', avisosServidor.get(idx)));
                    idx++;
                }
            }
        }.runTaskTimer(this, 144000L, 144000L); // 2 horas
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String n = cmd.getName().toLowerCase();

        if (Arrays.asList("modo", "sc", "set", "paredes", "angelwand", "clearlag", "control", "zoeira", "avisos", "rg", "flag", "pos1", "pos2").contains(n) && !p.isOp()) {
            p.sendMessage("§cSem permissão!"); return true;
        }

        switch (n) {
            case "ha": p.sendMessage("§b§lHUMAN ANGEL §fv1.5.1"); break;

            case "lista":
                p.sendMessage("§b§l--- COMANDOS ---");
                p.sendMessage("§f/home, /sethome, /spawn, /lista");
                if (p.isOp()) p.sendMessage("§eAdmin: /modo, /sc, /set, /paredes, /rg, /flag, /rglista, /control, /zoeira, /avisos, /clearlag");
                break;

            case "zoeira":
                if (args.length == 0) { p.sendMessage("§e/zoeira [add/remove/list]"); return true; }
                if (args[0].equalsIgnoreCase("add") && args.length > 1) {
                    zoeiraList.add(args[1].toLowerCase()); p.sendMessage("§aPalavra bloqueada!");
                } else if (args[0].equalsIgnoreCase("remove") && args.length > 1) {
                    zoeiraList.remove(args[1].toLowerCase()); p.sendMessage("§cPalavra liberada!");
                } else if (args[0].equalsIgnoreCase("list")) {
                    p.sendMessage("§eFiltro: §f" + String.join(", ", zoeiraList));
                } break;

            case "avisos":
                if (args.length == 0) { p.sendMessage("§e/avisos [add/remove/list]"); return true; }
                if (args[0].equalsIgnoreCase("add")) {
                    avisosServidor.add(String.join(" ", Arrays.copyOfRange(args, 1, args.length))); p.sendMessage("§aAviso salvo!");
                } else if (args[0].equalsIgnoreCase("remove")) {
                    try { avisosServidor.remove(Integer.parseInt(args[1])); p.sendMessage("§cAviso removido!"); } catch(Exception e) { p.sendMessage("§cUse o ID."); }
                } else if (args[0].equalsIgnoreCase("list")) {
                    for(int i=0; i<avisosServidor.size(); i++) p.sendMessage("§e" + i + ": §f" + avisosServidor.get(i));
                } break;

            case "rg":
                if (args.length == 0) { p.sendMessage("§e/rg [nome]"); return true; }
                Location l1 = pos1.get(p.getUniqueId()), l2 = pos2.get(p.getUniqueId());
                if (l1 == null || l2 == null) { p.sendMessage("§cUse o machado primeiro!"); return true; }
                regions.put(args[0], new Region(l1, l2)); p.sendMessage("§aRG '" + args[0] + "' criada!");
                break;

            case "flag":
                if (args.length < 3) { p.sendMessage("§e/flag [pvp/build/tnt] [nome_do_rg] [allow/deny]"); return true; }
                Region r = regions.get(args[1]);
                if (r != null) {
                    r.flags.put(args[0].toLowerCase(), args[2].equalsIgnoreCase("allow"));
                    p.sendMessage("§aFlag '" + args[0] + "' em '" + args[1] + "' definida!");
                } break;

            case "angelwand":
                ItemStack wand = new ItemStack(Material.WOODEN_AXE);
                ItemMeta mt = wand.getItemMeta(); mt.setDisplayName("§b§lAngel Wand"); wand.setItemMeta(mt);
                p.getInventory().addItem(wand); p.sendMessage("§bUse o machado para selecionar!");
                break;

            case "modo":
                if (args.length > 0) {
                    String m = args[0].toLowerCase();
                    if (m.equals("c")) p.setGameMode(GameMode.CREATIVE);
                    else if (m.equals("s")) p.setGameMode(GameMode.SURVIVAL);
                    else if (m.equals("a")) p.setGameMode(GameMode.ADVENTURE);
                    else if (m.equals("sp")) p.setGameMode(GameMode.SPECTATOR);
                } break;
                
            case "home": if(homes.containsKey(p.getUniqueId())) p.teleport(homes.get(p.getUniqueId())); break;
            case "sethome": homes.put(p.getUniqueId(), p.getLocation()); p.sendMessage("§aHome ok!"); break;
            case "spawn": p.teleport(p.getWorld().getSpawnLocation()); break;
        }
        return true;
    }

    @EventHandler
    public void onWandInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (p.isOp() && e.getItem() != null && e.getItem().getType() == Material.WOODEN_AXE) {
            if (e.getClickedBlock() == null) return;
            e.setCancelled(true);
            if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                pos1.put(p.getUniqueId(), e.getClickedBlock().getLocation()); p.sendMessage("§bPos 1!");
            } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                pos2.put(p.getUniqueId(), e.getClickedBlock().getLocation()); p.sendMessage("§dPos 2!");
            }
        }
    }

    // Proteção de Flags
    @EventHandler
    public void onBreak(BlockBreakEvent e) { if(!can(e.getPlayer(), e.getBlock().getLocation(), "build")) e.setCancelled(true); }
    @EventHandler
    public void onPlace(BlockPlaceEvent e) { if(!can(e.getPlayer(), e.getBlock().getLocation(), "build")) e.setCancelled(true); }

    private boolean can(Player p, Location loc, String flag) {
        if (p.isOp()) return true;
        for (Region r : regions.values()) if (r.isInside(loc)) return r.flags.getOrDefault(flag, true);
        return true;
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
