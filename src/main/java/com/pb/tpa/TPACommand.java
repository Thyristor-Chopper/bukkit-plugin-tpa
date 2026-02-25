package com.pb.tpa;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

public class TPACommand implements CommandExecutor {
	TPA plugin;
	
	public static void setTimeout(Runnable runnable, int delay) {
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                runnable.run();
            } catch (Exception e) {
                System.err.println(e);
            }
        }).start();
    }
	
	public static boolean isNumeric(String s) {
		try {
			Integer.parseInt(s);
			return true;
		} catch(Exception e) {
			return false;
		}
	}
	
	public TPACommand(TPA plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		final String cmd = command.getName().toLowerCase();
		if(!(sender instanceof Player)) return true;

		final Player player = (Player) sender;
		final String senderUUID = player.getUniqueId().toString();
		final String username = player.getName();
		
		if(cmd.equals("tpa") || cmd.equals("x__force_opponent_tpa")) {
			String argdestname = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
			Player dest = Bukkit.getPlayer(argdestname);
			if(plugin.cooltime.get(username) != null) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e ## 쿨타임이 활성화되어 있습니다"));
				return true;
			}
			if(argdestname.equals("") || args.length < 1) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## TPA할 친구의 이름을 적어주십시오"));
				return true;
			}
			if(dest == null) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## 플레이어 &r" + argdestname + "&r을(를) 찾을 수 없습니다"));
				return true;
			}
			final String destname = dest.getName();
			final String destUUID = dest.getUniqueId().toString();
			if(plugin.requests.get(destUUID) == null)
				plugin.requests.put(destUUID, new Stack<>());
			if(plugin.config.getBoolean("disallow-tpa-while-pvp", false) && (plugin.pvp.contains(destUUID) || plugin.pvp.contains(senderUUID))) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## PvP 중에 순간이동할 수 없습니다"));
				return true;
			}

			Team playerTeam = player.getScoreboard().getPlayerTeam(player);
			Team destTeam = dest.getScoreboard().getPlayerTeam(dest);
			if(!plugin.config.getBoolean("enable-team-guard", false) || (playerTeam != null && destTeam != null && playerTeam.getName().equalsIgnoreCase(destTeam.getName())) || cmd.startsWith("x__")) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b ## " + destname + "&r(으)로 TPA 요청 성공"));
				
				if(plugin.config.getStringList("blocked-players." + destUUID).contains(senderUUID))
					return true;
				while(plugin.requests.get(destUUID).remove(senderUUID));
				plugin.requests.get(destUUID).add(senderUUID);
				
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
					"tellraw \"" + dest.getName().replaceAll("\"", "\\\"") + "\" " +
					"[{\"text\":\" \",\"color\":\"yellow\",\"bold\":true}," + 
					"{\"text\":\"" + username.replaceAll("\"", "\\\"") + "이(가) TPA 요청을 보냈습니다...\\n\",\"color\":\"green\",\"bold\":false}," +
					"\"  -  -  -   \"," +
					"{\"text\":\"[ 수락 ]\",\"color\":\"green\",\"bold\":false,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/tpaccept " + username + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"TPA 요청을 받습니다 (/tpaccept " + username + ")\"},\"click_event\":{\"action\":\"run_command\",\"command\":\"/tpaccept " + username + "\"},\"hover_event\":{\"action\":\"show_text\",\"value\":\"TPA 요청을 받습니다 (/tpaccept " + username + ")\"}}," +
					"\"   \"," +
					"{\"text\":\"[ 거절 ]\",\"color\":\"gold\",\"bold\":false,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/tpdeny " + username + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"TPA 요청을 거절합니다 (/tpdeny " + username + ")\"},\"click_event\":{\"action\":\"run_command\",\"command\":\"/tpdeny " + username + "\"},\"hover_event\":{\"action\":\"show_text\",\"value\":\"TPA 요청을 거절합니다 (/tpdeny " + username + ")\"}}," + 
					"\"   \"," +
					"{\"text\":\"[ 무시 ]\",\"color\":\"yellow\",\"bold\":false,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/tpignore " + username + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"TPA 요청을 무시합니다 (/tpignore " + username + ")\"},\"click_event\":{\"action\":\"run_command\",\"command\":\"/tpignore " + username + "\"},\"hover_event\":{\"action\":\"show_text\",\"value\":\"TPA 요청을 무시합니다 (/tpignore " + username + ")\"}}," + 
					"\"   \"," +
					"{\"text\":\"[ 차단 ]\",\"color\":\"red\",\"bold\":false,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/tpblock " + username + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"관전 및 TPA 요청을 무시하고 차단합니다. 차단을 해제하려면 /tpunblock를 사용하십시오 (/tpblock " + username + ")\"},\"click_event\":{\"action\":\"run_command\",\"command\":\"/tpblock " + username + "\"},\"hover_event\":{\"action\":\"show_text\",\"value\":\"관전 및 TPA 요청을 무시하고 차단합니다. 차단을 해제하려면 /tpunblock를 사용하십시오 (/tpblock " + username + ")\"}}]"
				);
			} else {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
					"tellraw \"" + player.getName().replaceAll("\"", "\\\"") + "\" " +
					"[{\"text\":\"[경고]: \",\"color\":\"gold\",\"bold\":true}," + 
					"{\"text\":\"" + destname.replaceAll("\"", "\\\"") + "은(는) 같은 팀이 아닙니다. 그래도 순간이동을 요청하시겠습니까?\\n\",\"color\":\"yellow\",\"bold\":false}," +
					"\"  -  -  -   \"," +
					"{\"text\":\"[ 요청 보내기 ]\",\"color\":\"red\",\"bold\":false,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/x__force_opponent_tpa " + dest.getName() + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"TPA 요청을 보냅니다\"},\"click_event\":{\"action\":\"run_command\",\"command\":\"/x__force_opponent_tpa " + dest.getName() + "\"},\"hover_event\":{\"action\":\"show_text\",\"value\":\"TPA 요청을 보냅니다\"}}]"
				);
			}
			return true;
		} else if(cmd.equals("tpaccept") || cmd.equals("x__force_opponent_tpaccept")) {
			if(plugin.requests.get(senderUUID) == null)
				plugin.requests.put(senderUUID, new Stack<>());

			String reqname = "", requuid = "";
			if(args.length > 0) {
				reqname = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
				if(Bukkit.getPlayer(reqname) != null)
					requuid = Bukkit.getPlayer(reqname).getUniqueId().toString();
			} else if(!plugin.requests.get(senderUUID).empty()) {
				requuid = plugin.requests.get(senderUUID).peek();
			}
			if(reqname == null || requuid.equals("") || !plugin.requests.get(senderUUID).contains(requuid)) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## 받은 순간이동 요청이 없습니다"));
				return true;
			}
			final Player requester = Bukkit.getPlayer(UUID.fromString(requuid));
			if(requester == null) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## 요청자가 유령인가 봅니다"));
				return true;
			}
			
			Team playerTeam = requester.getScoreboard().getPlayerTeam(requester);
			Team destTeam = player.getScoreboard().getPlayerTeam(player);
			if(!plugin.config.getBoolean("enable-team-guard", false) || (playerTeam != null && destTeam != null && playerTeam.getName().equalsIgnoreCase(destTeam.getName())) || cmd.startsWith("x__")) {
				while(plugin.requests.get(senderUUID).remove(requuid));
				Entity vehicle = requester.getVehicle();
				if(vehicle != null && plugin.config.getBoolean("teleport-vehicle", false)) {
					requester.leaveVehicle();
					vehicle.teleport(player);
					requester.teleport(player);
					vehicle.addPassenger(requester);
				} else {
					requester.teleport(player);
				}
				
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b ## 요청 수락됨"));
				requester.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b ## " + player.getName() + "에게로 이동합니다..."));
				// Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e&l  TPA >>  &r&b" + reqname + "&b이(가) " + player.getName() + "&b에게로 이동했습니다"));
				// plugin.req.remove(player.getName());
				plugin.cooltime.put(requester.getName(), 1);
				setTimeout(() -> {
					plugin.cooltime.remove(requester.getName());
				}, 1000 * plugin.config.getInt("cooltime", 30));
			} else {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
					"tellraw \"" + player.getName().replaceAll("\"", "\\\"") + "\" " +
					"[{\"text\":\"[경고]: \",\"color\":\"gold\",\"bold\":true}," + 
					"{\"text\":\"" + reqname.replaceAll("\"", "\\\"") + "은(는) 같은 팀이 아닙니다. 그래도 순간이동 요청을 받으시겠습니까?\\n\",\"color\":\"yellow\",\"bold\":false}," +
					"\"  -  -  -   \"," +
					"{\"text\":\"[ --- ]\",\"color\":\"white\",\"bold\":false}," +
					"\"   \"," +
					"{\"text\":\"[ 계속 ]\",\"color\":\"red\",\"bold\":false,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/x__force_opponent_tpaccept " + reqname + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"TPA 요청을 받습니다\"},\"click_event\":{\"action\":\"run_command\",\"command\":\"/x__force_opponent_tpaccept " + reqname + "\"},\"hover_event\":{\"action\":\"show_text\",\"value\":\"TPA 요청을 받습니다\"}}," +
					"\"   \"," +
					"{\"text\":\"[ 거절 ]\",\"color\":\"green\",\"bold\":false,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/tpdeny " + reqname + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"TPA 요청을 거절합니다 (/tpdeny " + reqname + ")\"},\"click_event\":{\"action\":\"run_command\",\"command\":\"/tpdeny " + reqname + "\"},\"hover_event\":{\"action\":\"show_text\",\"value\":\"TPA 요청을 거절합니다 (/tpdeny " + reqname + ")\"}}," + 
					"\"   \"," +
					"{\"text\":\"[ 무시 ]\",\"color\":\"aqua\",\"bold\":false,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/tpignore " + reqname + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"관전 및 TPA 요청을 무시합니다\"},\"click_event\":{\"action\":\"run_command\",\"command\":\"/tpignore " + reqname + "\"},\"hover_event\":{\"action\":\"show_text\",\"value\":\"관전 및 TPA 요청을 무시합니다\"}}]"
				);
			}
			return true;
		} else if(cmd.equals("tpdeny")) {
			if(plugin.requests.get(senderUUID) == null)
				plugin.requests.put(senderUUID, new Stack<>());

			String reqname = "", requuid = "";
			if(args.length > 0) {
				reqname = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
				if(Bukkit.getPlayer(reqname) != null)
					requuid = Bukkit.getPlayer(reqname).getUniqueId().toString();
			} else if(!plugin.requests.get(senderUUID).empty()) {
				requuid = plugin.requests.get(senderUUID).peek();
			}
			if(reqname == null || requuid.equals("") || !plugin.requests.get(senderUUID).contains(requuid)) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## 들어온 요청 없음"));
				return true;
			}
			final Player requester = Bukkit.getPlayer(UUID.fromString(requuid));
			if(requester == null) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## 요청자가 유령인가 봅니다"));
				return true;
			}
			while(plugin.requests.get(senderUUID).remove(requuid));
			
			requester.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e ## " + player.getName() + "&f이(가) 요청을 닫았습니다"));
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b ## 요청 닫힘"));
			return true;
		} else if(cmd.equals("tpignore")) {
			if(plugin.requests.get(senderUUID) == null)
				plugin.requests.put(senderUUID, new Stack<>());

			String reqname = "", requuid = "";
			if(args.length > 0) {
				reqname = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
				if(Bukkit.getPlayer(reqname) != null)
					requuid = Bukkit.getPlayer(reqname).getUniqueId().toString();
			} else if(!plugin.requests.get(senderUUID).empty()) {
				requuid = plugin.requests.get(senderUUID).peek();
			}
			if(reqname == null || requuid.equals("") || !plugin.requests.get(senderUUID).contains(requuid)) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## 들어온 요청 없음"));
				return true;
			}
			final Player requester = Bukkit.getPlayer(UUID.fromString(requuid));
			if(requester == null) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## 요청자가 유령인가 봅니다"));
				return true;
			}
			while(plugin.requests.get(senderUUID).remove(requuid));

			player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b ## TPA 요청을 무시했습니다. 상대방은 거절 여부를 알 수 없습니다"));
			return true;
		} else if(cmd.equals("tpblock")) {
			if(plugin.requests.get(senderUUID) == null)
				plugin.requests.put(senderUUID, new Stack<>());

			String reqname = "", requuid = "";
			if(args.length > 0) {
				reqname = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
				if(Bukkit.getPlayer(reqname) != null)
					requuid = Bukkit.getPlayer(reqname).getUniqueId().toString();
			} else {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## 차단할 플레이어를 지정하십시오"));
				return true;
			}
			if(reqname == null || requuid.equals("") || !plugin.requests.get(senderUUID).contains(requuid)) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## 들어온 요청 없음"));
				return true;
			}
			final Player requester = Bukkit.getPlayer(UUID.fromString(requuid));
			if(requester == null) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## 요청자가 유령인가 봅니다"));
				return true;
			}
			plugin.requests.get(senderUUID).remove(requuid);

			List<String> blocked = plugin.config.getStringList("blocked-players." + senderUUID);
			if(blocked.contains(requuid)) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## 이미 차단된 플레이어입니다"));
				return true;
			}
			blocked.add(requuid);
			plugin.config.set("blocked-players." + senderUUID, blocked);
			plugin.saveConfig();
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b ## TPA가 거절되었으며, 이후에 " + requester.getName() + "이(가) 보내는 TPA 요청은 자동으로 거절됩니다. 차단을 해제하려면 </tpunblock " + requester.getName() + ">를 사용하십시오 (상대방은 차단 여부를 알 수 없습니다)"));
			return true;
		} else if(cmd.equals("tpunblock")) {
			if(plugin.requests.get(senderUUID) == null)
				plugin.requests.put(senderUUID, new Stack<>());

			String reqname = "", requuid = "";
			if(args.length > 0) {
				reqname = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
				if(Bukkit.getPlayer(reqname) != null)
					requuid = Bukkit.getPlayer(reqname).getUniqueId().toString();
			} else {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[오류!]: &f&r차단 해제할 플레이어를 지정하십시오"));
				return true;
			}
			final Player requester = Bukkit.getPlayer(UUID.fromString(requuid));
			if(requester == null) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[오류!]: &f&r요청자가 유령인가 봅니다"));
				return true;
			}
			
			List<String> blocked;
			try {
				blocked = plugin.config.getStringList("blocked-players." + senderUUID);
				if(!blocked.contains(requuid)) {
					throw new Exception("s");
				}
			} catch(Exception e) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## 차단되지 않은 플레이어입니다"));
				return true; 
			}
			blocked.remove(requuid);
			plugin.config.set("blocked-players." + senderUUID, blocked);
			plugin.saveConfig();
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b ## " + requester.getName() + "의 TPA 차단을 해제했습니다"));
			return true;
		} else if(cmd.equals("tpbed") || cmd.equals("tp-bed") || cmd.equals("homebed") || cmd.equals("home-bed")) {
			if(!player.getLocation().getWorld().getUID().toString().equals(plugin.getServer().getWorlds().get(0).getUID().toString())) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## 오버월드에서만 가능합니다"));
				return true;
			}
			if(plugin.cooltime.get(username) != null) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## 쿨타임이 활성화되어 있습니다"));
				return true;
			}
			if(plugin.config.getBoolean("disallow-tpa-while-pvp", false) && plugin.pvp.contains(senderUUID)) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## PvP 중에 순간이동할 수 없습니다"));
				return true;
			}
			
			if(args.length > 0 && isNumeric(args[0])) {
				int id = Integer.parseInt(args[0]);
				String entry = "second-beds.";
				if(id == 3)
					entry = "third-beds.";
				else if(id == 4)
					entry = "fourth-beds.";
				else if(id == 5)
					entry = "fifth-beds.";
				else if(id >= 6)
					entry = "bed-" + id + ".";
				String worldID = plugin.config.getString(entry + senderUUID + ".world", null);
				int x = plugin.config.getInt(entry + senderUUID + ".x", 0);
				int y = plugin.config.getInt(entry + senderUUID + ".y", 0);
				int z = plugin.config.getInt(entry + senderUUID + ".z", 0);
				if(worldID == null) {
					player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## 침대나 진입 지점이 설정되지 않아서 사용할 수 없습니다"));
					return true; }
				Location destLocation = new Location(Bukkit.getWorld(UUID.fromString(worldID)), x, y, z);
				Entity vehicle = player.getVehicle();
				if(vehicle != null && plugin.config.getBoolean("teleport-vehicle", false)) {
					player.leaveVehicle();
					vehicle.teleport(destLocation);
					player.teleport(destLocation);
					vehicle.addPassenger(player);
				} else {
					player.teleport(destLocation);
				}
				if(plugin.lead.get(player.getUniqueId().toString()) != null) {
					plugin.lead.get(player.getUniqueId().toString()).teleport(destLocation);
					while(plugin.lead.remove(player.getUniqueId().toString()) != null);
				}
			} else {
				if(player.getBedSpawnLocation() == null) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
							"tellraw \"" + player.getName().replaceAll("\"", "\\\"") + "\" " +
									"[{\"text\":\"[경고]: \",\"color\":\"gold\",\"bold\":true}," + 
									"{\"text\":\"진입 지점이 설정되지 않았습니다. 월드 스폰으로 이동하시겠습니까?\\n\",\"color\":\"yellow\",\"bold\":false}," +
									"\"  -  -  -   \"," +
									"{\"text\":\"[ 이동하기 ]\",\"color\":\"aqua\",\"bold\":false,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/tpspawn\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"월드 스폰으로 이동합니다 (/tpspawn)\"},\"click_event\":{\"action\":\"run_command\",\"command\":\"/tpspawn\"},\"hover_event\":{\"action\":\"show_text\",\"value\":\"월드 스폰으로 이동합니다 (/tpspawn)\"}}]"
						);
					return true; }
				Location destLocation = player.getBedSpawnLocation();
				Entity vehicle = player.getVehicle();
				if(vehicle != null && plugin.config.getBoolean("teleport-vehicle", false)) {
					player.leaveVehicle();
					vehicle.teleport(destLocation);
					player.teleport(destLocation);
					vehicle.addPassenger(player);
				} else {
					player.teleport(destLocation);
				}
				if(plugin.lead.get(player.getUniqueId().toString()) != null) {
					plugin.lead.get(player.getUniqueId().toString()).teleport(destLocation);
					while(plugin.lead.remove(player.getUniqueId().toString()) != null);
				}
				// player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[알림!]: &f&r진입지점으로 이동했습니다"));
			}
			
			plugin.cooltime.put(username, 1);
			setTimeout(() -> {
				plugin.cooltime.remove(username);
			}, 1000 * plugin.config.getInt("cooltime", 30));
			return true;
		} else if(cmd.equals("tpspawn")) {
			if(!plugin.config.getBoolean("allow-tpspawn", false)) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[알림!]: &f&r이 서버에서는 이 기능이 활성화되어 있지 않습니다"));
				return true;
			}
			if(!player.getLocation().getWorld().getUID().toString().equals(plugin.getServer().getWorlds().get(0).getUID().toString())) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## 오버월드에서만 가능합니다"));
				return true;
			}
			if(plugin.cooltime.get(username) != null) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## 쿨타임이 활성화되어 있습니다"));
				return true;
			}
			if(plugin.config.getBoolean("disallow-tpa-while-pvp", false) && plugin.pvp.contains(senderUUID)) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## PvP 중에 순간이동할 수 없습니다"));
				return true;
			}
			Location destLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
			Entity vehicle = player.getVehicle();
			if(vehicle != null && plugin.config.getBoolean("teleport-vehicle", false)) {
				player.leaveVehicle();
				vehicle.teleport(destLocation);
				player.teleport(destLocation);
				vehicle.addPassenger(player);
			} else {
				player.teleport(destLocation);
			}
			if(plugin.lead.get(player.getUniqueId().toString()) != null) {
				plugin.lead.get(player.getUniqueId().toString()).teleport(destLocation);
				while(plugin.lead.remove(player.getUniqueId().toString()) != null);
			}
			// player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[알림!]: &f&r진입지점으로 이동했습니다"));
			plugin.cooltime.put(username, 1);
			setTimeout(() -> {
				plugin.cooltime.remove(username);
			}, 1000 * plugin.config.getInt("cooltime", 30));
			return true;
		} else if(cmd.equals("bed")) {
			Block block = player.getTargetBlock(null, 5);
			if(block == null || !(block.getBlockData() instanceof org.bukkit.block.data.type.Bed)) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c ## 침대를 똑바로 바라보고 있는 상태에서 다시 명령어를 치세요"));
				return true; }
			int id = args.length > 0 ? Integer.parseInt(args[0]) : 2;
			String entry = "second-beds.";
			if(id == 3)
				entry = "third-beds.";
			else if(id == 4)
				entry = "fourth-beds.";
			else if(id == 5)
				entry = "fifth-beds.";
			else if(id >= 6)
				// entry = "bed-" + id + ".";
				return true;
			plugin.config.set(entry + senderUUID + ".world", block.getLocation().getWorld().getUID().toString());
			plugin.config.set(entry + senderUUID + ".x", block.getLocation().getBlockX());
			plugin.config.set(entry + senderUUID + ".y", block.getLocation().getBlockY());
			plugin.config.set(entry + senderUUID + ".z", block.getLocation().getBlockZ());
			plugin.saveConfig();
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', "침대를 설정했습니다"));
			return true;
		} else if(cmd.equals("spec")) {
			if(!plugin.config.getBoolean("enable-spectator", false)) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[알림!]: &f&r이 서버에서는 관전 기능이 활성화되어 있지 않습니다"));
				return true;
			}
			String argdestname = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
			Player dest = Bukkit.getPlayer(argdestname);
			if(plugin.cooltime.get(username) != null) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[오류!]: &f&r쿨타임이 활성화되어 있습니다"));
				return true;
			}
			if(argdestname.equals("") || args.length < 1) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[오류!]: &f&r관전할 친구의 이름을 적어주십시오"));
				return true;
			}
			if(dest == null) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[오류!]: &f&r플레이어 &r" + argdestname + "&r을(를) 찾을 수 없습니다"));
				return true;
			}
			final String destname = dest.getName();
			final String destUUID = dest.getUniqueId().toString();
			if(destUUID.equals(senderUUID)) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[오류!]: &f&r자기 자신을 관전할 수 없습니다"));
				return true;
			}
			if(plugin.sprequests.get(destUUID) == null)
				plugin.sprequests.put(destUUID, new Stack<>());
			if(plugin.config.getBoolean("disallow-tpa-while-pvp", false) && (plugin.pvp.contains(destUUID) || plugin.pvp.contains(senderUUID))) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[오류!]: &f&rPvP 중에 순간이동할 수 없습니다"));
				return true;
			}
			if(plugin.spec.get(destUUID) != null) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[오류!]: &f&r해당 플레이어가 이미 다른 플레이어를 관전하고 있습니다"));
				return true;
			}
			
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[알림!]: &f&r" + destname + "&r(으)로 관전 요청을 보냈습니다"));
			
			if(plugin.config.getStringList("blocked-players." + destUUID).contains(senderUUID))
				return true;
			while(plugin.sprequests.get(destUUID).remove(senderUUID));
			plugin.sprequests.get(destUUID).add(senderUUID);
			
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
				"tellraw \"" + dest.getName().replaceAll("\"", "\\\"") + "\" " +
				"[{\"text\":\"  관전 요청 >>  \",\"color\":\"yellow\",\"bold\":true}," + 
				"{\"text\":\"" + username.replaceAll("\"", "\\\"") + "이(가) 회원님을 관전하기를 원합니다...\\n\",\"color\":\"green\",\"bold\":false}," +
				"\"  -  -  -  -   \"," +
				"{\"text\":\"[ 수락 ]\",\"color\":\"green\",\"bold\":false,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/specaccept " + username + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"요청을 받습니다 (/specaccept " + username + ")\"},\"click_event\":{\"action\":\"run_command\",\"command\":\"/specaccept " + username + "\"},\"hover_event\":{\"action\":\"show_text\",\"value\":\"요청을 받습니다 (/specaccept " + username + ")\"}}," +
				"\"   \"," +
				"{\"text\":\"[ 거절 ]\",\"color\":\"gold\",\"bold\":false,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/specdeny " + username + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"요청을 거절합니다 (/specdeny " + username + ")\"},\"click_event\":{\"action\":\"run_command\",\"command\":\"/specdeny " + username + "\"},\"hover_event\":{\"action\":\"show_text\",\"value\":\"요청을 거절합니다 (/specdeny " + username + ")\"}}," + 
				"\"   \"," +
				"{\"text\":\"[ 무시 ]\",\"color\":\"yellow\",\"bold\":false,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/specignore " + username + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"요청을 무시합니다 (/specignore " + username + ")\"},\"click_event\":{\"action\":\"run_command\",\"command\":\"/specignore " + username + "\"},\"hover_event\":{\"action\":\"show_text\",\"value\":\"요청을 무시합니다 (/specignore " + username + ")\"}}," + 
				"\"   \"," +
				"{\"text\":\"[ 차단 ]\",\"color\":\"red\",\"bold\":false,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/tpblock " + username + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"관전 및 TPA 요청을 무시하고 차단합니다. 차단을 해제하려면 /tpunblock를 사용하십시오 (/tpblock " + username + ")\"},\"click_event\":{\"action\":\"run_command\",\"command\":\"/tpblock " + username + "\"},\"hover_event\":{\"action\":\"show_text\",\"value\":\"관전 및 TPA 요청을 무시하고 차단합니다. 차단을 해제하려면 /tpunblock를 사용하십시오 (/tpblock " + username + ")\"}}]"
			);
			return true;
		} else if(cmd.equals("specaccept")) {
			if(!plugin.config.getBoolean("enable-spectator", false)) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[알림!]: &f&r이 서버에서는 관전 기능이 활성화되어 있지 않습니다"));
				return true;
			}
			
			if(plugin.sprequests.get(senderUUID) == null)
				plugin.sprequests.put(senderUUID, new Stack<>());

			String reqname = "", requuid = "";
			if(args.length > 0) {
				reqname = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
				if(Bukkit.getPlayer(reqname) != null)
					requuid = Bukkit.getPlayer(reqname).getUniqueId().toString();
			} else if(!plugin.sprequests.get(senderUUID).empty()) {
				requuid = plugin.sprequests.get(senderUUID).peek();
			}
			if(reqname == null || requuid.equals("") || !plugin.sprequests.get(senderUUID).contains(requuid)) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[오류!]: &f&r들어온 요청 없음"));
				return true;
			}
			final Player requester = Bukkit.getPlayer(UUID.fromString(requuid));
			if(requester == null) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[오류!]: &f&r요청자가 유령인가 봅니다"));
				return true;
			}
			while(plugin.sprequests.get(senderUUID).remove(requuid));
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[알림!]: &f&r요청을 받았습니다. 관전을 그만하게 하려면 나갔다 들어와 주세요"));
			requester.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[알림!]: &f&r" + player.getName() + "을(를) 관전합니다. 그만하려면 <Shift>를 누르십시오..."));
			if(plugin.spec.containsKey(requuid)) {
				requester.setSpectatorTarget(null);
				requester.teleport(plugin.pos.get(requuid));
			}
			while(plugin.spec.remove(requuid) != null);
			plugin.spec.put(requuid, senderUUID);
			while(plugin.pos.remove(requuid) != null);
			plugin.pos.put(requuid, requester.getLocation());
			while(plugin.modes.remove(requuid) != null);
			plugin.modes.put(requuid, requester.getGameMode());
			requester.setGameMode(GameMode.SPECTATOR);
			requester.setSpectatorTarget(player);
			plugin.cooltime.put(requester.getName(), 1);
			setTimeout(() -> {
				plugin.cooltime.remove(requester.getName());
			}, 1000 * plugin.config.getInt("cooltime", 30));
			return true;
		} else if(cmd.equals("specdeny")) {
			if(!plugin.config.getBoolean("enable-spectator", false)) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[알림!]: &f&r이 서버에서는 관전 기능이 활성화되어 있지 않습니다"));
				return true; }
			
			if(plugin.sprequests.get(senderUUID) == null)
				plugin.sprequests.put(senderUUID, new Stack<>());

			String reqname = "", requuid = "";
			if(args.length > 0) {
				reqname = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
				if(Bukkit.getPlayer(reqname) != null)
					requuid = Bukkit.getPlayer(reqname).getUniqueId().toString();
			} else if(!plugin.sprequests.get(senderUUID).empty()) {
				requuid = plugin.sprequests.get(senderUUID).peek();
			}
			if(reqname == null || requuid.equals("") || !plugin.sprequests.get(senderUUID).contains(requuid)) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[오류!]: &f&r들어온 요청 없음"));
				return true; }
			final Player requester = Bukkit.getPlayer(UUID.fromString(requuid));
			if(requester == null) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[오류!]: &f&r요청자가 유령인가 봅니다"));
				return true; }
			while(plugin.sprequests.get(senderUUID).remove(requuid));
			
			requester.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[알림!]: &f&r" + player.getName() + "&f이(가) 요청을 닫았습니다"));
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[알림!]: &f&r요청 닫음"));
			return true;
		} else if(cmd.equals("specignore")) {
			if(!plugin.config.getBoolean("enable-spectator", false)) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[알림!]: &f&r이 서버에서는 관전 기능이 활성화되어 있지 않습니다"));
				return true; }
			
			if(plugin.sprequests.get(senderUUID) == null)
				plugin.sprequests.put(senderUUID, new Stack<>());

			String reqname = "", requuid = "";
			if(args.length > 0) {
				reqname = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
				if(Bukkit.getPlayer(reqname) != null)
					requuid = Bukkit.getPlayer(reqname).getUniqueId().toString();
			} else if(!plugin.sprequests.get(senderUUID).empty()) {
				requuid = plugin.sprequests.get(senderUUID).peek();
			}
			if(reqname == null || requuid.equals("") || !plugin.sprequests.get(senderUUID).contains(requuid)) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[오류!]: &f&r들어온 요청 없음"));
				return true; }
			final Player requester = Bukkit.getPlayer(UUID.fromString(requuid));
			if(requester == null) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[오류!]: &f&r요청자가 유령인가 봅니다"));
				return true; }
			while(plugin.sprequests.get(senderUUID).remove(requuid));
			
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l[알림!]: &f&r요청 닫음"));
			return true;
		}
		
		return false;
	}
}
