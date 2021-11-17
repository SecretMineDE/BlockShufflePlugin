package de.secretmine.plugins;

import org.bukkit.Material;
import org.bukkit.entity.Player;

public class BlockShufflePlayer {
    Player player;
    int score;
    Material blockToBeFound;
    boolean hasFoundBlock;
    boolean hasGivenUp;

    public BlockShufflePlayer(Player player) {
        this.player = player;
        score = 0;
        blockToBeFound = null;
        hasFoundBlock = false;
        hasGivenUp = false;
    }

    public String getName() {
        return this.player.getName();
    }

    public int getScore() {
        return this.score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean getHasFoundBlock() {
        return this.hasFoundBlock;
    }

    public void setHasFoundBlock(boolean hasFoundBlock) {
        this.hasFoundBlock = hasFoundBlock;
    }

    public Material getBlockToBeFound() {
        return blockToBeFound;
    }

    public void setBlockToBeFound(Material blockToBeFound) {
        this.blockToBeFound = blockToBeFound;
    }

    public void notifyFound() {
        player.sendMessage("You've found your block!");
    }

    public void resetBeginningOfRound() {
        this.hasFoundBlock = false;
        this.hasGivenUp = false;
    }

    public boolean isHasGivenUp() {
        return hasGivenUp;
    }

    public void setHasGivenUp(boolean hasGivenUp) {
        this.hasGivenUp = hasGivenUp;
    }

    public boolean roundHasEndedForPlayer() {
        return this.hasGivenUp || this.hasFoundBlock;
    }
}
