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
    private Map<UUID, UUID> tpaRequests = new HashMap<>();
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

        // TODOS OS COMANDOS (Antigos + Novos)
        String[] cmds = {
            "ha", "modo", "home", "sethome", "spawn", "control", "lista", "clearlag", 
            "tpa", "tpaccept", "tpdeny", "zueira", "avisos", "luz", "corrigir", 
            "mudarip", "chapeu", "lixeira", "perfil", "anuncio", "congelar", "morte", "compactar"
        };
        
        for (String s : cmds) {
            PluginCommand pc = getCommand(s);
            if (pc != null) pc.setExecutor(this);
        }

        // Sistema de Avisos Automáticos (1h 30min)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!listaAvisos.isEmpty()) {
                String aviso = listaAvisos.get(new Random().nextInt(listaAvisos.size()));
                Bukkit.broadcastMessage("§b§l[AVISO] §f" + aviso.replace("&", "§"));
            }
        }, 108000L, 108000L);

        getLogger().info("§a[HumanAngel] v1.6 - Todos os comandos integrados e salvando!");
    }

    // --- EVENTOS (ANTI-VPN, ZUEIRA, CONGELAR) ---
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        String ip = e.getPlayer().getAddress().getAddress().getHostAddress();
        if (ip.startsWith("127.") || ip.startsWith("0.")) {
            e.getPlayer().kickPlayer("§c§lHUMAN ANGEL\n\n§7VPN Detectada! Desligue para entrar.");
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (congelados.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cVocê está congelado por um Admin!");
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        String msg = e.getMessage().toLowerCase();
        for (String p : filtroZueira) {
            if (msg.contains(p.toLowerCase())) {
                e.setCancelled(true);
                e.getPlayer().sendMessage("§c§lZUEIRA! §7Palavra proibida no chat.");
                return;
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String n = cmd.getName().toLowerCase();

        // Filtro de Admins
        List<String> adminCmds = Arrays.asList("modo", "control", "zueira", "avisos", "clearlag", "corrigir", "mudarip", "anuncio", "congelar");
        if (adminCmds.contains(n) && !p.isOp()) {
            p.sendMessage("§cVocê não tem permissão para usar comandos de Admin!");
            return true;
        }

        switch (n) {
            // --- COMANDOS ANTIGOS ---
            case "ha": p.sendMessage("§b§lHUMAN ANGEL §f- v1.6 §a[ON]"); break;
            case "sethome": homes.put(p.getUniqueId(), p.getLocation()); saveData(); p.sendMessage("§aHome salva!"); break;
            case "home": if(homes.containsKey(p.getUniqueId())) p.teleport(homes.get(p.getUniqueId())); else p.sendMessage("§cSem home!"); break;
            case "spawn": p.teleport(p.getWorld().getSpawnLocation()); p.sendMessage("§eTeleportado ao Spawn!"); break;
            case "clearlag": clearLag(); break;
            case "tpa": handleTPA(p, args); break;
            case "tpaccept": acceptTPA(p); break;
            case "tpdeny": tpaRequests.entrySet().removeIf(e -> e.getValue().equals(p.getUniqueId())); p.sendMessage("§cTPA Negado."); break;

            // --- COMANDOS NOVOS ---
            case "modo":
                if(args.length == 0) return true;
                String m = args[0].toLowerCase();
                if(m.equals("c")) p.setGameMode(GameMode.CREATIVE);
                else if(m.equals("s")) p.setGameMode(GameMode.SURVIVAL);
                else if(m.equals("a")) p.setGameMode(GameMode.ADVENTURE);
                else if(m.equals("sp")) p.setGameMode(GameMode.SPECTATOR);
                p.sendMessage("§aModo alterado!"); break;

            case "control": openControlMenu(p); break;
            case "luz": toggleLuz(p); break;
            case "corrigir": repairItem(p); break;
            case "morte": p.setHealth(0); break;
            case "lixeira": p.openInventory(Bukkit.createInventory(null, 36, "§8Lixeira")); break;
            case "chapeu": toggleChapeu(p); break;
            case "compactar": compactar(p); break;
            case "anuncio": if(args.length > 0) broadcastTitle(String.join(" ", args)); break;
            case "congelar": handleFreeze(p, args); break;
            case "mudarip": if(args.length > 0) checkIP(p, args[0]); break;
            case "zueira": handleList(p, args, filtroZueira, "Zueira"); break;
            case "avisos": handleList(p, args, listaAvisos, "Aviso"); break;
            case "lista": showHelp(p); break;
        }
        return true;
    }

    // --- FUNÇÕES AUXILIARES ---
    private void clearLag() {
        int i = 0;
        for(World w : Bukkit.getWorlds()) for(Entity en : w.getEntities()) if(en instanceof Item) { en.remove(); i++; }
        Bukkit.broadcastMessage("§e§l[ClearLag] §6" + i + " §fitens removidos.");
    }

    private void handleTPA(Player p, String[] args) {
        if(args.length == 0) return;
        Player t = Bukkit.getPlayer(args[0]);
        if(t != null) { tpaRequests.put(p.getUniqueId(), t.getUniqueId()); p.sendMessage("§ePedido enviado!"); t.sendMessage("§6" + p.getName() + " §equer TPA. /tpaccept"); }
    }

    private void acceptTPA(Player p) {
        UUID req = null;
        for(Map.Entry<UUID, UUID> e : tpaRequests.entrySet()) if(e.getValue().equals(p.getUniqueId())) req = e.getKey();
        if(req != null) { Player r = Bukkit.getPlayer(req); if(r != null) r.teleport(p.getLocation()); tpaRequests.remove(req); }
    }

    private void openControlMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§0Controle");
        for(Player online : Bukkit.getOnlinePlayers()) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) skull.getItemMeta();
            sm.setOwningPlayer(online); sm.setDisplayName("§a" + online.getName());
            skull.setItemMeta(sm); inv.addItem(skull);
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        if(e.getView().getTitle().equals("§0Controle")) {
            e.setCancelled(true);
            if(e.getCurrentItem() == null) return;
            Player t = Bukkit.getPlayer(ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()));
            if(t != null) e.getWhoClicked().openInventory(t.getInventory());
        }
    }

    private void toggleLuz(Player p) {
        if(p.hasPotionEffect(PotionEffectType.NIGHT_VISION)) { p.removePotionEffect(PotionEffectType.NIGHT_VISION); p.sendMessage("§eLuz Off."); }
        else { p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 1)); p.sendMessage("§bLuz On."); }
    }

    private void repairItem(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        if(i != null && i.getType() != Material.AIR) { i.setDurability((short)0); p.sendMessage("§aItem Corrigido!"); }
    }

    private void toggleChapeu(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if(hand.getType() == Material.AIR) return;
        ItemStack helm = p.getInventory().getHelmet();
        p.getInventory().setHelmet(hand); p.getInventory().setItemInMainHand(helm);
        p.sendMessage("§eNovo chapéu!");
    }

    private void compactar(Player p) {
        for(ItemStack is : p.getInventory().getContents()) {
            if(is != null && is.getType() == Material.DIAMOND && is.getAmount() >= 9) {
                is.setAmount(is.getAmount() - 9); p.getInventory().addItem(new ItemStack(Material.DIAMOND_BLOCK));
            }
        }
        p.sendMessage("§aCompactado!");
    }

    private void broadcastTitle(String msg) {
        String f = msg.replace("&", "§");
        for(Player a : Bukkit.getOnlinePlayers()) a.sendTitle("§6§lAVISO", f, 10, 70, 20);
    }

    private void handleFreeze(Player p, String[] args) {
        Player t = Bukkit.getPlayer(args[0]);
        if(t != null) {
            if(congelados.contains(t.getUniqueId())) congelados.remove(t.getUniqueId());
            else congelados.add(t.getUniqueId());
            p.sendMessage("§eStatus de congelamento alterado para " + t.getName());
        }
    }

    private void checkIP(Player p, String name) {
        Player t = Bukkit.getPlayer(name);
        if(t != null) p.sendMessage("§eIP: §f" + t.getAddress().getAddress().getHostAddress());
    }

    private void handleList(Player p, String[] args, List<String> list, String tipo) {
        if(args.length < 2) { if(args.length > 0 && args[0].equalsIgnoreCase("lista")) list.forEach(s -> p.sendMessage("§7- " + s)); return; }
        if(args[0].equalsIgnoreCase("add")) { 
            StringBuilder sb = new StringBuilder(); for(int i=1; i<args.length; i++) sb.append(args[i]).append(" ");
            list.add(sb.toString().trim()); p.sendMessage("§aAdicionado!"); 
        } else if(args[0].equalsIgnoreCase("remove")) { list.remove(args[1]); p.sendMessage("§eRemovido!"); }
        saveData();
    }

    private void showHelp(Player p) {
        p.sendMessage("§b§l--- HUMAN ANGEL ---");
        p.sendMessage("§fComuns: /home, /sethome, /tpa, /tpaccept, /spawn, /chapeu, /lixeira, /perfil, /morte, /compactar, /luz");
        if(p.isOp()) p.sendMessage("§eAdmins: /modo, /control, /clearlag, /corrigir, /mudarip, /anuncio, /congelar, /zueira, /avisos");
    }

    // --- STORAGE ---
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
            for (Map.Entry<UUID, Location> e : homes.entrySet()) dataConfig.set("homes." + e.getKey().toString(), e.getValue());
            dataConfig.save(dataFile);
        } catch (IOException e) {}
    }
    }
