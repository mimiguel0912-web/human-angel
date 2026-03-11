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

        // Avisos a cada 2 horas
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
        }.runTaskTimer(this, 144000L, 144000L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String n = cmd.getName().toLowerCase();

        // Permissões Admin
        List<String> adminCmds = Arrays.asList("modo", "sc", "set", "paredes", "angelwand", "clearlag", "control", "zoeira", "avisos", "rg", "flag", "pos1", "pos2");
        if (adminCmds.contains(n) && !p.isOp()) {
            p.sendMessage("§cVocê não tem permissão para usar esse comando!");
            return true;
        }

        switch (n) {
            case "ha": p.sendMessage("§b§lHUMAN ANGEL §fv1.5.1 §a[ON]"); break;

            case "lista":
                p.sendMessage("§b§l--- MEUS COMANDOS ---");
                p.sendMessage("§f/home, /sethome, /spawn, /lista");
                if (p.isOp()) p.sendMessage("§eAdmin: /modo, /sc, /set, /paredes, /rg, /flag, /rglista, /control, /zoeira, /avisos");
                break;

            case "control": openControlMenu(p); break;

            case "zoeira":
                if (args.length < 1) { p.sendMessage("§e/zoeira [add/remove/list]"); return true; }
                if (args[0].equalsIgnoreCase("add") && args.length > 1) {
                    zoeiraList.add(args[1].toLowerCase()); p.sendMessage("§aPalavra bloqueada!");
                } else if (args[0].equalsIgnoreCase("remove") && args.length > 1) {
                    zoeiraList.remove(args[1].toLowerCase()); p.sendMessage("§cPalavra removida do filtro!");
                } else if (args[0].equalsIgnoreCase("list")) {
                    p.sendMessage("§eLista de palavras: §f" + String.join(", ", zoeiraList));
                } break;

            case "avisos":
                if (args.length < 1) { p.sendMessage("§e/avisos [add/remove/list]"); return true; }
                if (args[0].equalsIgnoreCase("add")) {
                    avisosServidor.add(String.join(" ", Arrays.copyOfRange(args, 1, args.length))); p.sendMessage("§aAviso salvo!");
                } else if (args[0].equalsIgnoreCase("remove")) {
                    try { avisosServidor.remove(Integer.parseInt(args[1])); p.sendMessage("§cAviso removido!"); } catch(Exception e) { p.sendMessage("§cUse o ID da lista."); }
                } else if (args[0].equalsIgnoreCase("list")) {
                    p.sendMessage("§b--- Lista de Avisos ---");
                    for(int i=0; i<avisosServidor.size(); i++) p.sendMessage("§e" + i + ": §f" + avisosServidor.get(i));
                } break;

            case "rg":
                if (args.length == 0) { p.sendMessage("§e/rg [nome]"); return true; }
                Location l1 = pos1.get(p.getUniqueId()), l2 = pos2.get(p.getUniqueId());
                if (l1 == null || l2 == null) { p.sendMessage("§cUse o AngelWand para marcar a área!"); return true; }
                regions.put(args[0], new Region(l1, l2)); p.sendMessage("§aRegião '" + args[0] + "' criada com sucesso!");
                break;

            case "flag":
                if (args.length < 3) { p.sendMessage("§e/flag [pvp/build] [nome_rg] [allow/deny]"); return true; }
                Region r = regions.get(args[1]);
                if (r != null) {
                    r.flags.put(args[0].toLowerCase(), args[2].equalsIgnoreCase("allow"));
                    p.sendMessage("§aFlag '" + args[0] + "' na região '" + args[1] + "' definida como: " + args[2]);
                } else { p.sendMessage("§cRegião não encontrada!"); }
                break;

            case "angelwand":
                ItemStack wand = new ItemStack(Material.WOODEN_AXE);
                ItemMeta mt = wand.getItemMeta(); mt.setDisplayName("§b§lAngel Wand");
                wand.setItemMeta(mt); p.getInventory().addItem(wand);
                p.sendMessage("§bVocê recebeu o Machado de Seleção!");
                break;

            case "modo":
                if (args.length == 0) return true;
                if (args[0].equalsIgnoreCase("c")) p.setGameMode(GameMode.CREATIVE);
                else if (args[0].equalsIgnoreCase("s")) p.setGameMode(GameMode.SURVIVAL);
                else if (args[0].equalsIgnoreCase("a")) p.setGameMode(GameMode.ADVENTURE);
                else if (args[0].equalsIgnoreCase("sp")) p.setGameMode(GameMode.SPECTATOR);
                p.sendMessage("§aModo de jogo alterado!");
                break;

            case "sethome": homes.put(p.getUniqueId(), p.getLocation()); p.sendMessage("§aHome salva!"); break;
            case "home": if(homes.containsKey(p.getUniqueId())) p.teleport(homes.get(p.getUniqueId())); else p.sendMessage("§cVocê não tem uma home!"); break;
            case "spawn": p.teleport(p.getWorld().getSpawnLocation()); p.sendMessage("§eTeleportado para o Spawn!"); break;
        }
        return true;
    }

    private void openControlMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§0Controle de Jogadores");
        for (Player o : Bukkit.getOnlinePlayers()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta m = head.getItemMeta();
            m.setDisplayName("§e" + o.getName());
            m.setLore(Arrays.asList("§7Clique para ver o inventário"));
            head.setItemMeta(m);
            inv.addItem(head);
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§0Controle de Jogadores")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;
            String targetName = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                e.getWhoClicked().openInventory(target.getInventory());
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (p.isOp() && e.getItem() != null && e.getItem().getType() == Material.WOODEN_AXE) {
            if (e.getClickedBlock() == null) return;
            e.setCancelled(true);
            if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                pos1.put(p.getUniqueId(), e.getClickedBlock().getLocation()); p.sendMessage("§bPosição 1 marcada!");
            } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                pos2.put(p.getUniqueId(), e.getClickedBlock().getLocation()); p.sendMessage("§dPosição 2 marcada!");
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        String msg = e.getMessage().toLowerCase();
        for (String s : zoeiraList) {
            if (msg.contains(s)) { e.setCancelled(true); e.getPlayer().sendMessage("§cPalavra bloqueada pelo filtro!"); return; }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) { if(!can(e.getPlayer(), e.getBlock().getLocation(), "build")) e.setCancelled(true); }
    @EventHandler
    public void onPlace(BlockPlaceEvent e) { if(!can(e.getPlayer(), e.getBlock().getLocation(), "build")) e.setCancelled(true); }

    private boolean can(Player p, Location loc, String flag) {
        if (p.isOp()) return true;
        for (Region r : regions.values()) {
            if (r.isInside(loc)) return r.flags.getOrDefault(flag, true);
        }
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
