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
        
        // CLEAR LAG AUTOMÁTICO (A cada 10 minutos)
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
            Bukkit.broadcastMessage("§6[HumanAngel] §fLimpeza: §e" + removidos + " itens §fremovidos do chão.");
        }, 0L, 12000L);

        getLogger().info("Human Angel v1.2 - TUDO ATIVADO!");
    }

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        // ANTI-VPN SIMPLES
        Player p = e.getPlayer();
        String host = p.getAddress().getHostName().toLowerCase();
        if (host.contains("proxy") || host.contains("vpn") || host.contains("cloud")) {
            p.kickPlayer("§cVPN/Proxy não permitida!");
        }
    }

    @EventHandler
    public void noMobLag(EntitySpawnEvent e) {
        // Se houver mais de 50 entidades perto, cancela o spawn
        if (e.getEntity().getLocation().getNearbyEntities(20, 20, 20).size() > 50) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void usarTridente(PlayerInteractEvent e) {
        // FERRAMENTA TRIDENTE (WorldEdit)
        ItemStack item = e.getItem();
        if (item != null && item.getType() == Material.TRIDENT && item.hasItemMeta()) {
            if (item.getItemMeta().getDisplayName().contains("Tridente do Arcanjo")) {
                Player p = e.getPlayer();
                if (e.getAction().name().contains("LEFT_CLICK")) {
                    if (e.getClickedBlock() != null) {
                        pos1.put(p.getUniqueId(), e.getClickedBlock().getLocation());
                        p.sendMessage("§b[HumanAngel] §fPosição 1 definida!");
                        e.setCancelled(true);
                    }
                } else if (e.getAction().name().contains("RIGHT_CLICK")) {
                    if (e.getClickedBlock() != null) {
                        pos2.put(p.getUniqueId(), e.getClickedBlock().getLocation());
                        p.sendMessage("§b[HumanAngel] §fPosição 2 definida!");
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender remetente, Command cmd, String label, String[] argumentos) {
        if (!(remetente instanceof Player)) return true;
        Player p = (Player) remetente;

        // /MODE <NICK> <C/S/A/SP>
        if (cmd.getName().equalsIgnoreCase("mode")) {
            if (argumentos.length < 2) {
                p.sendMessage("§eUse: /mode <nick> <c/s/a/sp>");
                return true;
            }
            Player alvo = Bukkit.getPlayer(argumentos[0]);
            if (alvo == null) return true;
            switch (argumentos[1].toLowerCase()) {
                case "c": alvo.setGameMode(GameMode.CREATIVE); break;
                case "s": alvo.setGameMode(GameMode.SURVIVAL); break;
                case "a": alvo.setGameMode(GameMode.ADVENTURE); break;
                case "sp": alvo.setGameMode(GameMode.SPECTATOR); break;
            }
            p.sendMessage("§aModo de " + alvo.getName() + " alterado.");
            return true;
        }

        // /TAMANHO <VALOR>
        if (cmd.getName().equalsIgnoreCase("tamanho")) {
            if (argumentos.length < 1) return true;
            double valor = Double.parseDouble(argumentos[0]);
            p.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(valor);
            p.sendMessage("§aSeu tamanho agora é " + valor);
            return true;
        }

        // /PVP
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

        // /ANGELWAND (Pega o Tridente)
        if (cmd.getName().equalsIgnoreCase("angelwand")) {
            ItemStack tridente = new ItemStack(Material.TRIDENT);
            ItemMeta meta = tridente.getItemMeta();
            meta.setDisplayName("§b§lTridente do Arcanjo (Edit)");
            meta.setLore(Arrays.asList("§7Clique Esquerdo: Pos 1", "§7Clique Direito: Pos 2"));
            tridente.setItemMeta(meta);
            p.getInventory().addItem(tridente);
            p.sendMessage("§aVocê recebeu a ferramenta sagrada!");
            return true;
        }

        // /CONTROL (Menu de Inventários)
        if (cmd.getName().equalsIgnoreCase("control")) {
            Inventory menu = Bukkit.createInventory(null, 54, "§0Controle de Jogadores");
            for (Player online : Bukkit.getOnlinePlayers()) {
                ItemStack itemMenu = new ItemStack(Material.PLAYER_HEAD);
                ItemMeta metaMenu = itemMenu.getItemMeta();
                metaMenu.setDisplayName("§e" + online.getName());
                itemMenu.setItemMeta(metaMenu);
                menu.addItem(itemMenu);
            }
            p.openInventory(menu);
            return true;
        }

        return false;
    }

    @EventHandler
    public void aoBater(EntityDamageByEntityEvent evento) {
        if (evento.getDamager() instanceof Player && evento.getEntity() instanceof Player) {
            if (pvpOff.contains(evento.getDamager().getName()) || pvpOff.contains(evento.getEntity().getName())) {
                evento.setCancelled(true);
                evento.getDamager().sendMessage("§cO combate está desligado!");
            }
        }
    }
    }
