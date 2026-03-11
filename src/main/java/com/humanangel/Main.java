package com.humanangel;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
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
    private Map<UUID, UUID> tpaRequests = new HashMap<>();
    private List<String> zoeiraList = new ArrayList<>();
    private List<String> avisosServidor = new ArrayList<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        
        String[] cmds = {"ha", "modo", "sc", "home", "sethome", "spawn", "set", "paredes", "pos1", "pos2", "angelwand", "clearlag", "control", "zoeira", "tpa", "tpaccept", "avisos", "rglista"};
        for (String s : cmds) {
            PluginCommand pc = getCommand(s);
            if (pc != null) pc.setExecutor(this);
        }

        // ClearLag Automático - 15 min
        new BukkitRunnable() {
            @Override
            public void run() { limparItens(); }
        }.runTaskTimer(this, 18000L, 18000L);

        // Avisos Automáticos - 2 horas
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

    // --- ANTI VPN ---
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        String country = e.getPlayer().getAddress().getAddress().getHostAddress();
        // Lógica simples: Se não for IP local/BR (exemplo didático), você pode expandir com API
        if (e.getPlayer().getAddress().getHostName().contains("vpn") || e.getPlayer().getAddress().getHostName().contains("proxy")) {
            e.getPlayer().kickPlayer("§cVPN/Proxy não permitido!");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String n = cmd.getName().toLowerCase();

        if (Arrays.asList("modo", "sc", "set", "paredes", "angelwand", "clearlag", "control", "zoeira", "avisos", "pos1", "pos2").contains(n) && !p.isOp()) {
            p.sendMessage("§cSem permissão!"); return true;
        }

        switch (n) {
            case "ha": p.sendMessage("§b§lHUMAN ANGEL §fv1.4.0 §a[ON]"); break;
            
            case "modo":
                if (args.length > 0) {
                    String m = args[0].toLowerCase();
                    if (m.equals("c")) p.setGameMode(GameMode.CREATIVE);
                    else if (m.equals("s")) p.setGameMode(GameMode.SURVIVAL);
                    else if (m.equals("a")) p.setGameMode(GameMode.ADVENTURE);
                    else if (m.equals("sp")) p.setGameMode(GameMode.SPECTATOR);
                    p.sendMessage("§aModo alterado!");
                } break;

            case "sc":
                Bukkit.broadcast("§d§l[STAFF] §f" + p.getName() + ": §7" + String.join(" ", args), Server.BROADCAST_CHANNEL_ADMINISTRATIVE);
                break;

            case "pos1": pos1.put(p.getUniqueId(), p.getLocation()); p.sendMessage("§bPos 1 definida!"); break;
            case "pos2": pos2.put(p.getUniqueId(), p.getLocation()); p.sendMessage("§dPos 2 definida!"); break;

            case "set": fill(p, args, false); break;
            case "paredes": fill(p, args, true); break;

            case "home":
                if (homes.containsKey(p.getUniqueId())) p.teleport(homes.get(p.getUniqueId()));
                else p.sendMessage("§cVocê não tem home!"); break;
            case "sethome": homes.put(p.getUniqueId(), p.getLocation()); p.sendMessage("§aHome salva!"); break;

            case "control": openControlMenu(p); break;
            case "clearlag": limparItens(); break;

            case "zoeira":
                if (args.length > 1 && args[0].equalsIgnoreCase("add")) {
                    zoeiraList.add(args[1].toLowerCase()); p.sendMessage("§aFiltro atualizado!");
                } else if (args[0].equalsIgnoreCase("list")) {
                    p.sendMessage("§eBloqueadas: " + String.join(", ", zoeiraList));
                } break;
        }
        return true;
    }

    private void fill(Player p, String[] args, boolean w) {
        Location l1 = pos1.get(p.getUniqueId()), l2 = pos2.get(p.getUniqueId());
        if (l1 == null || l2 == null || args.length == 0) { p.sendMessage("§cDefina as posições!"); return; }
        Material mat = Material.matchMaterial(args[0].toUpperCase());
        if (mat == null) return;
        int xMin = Math.min(l1.getBlockX(), l2.getBlockX()), xMax = Math.max(l1.getBlockX(), l2.getBlockX());
        int yMin = Math.min(l1.getBlockY(), l2.getBlockY()), yMax = Math.max(l1.getBlockY(), l2.getBlockY());
        int zMin = Math.min(l1.getBlockZ(), l2.getBlockZ()), zMax = Math.max(l1.getBlockZ(), l2.getBlockZ());
        for (int x = xMin; x <= xMax; x++) for (int y = yMin; y <= yMax; y++) for (int z = zMin; z <= zMax; z++) {
            if (w && (x != xMin && x != xMax && z != zMin && z != zMax)) continue;
            p.getWorld().getBlockAt(x, y, z).setType(mat);
        }
        p.sendMessage("§aBlocos alterados!");
    }

    private void openControlMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§0Controle");
        for (Player o : Bukkit.getOnlinePlayers()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta m = head.getItemMeta(); m.setDisplayName("§e" + o.getName());
            head.setItemMeta(m); inv.addItem(head);
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§0Controle")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            Player t = Bukkit.getPlayer(ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()));
            if (t != null) e.getWhoClicked().openInventory(t.getInventory());
        }
    }

    private void limparItens() {
        int i = 0;
        for (World w : Bukkit.getWorlds()) for (Entity en : w.getEntities()) if (en instanceof Item) { en.remove(); i++; }
        Bukkit.broadcastMessage("§e§l[ClearLag] §6" + i + " §fitens removidos.");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        for (String s : zoeiraList) {
            if (e.getMessage().toLowerCase().contains(s)) { e.setCancelled(true); e.getPlayer().sendMessage("§cNão diga isso!"); }
        }
    }
}
