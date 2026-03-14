package com.humanangel;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main extends JavaPlugin implements CommandExecutor, Listener {

    private Map<UUID, Location> homes = new HashMap<>();
    private Map<UUID, UUID> tpaRequests = new HashMap<>();
    private List<String> filtroZueira = new ArrayList<>();
    private List<String> listaAvisos = new ArrayList<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        setupStorage();
        loadData();
        getServer().getPluginManager().registerEvents(this, this);

        // Registro garantido de todos os comandos
        String[] cmds = {"ha","modo","home","sethome","spawn","control","lista","clearlag","tpa","tpaccept","zueira","avisos","luz","corrigir","chapeu","lixeira","perfil","anuncio","morte","compactar","sc"};
        for (String s : cmds) {
            PluginCommand pc = getCommand(s);
            if (pc != null) pc.setExecutor(this);
        }

        // Sistema de Avisos Automáticos (30 Minutos)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!listaAvisos.isEmpty()) {
                String aviso = listaAvisos.get(new Random().nextInt(listaAvisos.size())).replace("&", "§");
                Bukkit.broadcastMessage("§b§l[AVISO] §f" + aviso);
            }
        }, 36000L, 36000L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String n = cmd.getName().toLowerCase();

        // Verificação de Admin para comandos sensíveis
        List<String> adminCmds = Arrays.asList("modo", "control", "clearlag", "anuncio", "sc", "avisos", "zueira", "corrigir");
        if (adminCmds.contains(n) && !p.isOp()) {
            p.sendMessage("§cVocê precisa ser OP para usar este comando!");
            return true;
        }

        switch (n) {
            case "modo":
                if (args.length == 0) { p.sendMessage("§eUse: /modo [c, s, a, sp]"); break; }
                if (args[0].equalsIgnoreCase("c")) p.setGameMode(GameMode.CREATIVE);
                else if (args[0].equalsIgnoreCase("s")) p.setGameMode(GameMode.SURVIVAL);
                else if (args[0].equalsIgnoreCase("a")) p.setGameMode(GameMode.ADVENTURE);
                else if (args[0].equalsIgnoreCase("sp")) p.setGameMode(GameMode.SPECTATOR);
                p.sendMessage("§aSeu modo de jogo foi alterado!");
                break;

            case "control":
                openControlMenu(p);
                break;

            case "chapeu":
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) { p.sendMessage("§cSegure um bloco na mão!"); break; }
                ItemStack helm = p.getInventory().getHelmet();
                p.getInventory().setHelmet(hand);
                p.getInventory().setItemInMainHand(helm);
                p.updateInventory(); // Garante que o Bedrock veja a mudança
                p.sendMessage("§eChapéu equipado!");
                break;

            case "sc":
                if (args.length == 0) break;
                String msgSc = String.join(" ", args).replace("&", "§");
                for (Player staff : Bukkit.getOnlinePlayers()) {
                    if (staff.isOp()) staff.sendMessage("§d[STAFF] §f" + p.getName() + ": " + msgSc);
                }
                break;

            case "anuncio":
                if (args.length == 0) break;
                String texto = String.join(" ", args).replace("&", "§");
                for (Player all : Bukkit.getOnlinePlayers()) all.sendTitle("§6§lANUNCIO", texto, 10, 70, 20);
                break;

            case "sethome": homes.put(p.getUniqueId(), p.getLocation()); saveData(); p.sendMessage("§aHome salva!"); break;
            case "home": if(homes.containsKey(p.getUniqueId())) p.teleport(homes.get(p.getUniqueId())); else p.sendMessage("§cSem home!"); break;
            case "spawn": p.teleport(p.getWorld().getSpawnLocation()); break;
            case "perfil": p.sendMessage("§b§lPERFIL: §f" + p.getName() + " §7| §c" + (int)p.getHealth() + " HP"); break;
            case "clearlag": clearLag(); break;
            case "compactar": compactar(p); break;
            case "lixeira": p.openInventory(Bukkit.createInventory(null, 36, "§8Lixeira")); break;
        }
        return true;
    }

    // --- SISTEMA DE CONTROLE (MENU) ---
    private void openControlMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§0Menu de Controle");
        for (Player online : Bukkit.getOnlinePlayers()) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) skull.getItemMeta();
            sm.setOwningPlayer(online);
            sm.setDisplayName("§a" + online.getName());
            skull.setItemMeta(sm);
            inv.addItem(skull);
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§0Menu de Controle")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;
            String targetName = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) e.getWhoClicked().openInventory(target.getInventory());
        }
    }

    // --- AUXILIARES ---
    private void clearLag() {
        int count = 0;
        for(World w : Bukkit.getWorlds()) for(Entity en : w.getEntities()) if(en instanceof Item) { en.remove(); count++; }
        Bukkit.broadcastMessage("§e§l[Limpeza] §6" + count + " §fitens removidos do chão.");
    }

    private void compactar(Player p) {
        int diamantes = 0;
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND) {
                diamantes += item.getAmount();
                item.setAmount(0);
            }
        }
        int blocos = diamantes / 9;
        int resto = diamantes % 9;
        if (blocos > 0) p.getInventory().addItem(new ItemStack(Material.DIAMOND_BLOCK, blocos));
        if (resto > 0) p.getInventory().addItem(new ItemStack(Material.DIAMOND, resto));
        p.sendMessage("§aDiamantes compactados!");
    }

    private void setupStorage() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        dataFile = new File(getDataFolder(), "dados.yml");
        if (!dataFile.exists()) try { dataFile.createNewFile(); } catch (IOException e) {}
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void loadData() {
        filtroZueira = dataConfig.getStringList("zueira");
        listaAvisos = dataConfig.getStringList("avisos");
    }

    private void saveData() {
        try { dataConfig.set("zueira", filtroZueira); dataConfig.set("avisos", listaAvisos); dataConfig.save(dataFile); } catch (IOException e) {}
    }
}
