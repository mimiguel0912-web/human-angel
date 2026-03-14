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
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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

        // Lista de comandos registrados
        String[] cmds = {
            "ha", "modo", "home", "sethome", "spawn", "control", "lista", "clearlag", 
            "tpa", "tpaccept", "tpdeny", "zueira", "avisos", "luz", "corrigir", 
            "mudarip", "chapeu", "lixeira", "perfil", "anuncio", "congelar", "morte", "compactar", "sc"
        };
        
        for (String s : cmds) {
            PluginCommand pc = getCommand(s);
            if (pc != null) pc.setExecutor(this);
        }

        // --- SISTEMA DE AVISOS AUTOMÁTICOS (30 MINUTOS) ---
        // 36000L ticks = 30 minutos (20 ticks = 1 segundo)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!listaAvisos.isEmpty()) {
                String aviso = listaAvisos.get(new Random().nextInt(listaAvisos.size()));
                String formatado = aviso.replace("&", "§");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    // Envia na Action Bar (em cima da vida/fome)
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(formatado));
                }
            }
        }, 36000L, 36000L); 
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String n = cmd.getName().toLowerCase();

        // Verificação de Admin
        List<String> adminCmds = Arrays.asList("modo", "control", "zueira", "avisos", "clearlag", "corrigir", "mudarip", "anuncio", "congelar", "sc");
        if (adminCmds.contains(n) && !p.isOp()) {
            p.sendMessage("§cVocê não é Admin!");
            return true;
        }

        switch (n) {
            case "sc":
                if (args.length == 0) break;
                String msgSc = String.join(" ", args).replace("&", "§");
                for (Player staff : Bukkit.getOnlinePlayers()) {
                    if (staff.isOp()) staff.sendMessage("§d[STAFF] §f" + p.getName() + ": " + msgSc);
                }
                break;

            case "avisos": // Gerenciar os avisos de 30 min
                if (args.length < 2) {
                    p.sendMessage("§eUse: /avisos add [texto] ou /avisos remove [numero]");
                    p.sendMessage("§7Exemplo: /avisos add &6Seja bem-vindo!");
                    return true;
                }
                if (args[0].equalsIgnoreCase("add")) {
                    StringBuilder sb = new StringBuilder();
                    for(int i = 1; i < args.length; i++) sb.append(args[i]).append(" ");
                    listaAvisos.add(sb.toString().trim());
                    saveData();
                    p.sendMessage("§aNovo aviso automático adicionado!");
                } else if (args[0].equalsIgnoreCase("remove")) {
                    try {
                        int index = Integer.parseInt(args[1]);
                        listaAvisos.remove(index);
                        saveData();
                        p.sendMessage("§cAviso removido.");
                    } catch (Exception e) { p.sendMessage("§cUse o número do aviso."); }
                }
                break;

            case "chapeu":
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) break;
                ItemStack helm = p.getInventory().getHelmet();
                p.getInventory().setHelmet(hand);
                p.getInventory().setItemInMainHand(helm);
                p.updateInventory(); // Para Bedrock ver!
                p.sendMessage("§eChapéu equipado!");
                break;

            case "anuncio": // Título na tela para todos
                if (args.length == 0) break;
                String texto = String.join(" ", args);
                if (texto.contains("@p")) {
                    Player closest = null;
                    double dist = Double.MAX_VALUE;
                    for (Player o : Bukkit.getOnlinePlayers()) {
                        if (o.equals(p)) continue;
                        if (o.getWorld().equals(p.getWorld()) && o.getLocation().distance(p.getLocation()) < dist) {
                            dist = o.getLocation().distance(p.getLocation()); closest = o;
                        }
                    }
                    texto = texto.replace("@p", (closest != null ? closest.getName() : "ninguém"));
                }
                for (Player a : Bukkit.getOnlinePlayers()) a.sendTitle("§6§lAVISO", texto.replace("&", "§"), 10, 70, 20);
                break;

            case "perfil":
                p.sendMessage("§b§l--- PERFIL ---");
                p.sendMessage("§fNome: " + p.getName());
                p.sendMessage("§fVida: " + (int)p.getHealth());
                p.sendMessage("§fFome: " + p.getFoodLevel());
                break;

            case "sethome": homes.put(p.getUniqueId(), p.getLocation()); saveData(); p.sendMessage("§aHome salva!"); break;
            case "home": if(homes.containsKey(p.getUniqueId())) p.teleport(homes.get(p.getUniqueId())); break;
            case "spawn": p.teleport(p.getWorld().getSpawnLocation()); break;
            case "tpa": handleTPA(p, args); break;
            case "tpaccept": acceptTPA(p); break;
            case "luz": toggleLuz(p); break;
            case "morte": p.setHealth(0); break;
            case "clearlag": clearLag(); break;
            case "lixeira": p.openInventory(Bukkit.createInventory(null, 36, "§8Lixeira")); break;
            case "compactar": compactar(p); break;
            case "zueira": handleZueiraConfig(p, args); break;
            case "lista": showHelp(p); break;
            case "modo":
                if(args.length == 0) break;
                String m = args[0].toLowerCase();
                if(m.equals("c")) p.setGameMode(GameMode.CREATIVE);
                else if(m.equals("s")) p.setGameMode(GameMode.SURVIVAL);
                p.sendMessage("§aModo alterado!"); break;
        }
        return true;
    }

    // --- MÉTODOS AUXILIARES ---
    private void clearLag() {
        int i = 0;
        for(World w : Bukkit.getWorlds()) for(Entity en : w.getEntities()) if(en instanceof Item) { en.remove(); i++; }
        Bukkit.broadcastMessage("§e§l[ClearLag] §fLimpamos §6" + i + " §fitens.");
    }

    private void handleTPA(Player p, String[] args) {
        if(args.length == 0) return;
        Player t = Bukkit.getPlayer(args[0]);
        if(t != null) { tpaRequests.put(p.getUniqueId(), t.getUniqueId()); p.sendMessage("§ePedido enviado!"); t.sendMessage("§6" + p.getName() + " §equer TPA."); }
    }

    private void acceptTPA(Player p) {
        UUID rId = null;
        for(Map.Entry<UUID, UUID> e : tpaRequests.entrySet()) if(e.getValue().equals(p.getUniqueId())) rId = e.getKey();
        if(rId != null) { Player r = Bukkit.getPlayer(rId); if(r != null) r.teleport(p.getLocation()); tpaRequests.remove(rId); }
    }

    private void toggleLuz(Player p) {
        if(p.hasPotionEffect(PotionEffectType.NIGHT_VISION)) p.removePotionEffect(PotionEffectType.NIGHT_VISION);
        else p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 1));
    }

    private void compactar(Player p) {
        for(ItemStack is : p.getInventory().getContents()) {
            if(is != null && is.getType() == Material.DIAMOND && is.getAmount() >= 9) {
                is.setAmount(is.getAmount() - 9); p.getInventory().addItem(new ItemStack(Material.DIAMOND_BLOCK));
            }
        }
        p.sendMessage("§aCompactado!");
    }

    private void handleZueiraConfig(Player p, String[] args) {
        if (args.length < 2) return;
        if (args[0].equalsIgnoreCase("add")) filtroZueira.add(args[1]);
        else if (args[0].equalsIgnoreCase("remove")) filtroZueira.remove(args[1]);
        saveData();
    }

    private void showHelp(Player p) {
        p.sendMessage("§b§l--- COMANDOS ---");
        p.sendMessage("§f/home, /sethome, /tpa, /tpaccept, /spawn, /chapeu, /lixeira, /perfil, /morte, /compactar, /luz");
        if(p.isOp()) p.sendMessage("§eAdmin: /modo, /control, /clearlag, /corrigir, /mudarip, /anuncio, /zueira, /avisos, /sc");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        for (String s : filtroZueira) if (e.getMessage().toLowerCase().contains(s.toLowerCase())) e.setCancelled(true);
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
