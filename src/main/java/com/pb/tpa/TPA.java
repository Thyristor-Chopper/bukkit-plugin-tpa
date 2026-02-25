// 
// Decompiled by Procyon v0.6.0
// 

package com.pb.tpa;

import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.ChatColor;
import java.util.UUID;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import java.util.ArrayList;
import java.util.HashMap;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Entity;
import org.bukkit.GameMode;
import java.util.List;
import org.bukkit.Location;
import java.util.Stack;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.Map;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class TPA extends JavaPlugin implements Listener
{
    public Map<String, String> req;
    public Map<String, Integer> cooltime;
    FileConfiguration config;
    public Map<String, Stack<String>> requests;
    public Map<String, Stack<String>> sprequests;
    public Map<String, Location> pos;
    public List<String> pvp;
    public Map<String, String> spec;
    public Map<String, GameMode> modes;
    public GameMode mode;
    public Map<String, Entity> lead;
    public Map<String, Thread> timeouts;
    
    public TPA() {
        this.config = this.getConfig();
        this.mode = null;
    }
    
    public static Thread setTimeout(final Runnable runnable, final int delay) {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(delay);
                runnable.run();
            } catch (final Exception e) {
                System.err.println(e);
            }
        });
        thread.start();
        return thread;
    }
    
    public static void clearTimeout(final Thread thread) {
    	thread.stop();
    }
    
    public void onEnable() {
        this.config.options().copyDefaults(true);
        this.saveConfig();
        this.getServer().getPluginManager().registerEvents((Listener) this, (Plugin) this);
        this.req = new HashMap<String, String>();
        this.cooltime = new HashMap<String, Integer>();
        this.requests = new HashMap<String, Stack<String>>();
        this.sprequests = new HashMap<String, Stack<String>>();
        this.pos = new HashMap<String, Location>();
        this.spec = new HashMap<String, String>();
        this.modes = new HashMap<String, GameMode>();
        this.lead = new HashMap<String, Entity>();
        this.pvp = new ArrayList<String>();
        this.timeouts = new HashMap<String, Thread>();
        this.getCommand("tpa").setExecutor((CommandExecutor) new TPACommand(this));
        this.getCommand("tpaccept").setExecutor((CommandExecutor) new TPACommand(this));
        this.getCommand("tpdeny").setExecutor((CommandExecutor) new TPACommand(this));
        this.getCommand("tpignore").setExecutor((CommandExecutor) new TPACommand(this));
        this.getCommand("tpblock").setExecutor((CommandExecutor) new TPACommand(this));
        this.getCommand("tpunblock").setExecutor((CommandExecutor) new TPACommand(this));
        this.getCommand("tpbed").setExecutor((CommandExecutor) new TPACommand(this));
        this.getCommand("tpspawn").setExecutor((CommandExecutor) new TPACommand(this));
        this.getCommand("bed").setExecutor((CommandExecutor) new TPACommand(this));
        this.getCommand("x__force_opponent_tpa").setExecutor((CommandExecutor) new TPACommand(this));
        this.getCommand("x__force_opponent_tpaccept").setExecutor((CommandExecutor) new TPACommand(this));
        this.getCommand("x__force_opponent_unsafe_tpaccept").setExecutor((CommandExecutor) new TPACommand(this));
        this.getCommand("spec").setExecutor((CommandExecutor) new TPACommand(this));
        this.getCommand("specaccept").setExecutor((CommandExecutor) new TPACommand(this));
        this.getCommand("specdeny").setExecutor((CommandExecutor) new TPACommand(this));
        this.getCommand("specignore").setExecutor((CommandExecutor) new TPACommand(this));
        final String cmode = this.config.getString("gamemode", "lastmode").toLowerCase();
        if(cmode.equals("survival") || cmode.equals("0"))
            this.mode = GameMode.SURVIVAL;
        else if(cmode.equals("creative") || cmode.equals("1"))
            this.mode = GameMode.CREATIVE;
        else if(cmode.equals("adventure") || cmode.equals("2"))
            this.mode = GameMode.ADVENTURE;
        else if(cmode.equals("default"))
            this.mode = Bukkit.getDefaultGameMode();
        else
            this.mode = null;
    }
    
    @EventHandler
    public void onEntityDamage(final EntityDamageByEntityEvent event) {
    	if(!(event.getDamager() instanceof Player && event.getEntity() instanceof Player))
    		return;
        final Player attacker = (Player)event.getDamager();
        final Player victim = (Player)event.getEntity();
        final String x = attacker.getUniqueId().toString();
        final String y = victim.getUniqueId().toString();
        if(!pvp.remove(x))
            attacker.sendMessage("전투 시작(15초)!");
        if(!pvp.remove(y))
            victim.sendMessage("전투 시작(15초)!");
        pvp.add(x);
        pvp.add(y);
        if(timeouts.containsKey(x)) {
        	clearTimeout(timeouts.get(x));
        	timeouts.remove(x);
        }
        timeouts.put(x, setTimeout(() -> {
            pvp.remove(x);
            attacker.sendMessage("\uc804\ud22c \uc885\ub8cc!");
        }, 15000));
        if(timeouts.containsKey(y)) {
        	clearTimeout(timeouts.get(y));
        	timeouts.remove(y);
        }
        timeouts.put(y, setTimeout(() -> {
            pvp.remove(y);
            victim.sendMessage("\uc804\ud22c \uc885\ub8cc!");
        }, 15000));
    }
    
    @EventHandler
    public void onPlayerLeash(final PlayerLeashEntityEvent event) {
        while(this.lead.remove(event.getPlayer()) != null);
        this.lead.put(event.getPlayer().getUniqueId().toString(), event.getEntity());
    }
    
    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        if(!this.config.getBoolean("enable-spectator", false))
            return;
        final Player player = event.getPlayer();
        if(player == null)
            return;
        final String uuid = player.getUniqueId().toString();
        if(!this.spec.containsKey(uuid))
            return;
        if (player.getGameMode().equals((Object) GameMode.SPECTATOR)) {
            if(this.mode == null)
                player.setGameMode((GameMode) this.modes.get(uuid));
            else
                player.setGameMode(this.mode);
            player.teleport((Location)this.pos.get(uuid));
            while(this.pos.remove(uuid) != null);
            while(this.spec.remove(uuid) != null);
        }
    }
    
    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent event) {
        if(!this.config.getBoolean("enable-spectator", false))
            return;
        final Player player = event.getEntity();
        if (player == null)
            return;
        final String uuid = player.getUniqueId().toString();
        this.spec.forEach((spectatorID, targetID) -> {
            if(targetID.equals(uuid)) {
                final Player spectator = Bukkit.getPlayer(UUID.fromString(spectatorID));
                if(this.mode == null)
                    spectator.setGameMode((GameMode) this.modes.get(spectatorID));
                else
                    spectator.setGameMode(this.mode);
                spectator.teleport((Location) this.pos.get(spectatorID));
                while(this.pos.remove(spectatorID) != null);
                while(this.spec.remove(spectatorID) != null);
                spectator.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[\uc54c\ub9bc!]: &f&r\uad00\uc804 \uc911\uc774\ub358 \ud50c\ub808\uc774\uc5b4\uac00 \uc8fd\uc5c8\uc2b5\ub2c8\ub2e4"));
            }
        });
    }
    
    @EventHandler
    public void onPlayerDeath2(final PlayerDeathEvent event) {
        if(!this.config.getBoolean("enable-death-pos", false))
            return;
        final Player player = event.getEntity();
        if(player == null)
            return;
        final Location location = player.getLocation();
        player.sendMessage(String.format("죽은 좌표는 (%d, %d, %d)입니다", location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    }
    
    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        if(!this.config.getBoolean("enable-spectator", false))
            return;
        final Player player = event.getPlayer();
        if(player == null)
            return;
        final String uuid = player.getUniqueId().toString();
        this.spec.forEach((spectatorID, targetID) -> {
            if(targetID.equals(uuid)) {
                final Player spectator = Bukkit.getPlayer(UUID.fromString(spectatorID));
                if(this.mode == null)
                    spectator.setGameMode((GameMode)this.modes.get(spectatorID));
                else
                    spectator.setGameMode(this.mode);
                spectator.teleport((Location)this.pos.get(spectatorID));
                while(this.pos.remove(spectatorID) != null);
                while(this.spec.remove(spectatorID) != null);
                spectator.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[\uc54c\ub9bc!]: &f&r\uad00\uc804 \uc911\uc774\ub358 \ud50c\ub808\uc774\uc5b4\uac00 \ub098\uac14\uc2b5\ub2c8\ub2e4"));
            }
        });
    }
    
    @EventHandler
    public void onPlayerToggleSneak(final PlayerToggleSneakEvent event) {
        if(!this.config.getBoolean("enable-spectator", false))
            return;
        final Player spectator = event.getPlayer();
        if(spectator == null)
            return;
        final String uuid = spectator.getUniqueId().toString();
        if(!this.spec.containsKey(uuid))
            return;
        if(!spectator.getGameMode().equals(GameMode.SPECTATOR))
            return;
        final Player target = Bukkit.getPlayer(UUID.fromString(this.spec.get(uuid)));
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[\uc54c\ub9bc!]: &f&r" + spectator.getName() + "\uc774(\uac00) \uad00\uc804\uc744 \uc911\ub2e8\ud588\uc2b5\ub2c8\ub2e4"));
        spectator.teleport((Location) this.pos.get(uuid));
        while(this.pos.remove(uuid) != null);
        if (this.mode == null)
            spectator.setGameMode((GameMode) this.modes.get(uuid));
        else
            spectator.setGameMode(this.mode);
        while(this.spec.remove(uuid) != null);
    }
    
    public void onDisable() {
    }
}
