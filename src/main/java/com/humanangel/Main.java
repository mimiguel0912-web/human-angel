package com.humanangel;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main extends JavaPlugin implements CommandExecutor, Listener {

    private Map<UUID, Location> pos1 = new HashMap<>(), pos2 = new HashMap<>();
    private Map<UUID, Location> homes = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        
        // Inicializa o sistema de ficheiros com segurança
        setupStorage();
        loadHomes();

        // Registo de comandos (Precisam estar no plugin.yml!)
        String[] cmds = {"ha", "modo", "sc", "home", "sethome", "spawn", "set", "paredes", "angelwand", "control", "lista", "clearlag"};
        for (String s : cmds) {
            PluginCommand pc = getCommand(s);
            if (pc != null) {
                pc.setExecutor(this);
            }
        }
        
        getLogger().info("HumanAngel v1.6.0 carregado com sucesso!");
    }

    // --- SISTEMA DE ARMAZENAMENTO RESISTENTE A ERROS ---
    private void setupStorage() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        dataFile = new File(getDataFolder(), "dados.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Erro de Permissao: Nao foi possivel criar dados.yml!");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveHomes() {
        try {
            dataConfig.set("homes", null); // Limpa para atualizar
            for (Map.Entry<UUID, Location> entry : homes.entrySet()) {
                dataConfig.set("homes." + entry.getKey().toString(), entry.getValue());
            }
            dataConfig.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("Erro ao salvar dados das homes!");
        }
    }

    private void loadHomes() {
        if (dataConfig.contains("homes")) {
            for (String key : dataConfig.getConfigurationSection("homes").getKeys(false)) {
                homes.put(UUID.fromString(key), (Location) dataConfig.get("homes." + key));
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String n = cmd.getName().toLowerCase();

        // Proteção de permissão para Admin
        List<String> admin = Arrays.asList("modo", "set", "paredes", "angelwand", "control", "clearlag");
        if (admin.contains(n) && !p.isOp()) {
            p.sendMessage("§cComando restrito a administradores!");
            return true;
        }

        switch (n) {
            case "ha": p.sendMessage("§b§lHUMAN ANGEL §a[ON] §7v1.6.0"); break;
            
            case "sethome":
                homes.put(p.getUniqueId(), p.getLocation());
                saveHomes();
                p.sendMessage("§aHome salva no arquivo dados.yml!");
                break;

            case "home":
                if (homes.containsKey(p.getUniqueId())) p.teleport(homes.get(p.getUniqueId()));
                else p.sendMessage("§cVocê não possui uma home definida.");
                break;

            case "angelwand":
                ItemStack axe = new ItemStack(Material.WOODEN_AXE);
                ItemMeta meta = axe.getItemMeta();
                meta.setDisplayName("§b§lAngel Wand");
                axe.setItemMeta(meta);
                p.getInventory().addItem(axe);
                p.sendMessage("§bMachado de Seleção recebido!");
                break;

            case "set": fill(p, args, false); break;
            case "paredes": fill(p, args, true); break;
            case "spawn": p.teleport(p.getWorld().getSpawnLocation()); break;
            
            case "clearlag":
                int i = 0;
                for (Entity en : p.getWorld().getEntities()) {
                    if (en instanceof Item) { en.remove(); i++; }
                }
                Bukkit.broadcastMessage("§e§l[Limpeza] §fForam removidos §6" + i + " §fitens do chão.");
                break;
        }
        return true;
    }

    private void fill(Player p, String[] args, boolean walls) {
        Location l1 = pos1.get(p.getUniqueId()), l2 = pos2.get(p.getUniqueId());
        if (l1 == null || l2 == null || args.length == 0) {
            p.sendMessage("§cErro: Selecione a área com o AngelWand primeiro!");
            return;
        }
        Material mat = Material.matchMaterial(args[0].toUpperCase());
        if (mat == null) { p.sendMessage("§cBloco inválido!"); return; }

        int x1 = Math.min(l1.getBlockX(), l2.getBlockX()), x2 = Math.max(l1.getBlockX(), l2.getBlockX());
        int y1 = Math.min(l1.getBlockY(), l2.getBlockY()), y2 = Math.max(l1.getBlockY(), l2.getBlockY());
        int z1 = Math.min(l1.getBlockZ(), l2.getBlockZ()), z2 = Math.max(l1.getBlockZ(), l2.getBlockZ());

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    if (walls && (x != x1 && x != x2 && z != z1 && z != z2)) continue;
                    p.getWorld().getBlockAt(x, y, z).setType(mat);
                }
            }
        }
        p.sendMessage("§aAção concluída com sucesso!");
    }

    @EventHandler
    public void onWandUse(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (p.isOp() && e.getItem() != null && e.getItem().getType() == Material.WOODEN_AXE) {
            if (e.getClickedBlock() == null) return;
            e.setCancelled(true);
            if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                pos1.put(p.getUniqueId(), e.getClickedBlock().getLocation());
                p.sendMessage("§b§l[AngelWand] §fPosição 1 marcada!");
            } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                pos2.put(p.getUniqueId(), e.getClickedBlock().getLocation());
                p.sendMessage("§d§l[AngelWand] §fPosição 2 marcada!");
            }
        }
    }
                    }
