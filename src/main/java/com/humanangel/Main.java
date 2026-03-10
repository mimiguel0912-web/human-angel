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

    private Map<UUID, Boolean> pvpState = new HashMap<>(); // PvP por jogador
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
        
        // Avisos automáticos (2 horas)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!zoeiraMessages.isEmpty()) {
                String msg = zoeiraMessages.get(new Random().nextInt(zoeiraMessages.size()));
                Bukkit.broadcastMessage("§6§l[AVISO] §f" + ChatColor.translateAlternateColorCodes('&', msg));
            }
        }, 144000L, 144000L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String c = cmd.getName().toLowerCase();

        // Bloqueio ADM (Somente OP)
        if (Arrays.asList("mode", "control", "sc", "ha", "angelwand", "set", "paredes", "zoeira").contains(c)) {
            if (!p.isOp()) { p.sendMessage("§cSem permissão!"); return true; }
        }

        switch (c) {
            case "ha": p.sendMessage("§b§lHUMAN ANGEL §fv1.4"); break;
            case "home":
                if (!homes.containsKey(p.getUniqueId())) {
                    p.sendMessage("§cVocê não tem uma home!!");
                } else {
                    p.teleport(homes.get(p.getUniqueId()));
                    p.sendMessage("§aTeleportado para sua home!");
                }
                break;
            case "sethome":
                homes.put(p.getUniqueId(), p.getLocation());
                p.sendMessage("§aHome definida com sucesso!");
                break;
            case "pvp": // PvP por Jogador
                boolean atual = pvpState.getOrDefault(p.getUniqueId(), true);
                pvpState.put(p.getUniqueId(), !atual);
                p.sendMessage(!atual ? "§aSeu PvP agora está LIGADO!" : "§cSeu PvP agora está DESLIGADO!");
                break;
            case "control": // Correção do Menu
                Inventory inv = Bukkit.createInventory(null, 54, "§0Controle de Players");
                for (Player o : Bukkit.getOnlinePlayers()) {
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    ItemMeta m = head.getItemMeta(); m.setDisplayName("§e" + o.getName()); head.setItemMeta(m);
                    inv.addItem(head);
                }
                p.openInventory(inv);
                break;
            case "zoeira":
                if (args.length > 0) {
                    zoeiraMessages.add(String.join(" ", args));
                    p.sendMessage("§aAviso adicionado!");
                } break;
        }
        return true;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
            Player vitima = (Player) e.getEntity();
            Player atacante = (Player) e.getDamager();
            // Se um dos dois estiver com PvP OFF, cancela
            if (!pvpState.getOrDefault(vitima.getUniqueId(), true) || !pvpState.getOrDefault(atacante.getUniqueId(), true)) {
                e.setCancelled(true);
                atacante.sendMessage("§cO PvP de um de vocês está desativado!");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {
        // Melhora compatibilidade com Bedrock/Geyser
        if (scActive.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            String msg = "§d[StaffChat] " + e.getPlayer().getName() + ": §f" + e.getMessage();
            Bukkit.getConsoleSender().sendMessage(msg);
            for (UUID id : scActive) {
                Player s = Bukkit.getPlayer(id);
                if (s != null) s.sendMessage(msg);
            }
        }
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§0Controle de Players")) {
            e.setCancelled(true);
            if (e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta()) {
                String targetName = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
                Player target = Bukkit.getPlayer(targetName);
                if (target != null) e.getWhoClicked().openInventory(target.getInventory());
            }
        }
    }
}
