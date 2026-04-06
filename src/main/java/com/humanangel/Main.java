package com.humanangel;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("HumanAngel v2.0 - Todos os comandos e melhorias ativos!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        String comando = cmd.getName().toLowerCase();

        switch (comando) {
            [span_3](start_span)case "lista": //[span_3](end_span)
                p.sendMessage("§b--- COMANDOS HUMANANGEL ---");
                p.sendMessage("§fGeral: /home, /sethome, /tpa, /tpaccept, /spawn, /chapeu, /lixeira, /perfil, /morte, /compactar, /luz");
                if (p.hasPermission("humanangel.admin")) {
                    p.sendMessage("§eAdmin: /modo, /control, /clearlag, /corrigir, /mudarip, /anuncio, /aviso, /congelar, /zueira, /avisos");
                }
                break;

            [span_4](start_span)case "spawn": //[span_4](end_span)
                p.teleport(p.getWorld().getSpawnLocation());
                p.sendMessage("§aTeleportado ao Spawn do Servidor!");
                break;

            [span_5](start_span)case "sethome": //[span_5](end_span) (Upgrade: Infinitas com Nomes)
                String nomeSet = (args.length > 0) ? args[0].toLowerCase() : "home";
                salvarHome(p, nomeSet);
                p.sendMessage("§aHome '" + nomeSet + "' definida com sucesso!");
                break;

            [span_6](start_span)case "home": //[span_6](end_span) (Upgrade: Infinitas com Nomes)
                String nomeIr = (args.length > 0) ? args[0].toLowerCase() : "home";
                irHome(p, nomeIr);
                break;

            [span_7](start_span)case "luz": //[span_7](end_span)
                if (p.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                    p.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    p.sendMessage("§eVisão Noturna desativada.");
                } else {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 1));
                    p.sendMessage("§aVisão Noturna ativada.");
                }
                break;

            [span_8](start_span)case "lixeira": //[span_8](end_span)
                p.openInventory(Bukkit.createInventory(null, 36, "§8Lixeira"));
                break;

            [span_9](start_span)case "chapeu": //[span_9](end_span)
                if (p.getInventory().getItemInMainHand().getType() != Material.AIR) {
                    p.getInventory().setHelmet(p.getInventory().getItemInMainHand());
                    p.sendMessage("§aBloco colocado na cabeça!");
                }
                break;

            [span_10](start_span)case "modo": //[span_10](end_span)
                if (!p.hasPermission("humanangel.admin")) return true;
                p.setGameMode(p.getGameMode() == GameMode.SURVIVAL ? GameMode.CREATIVE : GameMode.SURVIVAL);
                p.sendMessage("§eModo de jogo alterado.");
                break;

            [span_11](start_span)case "control": //[span_11](end_span) (Upgrade: Menu de Cabeças)
                if (p.hasPermission("humanangel.admin")) abrirMenuControl(p);
                break;

            [span_12](start_span)case "corrigir": //[span_12](end_span)
                p.getInventory().getItemInMainHand().setDurability((short) 0);
                p.sendMessage("§aItem reparado!");
                break;

            [span_13](start_span)case "morte": //[span_13](end_span)
                p.setHealth(0);
                break;
                
            [span_14](start_span)case "anuncio": //[span_14](end_span)
                if (args.length > 0) {
                    String msg = String.join(" ", args).replace("&", "§");
                    for (Player all : Bukkit.getOnlinePlayers()) all.sendTitle("§6§lAVISO", msg, 10, 70, 20);
                }
                break;
        }
        return true;
    }

    // --- LÓGICA DE HOMES (Upgrade para Nomes e Infinitas) ---
    private void salvarHome(Player p, String nome) {
        String path = "homes." + p.getUniqueId() + "." + nome;
        getConfig().set(path + ".world", p.getLocation().getWorld().getName());
        getConfig().set(path + ".x", p.getLocation().getX());
        getConfig().set(path + ".y", p.getLocation().getY());
        getConfig().set(path + ".z", p.getLocation().getZ());
        saveConfig();
    }

    private void irHome(Player p, String nome) {
        String path = "homes." + p.getUniqueId() + "." + nome;
        if (!getConfig().contains(path)) {
            p.sendMessage("§cHome '" + nome + "' não encontrada!");
            return;
        }
        World w = Bukkit.getWorld(getConfig().getString(path + ".world"));
        double x = getConfig().getDouble(path + ".x");
        double y = getConfig().getDouble(path + ".y");
        double z = getConfig().getDouble(path + ".z");
        p.teleport(new Location(w, x, y, z));
        p.sendMessage("§aBem-vindo à sua home: " + nome);
    }

    // --- MENU DE CONTROLE AVANÇADO (Menu de Cabeças) ---
    public void abrirMenuControl(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8Controle de Jogadores");
        for (Player online : Bukkit.getOnlinePlayers()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(online);
            meta.setDisplayName("§e" + online.getName());
            head.setItemMeta(meta);
            inv.addItem(head);
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void aoClicar(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§8Controle de Jogadores")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;
            String alvoNome = e.getCurrentItem().getItemMeta().getDisplayName().replace("§e", "");
            Player alvo = Bukkit.getPlayer(alvoNome);
            if (alvo != null) {
                // Ao clicar na cabeça, abre o segundo menu de ações
                abrirMenuAcoes((Player) e.getWhoClicked(), alvo);
            }
        }
    }

    private void abrirMenuAcoes(Player adm, Player alvo) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Gerenciar: " + alvo.getName());
        // Botão TP
        ItemStack tp = new ItemStack(Material.COMPASS);
        ItemMeta m = tp.getItemMeta(); m.setDisplayName("§aIr até ele"); tp.setItemMeta(m);
        inv.setItem(11, tp);
        // Botão Puxar
        ItemStack puxar = new ItemStack(Material.LEAD);
        ItemMeta m2 = puxar.getItemMeta(); m2.setDisplayName("§eTrazer até mim"); puxar.setItemMeta(m2);
        inv.setItem(15, puxar);
        adm.openInventory(inv);
    }
}
