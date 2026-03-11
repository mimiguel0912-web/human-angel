package com.humanangel;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
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
    private Map<UUID, UUID> tpaRequests = new HashMap<>();
    private List<String> filtroZueira = new ArrayList<>();
    private List<String> listaAvisos = new ArrayList<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        setupStorage();
        loadData();

        String[] cmds = {"ha", "modo", "home", "sethome", "spawn", "set", "paredes", "angelwand", "control", "lista", "clearlag", "tpa", "tpaccept", "tpdeny", "zueira", "avisos"};
        for (String s : cmds) {
            PluginCommand pc = getCommand(s);
            if (pc != null) pc.setExecutor(this);
        }

        // Loop de Avisos (1h 30min)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!listaAvisos.isEmpty()) {
                String aviso = listaAvisos.get(new Random().nextInt(listaAvisos.size()));
                Bukkit.broadcastMessage("§b§l[AVISO] §f" + aviso.replace("&", "§"));
            }
        }, 108000L, 108000L);

        getLogger().info("HumanAngel v1.6 - TUDO ATIVADO (Modo SP/A inclusos)!");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        String ip = e.getPlayer().getAddress().getAddress().getHostAddress();
        if (ip.startsWith("127.") || ip.startsWith("0.")) e.getPlayer().kickPlayer("§cVPN Bloqueada pelo HumanAngel!");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        for (String p : filtroZueira) {
            if (e.getMessage().toLowerCase().contains(p.toLowerCase())) {
                e.setCancelled(true);
                e.getPlayer().sendMessage("§c§lZUEIRA! §7Palavra proibida detectada.");
                return;
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String n = cmd.getName().toLowerCase();

        List<String> admins = Arrays.asList("modo", "set", "paredes", "angelwand", "control", "clearlag", "zueira", "avisos");
        if (admins.contains(n) && !p.isOp()) {
            p.sendMessage("§cVocê não tem XP suficiente na vida pra usar esse comando!");
            return true;
        }

        switch (n) {
            case "ha": p.sendMessage("§b§lHUMAN ANGEL §f- v1.6"); break;
            case "sethome": homes.put(p.getUniqueId(), p.getLocation()); saveData(); p.sendMessage("§aHome salva!"); break;
            case "home": if(homes.containsKey(p.getUniqueId())) p.teleport(homes.get(p.getUniqueId())); else p.sendMessage("§cSem home!"); break;
            case "spawn": p.teleport(p.getWorld().getSpawnLocation()); break;
            case "clearlag": clearLag(); break;
            case "angelwand": p.getInventory().addItem(getWand()); break;
            
            case "modo":
                if(args.length == 0) { p.sendMessage("§cUse: /modo [c/s/a/sp]"); return true; }
                if(args[0].equalsIgnoreCase("c")) p.setGameMode(GameMode.CREATIVE);
                else if(args[0].equalsIgnoreCase("s")) p.setGameMode(GameMode.SURVIVAL);
                else if(args[0].equalsIgnoreCase("a")) p.setGameMode(GameMode.ADVENTURE);
                else if(args[0].equalsIgnoreCase("sp")) p.setGameMode(GameMode.SPECTATOR);
                p.sendMessage("§aModo alterado!");
                break;

            case "control":
                if(args.length > 0) { Player t = Bukkit.getPlayer(args[0]); if(t != null) p.openInventory(t.getInventory()); }
                break;

            case "set": fill(p, args, false); break;
            case "paredes": fill(p, args, true); break;
            case "zueira": handleList(p, args, filtroZueira, "Zueira"); break;
            case "avisos": handleList(p, args, listaAvisos, "Aviso"); break;
            case "tpa": handleTPA(p, args); break;
            case "tpaccept": acceptTPA(p); break;
            case "tpdeny": tpaRequests.entrySet().removeIf(e -> e.getValue().equals(p.getUniqueId())); p.sendMessage("§cTPA Negado."); break;
            case "lista": 
                p.sendMessage("§b§lComandos: §f/home, /sethome, /spawn, /tpa, /tpaccept");
                if(p.isOp()) p.sendMessage("§e§lAdmin: §f/set, /paredes, /modo, /control, /zueira, /avisos, /clearlag");
                break;
        }
        return true;
    }

    private void fill(Player p, String[] args, boolean w) {
        Location l1 = pos1.get(p.getUniqueId()), l2 = pos2.get(p.getUniqueId());
        if (l1 == null || l2 == null || args.length == 0) { p.sendMessage("§cSelecione a área com a Wand!"); return; }
        Material mat = Material.matchMaterial(args[0].toUpperCase());
        if (mat == null) { p.sendMessage("§cBloco inválido!"); return; }
        int x1 = Math.min(l1.getBlockX(), l2.getBlockX()), x2 = Math.max(l1.getBlockX(), l2.getBlockX());
        int y1 = Math.min(l1.getBlockY(), l2.getBlockY()), y2 = Math.max(l1.getBlockY(), l2.getBlockY());
        int z1 = Math.min(l1.getBlockZ(), l2.getBlockZ()), z2 = Math.max(l1.getBlockZ(), l2.getBlockZ());
        for (int x = x1; x <= x2; x++) for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) {
            if (w && (x != x1 && x != x2 && z != z1 && z != z2)) continue;
            p.getWorld().getBlockAt(x, y, z).setType(mat);
        }
        p.sendMessage("§aBlocos colocados!");
    }

    @EventHandler
    public void onWand(PlayerInteractEvent e) {
        if (e.getPlayer().isOp() && e.getItem() != null && e.getItem().getType() == Material.WOODEN_AXE) {
            if (e.getClickedBlock() == null) return;
            e.setCancelled(true);
            if (e.getAction() == Action.LEFT_CLICK_BLOCK) { pos1.put(e.getPlayer().getUniqueId(), e.getClickedBlock().getLocation()); e.getPlayer().sendMessage("§bPos 1!"); }
            else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) { pos2.put(e.getPlayer().getUniqueId(), e.getClickedBlock().getLocation()); e.getPlayer().sendMessage("§dPos 2!"); }
        }
    }

    private ItemStack getWand() {
        ItemStack s = new ItemStack(Material.WOODEN_AXE);
        ItemMeta m = s.getItemMeta(); m.setDisplayName("§b§lAngel Wand"); s.setItemMeta(m);
        return s;
    }

    private void handleTPA(Player p, String[] args) {
        if(args.length == 0) return;
        Player t = Bukkit.getPlayer(args[0]);
        if(t != null) { tpaRequests.put(p.getUniqueId(), t.getUniqueId()); p.sendMessage("§eTPA enviado!"); t.sendMessage("§6" + p.getName() + " §equer TPA. /tpaccept"); }
    }

    private void acceptTPA(Player p) {
        for (Map.Entry<UUID, UUID> e : tpaRequests.entrySet()) {
            if (e.getValue().equals(p.getUniqueId())) {
                Player r = Bukkit.getPlayer(e.getKey());
                if(r != null) r.teleport(p.getLocation());
                tpaRequests.remove(e.getKey()); return;
            }
        }
    }

    private void clearLag() {
        int i = 0; for(World w : Bukkit.getWorlds()) for(Entity en : w.getEntities()) if(en instanceof Item) { en.remove(); i++; }
        Bukkit.broadcastMessage("§e§l[ClearLag] §fLimpeza concluída: §6" + i + " §fitens removidos.");
    }

    private void handleList(Player p, String[] args, List<String> list, String tipo) {
        if(args.length < 2) { if(args.length > 0 && args[0].equalsIgnoreCase("lista")) list.forEach(s -> p.sendMessage("§7- " + s)); return; }
        if(args[0].equalsIgnoreCase("add")) { 
            StringBuilder sb = new StringBuilder(); for(int i=1; i<args.length; i++) sb.append(args[i]).append(" ");
            list.add(sb.toString().trim()); p.sendMessage("§a" + tipo + " adicionado!"); 
        } else if(args[0].equalsIgnoreCase("remove")) { list.remove(args[1]); p.sendMessage("§e" + tipo + " removido!"); }
        saveData();
    }

    private void setupStorage() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        dataFile = new File(getDataFolder(), "dados.yml");
        if (!dataFile.exists()) try { dataFile.createNewFile(); } catch (IOException e) {}
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void loadData() {
        if (dataConfig.contains("homes")) for (String k : dataConfig.getConfigurationSection("homes").getKeys(false)) homes.put(UUID.fromString(k), (Location) dataConfig.get("homes." + k));
        filtroZueira = dataConfig.getStringList("zueira");
        listaAvisos = dataConfig.getStringList("avisos");
    }

    private void saveData() {
        try {
            dataConfig.set("zueira", filtroZueira); dataConfig.set("avisos", listaAvisos);
            for (Map.Entry<UUID, Location> e : homes.entrySet()) dataConfig.set("homes." + e.getKey(), e.getValue());
            dataConfig.save(dataFile);
        } catch (IOException e) {}
    }
                   }
