package de.secretmine.plugins;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;

@SuppressWarnings("ALL")
public class BlockShuffleCommands implements CommandExecutor {
    private final Main plugin;

    public BlockShuffleCommands(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        BlockShuffleCommandsHelper helper = new BlockShuffleCommandsHelper(this.plugin, sender);
        if (label.equalsIgnoreCase("blockshuffle")) {
            if (args.length == 0) {
                return false;
            }
            if (args[0].equalsIgnoreCase("start")) {
                return helper.startGame();
            } else if (args[0].equalsIgnoreCase("stop")) {
                return helper.stopGame();
            } else if (args[0].equalsIgnoreCase("info")) {
                return helper.getInfo();
            } else if (args[0].equalsIgnoreCase("skipround")) {
                return helper.skipRound();
            } else if (args[0].equalsIgnoreCase("addall")) {
                return helper.addAllPlayers();
            } else if (args[0].equalsIgnoreCase("add")) {
                if (args.length == 2) return helper.addPlayer(args[1], false);
                else if (args.length == 3) return helper.addPlayer(args[1], true);
                else return false;
            } else if (args[0].equalsIgnoreCase("remove")) {
                if (args.length > 1) return helper.removePlayer(args[1]);
                else return false;
            } else if (args[0].equalsIgnoreCase("list")) {
                return helper.playerList();
            } else if (args[0].equalsIgnoreCase("set")) {
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("noOfRounds")) {
                        if (args.length == 3) return helper.setRounds(args[2], false);
                        else if (args.length == 4) return helper.setRounds(args[2], true);
                        else return false;
                    } else if (args[1].equalsIgnoreCase("roundTime")) {
                        if (args.length > 2) return helper.setRoundTime(args[2]);
                        else return false;
                    } else if (args[1].equalsIgnoreCase("foodAmount")) {
                        if (args.length > 2) return helper.setFoodAmount(args[2]);
                        else return false;
                    }
                } else return false;
            } else return false;
        } else if (label.equalsIgnoreCase("giveup")) {
            return helper.giveUp();
        }
        return false;
    }
}

class BlockShuffleCommandsHelper {
    Main plugin;
    CommandSender sender;
    BlockShuffleTask blockShuffleTask;

    public BlockShuffleCommandsHelper(Main plugin, CommandSender sender) {
        this.plugin = plugin;
        this.sender = sender;
    }

    public boolean skipRound() {
        if (!this.plugin.params.getIsGameRunning()) {
            this.sender.sendMessage("Game is not running.");
            return true;
        }
        for (BlockShufflePlayer player : plugin.params.getAvailablePlayers()) {
            if (!player.roundHasEndedForPlayer())
                player.setHasGivenUp(true);
        }
        return true;
    }

    public boolean giveUp() {
        if (!(sender instanceof Player)) {
            this.sender.sendMessage("Command can only be used as player");
            return true;
        }
        if (!this.plugin.params.getIsGameRunning()) {
            this.sender.sendMessage("Game is not running.");
            return true;
        }
        Player p = (Player) sender;
        for (BlockShufflePlayer player : plugin.params.getAvailablePlayers()) {
            if (player.player.getUniqueId() == p.getUniqueId()) {
                this.sender.sendMessage("You have given up this round.");
                player.setHasGivenUp(true);
                return true;
            }
        }
        this.sender.sendMessage("You are not part of a running game.");
        return true;
    }

    public boolean startGame() {
        if (this.plugin.params.getIsGameRunning()) {
            this.sender.sendMessage("A Game is already running!");
            return true;
        }

        if (this.plugin.params.getAvailablePlayers().size() < 1) {
            this.sender.sendMessage("No players added!");
            return true;
        }

        for (BlockShufflePlayer player : this.plugin.params.getAvailablePlayers()) {
            player.player.setHealth(player.player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
            player.player.setFoodLevel(20);
            player.player.setSaturation(5);
            player.player.getInventory().clear();
            player.player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, this.plugin.params.getInitialFoodAmount()));
        }
        float timeMinutes = (float) this.plugin.params.getRoundTime() / 20 / 60;
        Bukkit.broadcastMessage("Round time: " + ChatColor.BOLD + "" + Math.round(timeMinutes) + " min");
        blockShuffleTask = new BlockShuffleTask(this.plugin);
        BukkitTask task = blockShuffleTask.runTaskTimer(plugin, 0, 10);
        this.plugin.params.setTask(task);
        this.plugin.params.setGameRunning(true);

        return true;
    }

    public boolean stopGame() {
        if (this.plugin.params.getIsGameRunning()) {
            this.sender.sendMessage("Stopping Game");
            Bukkit.getScheduler().cancelTask(this.plugin.params.getTask().getTaskId());

            for (BlockShufflePlayer player : this.plugin.params.getAvailablePlayers()) {
                Player ply = Bukkit.getPlayer(player.getName());
                if (ply != null && Bukkit.getScoreboardManager() != null) {
                    ply.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                }
            }

            this.plugin.params.setGameRunning(false);
            this.blockShuffleTask = null;
        } else {
            this.sender.sendMessage("No Games Running!");
        }
        return true;
    }

    public boolean getInfo() {
        this.sender.sendMessage("No of Rounds : " + this.plugin.params.getNoOfRounds());
        this.sender.sendMessage("Round time : " + this.plugin.params.getRoundTime());
        this.sender.sendMessage("Initial Food Amount : " + this.plugin.params.getInitialFoodAmount());
        return true;
    }

    public boolean addPlayer(String playerString, boolean force) {
        if (this.plugin.params.getIsGameRunning() && !force) {
            sender.sendMessage("Cannot Add Players during game!");
            return true;
        }

        Player player = Bukkit.getPlayer(playerString);
        if (player == null)
            this.sender.sendMessage("No such player found");
        else {
            boolean wasAdded = this.plugin.params.addAvailablePlayer(playerString);
            if (wasAdded) this.sender.sendMessage("Added player : " + playerString);
            else this.sender.sendMessage("Player is already added!");
        }
        return true;
    }

    public boolean addAllPlayers() {
        if (this.plugin.params.getIsGameRunning()) {
            sender.sendMessage("Cannot Add Players during game!");
            return true;
        }
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        for (Player player : players) {
            String playerString = player.getName();
            boolean wasAdded = this.plugin.params.addAvailablePlayer(playerString);
            if (wasAdded) this.sender.sendMessage("Added player : " + playerString);
            else this.sender.sendMessage("Player is already added!");
        }
        return true;
    }

    public boolean removePlayer(String playerString) {
        if (this.plugin.params.getIsGameRunning()) {
            sender.sendMessage("Cannot Remove Players during game!");
            return true;
        }
        boolean wasRemoved = this.plugin.params.removeAvailablePlayer(playerString);
        if (wasRemoved) this.sender.sendMessage("Removed player : " + playerString);
        else this.sender.sendMessage("Player was not in player list!");
        return true;
    }

    public boolean playerList() {

        if (this.plugin.params.getAvailablePlayers().size() == 0) {
            sender.sendMessage("No added players");
        } else {
            this.sender.sendMessage("The list of added players are : ");
            for (BlockShufflePlayer player : this.plugin.params.getAvailablePlayers()) {
                this.sender.sendMessage(player.getName());
            }
        }

        return true;
    }

    public boolean setRounds(String numberOfRoundsString, boolean force) {
        int noOfRounds;

        try {
            noOfRounds = Integer.parseInt(numberOfRoundsString);
        } catch (Exception e) {
            return false;
        }

        if (noOfRounds < 1) {
            this.sender.sendMessage("Number of rounds cannot be less than one!");
        } else if (noOfRounds > 10000) {
            this.sender.sendMessage("Bruh. Get a life");
        } else {
            if (this.plugin.params.getIsGameRunning() && !force) {
                sender.sendMessage("Game is running! Cannot modify parameters during game");
            } else {
                this.plugin.params.setNoOfRounds(noOfRounds);
                sender.sendMessage("Set Number of rounds to : " + noOfRounds);
            }
        }
        return true;
    }

    public boolean setRoundTime(String roundTimeString) {
        int roundTime;

        try {
            roundTime = Integer.parseInt(roundTimeString);
        } catch (Exception e) {
            return false;
        }

        if (roundTime < 600) {
            this.sender.sendMessage("Round time cannot be less than 30 seconds!");
        } else if (roundTime > 720000) {
            this.sender.sendMessage("Bruh. Get a life");
        } else {
            if (this.plugin.params.getIsGameRunning()) {
                sender.sendMessage("Game is running! Cannot modify parameters during game");
            } else {
                this.plugin.params.setRoundTime(roundTime);
                sender.sendMessage("Set round time to : " + roundTime);
            }
        }
        return true;
    }

    public boolean setFoodAmount(String foodAmountString) {
        int foodAmount;

        try {
            foodAmount = Integer.parseInt(foodAmountString);
        } catch (Exception e) {
            return false;
        }

        if (foodAmount < 0) {
            this.sender.sendMessage("Food amount cannot be less than zero!");
        } else if (foodAmount > 2304) {
            this.sender.sendMessage("Food amount cannot be larger than inventory size smh");
        } else {
            if (this.plugin.params.getIsGameRunning()) {
                sender.sendMessage("Game is running! Cannot modify parameters during game");
            } else {
                this.plugin.params.setInitialFoodAmount(foodAmount);
                sender.sendMessage("Set Food amount to : " + foodAmount);
            }
        }
        return true;
    }
}
