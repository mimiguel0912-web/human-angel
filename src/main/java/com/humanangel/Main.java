package com.humanangel;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashSet;
import java.util.Set;

public class Main extends JavaPlugin implements Listener {

    private final Set<String> pvpOff = new HashSet<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Human Angel 1.2 - Pronto para a zoeira!");
    }

    @Override
    public boolean onCommand(CommandSender remetente, Command cmd, String label, String[] argumentos) {
        if (!(remetente instanceof Player)) return true;
        Player p = (Player) remetente;

        // COMANDO /MODE (Rápido)
        if (cmd.getName().equalsIgnoreCase("mode")) {
            if (argumentos.length < 2) {
                p.sendMessage("§eUse: /mode <nick> <c/s/a/sp>");
                return true;
            }
            Player alvo = Bukkit.getPlayer(argumentos[0]);
            if (alvo == null) return true;
            
            switch (argumentos[1].toLowerCase()) {
                case "c": alvo.setGameMode(GameMode.CREATIVE); break;
                case "s": alvo.setGameMode(GameMode.SURVIVAL); break;
                case "a": alvo.setGameMode(GameMode.ADVENTURE); break;
                case "sp": alvo.setGameMode(GameMode.SPECTATOR); break;
            }
            p.sendMessage("§aModo de " + alvo.getName() + " alterado!");
            return true;
        }

        // COMANDO /TAMANHO (Escala 1.21)
        if (cmd.getName().equalsIgnoreCase("tamanho")) {
            if (argumentos.length < 1) return true;
            double valor = Double.parseDouble(argumentos[0]);
            p.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(valor);
            p.sendMessage("§aSeu tamanho agora é " + valor);
            return true;
        }

        // COMANDO /PVP
        if (cmd.getName().equalsIgnoreCase("pvp")) {
            if (pvpOff.contains(p.getName())) {
                pvpOff.remove(p.getName());
                p.sendMessage("§cSeu PvP foi ATIVADO!");
            } else {
                pvpOff.add(p.getName());
                p.sendMessage("§aSeu PvP foi DESATIVADO!");
            }
            return true;
        }
        
        // COMANDO /SC (Staff Chat)
        if (cmd.getName().equalsIgnoreCase("sc")) {
            String mensagem = String.join(" ", argumentos);
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("humanangel.staff")) {
                    staff.sendMessage("§d[StaffChat] " + p.getName() + ": §f" + mensagem);
                }
            }
            return true;
        }

        return false;
    }

    @EventHandler
    public void aoBater(EntityDamageByEntityEvent evento) {
        if (evento.getDamager() instanceof Player && evento.getEntity() instanceof Player) {
            if (pvpOff.contains(evento.getDamager().getName()) || pvpOff.contains(evento.getEntity().getName())) {
                evento.setCancelled(true);
                evento.getDamager().sendMessage("§cO combate está desligado para um de vocês!");
            }
        }
    }
}
