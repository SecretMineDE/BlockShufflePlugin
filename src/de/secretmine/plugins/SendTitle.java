/*
* COMPLETELY COPY PASTED FROM TITLE API BY CONNOR LINFOOT
*
* https://github.com/connorlinfoot/TitleAPI
*
* */

package de.secretmine.plugins;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.lang.reflect.Constructor;

@Deprecated
public class SendTitle {
    @Deprecated
    public void sendTitle(Player player, Integer fadeIn, Integer stay, Integer fadeOut, String title, String subtitle) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }
}
