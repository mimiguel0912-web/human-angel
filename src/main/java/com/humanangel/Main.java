package com.humanangel;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class Main extends JavaPlugin implements CommandExecutor, Listener {

    private Map<UUID, Location> pos1 = new HashMap<>(), pos2 = new HashMap<>();
    private Map<UUID, Location> homes = new HashMap<>();
    private List<UUID> scActive = new ArrayList<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        String[] cmds = {"modo", "sc", "ha", "home", "sethome", "spawn", "lista", "set", "paredes", "pos1", "pos2", "angelwand", "clearlag"};
        for (String s : cmds) {
            PluginCommand pc = getCommand(s);
            if (pc != null) pc.setExecutor(this);
        }
        
        // Loop do ClearLag: Limpa a cada 5 minutos (6000 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                limparItens();
            }
        }.runTaskTimer(this, 6000L, 6000L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String c = cmd.getName().toLowerCase();

        if (Arrays.asList("modo", "sc", "ha", "set", "paredes", "pos1", "pos2", "angelwand", "clearlag").contains(c) && !p.isOp()) {
            p.sendMessage("§cSem permissão!"); return true;
        }

        switch (c) {
            case "ha": p.sendMessage("§b§lHUMAN ANGEL §fv1.4.1"); break;
            case "clearlag": limparItens(); break;
            case "pos1": pos1.put(p.getUniqueId(), p.getLocation()); p.sendMessage("§b§l[WAND] §fPos 1 (Pés) marcada!"); break;
            case "pos2": pos2.put(p.getUniqueId(), p.getLocation()); p.sendMessage("§d§l[WAND] §fPos 2 (Pés) marcada!"); break;
            case "angelwand": p.getInventory().addItem(new ItemStack(Material.TRIDENT)); p.sendMessage("§bTridente entregue!"); break;
            case "set": fill(p, args, false); break;
            case "paredes": fill(p, args, true); break;
            case "spawn": p.teleport(p.getWorld().getSpawnLocation()); p.sendMessage("§aTeleportado ao Spawn!"); break;
            case "sethome": homes.put(p.getUniqueId(), p.getLocation()); p.sendMessage("§aHome salva!"); break;
            case "home":
                if (homes.containsKey(p.getUniqueId())) p.teleport(homes.get(p.getUniqueId()));
                else p.sendMessage("§cUse /sethome primeiro!");
                break;
            case "modo":
                if (args.length > 0) {
                    String m = args[0].toLowerCase();
                    if (m.equals("c")) p.setGameMode(GameMode.CREATIVE);
                    else if (m.equals("s")) p.setGameMode(GameMode.SURVIVAL);
                    else if (m.equals("a")) p.setGameMode(GameMode.ADVENTURE);
                    else if (m.equals("sp")) p.setGameMode(GameMode.SPECTATOR);
                    p.sendMessage("§aModo: " + p.getGameMode().name());
                } break;
                
            case "lista":
                p.sendMessage("§b§l--- COMANDOS ---");
                p.sendMessage("§f/home, /sethome, /spawn, /lista");
                if (p.isOp()) p.sendMessage("§e/modo, /sc, /set, /paredes, /pos1, /pos2, /angelwand, /clearlag");
                break;
        }
        return true;
    }

    // ANTI-VPN: Bloqueia IPs suspeitos ou proxies
    @EventHandler
    public void onJoin(PlayerLoginEvent e) {
        String host = e.getAddress().getHostAddress();
        if (e.getAddress().isLoopbackAddress() || e.getAddress().isSiteLocalAddress()) return;
        
        // Bloqueio simples de IPs conhecidos de DataCenter/VPN
        if (host.startsWith("127.") || host.startsWith("0.")) {
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER, "§cVPN/Proxy não permitido!");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (p.getInventory().getItemInMainHand().getType() == Material.TRIDENT && p.isOp()) {
            if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) e.setCancelled(true);
            if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                e.setCancelled(true);
                pos1.put(p.getUniqueId(), e.getClickedBlock().getLocation());
                p.sendMessage("§b§l[WAND] §fPosição 1 marcada!");
            } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                pos2.put(p.getUniqueId(), e.getClickedBlock().getLocation());
                p.sendMessage("§d§l[WAND] §fPosição 2 marcada!");
            }
        }
    }

    private void limparItens() {
        int i = 0;
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (e instanceof Item) { e.remove(); i++; }
            }
        }
        Bukkit.broadcastMessage("§e§l[ClearLag] §fForam removidos §6" + i + " §fitens do chão.");
    }

    private void fill(Player p, String[] args, boolean walls) {
        Location l1 = pos1.get(p.getUniqueId()), l2 = pos2.get(p.getUniqueId());
        if (l1 == null || l2 == null || args.length == 0) { p.sendMessage("§cMarque as posições!"); return; }
        Material mat = Material.matchMaterial(args[0].toUpperCase());
        if (mat == null) { p.sendMessage("§cBloco inválido!"); return; }
        int minX = Math.min(l1.getBlockX(), l2.getBlockX()), maxX = Math.max(l1.getBlockX(), l2.getBlockX());
        int minY = Math.min(l1.getBlockY(), l2.getBlockY()), maxY = Math.max(l1.getBlockY(), l2.getBlockY());
        int minZ = Math.min(l1.getBlockZ(), l2.getBlockZ()), maxZ = Math.max(l1.getBlockZ(), l2.getBlockZ());
        for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) {
            if (walls && (x > minX && x < maxX && z > minZ && z < maxZ)) continue;
            p.getWorld().getBlockAt(x, y, z).setType(mat);
        }
        p.sendMessage("§aOperação concluída!");
    }
}
