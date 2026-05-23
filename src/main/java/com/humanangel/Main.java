package com.humanangel;

import org.bukkit.*;
import org.bukkit.ban.BanList;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class Main extends JavaPlugin implements CommandExecutor {

    private File homesFile;
    private FileConfiguration homes;

    private final HashMap<UUID, UUID> tpa = new HashMap<>();

    @Override
    public void onEnable() {

        saveDefaultConfig();

        createHomesFile();

        // COMANDOS

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

        // /AJUDA

        if (command.getName().equalsIgnoreCase("ajuda")) {

            p.sendMessage("§8§m----------------------");
            p.sendMessage("§9§lHumanAngel");

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

            p.sendMessage("§8§m----------------------");

            return true;
        }

        // /LISTA

        if (command.getName().equalsIgnoreCase("lista")) {

            p.sendMessage(" ");
            p.sendMessage("§9§lJogadores Online");

            for (Player online : Bukkit.getOnlinePlayers()) {

                p.sendMessage("§f- §b" + online.getName());
            }

            p.sendMessage(" ");

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

                removidos += world.getEntities().size();
            }

            Bukkit.broadcastMessage("§cClearLag executado.");

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

            alvo.sendMessage("§aPedido de TPA recebido.");
            alvo.sendMessage("§e/tpaccept");
            alvo.sendMessage("§e/tpdeny");

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

        // /LUZ

        if (command.getName().equalsIgnoreCase("luz")) {

            if (p.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {

                p.removePotionEffect(PotionEffectType.NIGHT_VISION);

                p.sendMessage("§cVisão noturna desativada.");

            } else {

                p.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.NIGHT_VISION,
                                999999,
                                1
                        )
                );

                p.sendMessage("§aVisão noturna ativada.");
            }

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

            p.sendMessage("§aIP: §f" + ip);

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

        // /LIXEIRA

        if (command.getName().equalsIgnoreCase("lixeira")) {

            Inventory lixo = Bukkit.createInventory(null, 27, "§cLixeira");

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

            p.sendMessage("§8§m----------------------");

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

            StringBuilder sb = new StringBuilder();

            for (String s : args) {

                sb.append(s).append(" ");
            }

            Bukkit.broadcastMessage(" ");
            Bukkit.broadcastMessage("§c§lAVISO");
            Bukkit.broadcastMessage("§f" + sb);
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

            alvo.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.SLOWNESS,
                            999999,
                            255
                    )
            );

            alvo.sendMessage("§cVocê foi congelado.");

            p.sendMessage("§aJogador congelado.");

            return true;
        }

        // /MORTE

        if (command.getName().equalsIgnoreCase("morte")) {

            p.setHealth(0);

            p.sendMessage("§cVocê morreu.");

            return true;
        }

        // /CONTROL

        if (command.getName().equalsIgnoreCase("control")) {

            if (!p.hasPermission("humanangel.control")) {

                p.sendMessage("§cSem permissão.");
                return true;
            }

            if (args.length < 2) {

                p.sendMessage("§c/control tp <player>");
                p.sendMessage("§c/control ban <player>");
                p.sendMessage("§c/control ip <player>");
                p.sendMessage("§c/control inv <player>");

                return true;
            }

            Player alvo = Bukkit.getPlayer(args[1]);

            if (alvo == null) {

                p.sendMessage("§cJogador offline.");
                return true;
            }

            if (args[0].equalsIgnoreCase("tp")) {

                p.teleport(alvo);

                p.sendMessage("§aTeleportado.");
            }

            if (args[0].equalsIgnoreCase("ban")) {

                Bukkit.getBanList(BanList.Type.NAME).addBan(
                        alvo.getName(),
                        "Banido",
                        null,
                        p.getName()
                );

                alvo.kickPlayer("§cVocê foi banido.");

                Bukkit.broadcastMessage("§c" + alvo.getName() + " foi banido.");
            }

            if (args[0].equalsIgnoreCase("ip")) {

                String ip = alvo.getAddress().getAddress().getHostAddress();

                p.sendMessage("§aIP: §f" + ip);
            }

            if (args[0].equalsIgnoreCase("inv")) {

                Inventory inv = alvo.getInventory();

                p.openInventory(inv);
            }

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

            if (!homes.contains(path)) {

                p.sendMessage("§cHome não encontrada.");
                return true;
            }

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
                  }
