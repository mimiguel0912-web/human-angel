package com.humanangel;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class Main extends JavaPlugin implements CommandExecutor, Listener {

    private File homesFile;
    private FileConfiguration homes;

    private final HashMap<UUID, UUID> tpa = new HashMap<>();

    @Override
    public void onEnable() {

        saveDefaultConfig();

        createHomesFile();

        Bukkit.getPluginManager().registerEvents(this, this);

        getCommand("ha").setExecutor(this);
        getCommand("modo").setExecutor(this);

        getCommand("home").setExecutor(this);
        getCommand("sethome").setExecutor(this);
        getCommand("delhome").setExecutor(this);
        getCommand("homes").setExecutor(this);

        getCommand("spawn").setExecutor(this);

        getCommand("control").setExecutor(this);

        getCommand("lista").setExecutor(this);
        getCommand("ajuda").setExecutor(this);

        getCommand("clearlag").setExecutor(this);

        getCommand("tpa").setExecutor(this);
        getCommand("tpaccept").setExecutor(this);
        getCommand("tpdeny").setExecutor(this);

        getCommand("luz").setExecutor(this);

        getCommand("corrigir").setExecutor(this);

        getCommand("mudarip").setExecutor(this);

        getCommand("chapeu").setExecutor(this);

        getCommand("lixeira").setExecutor(this);

        getCommand("perfil").setExecutor(this);

        getCommand("aviso").setExecutor(this);

        getCommand("congelar").setExecutor(this);

        getCommand("morte").setExecutor(this);

        Bukkit.getConsoleSender().sendMessage("§9HumanAngel §aLigado.");
    }

    @Override
    public void onDisable() {

        saveHomes();
    }

    // HOMES

    private void createHomesFile() {

        homesFile = new File(getDataFolder(), "homes.yml");

        if (!homesFile.exists()) {

            homesFile.getParentFile().mkdirs();

            try {

                homesFile.createNewFile();

            } catch (IOException e) {

                e.printStackTrace();
            }
        }

        homes = YamlConfiguration.loadConfiguration(homesFile);
    }

    private void saveHomes() {

        try {

            homes.save(homesFile);

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {

            sender.sendMessage("Somente jogadores.");
            return true;
        }

        Player p = (Player) sender;

        // /HA

        if (command.getName().equalsIgnoreCase("ha")) {

            if (!p.hasPermission("humanangel.ha")) {

                p.sendMessage("§cSem permissão.");
                return true;
            }

            p.sendMessage(" ");
            p.sendMessage("§9§lHUMAN ANGEL");
            p.sendMessage("§fFuncionando.");
            p.sendMessage("§fVersão: §b1.6");
            p.sendMessage(" ");

            return true;
        }

        // /LISTA

        if (command.getName().equalsIgnoreCase("lista")) {

            p.sendMessage("§8§m----------------------");
            p.sendMessage("§9§lLista de Comandos");

            p.sendMessage("§b/home");
            p.sendMessage("§b/sethome");
            p.sendMessage("§b/delhome");
            p.sendMessage("§b/homes");

            p.sendMessage("§b/spawn");

            p.sendMessage("§b/tpa");
            p.sendMessage("§b/tpaccept");
            p.sendMessage("§b/tpdeny");

            p.sendMessage("§b/luz");
            p.sendMessage("§b/chapeu");
            p.sendMessage("§b/lixeira");
            p.sendMessage("§b/perfil");

            if (p.isOp()) {

                p.sendMessage(" ");
                p.sendMessage("§c§lAdmin");

                p.sendMessage("§c/modo");
                p.sendMessage("§c/clearlag");
                p.sendMessage("§c/corrigir");
                p.sendMessage("§c/mudarip");
                p.sendMessage("§c/congelar");
                p.sendMessage("§c/aviso");
                p.sendMessage("§c/control");
                p.sendMessage("§c/ha");
            }

            p.sendMessage("§8§m----------------------");

            return true;
        }

        // /AJUDA

        if (command.getName().equalsIgnoreCase("ajuda")) {

            p.sendMessage("§8§m----------------------");
            p.sendMessage("§9§lAjuda");

            p.sendMessage("§b/home §f- Teleporta para sua home");
            p.sendMessage("§b/sethome §f- Cria uma home");
            p.sendMessage("§b/delhome §f- Remove uma home");
            p.sendMessage("§b/homes §f- Lista suas homes");

            p.sendMessage("§b/tpa §f- Envia pedido de teleporte");
            p.sendMessage("§b/tpaccept §f- Aceita pedido");
            p.sendMessage("§b/tpdeny §f- Recusa pedido");

            p.sendMessage("§b/luz §f- Ativa visão noturna");
            p.sendMessage("§b/chapeu §f- Coloca item na cabeça");
            p.sendMessage("§b/lixeira §f- Abre lixeira");
            p.sendMessage("§b/perfil §f- Mostra perfil");

            if (p.isOp()) {

                p.sendMessage(" ");
                p.sendMessage("§c§lAdmin");

                p.sendMessage("§c/control §f- Painel admin");
                p.sendMessage("§c/clearlag §f- Limpa itens");
                p.sendMessage("§c/congelar §f- Congela jogador");
                p.sendMessage("§c/corrigir §f- Repara item");
                p.sendMessage("§c/mudarip §f- Mostra IP");
                p.sendMessage("§c/aviso §f- Aviso global");
            }

            p.sendMessage("§8§m----------------------");

            return true;
        }

        // /CONTROL

        if (command.getName().equalsIgnoreCase("control")) {

            if (!p.hasPermission("humanangel.control")) {

                p.sendMessage("§cSem permissão.");
                return true;
            }

            Inventory menu = Bukkit.createInventory(null, 54, "§9Control");

            for (Player online : Bukkit.getOnlinePlayers()) {

                ItemStack head = new ItemStack(Material.PLAYER_HEAD);

                SkullMeta meta = (SkullMeta) head.getItemMeta();

                meta.setOwningPlayer(online);

                meta.setDisplayName("§b" + online.getName());

                head.setItemMeta(meta);

                menu.addItem(head);
            }

            p.openInventory(menu);

            return true;
        }

        // /TPA

        if (command.getName().equalsIgnoreCase("tpa")) {

            if (args.length == 0) {

                p.sendMessage("§cUse /tpa <player>");
                return true;
            }

            Player alvo = Bukkit.getPlayer(args[0]);

            if (alvo == null) {

                p.sendMessage("§cJogador offline.");
                return true;
            }

            tpa.put(alvo.getUniqueId(), p.getUniqueId());

            alvo.sendMessage(" ");
            alvo.sendMessage("§9§lTPA");
            alvo.sendMessage("§f" + p.getName() + " quer teleportar até você.");
            alvo.sendMessage("§a/tpaccept");
            alvo.sendMessage("§c/tpdeny");
            alvo.sendMessage(" ");

            p.sendMessage("§aPedido enviado.");

            return true;
        }

        // /TPACCEPT

        if (command.getName().equalsIgnoreCase("tpaccept")) {

            if (!tpa.containsKey(p.getUniqueId())) {

                p.sendMessage("§cNenhum pedido.");
                return true;
            }

            Player requester = Bukkit.getPlayer(tpa.get(p.getUniqueId()));

            if (requester != null) {

                requester.teleport(p);

                requester.sendMessage("§aTPA aceito.");
                p.sendMessage("§aTPA aceito.");
            }

            tpa.remove(p.getUniqueId());

            return true;
        }

        // /TPDENY

        if (command.getName().equalsIgnoreCase("tpdeny")) {

            if (!tpa.containsKey(p.getUniqueId())) {

                p.sendMessage("§cNenhum pedido.");
                return true;
            }

            Player requester = Bukkit.getPlayer(tpa.get(p.getUniqueId()));

            if (requester != null) {

                requester.sendMessage("§cTPA negado.");
            }

            tpa.remove(p.getUniqueId());

            p.sendMessage("§cPedido negado.");

            return true;
        }

        // /PERFIL

        if (command.getName().equalsIgnoreCase("perfil")) {

            p.sendMessage("§8§m----------------------");
            p.sendMessage("§9§lPerfil");

            p.sendMessage("§bNome: §f" + p.getName());
            p.sendMessage("§bVida: §f" + p.getHealth());
            p.sendMessage("§bFome: §f" + p.getFoodLevel());
            p.sendMessage("§bXP: §f" + p.getLevel());

            p.sendMessage("§bMundo: §f" + p.getWorld().getName());

            p.sendMessage("§bX: §f" + p.getLocation().getBlockX());
            p.sendMessage("§bY: §f" + p.getLocation().getBlockY());
            p.sendMessage("§bZ: §f" + p.getLocation().getBlockZ());

            p.sendMessage("§bOnline: §f" + Bukkit.getOnlinePlayers().size());

            p.sendMessage("§8§m----------------------");

            return true;
        }

        // /LIXEIRA

        if (command.getName().equalsIgnoreCase("lixeira")) {

            Inventory lixo = Bukkit.createInventory(null, 54, "§cLixeira");

            p.openInventory(lixo);

            return true;
        }

        // /LUZ

        if (command.getName().equalsIgnoreCase("luz")) {

            if (p.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {

                p.removePotionEffect(PotionEffectType.NIGHT_VISION);

                p.sendMessage("§cVisão noturna desativada.");

            } else {

                p.addPotionEffect(new PotionEffect(
                        PotionEffectType.NIGHT_VISION,
                        999999,
                        1
                ));

                p.sendMessage("§aVisão noturna ativada.");
            }

            return true;
        }

        // /CHAPEU

        if (command.getName().equalsIgnoreCase("chapeu")) {

            ItemStack item = p.getInventory().getItemInMainHand();

            if (item == null || item.getType() == Material.AIR) {

                p.sendMessage("§cSegure um item.");
                return true;
            }

            ItemStack helmet = p.getInventory().getHelmet();

            p.getInventory().setHelmet(item);

            p.getInventory().setItemInMainHand(helmet);

            p.sendMessage("§aChapéu equipado.");

            return true;
        }

        // /SPAWN

        if (command.getName().equalsIgnoreCase("spawn")) {

            p.teleport(p.getWorld().getSpawnLocation());

            p.sendMessage("§aTeleportado.");

            return true;
        }

        // /CLEARLAG

        if (command.getName().equalsIgnoreCase("clearlag")) {

            if (!p.hasPermission("humanangel.clearlag")) {

                p.sendMessage("§cSem permissão.");
                return true;
            }

            int removidos = 0;

            for (World world : Bukkit.getWorlds()) {

                for (Entity entity : world.getEntities()) {

                    if (entity instanceof Item) {

                        entity.remove();
                        removidos++;
                    }
                }
            }

            Bukkit.broadcastMessage("§cItens removidos: §f" + removidos);

            return true;
        }

        return false;
    }
                                   }
