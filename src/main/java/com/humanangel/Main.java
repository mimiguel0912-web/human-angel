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
    private List<String> zoeiraList = new ArrayList<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        String[] cmds = {"modo", "sc", "ha", "home", "sethome", "spawn", "lista", "set", "paredes", "pos1", "pos2", "angelwand", "clearlag", "rg", "control", "zoeira"};
        for (String s : cmds) {
            PluginCommand pc = getCommand(s);
            if (pc != null) pc.setExecutor(this);
        }
        
        new BukkitRunnable() {
            @Override
            public void run() { limparItens(); }
        }.runTaskTimer(this, 18000L, 18000L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String c = cmd.getName().toLowerCase();

        if (Arrays.asList("modo", "sc", "set", "paredes", "angelwand", "clearlag", "rg", "control", "zoeira").contains(c) && !p.isOp()) {
            p.sendMessage("§cSem permissão!"); return true;
        }

        switch (c) {
            case "ha": p.sendMessage("§b§lHUMAN ANGEL §fv1.4.0"); break;

            case "control":
                openControlMenu(p);
                break;

            case "zoeira":
                if (args.length == 0) { p.sendMessage("§e/zoeira [add/remove/list]"); return true; }
                if (args[0].equalsIgnoreCase("add") && args.length > 1) {
                    zoeiraList.add(args[1].toLowerCase()); p.sendMessage("§aBloqueado!");
                } else if (args[0].equalsIgnoreCase("remove") && args.length > 1) {
                    zoeiraList.remove(args[1].toLowerCase()); p.sendMessage("§cRemovido!");
                } else if (args[0].equalsIgnoreCase("list")) {
                    p.sendMessage("§eFiltro: §f" + String.join(", ", zoeiraList));
                }
                break;

            case "rg":
                if (args.length < 2) { p.sendMessage("§e/rg define [nome] | flag [nome] [flag] [allow/deny]"); return true; }
                if (args[0].equalsIgnoreCase("define")) {
                    Location l1 = pos1.get(p.getUniqueId()), l2 = pos2.get(p.getUniqueId());
                    if (l1 == null || l2 == null) p.sendMessage("§cMarque as posições!");
                    else { regions.put(args[1], new Region(l1, l2)); p.sendMessage("§aRegião salva!"); }
                } else if (args[0].equalsIgnoreCase("flag") && args.length >= 4) {
                    Region r = regions.get(args[1]);
                    if (r != null) { r.flags.put(args[2].toLowerCase(), args[3].equalsIgnoreCase("allow")); p.sendMessage("§aFlag alterada!"); }
                }
                break;

            case "sc":
                Bukkit.broadcast("§d§l[STAFF] §f" + p.getName() + ": §7" + String.join(" ", args), Server.BROADCAST_CHANNEL_ADMINISTRATIVE);
                break;

            case "angelwand":
                p.getInventory().addItem(new ItemStack(Material.WOODEN_AXE));
                p.sendMessage("§bMachado entregue!");
                break;

            case "modo":
                if (args.length > 0) {
                    String m = args[0].toLowerCase();
                    if (m.equals("c")) p.setGameMode(GameMode.CREATIVE);
                    else if (m.equals("s")) p.setGameMode(GameMode.SURVIVAL);
                    p.sendMessage("§aModo alterado!");
                } break;

            case "home": if(homes.containsKey(p.getUniqueId())) p.teleport(homes.get(p.getUniqueId())); break;
            case "sethome": homes.put(p.getUniqueId(), p.getLocation()); p.sendMessage("§aHome salva!"); break;
            case "spawn": p.teleport(p.getWorld().getSpawnLocation()); break;
            case "clearlag": limparItens(); break;
        }
        return true;
    }

    // --- MENU CONTROL ---
    private void openControlMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§0Controle de Jogadores");
        for (Player online : Bukkit.getOnlinePlayers()) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = skull.getItemMeta();
            meta.setDisplayName("§e" + online.getName());
            meta.setLore(Arrays.asList("§7Clique para ver o inventário"));
            skull.setItemMeta(meta);
            inv.addItem(skull);
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§0Controle de Jogadores")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || e.getCurrentItem().getType() != Material.PLAYER_HEAD) return;
            String name = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
            Player target = Bukkit.getPlayer(name);
            if (target != null) {
                e.getWhoClicked().openInventory(target.getInventory());
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        String msg = e.getMessage().toLowerCase();
        for (String s : zoeiraList) {
            if (msg.contains(s)) { e.setCancelled(true); e.getPlayer().sendMessage("§cPalavra proibida!"); return; }
        }
    }

    // Proteção de Região
    @EventHandler
    public void onBreak(BlockBreakEvent e) { if (!canDo(e.getPlayer(), e.getBlock().getLocation(), "build")) e.setCancelled(true); }
    @EventHandler
    public void onPlace(BlockPlaceEvent e) { if (!canDo(e.getPlayer(), e.getBlock().getLocation(), "build")) e.setCancelled(true); }

    private boolean canDo(Player p, Location loc, String flag) {
        if (p.isOp()) return true;
        for (Region r : regions.values()) if (r.isInside(loc)) return r.flags.getOrDefault(flag, true);
        return true;
    }

    private void limparItens() {
        int i = 0;
        for (World w : Bukkit.getWorlds()) {
            for (Entity en : w.getEntities()) if (en instanceof Item) { en.remove(); i++; }
        }
        Bukkit.broadcastMessage("§e§l[ClearLag] §6" + i + " §fitens limpos.");
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
