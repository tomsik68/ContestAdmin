/*
 * This file is part of ContestAdmin. ContestAdmin is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any later version. ContestAdmin is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received
 * a copy of the GNU General Public License along with ContestAdmin. If not, see <http://www.gnu.org/licenses/>.
 */
package sk.tomsik68.contestadmn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

/** ContestAdmin - Contest administration helper plugin for Bukkit.
 * Check <a href="http://dev.bukkit.org/server-mods/contestadmin/">Plugin's Homepage</a>
 * for more information.
 * @author Tomsik68
 *
 */
public class ContestAdmin extends JavaPlugin {
    private String consolePurge;
    private static final HashSet<String> contestProperties = new HashSet<String>(Arrays.asList("name", "creator", "desc", "rules", "banned", "ended"));

    @Override
    public void onEnable() {
        getCommand("ca").setExecutor(this);
        try {
            getDatabase().find(Contest.class).findRowCount();
            System.out.println("[ContestAdmin] DB is ok.");
        } catch (Exception e) {
            System.out.println("[ContestAdmin] Installing database due to first time usage...");
            installDDL();
            System.out.println("[ContestAdmin] Database setup success!");
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
        }
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("list")) {
                if (!sender.hasPermission("ca.list")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                List<Contest> contests = getContests();
                StringBuilder sb = new StringBuilder();
                for (Contest con : contests) {
                    if (tookPartIn(con.getName(), sender.getName()))
                        sb = sb.append(ChatColor.BLUE).append(con.getName()).append(',');
                    else if (sender.hasPermission("ca.sub") && !isBanned(con.getName(), sender.getName()))
                        sb = sb.append(ChatColor.GREEN).append(con.getName()).append(',');
                    else
                        sb = sb.append(ChatColor.RED).append(con.getName()).append(',');
                }
                if (sb.length() > 0) {
                    sb = sb.deleteCharAt(sb.length() - 1);
                    sender.sendMessage("");
                }
                sender.sendMessage("[ContestAdmin] There are no on-going contests.");
            }
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("start")) {
                if (!sender.hasPermission("ca.start")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (contestExists(args[1])) {
                    sender.sendMessage("[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                } else {
                    addContest(args[1], sender.getName());
                    getServer().broadcastMessage("[ContestAdmin] Contest " + args[1] + " has just been promoted! Type /ca info " + args[1] + " to find out more!");
                }

            } else if (args[0].equalsIgnoreCase("stop")) {
                if (!sender.hasPermission("ca.stop")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (contestExists(args[1])) {
                    Contest con = getContest(args[1]);
                    removeContest(args[1]);
                    con.setEnded(true);
                    addContest(con);
                    getServer().broadcastMessage("[ContestAdmin] Contest " + args[1] + " has just ended. Let it be judged. Good luck there!");
                } else {
                    sender.sendMessage("[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
            } else if (args[0].equalsIgnoreCase("tp")) {
                if (!sender.hasPermission("ca.tpmy")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage("[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                }
                ContestEntry entry = getEntry(args[1], sender.getName());
                if (entry != null) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        player.teleport(entry.getLocation());
                        sender.sendMessage("[ContestAdmin] Welcome to your contest entry.");
                    } else {

                    }
                } else {
                    if (getContest(args[1]).getEnded())
                        sender.sendMessage("[ContestAdmin] You haven't submitted any entry to " + args[1] + ". Bad luck, contest has already ended.");
                    else
                        sender.sendMessage("[ContestAdmin] You haven't submitted any entry to " + args[1] + ". However, you can still submit your entry.");
                }

            } else if (args[0].equalsIgnoreCase("sub")) {
                if (!sender.hasPermission("ca.sub")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage("[ContestAdmin] You need to be player to use this command.");
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage("[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
                enterContest(args[1], (Player) sender);
                sender.sendMessage("[ContestAdmin] You've just entered " + args[1] + ". Good luck!");
            } else if (args[0].equalsIgnoreCase("unsub")) {
                if (!sender.hasPermission("ca.unsub")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage("[ContestAdmin] You need to be player to use this command.");
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage("[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
                // TODO discuss
                if (!getContest(args[1]).getEnded()) {
                    unsubmit(args[1], (Player) sender);
                    sender.sendMessage("[ContestAdmin] You've just left " + args[1] + ".");
                } else {
                    sender.sendMessage("[ContestAdmin] You can't left " + args[1] + ". Contest has already been closed.");
                }
            } else if (args[0].equalsIgnoreCase("purge")) {
                if (!sender.hasPermission("ca.purge")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (args[1].equalsIgnoreCase("confirm")) {
                    if (!(sender instanceof Player)) {
                        if (consolePurge == null) {
                            sender.sendMessage("[ContestAdmin] You've got nothing to confirm.");
                        }
                        if (!contestExists(consolePurge)) {
                            sender.sendMessage("[ContestAdmin] Contest " + consolePurge + " doesn't exist.");
                            return true;
                        }
                        removeContestData(consolePurge, sender);
                    }
                } else {
                    if (!(sender instanceof Player)) {
                        consolePurge = args[1];
                    } else {
                        ((Player) sender).setMetadata("ca-purge", new FixedMetadataValue(this, args[1]));
                    }
                    sender.sendMessage("[ContestAdmin] Purging " + args[1] + ". Are you sure? confirm with /ca purge confirm");
                }
            } else if (args[0].equalsIgnoreCase("tpord")) {
                if (!sender.hasPermission("ca.tpord")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage("[ContestAdmin] You need to be player to use this command.");
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage("[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
                tpOrd(args[1], (Player) sender);
            } else if (args[0].equalsIgnoreCase("info")) {
                if (!sender.hasPermission("ca.info")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage("[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
                Contest contest = getContest(args[1]);
                sender.sendMessage("Contest Name: " + args[1]);
                sender.sendMessage("Organisator: " + contest.getStarter());
                if (contest.getBannedUsers() != null && contest.getBannedUsers().length() > 0)
                    sender.sendMessage("Banned: " + contest.getBannedUsers());
                if (contest.getEnded())
                    sender.sendMessage("Ended");
                else
                    sender.sendMessage("Accepting entries");
                if (contest.getDescription() != null && contest.getDescription().length() > 0)
                    sender.sendMessage(contest.getDescription());
                if (contest.getRules() != null && contest.getRules().length() > 0) {
                    sender.sendMessage("Rules:");
                    sender.sendMessage(contest.getRules());
                }
                if (contest.getWinner() != null && contest.getWinner().length() > 0) {
                    sender.sendMessage("Winner: " + contest.getWinner());
                }

            } else if (args[0].equalsIgnoreCase("who")) {
                if (!sender.hasPermission("ca.who")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage("[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
                List<ContestEntry> entries = getEntries(args[1]);
                StringBuilder sb = new StringBuilder();
                for (ContestEntry entry : entries) {
                    if (getServer().getPlayer(entry.getPlayerName()) != null)
                        sb = sb.append(ChatColor.AQUA).append(entry.getPlayerName()).append(ChatColor.WHITE).append(',');
                    else
                        sb = sb.append(ChatColor.RED).append(entry.getPlayerName()).append(ChatColor.WHITE).append(',');
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("tp")) {
                if (!sender.hasPermission("ca.tp")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage("[ContestAdmin] You need to be player to use this command.");
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage("[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
                if (!tookPartIn(args[1], args[2])) {
                    sender.sendMessage("[ContestAdmin] " + args[2] + " hasn't submitted any entry to " + args[1]);
                }
                ((Player) sender).teleport(getEntry(args[1], args[2]).getLocation());
            } else if (args[0].equalsIgnoreCase("win")) {
                if (!sender.hasPermission("ca.win")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage("[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
                if (!getContest(args[1]).getEnded()) {
                    sender.sendMessage("[ContestAdmin] Contest " + args[1] + " hasn't ended yet, but we'll end it for you. :)");
                }
                Contest con = getContest(args[1]);
                removeContest(args[1]);
                con.setEnded(true);
                con.setWinner(args[2]);
                addContest(con);
                getServer().broadcastMessage("[ContestAdmin] And the winner of " + args[1] + " iiiiiiiiiiiiis: ");
                getServer().broadcastMessage(args[2]);
            } else if (args[0].equalsIgnoreCase("mod")) {
                if (!sender.hasPermission("ca.mod")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage("[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
                if (!contestProperties.contains(args[2])) {
                    sender.sendMessage("[ContestAdmin] Property " + args[2] + " doesn't exist");
                    return true;
                }
                if (args[2].equalsIgnoreCase("name")) {
                    Contest con = getContest(args[1]);
                    removeContest(args[1]);
                    con.setName(args[3]);
                    addContest(con);
                }
                if (args[2].equalsIgnoreCase("creator")) {
                    Contest con = getContest(args[1]);
                    removeContest(args[1]);
                    con.setStarter(args[3]);
                    addContest(con);
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
                    con.setEnded(Boolean.valueOf(args[3]));
                    addContest(con);
                }
            }
        } else if (args.length == 5) {
            if (args[0].equalsIgnoreCase("mod") && args[2].equalsIgnoreCase("banned")) {
                if (!sender.hasPermission("ca.mod")) {
                    sender.sendMessage(command.getPermissionMessage());
                    return true;
                }
                if (!contestExists(args[1])) {
                    sender.sendMessage("[ContestAdmin] Contest " + args[1] + " doesn't exist.");
                    return true;
                }
                Contest con = getContest(args[1]);
                removeContest(args[1]);
                if (args[3].equalsIgnoreCase("add")) {
                    con.setBannedUsers(con.getBannedUsers() + ("," + args[4]));
                    sender.sendMessage("[ContestAdmin] Successfully banned " + args[4] + " from " + args[1]);
                } else if (args[3].equalsIgnoreCase("remove")) {
                    con.getBannedUsers().replace(args[4], "");
                    con.getBannedUsers().replace(",,", ",");
                    sender.sendMessage("[ContestAdmin] Successfully un-banned " + args[4] + " from " + args[1]);
                }
                addContest(con);

            }
        }

        return true;
    }

    private void removeContestData(String contest, CommandSender sender) {
        getDatabase().delete(getDatabase().find(Contest.class).where().ieq("name", contest));
        getDatabase().delete(getDatabase().find(ContestEntry.class).where().ieq("contestName", contest));
        sender.sendMessage("[ContestAdmin] Data of " + contest + " were removed.");
    }

    private void tpOrd(String contest, Player player) {
        if (!contestExists(contest)) {
            player.sendMessage("[ContestAdmin] Contest " + contest + " doesn't exist.");
            return;
        }
        if (!player.hasMetadata("ca-tpord")) {
            player.setMetadata("ca-tpord", new FixedMetadataValue(this, -1));
            if (!getContest(contest).getEnded()) {
                player.sendMessage("[ContestAdmin] Please note contest hasn't ended yet. Players can still submit new entries.");
            }
        }
        List<MetadataValue> values = player.getMetadata("ca-tpord");
        int ord = -1;
        for (MetadataValue value : values) {
            if (value.getOwningPlugin().getName().equals(getDescription().getName())) {
                ord = value.asInt();
                break;
            }
        }
        player.removeMetadata("ca-tpord", this);
        player.setMetadata("ca-tpord", new FixedMetadataValue(this, ord++));
        List<ContestEntry> entries = getEntries(contest);
        if (ord < entries.size()) {
            player.teleport(entries.get(ord).getLocation());
            player.sendMessage("This is entry by: " + entries.get(ord).getPlayerName());
        } else {
            player.sendMessage("[ContestAdmin] No more entries on this contest. Move on to judging results.");
            player.removeMetadata("ca-tpord", this);
        }
    }

    private List<ContestEntry> getEntries(String contest) {
        return getDatabase().find(ContestEntry.class).where().ieq("contestName", contest).findList();
    }

    private void unsubmit(String contest, Player player) {
        if (!contestExists(contest)) {
            player.sendMessage("[ContestAdmin] Contest " + contest + " doesn't exist.");
            return;
        }
        if (!tookPartIn(contest, player.getName())) {
            player.sendMessage("[ContestAdmin] You haven't even took part in " + contest);
            return;
        }
        getDatabase().delete(getEntry(contest, player.getName()));
    }

    private void enterContest(String contest, Player player) {
        if (!contestExists(contest)) {
            player.sendMessage("[ContestAdmin] Contest " + contest + " doesn't exist.");
            return;
        }
        if (isBanned(contest, player.getName())) {
            player.sendMessage("[ContestAdmin] You can't take part in " + contest + ".");
            return;
        }
        if (tookPartIn(contest, player.getName())) {
            player.sendMessage("[ContestAdmin] You've already took part in " + contest);
        }
        ContestEntry entry = new ContestEntry();
        entry.setContestName(contest);
        entry.setPlayerName(player.getName());
        entry.setWorldName(player.getWorld().getName());
        entry.setX(player.getLocation().getX());
        entry.setY(player.getLocation().getY());
        entry.setZ(player.getLocation().getZ());
        getDatabase().save(entry);
        player.sendMessage("[ContestAdmin] Entry submitted. You can unsubmit your entry by /ca unsub " + contest);
    }

    private boolean tookPartIn(String contest, String player) {
        return getEntry(contest, player) != null;
    }

    private boolean isBanned(String contest, String string) {
        return Arrays.asList(getContest(contest).getBannedUsers().split(",")).contains(string);
    }

    private ContestEntry getEntry(String contest, String player) {
        return getDatabase().find(ContestEntry.class).where().ieq("playerName", player).findUnique();
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
