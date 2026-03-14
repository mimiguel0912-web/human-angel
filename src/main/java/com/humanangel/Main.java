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

        // REGISTRO OBRIGATÓRIO DE TODOS OS COMANDOS
        String[] cmds = {"ha","modo","home","sethome","spawn","control","lista","clearlag","tpa","tpaccept","zueira","avisos","luz","corrigir","chapeu","lixeira","perfil","anuncio","morte","compactar","sc"};
        for (String s : cmds) {
            PluginCommand pc = getCommand(s);
            if (pc != null) {
                pc.setExecutor(this);
            } else {
                getLogger().warning("Comando nao encontrado no plugin.yml: " + s);
            }
        }

        // Sistema de Avisos (30 minutos)
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

        // Verificação de Admin
        List<String> adminCmds = Arrays.asList("modo", "control", "clearlag", "anuncio", "sc", "avisos", "zueira", "corrigir");
        if (adminCmds.contains(n) && !p.isOp()) {
            p.sendMessage("§cERRO: Apenas STAFF pode usar este comando.");
            return true;
        }

        switch (n) {
            case "modo":
                if (args.length == 0) { p.sendMessage("§eUse: /modo [c, s, a, sp]"); break; }
                String m = args[0].toLowerCase();
                if (m.equals("c")) p.setGameMode(GameMode.CREATIVE);
                else if (m.equals("s")) p.setGameMode(GameMode.SURVIVAL);
                else if (m.equals("a")) p.setGameMode(GameMode.ADVENTURE);
                else if (m.equals("sp")) p.setGameMode(GameMode.SPECTATOR);
                p.sendMessage("§aModo alterado!");
                break;

            case "perfil":
                p.sendMessage("§b§l--- SEU PERFIL ---");
                p.sendMessage("§fNome: §b" + p.getName());
                p.sendMessage("§fVida: §c" + (int)p.getHealth() + " HP");
                p.sendMessage("§fFome: §6" + p.getFoodLevel());
                p.sendMessage("§fXP: §aNivel " + p.getLevel());
                p.sendMessage("§fMundo: §e" + p.getWorld().getName());
                p.sendMessage("§fPing: §f" + p.getPing() + "ms");
                break;

            case "lista":
                p.sendMessage("§b§l--- COMANDOS DISPONÍVEIS ---");
                p.sendMessage("§fJogadores: §7/home, /sethome, /spawn, /tpa, /tpaccept, /perfil, /luz, /chapeu, /lixeira, /compactar, /morte");
                if (p.isOp()) p.sendMessage("§dStaff: §7/modo, /control, /anuncio, /sc, /avisos, /zueira, /clearlag");
                break;

            case "chapeu":
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) { p.sendMessage("§cSegure algo!"); break; }
                ItemStack helm = p.getInventory().getHelmet();
                p.getInventory().setHelmet(hand);
                p.getInventory().setItemInMainHand(helm);
                p.updateInventory(); // Crucial para Bedrock
                p.sendMessage("§aChapéu colocado!");
                break;

            case "control":
                openControlMenu(p);
                break;

            case "sc":
                if (args.length == 0) break;
                String msg = "§d[STAFF] §f" + p.getName() + ": " + String.join(" ", args).replace("&", "§");
                Bukkit.getOnlinePlayers().stream().filter(Player::isOp).forEach(s -> s.sendMessage(msg));
                break;

            case "anuncio":
                if (args.length == 0) break;
                String txt = String.join(" ", args).replace("&", "§");
                Bukkit.getOnlinePlayers().forEach(all -> all.sendTitle("§6§lAVISO", txt, 10, 70, 20));
                break;

            case "sethome": homes.put(p.getUniqueId(), p.getLocation()); saveData(); p.sendMessage("§aHome salva!"); break;
            case "home": if(homes.containsKey(p.getUniqueId())) p.teleport(homes.get(p.getUniqueId())); else p.sendMessage("§cSem home!"); break;
            case "spawn": p.teleport(p.getWorld().getSpawnLocation()); break;
            case "clearlag": clearLag(); break;
            case "compactar": compactar(p); break;
            case "lixeira": p.openInventory(Bukkit.createInventory(null, 36, "§8Lixeira")); break;
        }
        return true;
    }

    private void openControlMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§0Controle de Jogadores");
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
        if (e.getView().getTitle().equals("§0Controle de Jogadores")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;
            String tName = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
            Player target = Bukkit.getPlayer(tName);
            if (target != null) e.getWhoClicked().openInventory(target.getInventory());
        }
    }

    private void clearLag() {
        int c = 0;
        for(World w : Bukkit.getWorlds()) for(Entity en : w.getEntities()) if(en instanceof Item) { en.remove(); c++; }
        Bukkit.broadcastMessage("§e§l[Limpeza] §6" + c + " §fitens removidos.");
    }

    private void compactar(Player p) {
        int d = 0;
        for (ItemStack i : p.getInventory().getContents()) {
            if (i != null && i.getType() == Material.DIAMOND) { d += i.getAmount(); i.setAmount(0); }
        }
        if (d >= 9) {
            p.getInventory().addItem(new ItemStack(Material.DIAMOND_BLOCK, d / 9));
            if (d % 9 > 0) p.getInventory().addItem(new ItemStack(Material.DIAMOND, d % 9));
            p.sendMessage("§aCompactado!");
        } else p.sendMessage("§cPrecisa de 9 diamantes.");
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
