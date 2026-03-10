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
        
        // ClearLag - 15 minutos
        new BukkitRunnable() {
            @Override
            public void run() { limparItens(); }
        }.runTaskTimer(this, 18000L, 18000L);

        // Sistema de Avisos Automáticos - A cada 2 horas (144.000 ticks)
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

        // Verificação de permissão OP para comandos admin
        if (Arrays.asList("modo", "sc", "set", "paredes", "angelwand", "clearlag", "rg", "control", "zoeira", "avisos").contains(c) && !p.isOp()) {
            p.sendMessage("§cSem permissão!"); return true;
        }

        switch (c) {
            case "ha": p.sendMessage("§b§lHUMAN ANGEL §fv1.4.0"); break;

            case "avisos":
                if (args.length == 0) { p.sendMessage("§e/avisos [add/remove/list] [mensagem]"); return true; }
                if (args[0].equalsIgnoreCase("add")) {
                    avisosServidor.add(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                    p.sendMessage("§aAviso adicionado!");
                } else if (args[0].equalsIgnoreCase("remove")) {
                    try { avisosServidor.remove(Integer.parseInt(args[1])); p.sendMessage("§cAviso removido!"); } catch(Exception e) { p.sendMessage("§cUse o ID da lista."); }
                } else if (args[0].equalsIgnoreCase("list")) {
                    p.sendMessage("§b§l--- LISTA DE AVISOS ---");
                    for (int i=0; i<avisosServidor.size(); i++) p.sendMessage("§e" + i + ": §f" + avisosServidor.get(i));
                }
                break;

            case "rglista":
                p.sendMessage("§b§l--- REGIÕES REGISTRADAS ---");
                if (regions.isEmpty()) p.sendMessage("§7Nenhuma região criada.");
                else for (String name : regions.keySet()) p.sendMessage("§f- " + name);
                break;

            case "tpa":
                if (args.length == 0) { p.sendMessage("§eUse: /tpa [nick]"); return true; }
                Player targetTpa = Bukkit.getPlayer(args[0]);
                if (targetTpa == null) { p.sendMessage("§cOffline!"); return true; }
                tpaRequests.put(targetTpa.getUniqueId(), p.getUniqueId());
                p.sendMessage("§aPedido enviado para " + targetTpa.getName());
                targetTpa.sendMessage("§e" + p.getName() + " quer ir até você. /tpaccept ou /tpadeny");
                break;

            case "tpaccept":
                if (tpaRequests.containsKey(p.getUniqueId())) {
                    Player requester = Bukkit.getPlayer(tpaRequests.get(p.getUniqueId()));
                    if (requester != null) { requester.teleport(p.getLocation()); requester.sendMessage("§aTeleportado!"); }
                    tpaRequests.remove(p.getUniqueId());
                } else p.sendMessage("§cSem pedidos.");
                break;

            case "tpadeny":
                tpaRequests.remove(p.getUniqueId()); p.sendMessage("§cRecusado."); break;

            case "set":
                fill(p, args, false); break;
            case "paredes":
                fill(p, args, true); break;

            case "lista":
                p.sendMessage("§b§l--- MEUS COMANDOS ---");
                p.sendMessage("§f/home, /sethome, /spawn, /tpa, /tpaccept, /tpadeny, /lista");
                if (p.isOp()) p.sendMessage("§eAdmin: /modo, /sc, /set, /paredes, /rg, /rglista, /control, /zoeira, /avisos");
                break;

            case "home":
                if (homes.containsKey(p.getUniqueId())) p.teleport(homes.get(p.getUniqueId()));
                else p.sendMessage("§c§lERRO: §fVocê não tem uma home salva! Use /sethome");
                break;

            case "sethome":
                homes.put(p.getUniqueId(), p.getLocation()); p.sendMessage("§aHome salva com sucesso!"); break;

            case "spawn": p.teleport(p.getWorld().getSpawnLocation()); p.sendMessage("§aIndo para o Spawn!"); break;

            case "control": openControlMenu(p); break;

            case "modo":
                if (args.length > 0) {
                    String m = args[0].toLowerCase();
                    if (m.equals("c")) p.setGameMode(GameMode.CREATIVE);
                    else if (m.equals("s")) p.setGameMode(GameMode.SURVIVAL);
                    p.sendMessage("§aModo alterado!");
                } break;

            case "angelwand":
                p.getInventory().addItem(new ItemStack(Material.WOODEN_AXE));
                p.sendMessage("§bMachado entregue!"); break;
            
            case "clearlag": limparItens(); break;
        }
        return true;
    }

    private void fill(Player p, String[] args, boolean walls) {
        Location l1 = pos1.get(p.getUniqueId()), l2 = pos2.get(p.getUniqueId());
        if (l1 == null || l2 == null || args.length == 0) { p.sendMessage("§cMarque Pos 1 e Pos 2!"); return; }
        Material mat = Material.matchMaterial(args[0].toUpperCase());
        if (mat == null) { p.sendMessage("§cBloco inválido!"); return; }
        int minX = Math.min(l1.getBlockX(), l2.getBlockX()), maxX = Math.max(l1.getBlockX(), l2.getBlockX());
        int minY = Math.min(l1.getBlockY(), l2.getBlockY()), maxY = Math.max(l1.getBlockY(), l2.getBlockY());
        int minZ = Math.min(l1.getBlockZ(), l2.getBlockZ()), maxZ = Math.max(l1.getBlockZ(), l2.getBlockZ());
        for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) {
            if (walls && (x > minX && x < maxX && z > minZ && z < maxZ)) continue;
            p.getWorld().getBlockAt(x, y, z).setType(mat);
        }
        p.sendMessage("§aBlocos alterados!");
    }

    // --- GUI CONTROL ---
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
                e.setCancelled(true); pos1.put(p.getUniqueId(), e.getClickedBlock().getLocation()); p.sendMessage("§bPos 1 marcada!");
            } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                e.setCancelled(true); pos2.put(p.getUniqueId(), e.getClickedBlock().getLocation()); p.sendMessage("§dPos 2 marcada!");
            }
        }
    }

    private void limparItens() {
        int i = 0;
        for (World w : Bukkit.getWorlds()) for (Entity en : w.getEntities()) if (en instanceof Item) { en.remove(); i++; }
        Bukkit.broadcastMessage("§e§l[ClearLag] §fForam limpos §6" + i + " §fitens.");
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
