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
        createDataConfig();
        loadHomes(); // Carrega as homes salvas no arquivo

        String[] cmds = {"ha", "modo", "sc", "home", "sethome", "spawn", "set", "paredes", "angelwand", "control", "lista", "clearlag"};
        for (String s : cmds) {
            PluginCommand pc = getCommand(s);
            if (pc != null) pc.setExecutor(this);
        }
        Bukkit.getConsoleSender().sendMessage("§a[HumanAngel] v1.6.0 Ativado - RGs agora via WorldGuard!");
    }

    // --- SISTEMA DE SALVAMENTO DE HOMES ---
    private void createDataConfig() {
        dataFile = new File(getDataFolder(), "dados.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveHomes() {
        try {
            for (UUID uuid : homes.keySet()) {
                dataConfig.set("homes." + uuid.toString(), homes.get(uuid));
            }
            dataConfig.save(dataFile);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadHomes() {
        if (dataConfig.getConfigurationSection("homes") != null) {
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

        // Permissão para comandos Admin
        if (Arrays.asList("modo", "set", "paredes", "angelwand", "control", "clearlag").contains(n) && !p.isOp()) {
            p.sendMessage("§cVocê precisa ser OP!"); return true;
        }

        switch (n) {
            case "ha": p.sendMessage("§b§lHUMAN ANGEL §f- Integrado ao WorldGuard"); break;

            case "sethome":
                homes.put(p.getUniqueId(), p.getLocation());
                saveHomes();
                p.sendMessage("§aSua home foi salva e não será perdida no reinício!");
                break;

            case "home":
                if (homes.containsKey(p.getUniqueId())) p.teleport(homes.get(p.getUniqueId()));
                else p.sendMessage("§cVocê não tem uma home salva.");
                break;

            case "angelwand":
                ItemStack wand = new ItemStack(Material.WOODEN_AXE);
                ItemMeta meta = wand.getItemMeta();
                meta.setDisplayName("§b§lAngel Wand");
                wand.setItemMeta(meta);
                p.getInventory().addItem(wand);
                p.sendMessage("§aVocê recebeu o machado de seleção!");
                break;

            case "set": fill(p, args, false); break;
            case "paredes": fill(p, args, true); break;
            case "spawn": p.teleport(p.getWorld().getSpawnLocation()); break;
            
            case "modo":
                if (args.length > 0) {
                    String m = args[0].toLowerCase();
                    if (m.equals("c")) p.setGameMode(GameMode.CREATIVE);
                    else if (m.equals("s")) p.setGameMode(GameMode.SURVIVAL);
                    else if (m.equals("a")) p.setGameMode(GameMode.ADVENTURE);
                    else if (m.equals("sp")) p.setGameMode(GameMode.SPECTATOR);
                } break;
        }
        return true;
    }

    private void fill(Player p, String[] args, boolean walls) {
        Location l1 = pos1.get(p.getUniqueId()), l2 = pos2.get(p.getUniqueId());
        if (l1 == null || l2 == null || args.length == 0) { p.sendMessage("§cSelecione a área com o machado!"); return; }
        Material mat = Material.matchMaterial(args[0].toUpperCase());
        if (mat == null) return;
        
        int minX = Math.min(l1.getBlockX(), l2.getBlockX()), maxX = Math.max(l1.getBlockX(), l2.getBlockX());
        int minY = Math.min(l1.getBlockY(), l2.getBlockY()), maxY = Math.max(l1.getBlockY(), l2.getBlockY());
        int minZ = Math.min(l1.getBlockZ(), l2.getBlockZ()), maxZ = Math.max(l1.getBlockZ(), l2.getBlockZ());

        for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) {
            if (walls && (x != minX && x != maxX && z != minZ && z != maxZ)) continue;
            p.getWorld().getBlockAt(x, y, z).setType(mat);
        }
        p.sendMessage("§aConstrução finalizada!");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (p.isOp() && e.getItem() != null && e.getItem().getType() == Material.WOODEN_AXE) {
            if (e.getClickedBlock() == null) return;
            e.setCancelled(true);
            if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                pos1.put(p.getUniqueId(), e.getClickedBlock().getLocation());
                p.sendMessage("§bPosição 1 definida!");
            } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                pos2.put(p.getUniqueId(), e.getClickedBlock().getLocation());
                p.sendMessage("§dPosição 2 definida!");
            }
        }
    }
            }
