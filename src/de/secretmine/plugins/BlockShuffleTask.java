package de.secretmine.plugins;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlockShuffleTask extends BukkitRunnable {
    Logger logger;
    boolean hasRoundEnded;
    Main plugin;
    BlockShuffleTaskHelper helper;
    int currentRoundTime;
    int currentRound;
    List<BlockShufflePlayer> players;
    int counter;
    SendTitle titleSender;
    static BossBar bossBar;


    public BlockShuffleTask(Main plugin) {
        this.logger = Bukkit.getLogger();
        this.plugin = plugin;
        this.hasRoundEnded = true;
        this.currentRoundTime = 0;
        this.currentRound = 0;
        this.counter = 100;
        this.titleSender = new SendTitle();
        this.helper = new BlockShuffleTaskHelper(this.plugin, this.currentRound);
        if (bossBar == null)
            bossBar = Bukkit.getServer().createBossBar("BlockShuffle", BarColor.YELLOW, BarStyle.SEGMENTED_10);
        this.players = this.plugin.params.getAvailablePlayers();
    }

    private void updateBossBar() {
        double progress = 0.0;
        if (currentRoundTime > 0)
            progress = 1.0 - (float) currentRoundTime / (float) this.plugin.params.getRoundTime();
        bossBar.setProgress(progress);
    }

    public static void removeBossbarIfExists() {
        if (bossBar != null)
            bossBar.removeAll();
    }

    private boolean allPlayersFoundOrGivenUp() {
        for (BlockShufflePlayer player : players) {
            if (!player.isHasGivenUp() && !player.getHasFoundBlock()) {
                return false;
            }
        }
        return true;
    }

    private int getFinishedPlayerCount() {
        int count = 0;
        for (BlockShufflePlayer player : players) {
            if (player.isHasGivenUp() || player.getHasFoundBlock()) {
                count++;
            }
        }
        return count;
    }

    private void resetAllPlayerStatus() {
        for (BlockShufflePlayer player : players) {
            player.resetBeginningOfRound();
        }
    }

    @Override
    public void run() {
        for (BlockShufflePlayer player : this.plugin.params.getAvailablePlayers())
            bossBar.addPlayer(player.player);

        if (counter > 0) {
            if (counter % 20 == 0) {
                for (BlockShufflePlayer player : this.plugin.params.getAvailablePlayers())
                    titleSender.sendTitle(player.player, 5, 10, 5, ChatColor.BLUE + "Game Starting", ChatColor.RED + "" + (counter / 20));
            }

            counter -= 10;
        } else {
            if (hasRoundEnded) {
                this.currentRound += 1;
                Bukkit.broadcastMessage("Starting Round: " + ChatColor.BOLD + "" + this.currentRound);
                this.currentRoundTime = 0;
                this.hasRoundEnded = false;
                resetAllPlayerStatus();
                helper.startRound(this.currentRound);
                bossBar.setVisible(true);
                updateBossBar();
            } else {
                for (BlockShufflePlayer player : this.plugin.params.getAvailablePlayers()) {
                    if (!player.roundHasEndedForPlayer()) {
                        boolean hasFound = helper.checkPlayer(player);
                        if (hasFound) {
                            player.setHasFoundBlock(true);
                            //player.notifyFound();
                            helper.updateBoard(player);
                        }
                    }
                }

                String bossBarTitle = getFinishedPlayerCount() + " / " + this.plugin.params.getAvailablePlayers().size() + " players finished";
                bossBar.setTitle(bossBarTitle);

                int timeRemaining = this.plugin.params.getRoundTime() - this.currentRoundTime;
                updateBossBar();

                if (allPlayersFoundOrGivenUp()) {
                    Bukkit.broadcastMessage(ChatColor.GREEN + "Everyone Found their block!");
                    this.hasRoundEnded = true;
                } else if (timeRemaining <= 0) {
                    Bukkit.broadcastMessage("\nTime Up!");
                    this.hasRoundEnded = true;
                } else if (timeRemaining <= 200) {
                    if (timeRemaining % 20 == 0)
                        Bukkit.broadcastMessage(ChatColor.RED + "Time Remaining : " + ChatColor.BOLD + (timeRemaining / 20) + " seconds");
                    this.currentRoundTime += 10;
                } else {
                    this.currentRoundTime += 10;
                }


                if (this.hasRoundEnded && this.currentRound == this.plugin.params.getNoOfRounds()) {
                    this.cancel();
                }
            }
        }
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        super.cancel();
        helper.endGame();
        bossBar.setVisible(false);
        bossBar.removeAll();
    }
}

class BlockShuffleTaskHelper {

    Main plugin;
    int currentRound;

    public BlockShuffleTaskHelper(Main plugin, int currentRound) {
        this.plugin = plugin;
        this.currentRound = currentRound;
    }

    public void startRound(int currentRound) {
        this.currentRound = currentRound;
        for (BlockShufflePlayer player : this.plugin.params.getAvailablePlayers()) {
            player.setHasFoundBlock(false);
            player.setBlockToBeFound(getRandomBlock());
            Bukkit.getLogger().info("Assigned " + player.getName() + " with " + player.getBlockToBeFound());
            createBoard(player);
        }
    }

    public Material getRandomBlock() {
        Random rand = new Random();
        int randomNumber = rand.nextInt(this.plugin.params.getAvailableBlocks().size());
        return this.plugin.params.getAvailableBlocks().get(randomNumber);
    }

    public void updateBoard(BlockShufflePlayer player) {
        createBoard(player);
    }

    public void createBoard(BlockShufflePlayer player) {
        Bukkit.getLogger().info("Creating Scoreboard for " + player.getName());
        Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();
        Objective obj = scoreboard.registerNewObjective("BSObjective", "dummy", "Block Shuffle");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        Score s1 = obj.getScore("Round: " + this.currentRound + "/" + this.plugin.params.getNoOfRounds());
        s1.setScore(5);

        Score s3 = obj.getScore("Score: " + player.getScore());
        s3.setScore(4);

        Score s4 = obj.getScore("");
        s4.setScore(3);

        String s6Text = player.getBlockToBeFound().toString();
        if (player.hasFoundBlock)
            s6Text = s6Text + " ✔";

        Score s5 = obj.getScore("Block:");
        s5.setScore(2);

        Score s6 = obj.getScore(s6Text);
        s6.setScore(1);


        player.player.setScoreboard(scoreboard);
    }

    public boolean checkPlayer(BlockShufflePlayer player) {
        if (Bukkit.getPlayer(player.getName()) == null) {
            player.setHasFoundBlock(true);
            return true;
        }
        Material standingOn = Objects.requireNonNull(Bukkit.getPlayer(player.getName())).getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
        if (standingOn.equals(player.getBlockToBeFound())) {
            player.setHasFoundBlock(true);
            player.setScore(player.getScore() + 1);
            broadcastSoundToEveryoneBut(Sound.BLOCK_END_PORTAL_SPAWN, player.player);
            player.player.playSound(player.player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            Bukkit.broadcastMessage(ChatColor.GREEN + player.getName() + " has found their block!");
            return true;
        }

        return false;
    }

    public void broadcastSoundToEveryoneBut(Sound sound, Player removePlayer) {
        for (BlockShufflePlayer player : this.plugin.params.getAvailablePlayers()) {
            if (player.player == removePlayer)
                continue;
            player.player.playSound(player.player.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    public void broadcastSound(Sound sound) {
        for (BlockShufflePlayer player : this.plugin.params.getAvailablePlayers()) {
            player.player.playSound(player.player.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    public void printScores() {
        Map<String, Integer> scores = new HashMap<>();
        for (BlockShufflePlayer player : this.plugin.params.getAvailablePlayers()) {
            Player ply = Bukkit.getPlayer(player.getName());
            if (ply == null)
                continue;
            scores.put(player.getName(), player.getScore());
        }
        Map<String, Integer> sorted =
                scores.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));


        //Output
        Bukkit.broadcastMessage("\nBlockShuffle Scores: \n");
        for (Map.Entry<String, Integer> mapEntry : scores.entrySet()) {
            String message = ChatColor.BLUE + mapEntry.getKey() + ": " + ChatColor.GREEN + mapEntry.getValue() + " Points";
            Bukkit.broadcastMessage(message);
        }
    }

    public void endGame() {
        SendTitle titleSender = new SendTitle();
        broadcastSound(Sound.UI_TOAST_CHALLENGE_COMPLETE);

        for (BlockShufflePlayer player : this.plugin.params.getAvailablePlayers()) {
            Player ply = Bukkit.getPlayer(player.getName());
            if (ply == null)
                continue;
            ply.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            titleSender.sendTitle(ply, 10, 30, 10, ChatColor.RED + "" + "Game Over", "");
        }

        printScores();
        this.plugin.params.setGameRunning(false);
    }
}
