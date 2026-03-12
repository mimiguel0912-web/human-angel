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

        String[] cmds = {
            "ha", "modo", "home", "sethome", "spawn", "control", "lista", "clearlag", 
            "tpa", "tpaccept", "tpdeny", "zueira", "avisos", "luz", "corrigir", 
            "mudarip", "chapeu", "lixeira", "perfil", "anuncio", "aviso", "congelar", "morte", "compactar"
        };
        
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

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        String ip = e.getPlayer().getAddress().getAddress().getHostAddress();
        if (ip.startsWith("127.") || ip.startsWith("0.")) {
            e.getPlayer().kickPlayer("§c§lHUMAN ANGEL\n§7VPN Detectada!");
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (congelados.contains(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        for (String p : filtroZueira) {
            if (e.getMessage().toLowerCase().contains(p.toLowerCase())) {
                e.setCancelled(true);
                e.getPlayer().sendMessage("§c§lZUEIRA! §7Palavra proibida.");
                return;
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String n = cmd.getName().toLowerCase();

        List<String> adminCmds = Arrays.asList("modo", "control", "zueira", "avisos", "clearlag", "corrigir", "mudarip", "anuncio", "aviso", "congelar");
        if (adminCmds.contains(n) && !p.isOp()) {
            p.sendMessage("§cVocê não tem permissão de Admin!");
            return true;
        }

        switch (n) {
            case "ha": p.sendMessage("§b§lHUMAN ANGEL §f- v1.6.1"); break;
            case "sethome": homes.put(p.getUniqueId(), p.getLocation()); saveData(); p.sendMessage("§aHome salva!"); break;
            case "home": if(homes.containsKey(p.getUniqueId())) p.teleport(homes.get(p.getUniqueId())); else p.sendMessage("§cSem home!"); break;
            case "spawn": p.teleport(p.getWorld().getSpawnLocation()); break;
            case "clearlag": clearLag(); break;
            case "tpa": handleTPA(p, args); break;
            case "tpaccept": acceptTPA(p); break;
            case "tpdeny": tpaRequests.entrySet().removeIf(e -> e.getValue().equals(p.getUniqueId())); break;
            
            case "anuncio":
    if (args.length == 0) break;
    String texto = String.join(" ", args);

    // Lógica para o @p funcionar
    if (texto.contains("@p")) {
        Player maisProximo = null;
        double distanciaCurta = Double.MAX_VALUE;
        
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(p)) continue; // ignora você mesmo
            if (online.getWorld().equals(p.getWorld())) {
                double dist = online.getLocation().distance(p.getLocation());
                if (dist < distanciaCurta) {
                    distanciaCurta = dist;
                    maisProximo = online;
                }
            }
        }
        
        if (maisProximo != null) {
            texto = texto.replace("@p", maisProximo.getName());
        } else {
            texto = texto.replace("@p", "ninguém");
        }
    }

    String msgFinal = texto.replace("&", "§");
    for (Player a : Bukkit.getOnlinePlayers()) {
        a.sendTitle("§6§lAVISO", msgFinal, 10, 70, 20);
    }
    break;

            case "aviso": // PRIVADO com espaços
                if(args.length < 2) break;
                Player targetA = Bukkit.getPlayer(args[0]);
                if(targetA != null) {
                    String msgP = "";
                    for(int i=1; i<args.length; i++) msgP += args[i] + " ";
                    targetA.sendTitle("§e§lAVISO", msgP.trim().replace("&", "§"), 10, 70, 20);
                } break;

            case "perfil": // REFEITO
                p.sendMessage("§b§l--- SEU PERFIL ---");
                p.sendMessage("§fNome: §7" + p.getName());
                p.sendMessage("§fVida: §c" + (int)p.getHealth() + " HP");
                p.sendMessage("§fFome: §6" + p.getFoodLevel() + "/20");
                p.sendMessage("§fMundo: §e" + p.getWorld().getName());
                p.sendMessage("§fGamemode: §a" + p.getGameMode().toString());
                break;

            case "luz": toggleLuz(p); break;
            case "corrigir": repairItem(p); break;
            case "morte": p.setHealth(0); break;
            case "lixeira": p.openInventory(Bukkit.createInventory(null, 36, "§8Lixeira")); break;
            case "chapeu": toggleChapeu(p); break;
            case "compactar": compactar(p); break;
            case "congelar": handleFreeze(p, args); break;
            case "mudarip": if(args.length > 0) checkIP(p, args[0]); break;
            case "zueira": handleList(p, args, filtroZueira, "Zueira"); break;
            case "avisos": handleList(p, args, listaAvisos, "Aviso"); break;
            case "control": openControlMenu(p); break;
            case "lista": showHelp(p); break;
            case "modo":
                if(args.length == 0) break;
                String m = args[0].toLowerCase();
                if(m.equals("c")) p.setGameMode(GameMode.CREATIVE);
                else if(m.equals("s")) p.setGameMode(GameMode.SURVIVAL);
                else if(m.equals("a")) p.setGameMode(GameMode.ADVENTURE);
                else if(m.equals("sp")) p.setGameMode(GameMode.SPECTATOR);
                p.sendMessage("§aModo alterado!"); break;
        }
        return true;
    }

    // --- MÉTODOS AUXILIARES ---
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
        UUID rId = null;
        for(Map.Entry<UUID, UUID> e : tpaRequests.entrySet()) if(e.getValue().equals(p.getUniqueId())) rId = e.getKey();
        if(rId != null) { Player r = Bukkit.getPlayer(rId); if(r != null) r.teleport(p.getLocation()); tpaRequests.remove(rId); }
    }

    private void toggleLuz(Player p) {
        if(p.hasPotionEffect(PotionEffectType.NIGHT_VISION)) { p.removePotionEffect(PotionEffectType.NIGHT_VISION); p.sendMessage("§eLuz Off."); }
        else { p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 1)); p.sendMessage("§bLuz On."); }
    }

    private void repairItem(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        if(i != null && i.getType() != Material.AIR) { i.setDurability((short)0); p.sendMessage("§aItem reparado!"); }
    }

    private void toggleChapeu(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if(hand.getType() == Material.AIR) return;
        ItemStack helm = p.getInventory().getHelmet();
        p.getInventory().setHelmet(hand); p.getInventory().setItemInMainHand(helm);
        p.sendMessage("§eEstiloso!");
    }

    private void compactar(Player p) {
        for(ItemStack is : p.getInventory().getContents()) {
            if(is != null && is.getType() == Material.DIAMOND && is.getAmount() >= 9) {
                is.setAmount(is.getAmount() - 9); p.getInventory().addItem(new ItemStack(Material.DIAMOND_BLOCK));
            }
        }
        p.sendMessage("§aMinérios compactados!");
    }

    private void handleFreeze(Player p, String[] args) {
        if(args.length == 0) return;
        Player t = Bukkit.getPlayer(args[0]);
        if(t != null) {
            if(congelados.contains(t.getUniqueId())) { congelados.remove(t.getUniqueId()); p.sendMessage("§aDescongelado!"); }
            else { congelados.add(t.getUniqueId()); p.sendMessage("§cCongelado!"); }
        }
    }

    private void checkIP(Player p, String name) {
        Player t = Bukkit.getPlayer(name);
        if(t != null) p.sendMessage("§eIP de " + t.getName() + ": §f" + t.getAddress().getAddress().getHostAddress());
    }

    private void handleList(Player p, String[] args, List<String> list, String tipo) {
        if(args.length < 2) { if(args.length > 0 && args[0].equalsIgnoreCase("lista")) list.forEach(s -> p.sendMessage("§7- " + s)); return; }
        if(args[0].equalsIgnoreCase("add")) { 
            String msg = ""; for(int i=1; i<args.length; i++) msg += args[i] + " ";
            list.add(msg.trim()); p.sendMessage("§a" + tipo + " adicionado!"); 
        } else if(args[0].equalsIgnoreCase("remove")) { list.remove(args[1]); p.sendMessage("§eRemovido!"); }
        saveData();
    }

    public void openControlMenu(Player p) {
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
            if(e.getCurrentItem() == null || e.getCurrentItem().getType() != Material.PLAYER_HEAD) return;
            Player t = Bukkit.getPlayer(ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()));
            if(t != null) e.getWhoClicked().openInventory(t.getInventory());
        }
    }

    private void showHelp(Player p) {
        p.sendMessage("§b§l--- COMANDOS HUMANANGEL ---");
        p.sendMessage("§fGeral: /home, /sethome, /tpa, /tpaccept, /spawn, /chapeu, /lixeira, /perfil, /morte, /compactar, /luz");
        if(p.isOp()) p.sendMessage("§eAdmin: /modo, /control, /clearlag, /corrigir, /mudarip, /anuncio, /aviso, /congelar, /zueira, /avisos");
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
            for (Map.Entry<UUID, Location> e : homes.entrySet()) dataConfig.set("homes." + e.getKey().toString(), e.getValue());
            dataConfig.save(dataFile);
        } catch (IOException e) {}
    }
}
