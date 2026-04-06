package com.humanangel;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("control").setExecutor(this);
        // Os outros comandos (home, sethome, etc) continuam aqui...
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("control")) {
            if (!p.hasPermission("humanangel.adm")) {
                p.sendMessage("§cSem permissão!");
                return true;
            }
            abrirMenuControl(p);
            return true;
        }
        return true;
    }

    // MENU 1: Lista de Jogadores (Cabeças)
    public void abrirMenuControl(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8Controle de Jogadores");

        for (Player online : Bukkit.getOnlinePlayers()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(online);
            meta.setDisplayName("§e" + online.getName());
            List<String> lore = new ArrayList<>();
            lore.add("§7Clique para gerenciar este jogador.");
            meta.setLore(lore);
            head.setItemMeta(meta);
            inv.addItem(head);
        }
        p.openInventory(inv);
    }

    // MENU 2: Ações para o Jogador Selecionado
    public void abrirMenuAcoes(Player adm, String alvoNome) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Gerenciar: " + alvoNome);

        // Botão Teleporte (Bússola)
        inv.setItem(10, criarItem(Material.COMPASS, "§aIr até o jogador", "§7Teleporta você até ele."));
        
        // Botão Puxar (Corda/Lead)
        inv.setItem(12, criarItem(Material.LEAD, "§ePuxar jogador", "§7Teleporta o jogador até você."));

        // Botão Ver Inventário (Baú)
        inv.setItem(14, criarItem(Material.CHEST, "§bVer Inventário", "§7Abre o inventário do jogador."));

        // Botão Congelar (Gelo)
        inv.setItem(16, criarItem(Material.ICE, "§fCongelar/Descongelar", "§7Trava o jogador no lugar."));

        adm.openInventory(inv);
    }

    private ItemStack criarItem(Material mat, String nome, String loreMsg) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(nome);
        List<String> lore = new ArrayList<>();
        lore.add(loreMsg);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void aoClicarNoMenu(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§8Controle de Jogadores")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

            String nomeAlvo = e.getCurrentItem().getItemMeta().getDisplayName().replace("§e", "");
            abrirMenuAcoes((Player) e.getWhoClicked(), nomeAlvo);
        }

        if (e.getView().getTitle().startsWith("§8Gerenciar: ")) {
            e.setCancelled(true);
            Player adm = (Player) e.getWhoClicked();
            String nomeAlvo = e.getView().getTitle().replace("§8Gerenciar: ", "");
            Player alvo = Bukkit.getPlayer(nomeAlvo);

            if (alvo == null) {
                adm.sendMessage("§cJogador offline!");
                adm.closeInventory();
                return;
            }

            switch (e.getRawSlot()) {
                case 10: // Teleporte
                    adm.teleport(alvo);
                    adm.sendMessage("§aVocê foi até " + alvo.getName());
                    break;
                case 12: // Puxar
                    alvo.teleport(adm);
                    adm.sendMessage("§aVocê puxou " + alvo.getName());
                    break;
                case 14: // Invsee
                    adm.openInventory(alvo.getInventory());
                    break;
                case 16: // Congelar
                    Bukkit.dispatchCommand(adm, "congelar " + alvo.getName());
                    adm.closeInventory();
                    break;
            }
        }
    }
}
