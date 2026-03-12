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
import org.bukkit.potion.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main extends JavaPlugin implements CommandExecutor, Listener {

    private Map<UUID, Location> homes = new HashMap<>();
    private List<String> filtroZueira = new ArrayList<>();
    private List<String> listaAvisos = new ArrayList<>();
    private Set<UUID> congelados = new HashSet<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        setupStorage();
        loadData();

        String[] cmds = {"ha", "modo", "home", "sethome", "spawn", "control", "lista", "clearlag", "tpa", "tpaccept", "zueira", "avisos", "luz", 
                         "corrigir", "mudarip", "chapeu", "lixeira", "perfil", "anuncio", "congelar", "morte", "compactar"};
        for (String s : cmds) {
            PluginCommand pc = getCommand(s);
            if (pc != null) pc.setExecutor(this);
        }

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!listaAvisos.isEmpty()) {
                String aviso = listaAvisos.get(new Random().nextInt(listaAvisos.size()));
                Bukkit.broadcastMessage("§b§l[AVISO] §f" + aviso.replace("&", "§"));
            }
        }, 108000L, 108000L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String n = cmd.getName().toLowerCase();

        // Verificação de Admin
        List<String> adminOnly = Arrays.asList("modo", "control", "zueira", "avisos", "clearlag", "corrigir", "mudarip", "anuncio", "congelar");
        if (adminOnly.contains(n) && !p.isOp()) {
            p.sendMessage("§cVocê não tem nível de Admin para isso!");
            return true;
        }

        switch (n) {
            case "control": openControlMenu(p); break;
            case "luz": toggleNightVision(p); break;
            case "sethome": homes.put(p.getUniqueId(), p.getLocation()); saveData(); p.sendMessage("§aHome salva!"); break;
            case "home": if(homes.containsKey(p.getUniqueId())) p.teleport(homes.get(p.getUniqueId())); else p.sendMessage("§cSem home!"); break;
            case "morte": p.setHealth(0); p.sendMessage("§7Você escolheu o descanso eterno..."); break;
            case "lixeira": p.openInventory(Bukkit.createInventory(null, 36, "§8Lixeira Automática")); break;
            
            case "corrigir":
                ItemStack item = p.getInventory().getItemInMainHand();
                if (item != null && item.getType() != Material.AIR) {
                    item.setDurability((short) 0);
                    p.sendMessage("§aItem restaurado com sucesso!");
                } break;

            case "chapeu":
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) break;
                ItemStack helm = p.getInventory().getHelmet();
                p.getInventory().setHelmet(hand);
                p.getInventory().setItemInMainHand(helm);
                p.sendMessage("§eBelo chapéu!"); break;

            case "compactar":
                compactar(p); break;

            case "anuncio":
                if (args.length == 0) break;
                String msg = String.join(" ", args).replace("&", "§");
                for (Player all : Bukkit.getOnlinePlayers()) all.sendTitle("§6§lAVISO", msg, 10, 70, 20);
                break;

            case "congelar":
                if (args.length == 0) break;
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) break;
                if (congelados.contains(target.getUniqueId())) {
                    congelados.remove(target.getUniqueId());
                    p.sendMessage("§aJogador descongelado.");
                } else {
                    congelados.add(target.getUniqueId());
                    p.sendMessage("§cJogador congelado para inspeção.");
                } break;

            case "mudarip":
                if (args.length == 0) break;
                Player t = Bukkit.getPlayer(args[0]);
                if (t != null) p.sendMessage("§eIP de " + t.getName() + ": §f" + t.getAddress().getAddress().getHostAddress());
                break;
                
            case "perfil":
                p.sendMessage("§b§l--- SEU PERFIL ---");
                p.sendMessage("§fNome: " + p.getName());
                p.sendMessage("§fVida: " + (int)p.getHealth() + "/20");
                p.sendMessage("§fXP: " + p.getLevel());
                break;
        }
        return true;
    }

    private void compactar(Player p) {
        for (ItemStack is : p.getInventory().getContents()) {
            if (is == null) continue;
            if (is.getType() == Material.DIAMOND && is.getAmount() >= 9) {
                is.setAmount(is.getAmount() - 9);
                p.getInventory().addItem(new ItemStack(Material.DIAMOND_BLOCK));
            }
            // Adicione outros materiais aqui se quiser (Ferro, Ouro, etc)
        }
        p.sendMessage("§aMinérios compactados!");
    }

    private void toggleNightVision(Player p) {
        if (p.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            p.removePotionEffect(PotionEffectType.NIGHT_VISION);
            p.sendMessage("§eVisão noturna desativada.");
        } else {
            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 1));
            p.sendMessage("§bVisão noturna ativada.");
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (congelados.contains(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    public void openControlMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§0Controle de Jogadores");
        for (Player online : Bukkit.getOnlinePlayers()) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) skull.getItemMeta();
            sm.setOwningPlayer(online);
            sm.setDisplayName("§a" + online.getName());
            sm.setLore(Arrays.asList("§7Clique para ver o inventário"));
            skull.setItemMeta(sm);
            inv.addItem(skull);
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§0Controle de Jogadores")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            Player target = Bukkit.getPlayer(ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()));
            if (target != null) e.getWhoClicked().openInventory(target.getInventory());
        }
    }

    private void setupStorage() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        dataFile = new File(getDataFolder(), "dados.yml");
        if (!dataFile.exists()) try { dataFile.createNewFile(); } catch (IOException e) {}
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void loadData() {
        if (dataConfig.contains("homes")) {
            for (String key : dataConfig.getConfigurationSection("homes").getKeys(false))
                homes.put(UUID.fromString(key), (Location) dataConfig.get("homes." + key));
        }
        filtroZueira = dataConfig.getStringList("zueira");
        listaAvisos = dataConfig.getStringList("avisos");
    }

    private void saveData() {
        try {
            dataConfig.set("zueira", filtroZueira);
            dataConfig.set("avisos", listaAvisos);
            for (Map.Entry<UUID, Location> e : homes.entrySet()) dataConfig.set("homes." + e.getKey().toString(), e.getValue());
            dataConfig.save(dataFile);
        } catch (IOException e) {}
    }
}
