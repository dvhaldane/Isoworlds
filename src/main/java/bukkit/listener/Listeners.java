/*
 * This file is part of Isoworlds, licensed under the MIT License (MIT).
 *
 * Copyright (c) Edwin Petremann <https://github.com/Isolonice/>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package bukkit.listener;

import bukkit.Main;
import bukkit.configuration.Configuration;
import bukkit.location.Locations;
import bukkit.util.console.Logger;
import bukkit.util.message.Message;
import common.ManageFiles;
import common.Msg;
import common.action.IsoworldsAction;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static common.Msg.msgNode;

public class Listeners implements Listener {

    private final Main instance = Main.getInstance();

    @EventHandler
    public void onRespawnPlayerEvent(PlayerRespawnEvent event) {

        Player p = event.getPlayer();
        String worldname = p.getUniqueId() + "-Isoworld";

        new BukkitRunnable() {

            @Override
            public void run() {
                if (Bukkit.getServer().getWorld(worldname) != null) {
                    Locations.teleport(p, worldname);
                } else {
                    Logger.info("SPAWN ISOLONICE");
                    Locations.teleport(p, "Isolonice");
                }
            }

        }.runTaskLater(this.instance, 1);
    }

    // Set autosave for a new loaded world to avoid crash ?
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        new BukkitRunnable() {

            @Override
            public void run() {
                event.getWorld().setAutoSave(true);
                Logger.info("- Isoworld setAutoSave to TRUE");
            }
        }.runTaskLater(this.instance, 20);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Welcome message and open menu for those who do not have their own Isoworld
        if (!IsoworldsAction.iwExists(event.getPlayer().getUniqueId().toString())) {
            new BukkitRunnable() {

                @Override
                public void run() {
                    event.getPlayer().sendMessage(ChatColor.GOLD + "[Isoworlds]: " + ChatColor.GREEN + msgNode.get("InvIsochat"));
                    event.getPlayer().performCommand("iw");
                }

            }.runTaskLater(this.instance, 100);
        }

        String world = event.getPlayer().getWorld().getName();
        Logger.info("DEBUG1: " + world);
    }

    // Logout event, tp spawn
    @EventHandler
    public void onLogout(PlayerQuitEvent event) {

        Player p = event.getPlayer();

        Locations.teleport(p, "Isolonice");
        Logger.info("Joueur téléporté au spawn");
    }

    @EventHandler
    // Anti grief spawn
    public void onInteractSpawn(PlayerInteractEvent event) {

        // ****** MODULES ******
        // Spawn Protection
        if (Configuration.getSpawnProtection()) {
            Player p = event.getPlayer();
            if (p.hasPermission("Isoworlds.bypass.spawn")) {
                return;
            }
            if (p.getLocation().getWorld().getName().equals("Isolonice")) {
                event.setCancelled(true);
            }
        }
        // *********************
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerTeleportEvent event) {
        final String CHECK = "SELECT * FROM `autorisations` WHERE `uuid_p` = ? AND `uuid_w` = ?";
        String check_p;
        String check_w;
        Location worldTo = event.getTo();
        Location worldFrom = event.getFrom();
        Player pPlayer = event.getPlayer();

        Logger.info("Téléporation: " + pPlayer.getName() + " [FROM: " + worldFrom.toString() + "] - [TO: " + worldTo.toString() + "] - [CAUSE: "
                + event.getCause().toString() + "]");
        if (worldTo.toString().equals(worldFrom.toString())) {
            return;
        }

        // Deny teleport to unloaded world with /back that load world DIM number instead
        File checkFolder = new File(ManageFiles.getPath() + worldTo.getWorld().getName());
        if (!checkFolder.exists() & worldTo.getWorld().getName().contains("Isoworld")) {
            event.setCancelled(true);
            Logger.warning("Isoworld non actif, téléporation annulée !");
            return;
        }

        String eventworld = worldTo.getWorld().getName();

        if (eventworld.contains("-Isoworld")) {
            try {
                PreparedStatement check = instance.database.prepare(CHECK);
                // UUID_P
                check_p = pPlayer.getUniqueId().toString();
                check.setString(1, check_p);
                // UUID_W
                check_w = eventworld;
                check.setString(2, check_w);
                // Requête
                ResultSet rselect = check.executeQuery();
                if (pPlayer.hasPermission("Isoworlds.bypass.teleport")) {
                    return;
                }

                // To clean
                if (rselect.isBeforeFirst()) {
                    return;
                    // Cas du untrust, pour ne pas rester bloquer
                } else if (pPlayer.getWorld().getName() == eventworld) {
                    return;
                } else {
                    event.setCancelled(true);
                    pPlayer.sendMessage(ChatColor.GOLD + "[Isoworlds]: " + ChatColor.AQUA + Msg.msgNode.get("DenyTeleport"));
                    return;
                }

            } catch (Exception se) {
                pPlayer.sendMessage(Message.error(Msg.msgNode.get("IsoworldNotFound")));
            }

        }
    }

//    // Method from worldborder
//    // https://github.com/Brettflan/WorldBorder/blob/master/src/main/java/com/wimbli/WorldBorder/WBListener.java
//    @EventHandler(priority = EventPriority.MONITOR)
//    public void onChunkLoad(ChunkLoadEvent event) {
//        if (event.isNewChunk() & event.getWorld().getName().contains("-Isoworld")) {
//            Chunk chunk = event.getChunk();
//            chunk.unload(false, false);
//        }
//    }
}