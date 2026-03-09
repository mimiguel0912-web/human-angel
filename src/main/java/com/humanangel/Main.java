package com.humanangel;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public class Main extends JavaPlugin implements Listener {

    private final Set<String> pvpOff = new HashSet<>();
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            int removidos = 0;
            for (World mundo : Bukkit.getWorlds()) {
                for (Entity entidade : mundo.getEntities()) {
                    if (entidade instanceof Item) {
                        entidade.remove();
                        removidos++;
                    }
                }
            }
            if (removidos > 0) {
                Bukkit.broadcastMessage("§6[HumanAngel] §fLimpeza: §e" + removidos + " itens §fremovidos.");
            }
        }, 0L, 12000L);

        getLogger().info("Human Angel v1.2 - TUDO ATIVADO!");
    }

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        String host = p.getAddress().getHostName().toLowerCase();
        if (host.contains("proxy") || host.contains("vpn")) {
            p.kickPlayer("§cVPN nao permitida!");
        }
    }

    @EventHandler
    public void noMobLag(EntitySpawnEvent e) {
        // CORREÇÃO DO ERRO: Usando double em vez de int para a 1.21
        if (e.getEntity().getLocation().getNearbyEntities(20.0, 20.0, 20.0).size() > 50) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void usarTridente(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if (item != null && item.getType() == Material.TRIDENT && item.hasItemMeta()) {
            if (item.getItemMeta().getDisplayName().contains("Tridente do Arcanjo")) {
                Player p = e.getPlayer();
                if (e.getAction().name().contains("LEFT_CLICK")) {
                    if (e.getClickedBlock() != null) {
                        pos1.put(p.getUniqueId(), e.getClickedBlock().getLocation());
                        p.sendMessage("§b[HumanAngel] §fPosicao 1 definida!");
                        e.setCancelled(true);
                    }
                } else if (e.getAction().name().contains("RIGHT_CLICK")) {
                    if (e.getClickedBlock() != null) {
                        pos2.put(p.getUniqueId(), e.getClickedBlock().getLocation());
                        p.sendMessage("§b[HumanAngel] §fPosicao 2 definida!");
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("mode")) {
            if (args.length < 2) return true;
            Player alvo = Bukkit.getPlayer(args[0]);
            if (alvo == null) return true;
            if (args[1].equalsIgnoreCase("c")) alvo.setGameMode(GameMode.CREATIVE);
            if (args[1].equalsIgnoreCase("s")) alvo.setGameMode(GameMode.SURVIVAL);
            if (args[1].equalsIgnoreCase("sp")) alvo.setGameMode(GameMode.SPECTATOR);
            p.sendMessage("§aModo de " + alvo.getName() + " alterado.");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("tamanho")) {
            if (args.length < 1) return true;
            double valor = Double.parseDouble(args[0]);
            p.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(valor);
            p.sendMessage("§aTamanho definido para " + valor);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("pvp")) {
            if (pvpOff.contains(p.getName())) {
                pvpOff.remove(p.getName());
                p.sendMessage("§cPvP ATIVADO!");
            } else {
                pvpOff.add(p.getName());
                p.sendMessage("§aPvP DESATIVADO!");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("angelwand")) {
            ItemStack tridente = new ItemStack(Material.TRIDENT);
            ItemMeta meta = tridente.getItemMeta();
            meta.setDisplayName("§b§lTridente do Arcanjo (Edit)");
            meta.setLore(Arrays.asList("§7Clique Esquerdo: Pos 1", "§7Clique Direito: Pos 2"));
            tridente.setItemMeta(meta);
            p.getInventory().addItem(tridente);
            p.sendMessage("§aVoce recebeu a ferramenta!");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("control")) {
            Inventory menu = Bukkit.createInventory(null, 54, "§0Controle");
            for (Player online : Bukkit.getOnlinePlayers()) {
                ItemStack itemMenu = new ItemStack(Material.PLAYER_HEAD);
                ItemMeta m = itemMenu.getItemMeta();
                m.setDisplayName("§e" + online.getName());
                itemMenu.setItemMeta(m);
                menu.addItem(itemMenu);
            }
            p.openInventory(menu);
            return true;
        }

        return false;
    }

    @EventHandler
    public void aoBater(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player && e.getEntity() instanceof Player) {
            if (pvpOff.contains(e.getDamager().getName()) || pvpOff.contains(e.getEntity().getName())) {
                e.setCancelled(true);
            }
        }
    }
}
