/*
 * This file is part of ContestAdmin. ContestAdmin is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any later version. ContestAdmin is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received
 * a copy of the GNU General Public License along with ContestAdmin. If not, see <http://www.gnu.org/licenses/>.
 */
package sk.tomsik68.contestadmn;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import sk.tomsik68.permsguru.EPermissions;

/**
 * ContestAdmin - Contest administration helper plugin for Bukkit. Check <a
 * href="http://dev.bukkit.org/server-mods/contestadmin/">Plugin's Homepage</a>
 * for more information.
 * 
 * @author Tomsik68
 * 
 */
public class ContestAdmin extends JavaPlugin {
    private String consolePurge;
    private static final HashSet<String> contestProperties = new HashSet<String>(Arrays.asList("name", "creator", "desc", "rules", "banned", "ended"));
    private static ChatColor color1, color2;
    private static EPermissions perms;
    @Override
    public void onEnable() {
        if (!new File(getDataFolder(), "config.yml").exists()) {
            getDataFolder().mkdir();
            YamlConfiguration config = new YamlConfiguration();
            StringBuilder sb = new StringBuilder();
            for (ChatColor cc : ChatColor.values()) {
                sb = sb.append(cc.name()).append(',');
            }
            sb = sb.deleteCharAt(sb.length() - 1);
            config.options().header("Available colors: " + sb.toString());
            config.options().header("Available permission systems(values must exactly match):");
            config.options().header("    SP - SuperPerms Use for permissions provided by server, or an external plugin");
            config.options().header("    OP - OPs can do everything");
            config.options().header("    None - Use for disabled permissions => everyone can do everything");
            config.set("colors", "RED,GREEN");
            try {
                try {
                    new File(getDataFolder(), "config.yml").createNewFile();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                config.save(new File(getDataFolder(), "config.yml"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            color1 = ChatColor.RED;
            color2 = ChatColor.GREEN;
            perms = EPermissions.SP;
        } else {
            color1 = ChatColor.valueOf(getConfig().getString("colors").split(",")[0]);
            color2 = ChatColor.valueOf(getConfig().getString("colors").split(",")[1]);
            perms = EPermissions.parse(getConfig().getString("permissions"));
        }
        getCommand("ca").setExecutor(this);
        try {
            getDatabase().find(Contest.class).findRowCount();
            System.out.println(color2 + "[ContestAdmin] DB is ok.");
        } catch (Exception e) {
            System.out.println(color2 + "[ContestAdmin] Installing database due to first time usage...");
            installDDL();
            System.out.println(color2 + "[ContestAdmin] Database setup success!");
        }
        super.onEnable();
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        ArrayList<Class<?>> list = new ArrayList<Class<?>>();
        list.add(Contest.class);
        list.add(ContestEntry.class);
        return list;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("list")) {
                if (!perms.has(sender,"ca.list")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                List<Contest> contests = getContests();
                sender.sendMessage(color2 + "[ContestAdmin] Showing " + contests.size() + " contests: ");
                StringBuilder sb = new StringBuilder();
                for (Contest con : contests) {
                    if (tookPartIn(con.getName(), sender.getName()))
                        sb = sb.append(ChatColor.BLUE).append(con.getName()).append(',');
                    else if (perms.has(sender,"ca.sub") && !isBanned(con.getName(), sender.getName()))
                        sb = sb.append(ChatColor.GREEN).append(con.getName()).append(',');
                    else
                        sb = sb.append(ChatColor.RED).append(con.getName()).append(',');
                }
                if (sb.toString().length() > 0) {
                    sb = sb.deleteCharAt(sb.length() - 1);
                    sender.sendMessage(sb.toString());
                } else
                    sender.sendMessage(color1 + "[ContestAdmin] There are no on-going contests.");
                return true;
            }
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("start")) {
                if (!perms.has(sender,"ca.start")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (contestExists(args[1])) {
                    sender.sendMessage(color1 + "[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                } else {
                    addContest(args[1], sender.getName());
                    getServer().broadcastMessage(color2 + "[ContestAdmin] Contest " + args[1] + " has just been promoted! Type /ca info " + args[1] + " to find out more!");
                    return true;
                }

            } else if (args[0].equalsIgnoreCase("stop")) {
                if (!perms.has(sender,"ca.stop")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (contestExists(args[1])) {
                    Contest con = getContest(args[1]);
                    removeContest(args[1]);
                    Contest contest = (Contest) con.clone();
                    contest.setEnded(true);

                    addContest(contest);
                    getServer().broadcastMessage(color2 + "[ContestAdmin] Contest " + args[1] + " has just ended. Let it be judged. Good luck there!");
                    return true;
                } else {
                    sender.sendMessage(color1 + "[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
            } else if (args[0].equalsIgnoreCase("tp")) {
                if (!perms.has(sender,"ca.tpmy")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage(color1 + "[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
                ContestEntry entry = getEntry(args[1], sender.getName());
                if (entry != null) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        player.teleport(entry.getLocation());
                        sender.sendMessage(color2 + "[ContestAdmin] Welcome to your contest entry.");
                    } else {
                        sender.sendMessage(color1 + "[ContestAdmin] You need to be player to use this command.");
                        return true;
                    }
                } else {
                    if (getContest(args[1]).isEnded())
                        sender.sendMessage(color1 + "[ContestAdmin] You haven't submitted any entry to " + args[1] + ". Bad luck, contest has already ended.");
                    else
                        sender.sendMessage(color1 + "[ContestAdmin] You haven't submitted any entry to " + args[1] + ". However, you can still submit your entry.");
                    return true;
                }

            } else if (args[0].equalsIgnoreCase("sub")) {
                if (!perms.has(sender,"ca.sub")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(color1 + "[ContestAdmin] You need to be player to use this command.");
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage(color1 + "[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
                enterContest(args[1], (Player) sender);
                return true;
            } else if (args[0].equalsIgnoreCase("unsub")) {
                if (!perms.has(sender,"ca.unsub")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(color1 + "[ContestAdmin] You need to be player to use this command.");
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage(color1 + "[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
                // TODO discuss
                if (!getContest(args[1]).isEnded()) {
                    unsubmit(args[1], (Player) sender);
                    sender.sendMessage(color2 + "[ContestAdmin] You've just left " + args[1] + ".");
                } else {
                    sender.sendMessage(color1 + "[ContestAdmin] You can't left " + args[1] + ". Contest has already been closed.");
                }
            } else if (args[0].equalsIgnoreCase("purge")) {
                if (!perms.has(sender,"ca.purge")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (args[1].equalsIgnoreCase("confirm")) {
                    if (!(sender instanceof Player)) {
                        if (consolePurge == null) {
                            sender.sendMessage(color1 + "[ContestAdmin] You've got nothing to confirm.");
                            return true;
                        }
                        if (!contestExists(consolePurge)) {
                            sender.sendMessage(color1 + "[ContestAdmin] Contest " + consolePurge + " doesn't exist.");
                            return true;
                        }
                        removeContestData(consolePurge, sender);
                    } else {
                        List<MetadataValue> values = ((Player) sender).getMetadata("ca-purge");
                        for (MetadataValue value : values) {
                            if (value.getOwningPlugin().getName().equalsIgnoreCase(getName())) {
                                removeContestData(value.asString(), sender);
                                break;
                            }
                        }
                        ((Player) sender).removeMetadata("ca-purge", this);
                    }
                } else {
                    if (!(sender instanceof Player)) {
                        consolePurge = args[1];
                    } else {
                        ((Player) sender).setMetadata("ca-purge", new FixedMetadataValue(this, args[1]));
                    }
                    if (!contestExists(args[1])) {
                        sender.sendMessage(color1 + "[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                        return true;
                    }
                    sender.sendMessage(color1 + "[ContestAdmin] Purging " + args[1] + ". Are you sure? confirm with /ca purge confirm");
                    return true;
                }
            } else if (args[0].equalsIgnoreCase("tpord")) {
                if (!perms.has(sender,"ca.tpord")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(color1 + "[ContestAdmin] You need to be player to use this command.");
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage(color1 + "[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
                tpOrd(args[1], (Player) sender);
                return true;
            } else if (args[0].equalsIgnoreCase("info")) {
                if (!perms.has(sender,"ca.info")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage(color1 + "[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
                Contest contest = getContest(args[1]);
                sender.sendMessage(color2 + "Contest Name: " + args[1]);
                sender.sendMessage(color2 + "Organisator: " + contest.getStarter());
                if (contest.getBannedUsers() != null && contest.getBannedUsers().length() > 0)
                    sender.sendMessage(color2 + "Banned: " + contest.getBannedUsers());
                if (contest.isEnded())
                    sender.sendMessage(color1 + "Ended");
                else
                    sender.sendMessage(color2 + "Accepting entries");
                if (contest.getDescription() != null && contest.getDescription().length() > 0)
                    sender.sendMessage(contest.getDescription());
                if (contest.getRules() != null && contest.getRules().length() > 0) {
                    sender.sendMessage(color2 + "Rules:");
                    sender.sendMessage(color2 + contest.getRules());
                }
                if (contest.getWinner() != null && contest.getWinner().length() > 0) {
                    sender.sendMessage(color2 + "Winner: " + contest.getWinner());
                }
                return true;
            } else if (args[0].equalsIgnoreCase("who")) {
                if (!perms.has(sender,"ca.who")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage(color1 + "[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
                List<ContestEntry> entries = getEntries(args[1]);
                if (entries.size() == 0) {
                    sender.sendMessage(color1 + "[ContestAdmin] No one has took part in this contest yet.");
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                for (ContestEntry entry : entries) {
                    if (getServer().getPlayer(entry.getPlayerName()) != null)
                        sb = sb.append(ChatColor.AQUA).append(entry.getPlayerName()).append(ChatColor.WHITE).append(',');
                    else
                        sb = sb.append(ChatColor.RED).append(entry.getPlayerName()).append(ChatColor.WHITE).append(',');
                }
                sb = sb.deleteCharAt(sb.length() - 1);
                sender.sendMessage(color2 + "[ContestAdmin] Users who took part in " + args[1] + " [LIGHT BLUE] = online [RED] = offline");
                sender.sendMessage(sb.toString());
                return true;
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("tp")) {
                if (!perms.has(sender,"ca.tp")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(color1 + "[ContestAdmin] You need to be player to use this command.");
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage(color1 + "[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
                if (!tookPartIn(args[1], args[2])) {
                    sender.sendMessage(color1 + "[ContestAdmin] " + args[2] + " hasn't submitted any entry to " + args[1]);
                    return true;
                }
                ((Player) sender).teleport(getEntry(args[1], args[2]).getLocation());
                return true;
            } else if (args[0].equalsIgnoreCase("remove")) {
                if (!perms.has(sender,"ca.remove")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage(color1 + "[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
                if (!tookPartIn(args[1], args[2])) {
                    sender.sendMessage(color1 + "[ContestAdmin] " + args[2] + " hasn't submitted any entry to " + args[1]);
                    return true;
                }
                removeEntry(args[1], args[2]);
            } else if (args[0].equalsIgnoreCase("win")) {
                if (!perms.has(sender,"ca.win")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage(color1 + "[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
                if (!getContest(args[1]).isEnded()) {
                    sender.sendMessage(color1 + "[ContestAdmin] Contest " + args[1] + " hasn't ended yet, but we'll end it for you. :)");
                }
                Contest con = getContest(args[1]);
                removeContest(args[1]);
                Contest contest = (Contest) con.clone();
                contest.setEnded(true);
                contest.setWinner(args[2]);

                addContest(contest);
                getServer().broadcastMessage(color2 + "[ContestAdmin] And the winner of " + args[1] + " iiiiiiiiiiiiis: ");
                getServer().broadcastMessage(args[2]);
                return true;
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("mod")) {
                if (!perms.has(sender,"ca.mod")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage(color1 + "[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
                if (!contestProperties.contains(args[2])) {
                    sender.sendMessage(color1 + "[ContestAdmin] Property " + args[2] + " doesn't exist");
                    return true;
                }
                if (args[2].equalsIgnoreCase("name")) {
                    Contest con = getContest(args[1]);
                    removeContest(args[1]);
                    Contest contest = (Contest) con.clone();
                    contest.setName(args[3]);
                    addContest(contest);
                }
                if (args[2].equalsIgnoreCase("creator")) {
                    Contest con = getContest(args[1]);
                    removeContest(args[1]);
                    Contest contest = (Contest) con.clone();
                    contest.setStarter(args[3]);
                    addContest(contest);
                }
                /*
                 * if (args[2].equalsIgnoreCase("desc")) { Contest con =
                 * getContest(args[1]); removeContest(args[1]); con.description
                 * = args[3]; addContest(con); } if
                 * (args[2].equalsIgnoreCase("rules")) { Contest con =
                 * getContest(args[1]); removeContest(args[1]); con.rules =
                 * args[3]; addContest(con); }
                 */
                if (args[2].equalsIgnoreCase("ended")) {
                    Contest con = getContest(args[1]);
                    removeContest(args[1]);
                    Contest contest = (Contest) con.clone();
                    contest.setEnded(Boolean.valueOf(args[3]));
                    addContest(contest);
                }
                sender.sendMessage(color2 + "[ContestAdmin] " + args[2] + " of " + args[1] + " set to " + args[3]);
                return true;
            }

        } else if (args.length == 5) {
            if (args[0].equalsIgnoreCase("mod") && args[2].equalsIgnoreCase("banned")) {
                if (!perms.has(sender,"ca.mod")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage(color1 + "[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
                Contest con = getContest(args[1]);
                removeContest(args[1]);
                Contest contest = (Contest) con.clone();
                if (args[3].equalsIgnoreCase("add")) {
                    contest.setBannedUsers(contest.getBannedUsers() + ("," + args[4]));
                    sender.sendMessage(color2 + "[ContestAdmin] Successfully banned " + args[4] + " from " + args[1]);
                } else if (args[3].equalsIgnoreCase("remove")) {
                    contest.getBannedUsers().replace(args[4], "");
                    contest.getBannedUsers().replace(",,", ",");
                    sender.sendMessage(color2 + "[ContestAdmin] Successfully un-banned " + args[4] + " from " + args[1]);
                }
                addContest(contest);
                return true;
            }
        } else if (args.length > 3 && args[0].equalsIgnoreCase("mod") && (args[2].equalsIgnoreCase("desc") || args[2].equalsIgnoreCase("rules"))) {
            StringBuilder sb = new StringBuilder();
            for (int i = 3; i < args.length; i++) {
                sb = sb.append(args[i]).append(' ');
            }
            if (sb.length() > 0)
                sb = sb.deleteCharAt(sb.length() - 1);
            else {
                sender.sendMessage(color1 + "[ContestAdmin] Property value invalid.");
                return true;
            }

            Contest contest = (Contest) getContest(args[1]);
            removeContest(args[1]);
            contest = (Contest) contest.clone();
            if (args[2].equalsIgnoreCase("desc")) {
                contest.setDescription(sb.toString());
            } else if (args[2].equalsIgnoreCase("rules")) {
                contest.setRules(sb.toString());
            } else {
                sender.sendMessage(color1 + "[ContestAdmin] Property " + args[2] + " unknown");
                return true;
            }
            addContest(contest);
            sender.sendMessage(color2 + "[ContestAdmin] " + args[2] + " of " + args[1] + " set.");
            return true;
        }

        return false;
    }

    private void removeEntry(String contest, String player) {
        getDatabase().delete(getDatabase().find(ContestEntry.class).where().ieq("contestName", contest).ieq("playerName", player));
    }

    private void removeContestData(String contest, CommandSender sender) {
        getDatabase().delete(getDatabase().find(Contest.class).where().ieq("name", contest).findUnique());
        getDatabase().delete(getDatabase().find(ContestEntry.class).where().ieq("contestName", contest).findList());
        sender.sendMessage(color2 + "[ContestAdmin] Data of " + contest + " were removed.");
    }

    private void tpOrd(String contest, Player player) {
        if (!contestExists(contest)) {
            player.sendMessage(color1 + "[ContestAdmin] Contest " + contest + " doesn't exist.");
            return;
        }
        if (!player.hasMetadata("ca-tpord")) {
            player.setMetadata("ca-tpord", new FixedMetadataValue(this, -1));
            if (!getContest(contest).isEnded()) {
                player.sendMessage(color1 + "[ContestAdmin] Please note contest hasn't ended yet. Players can still submit new entries.");
            }
        }
        List<MetadataValue> values = player.getMetadata("ca-tpord");
        int ord = -1;
        for (MetadataValue value : values) {
            if (value.getOwningPlugin().getName().equalsIgnoreCase(getDescription().getName())) {
                ord = value.asInt();
                break;
            }
        }
        ord += 1;
        player.removeMetadata("ca-tpord", this);
        player.setMetadata("ca-tpord", new FixedMetadataValue(this, ord));
        List<ContestEntry> entries = getEntries(contest);
        if (ord < entries.size()) {
            player.teleport(entries.get(ord).getLocation());
            player.sendMessage(color2 + "This is entry by: " + entries.get(ord).getPlayerName());
        } else {
            player.sendMessage(color1 + "[ContestAdmin] No more entries on this contest. Move on to judging results.");
            player.removeMetadata("ca-tpord", this);
        }

    }

    private List<ContestEntry> getEntries(String contest) {
        return getDatabase().find(ContestEntry.class).where().ieq("contestName", contest).findList();
    }

    private void unsubmit(String contest, Player player) {
        if (!contestExists(contest)) {
            player.sendMessage(color1 + "[ContestAdmin] Contest " + contest + " doesn't exist.");
            return;
        }
        if (!tookPartIn(contest, player.getName())) {
            player.sendMessage(color1 + "[ContestAdmin] You haven't even took part in " + contest);
            return;
        }
        getDatabase().delete(getEntry(contest, player.getName()));
    }

    private void enterContest(String contest, Player player) {
        if (!contestExists(contest)) {
            player.sendMessage(color1 + "[ContestAdmin] Contest " + contest + " doesn't exist.");
            return;
        }
        if (isBanned(contest, player.getName())) {
            player.sendMessage(color1 + "[ContestAdmin] You can't take part in " + contest + ".");
            return;
        }
        if (tookPartIn(contest, player.getName())) {
            player.sendMessage(color1 + "[ContestAdmin] You've already took part in " + contest);
            return;
        }
        ContestEntry entry = new ContestEntry();
        entry.setContestName(contest);
        entry.setPlayerName(player.getName());
        entry.setWorldName(player.getWorld().getName());
        entry.setX(player.getLocation().getX());
        entry.setY(player.getLocation().getY());
        entry.setZ(player.getLocation().getZ());
        getDatabase().save(entry);
        player.sendMessage(color2 + "[ContestAdmin] Entry submitted. Good luck! You can unsubmit your entry by /ca unsub " + contest);
    }

    private boolean tookPartIn(String contest, String player) {
        return getEntry(contest, player) != null;
    }

    private boolean isBanned(String contest, String string) {
        if (getContest(contest).getBannedUsers() != null && getContest(contest).getBannedUsers().length() > 0)
            return Arrays.asList(getContest(contest).getBannedUsers().split(",")).contains(string);
        else
            return false;
    }

    private ContestEntry getEntry(String contest, String player) {
        return getDatabase().find(ContestEntry.class).where().ieq("playerName", player).ieq("contestName", contest).findUnique();
    }

    private void addContest(Contest con) {
        getDatabase().save(con);
    }

    private List<Contest> getContests() {
        return getDatabase().find(Contest.class).orderBy("name").findList();
    }

    private void removeContest(String name) {
        getDatabase().delete(getContest(name));
    }

    private Contest getContest(String name) {
        return getDatabase().find(Contest.class).where().ieq("name", name).findUnique();
    }

    private boolean contestExists(String name) {
        return getDatabase().find(Contest.class).where().ieq("name", name).findRowCount() > 0;

    }

    private void addContest(String name, String creator) {
        addContest(new Contest(name, creator));
    }

    private void sendHelp(CommandSender sender) {
        String[] helpString = new String[] { "ContestAdmin v" + getDescription().getVersion(), "/ca start <contest name> - Creates a contest with given name", "/ca stop <contest name> stops given contest", "/ca sub <contest name> - Submits your selection to given contest", "/ca unsub <contest name> - Unsubmits your contest entry", "/ca tp <contest name> <player name> - Teleports you to player's creation in given contest", "/ca tp <contest name> - Teleports you to your entry in given contest", "/ca tpord <contest name> - Starts tping you to entries in specific contest in order(no need to enter player's name)", "/ca purge <contest name> - Removes data about specified contest", "/ca purge confirm - Confirms deletion of the data", "/ca win <contest name> <player> - Promotes winner of given contest", "/ca mod <contest name> <property> <value> - Changes property of given contest", "/ca list - Lists on-going contests(blue = already took part in | green = can take part in | red = can't take part in)", "/ca who <contest name> - Names of Players who took part in specified contest" };
        sender.sendMessage(helpString);
    }
}
