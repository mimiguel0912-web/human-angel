package com.humanangel;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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

        String[] cmds = {
                "ha",
                "modo",
                "home",
                "sethome",
                "delhome",
                "homes",
                "spawn",
                "control",
                "lista",
                "ajuda",
                "clearlag",
                "tpa",
                "tpaccept",
                "tpdeny",
                "luz",
                "corrigir",
                "mudarip",
                "chapeu",
                "lixeira",
                "perfil",
                "aviso",
                "congelar",
                "morte"
        };

        for (String cmd : cmds) {

            getCommand(cmd).setExecutor(this);
        }

        Bukkit.getConsoleSender().sendMessage("§9HumanAngel §aLigado.");
    }

    @Override
    public void onDisable() {

        saveHomes();
    }

    // HOMES FILE

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

    // COMMANDS

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
                p.sendMessage("§c/control");
                p.sendMessage("§c/clearlag");
                p.sendMessage("§c/corrigir");
                p.sendMessage("§c/mudarip");
                p.sendMessage("§c/aviso");
                p.sendMessage("§c/congelar");
                p.sendMessage("§c/ha");
            }

            p.sendMessage("§8§m----------------------");

            return true;
        }

        // /AJUDA

        if (command.getName().equalsIgnoreCase("ajuda")) {

            p.sendMessage("§8§m----------------------");
            p.sendMessage("§9§lAjuda");

            p.sendMessage("§b/home §f- Teleporta para home");
            p.sendMessage("§b/sethome §f- Cria uma home");
            p.sendMessage("§b/delhome §f- Remove uma home");
            p.sendMessage("§b/homes §f- Lista homes");

            p.sendMessage("§b/tpa §f- Pedido de teleporte");
            p.sendMessage("§b/tpaccept §f- Aceita TPA");
            p.sendMessage("§b/tpdeny §f- Recusa TPA");

            p.sendMessage("§b/luz §f- Visão noturna");
            p.sendMessage("§b/chapeu §f- Item na cabeça");
            p.sendMessage("§b/lixeira §f- Abre lixeira");
            p.sendMessage("§b/perfil §f- Ver perfil");

            if (p.isOp()) {

                p.sendMessage(" ");
                p.sendMessage("§c§lAdmin");

                p.sendMessage("§c/control §f- Painel admin");
                p.sendMessage("§c/clearlag §f- Limpa itens");
                p.sendMessage("§c/corrigir §f- Repara item");
                p.sendMessage("§c/mudarip §f- Ver IP");
                p.sendMessage("§c/aviso §f- Aviso global");
                p.sendMessage("§c/congelar §f- Congelar jogador");
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

        // /SPAWN

        if (command.getName().equalsIgnoreCase("spawn")) {

            p.teleport(p.getWorld().getSpawnLocation());

            p.sendMessage("§aTeleportado.");

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

            ItemStack old = p.getInventory().getHelmet();

            p.getInventory().setHelmet(item);

            p.getInventory().setItemInMainHand(old);

            p.sendMessage("§aChapéu equipado.");

            return true;
        }

        // /LIXEIRA

        if (command.getName().equalsIgnoreCase("lixeira")) {

            Inventory lixo = Bukkit.createInventory(null, 54, "§cLixeira");

            p.openInventory(lixo);

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

        // /CORRIGIR

        if (command.getName().equalsIgnoreCase("corrigir")) {

            if (!p.hasPermission("humanangel.corrigir")) {

                p.sendMessage("§cSem permissão.");
                return true;
            }

            ItemStack item = p.getInventory().getItemInMainHand();

            if (item == null || item.getType() == Material.AIR) {

                p.sendMessage("§cSegure um item.");
                return true;
            }

            item.setDurability((short) 0);

            p.sendMessage("§aItem reparado.");

            return true;
        }

        // /MUDARIP

        if (command.getName().equalsIgnoreCase("mudarip")) {

            if (!p.hasPermission("humanangel.mudarip")) {

                p.sendMessage("§cSem permissão.");
                return true;
            }

            if (args.length == 0) {

                p.sendMessage("§cUse /mudarip <player>");
                return true;
            }

            Player alvo = Bukkit.getPlayer(args[0]);

            if (alvo == null) {

                p.sendMessage("§cJogador offline.");
                return true;
            }

            String ip = alvo.getAddress().getAddress().getHostAddress();

            p.sendMessage("§bIP de " + alvo.getName() + ": §f" + ip);

            return true;
        }

        // /AVISO

        if (command.getName().equalsIgnoreCase("aviso")) {

            if (!p.hasPermission("humanangel.aviso")) {

                p.sendMessage("§cSem permissão.");
                return true;
            }

            if (args.length == 0) {

                p.sendMessage("§cUse /aviso <mensagem>");
                return true;
            }

            StringBuilder msg = new StringBuilder();

            for (String s : args) {

                msg.append(s).append(" ");
            }

            Bukkit.broadcastMessage(" ");
            Bukkit.broadcastMessage("§c§lAVISO");
            Bukkit.broadcastMessage("§f" + msg);
            Bukkit.broadcastMessage(" ");

            return true;
        }

        // /CONGELAR

        if (command.getName().equalsIgnoreCase("congelar")) {

            if (!p.hasPermission("humanangel.congelar")) {

                p.sendMessage("§cSem permissão.");
                return true;
            }

            if (args.length == 0) {

                p.sendMessage("§cUse /congelar <player>");
                return true;
            }

            Player alvo = Bukkit.getPlayer(args[0]);

            if (alvo == null) {

                p.sendMessage("§cJogador offline.");
                return true;
            }

            alvo.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOW,
                    999999,
                    255
            ));

            alvo.sendMessage("§cVocê foi congelado.");

            p.sendMessage("§aJogador congelado.");

            return true;
        }

        // /MODO

        if (command.getName().equalsIgnoreCase("modo")) {

            if (!p.hasPermission("humanangel.modo")) {

                p.sendMessage("§cSem permissão.");
                return true;
            }

            if (args.length == 0) {

                p.sendMessage("§cUse /modo <0/1/2/3>");
                return true;
            }

            switch (args[0]) {

                case "0":
                    p.setGameMode(GameMode.SURVIVAL);
                    break;

                case "1":
                    p.setGameMode(GameMode.CREATIVE);
                    break;

                case "2":
                    p.setGameMode(GameMode.ADVENTURE);
                    break;

                case "3":
                    p.setGameMode(GameMode.SPECTATOR);
                    break;

                default:
                    p.sendMessage("§cModo inválido.");
                    return true;
            }

            p.sendMessage("§aGamemode alterado.");

            return true;
        }

        // /MORTE

        if (command.getName().equalsIgnoreCase("morte")) {

            p.setHealth(0.0);

            Bukkit.broadcastMessage(
                    "§c" + p.getName() + " morreu brutalmente."
            );

            return true;
        }

        // /SETHOME

        if (command.getName().equalsIgnoreCase("sethome")) {

            if (args.length == 0) {

                p.sendMessage("§cUse /sethome <nome>");
                return true;
            }

            int maxHomes = getConfig().getInt("home", 3);

            int total = 0;

            if (homes.contains(p.getUniqueId().toString())) {

                total = homes.getConfigurationSection(
                        p.getUniqueId().toString()).getKeys(false).size();
            }

            if (total >= maxHomes) {

                p.sendMessage("§cMáximo de homes atingido.");
                return true;
            }

            String nome = args[0];

            String path = p.getUniqueId() + "." + nome;

            homes.set(path + ".world", p.getWorld().getName());

            homes.set(path + ".x", p.getLocation().getX());
            homes.set(path + ".y", p.getLocation().getY());
            homes.set(path + ".z", p.getLocation().getZ());

            homes.set(path + ".yaw", p.getLocation().getYaw());
            homes.set(path + ".pitch", p.getLocation().getPitch());

            saveHomes();

            p.sendMessage("§aHome criada.");

            return true;
        }

        // /HOME

        if (command.getName().equalsIgnoreCase("home")) {

            if (args.length == 0) {

                p.sendMessage("§cUse /home <nome>");
                return true;
            }

            String nome = args[0];

            String path = p.getUniqueId() + "." + nome;

            if (!homes.contains(path)) {

                p.sendMessage("§cHome não encontrada.");
                return true;
            }

            World world = Bukkit.getWorld(homes.getString(path + ".world"));

            double x = homes.getDouble(path + ".x");
            double y = homes.getDouble(path + ".y");
            double z = homes.getDouble(path + ".z");

            float yaw = (float) homes.getDouble(path + ".yaw");
            float pitch = (float) homes.getDouble(path + ".pitch");

            Location loc = new Location(world, x, y, z, yaw, pitch);

            p.teleport(loc);

            p.sendMessage("§aTeleportado.");

            return true;
        }

        // /DELHOME

        if (command.getName().equalsIgnoreCase("delhome")) {

            if (args.length == 0) {

                p.sendMessage("§cUse /delhome <nome>");
                return true;
            }

            String nome = args[0];

            String path = p.getUniqueId() + "." + nome;

            homes.set(path, null);

            saveHomes();

            p.sendMessage("§aHome removida.");

            return true;
        }

        // /HOMES

        if (command.getName().equalsIgnoreCase("homes")) {

            if (!homes.contains(p.getUniqueId().toString())) {

                p.sendMessage("§cVocê não possui homes.");
                return true;
            }

            p.sendMessage("§9§lSuas Homes");

            for (String home : homes.getConfigurationSection(
                    p.getUniqueId().toString()).getKeys(false)) {

                p.sendMessage("§f- §b" + home);
            }

            return true;
        }

        return false;
    }

    // MENUS

    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (!(e.getWhoClicked() instanceof Player)) return;

        Player p = (Player) e.getWhoClicked();

        if (e.getView().getTitle().equalsIgnoreCase("§9Control")) {

            e.setCancelled(true);

            if (e.getCurrentItem() == null) return;

            if (e.getCurrentItem().getType() != Material.PLAYER_HEAD) return;

            String nome = ChatColor.stripColor(
                    e.getCurrentItem().getItemMeta().getDisplayName()
            );

            Player alvo = Bukkit.getPlayer(nome);

            if (alvo == null) return;

            Inventory menu = Bukkit.createInventory(
                    null,
                    27,
                    "§bControl: " + alvo.getName()
            );

            ItemStack tp = new ItemStack(Material.COMPASS);
            ItemMeta tpMeta = tp.getItemMeta();
            tpMeta.setDisplayName("§aTeleportar");
            tp.setItemMeta(tpMeta);

            ItemStack inv = new ItemStack(Material.CHEST);
            ItemMeta invMeta = inv.getItemMeta();
            invMeta.setDisplayName("§eInventário");
            inv.setItemMeta(invMeta);

            ItemStack ip = new ItemStack(Material.PAPER);
            ItemMeta ipMeta = ip.getItemMeta();
            ipMeta.setDisplayName("§bVer IP");
            ip.setItemMeta(ipMeta);

            ItemStack ban = new ItemStack(Material.BARRIER);
            ItemMeta banMeta = ban.getItemMeta();
            banMeta.setDisplayName("§cBanir");
            ban.setItemMeta(banMeta);

            menu.setItem(10, tp);
            menu.setItem(12, inv);
            menu.setItem(14, ip);
            menu.setItem(16, ban);

            p.openInventory(menu);
        }

        if (e.getView().getTitle().startsWith("§bControl: ")) {

            e.setCancelled(true);

            if (e.getCurrentItem() == null) return;

            String nome = ChatColor.stripColor(
                    e.getView().getTitle().replace("§bControl: ", "")
            );

            Player alvo = Bukkit.getPlayer(nome);

            if (alvo == null) return;

            Material type = e.getCurrentItem().getType();

            if (type == Material.COMPASS) {

                p.teleport(alvo);

                p.sendMessage("§aTeleportado.");
            }

            if (type == Material.CHEST) {

                p.openInventory(alvo.getInventory());
            }

            if (type == Material.PAPER) {

                String ip = alvo.getAddress().getAddress().getHostAddress();

                p.sendMessage("§bIP: §f" + ip);
            }

            if (type == Material.BARRIER) {

                Bukkit.banIP(
                        alvo.getAddress().getAddress().getHostAddress()
                );

                alvo.kickPlayer("§cVocê foi banido.");
            }
        }
    }
                }
