/*
 * This file is part of IsoWorlds, licensed under the MIT License (MIT).
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
package sponge.command.sub;

import common.Cooldown;
import common.ManageFiles;
import common.Msg;
import common.action.IsoWorldsAction;
import sponge.Main;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.*;
import org.spongepowered.api.world.storage.WorldProperties;
import sponge.util.action.StatAction;
import sponge.util.console.Logger;
import sponge.util.message.Message;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Reforge implements CommandExecutor {

    private final Main plugin = Main.instance;
    final static Map<String, Timestamp> confirm = new HashMap<String, Timestamp>();

    @Override
    public CommandResult execute(CommandSource source, CommandContext args) throws CommandException {

        // Variables
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String fullpath = "";
        String worldname = "";
        Player pPlayer = (Player) source;

        //If the method return true then the command is in lock
        if (!plugin.cooldown.isAvailable(pPlayer, Cooldown.REFONTE)) {
            return CommandResult.success();
        }

        // SELECT WORLD
        if (!IsoWorldsAction.isPresent(pPlayer, false)) {
            pPlayer.sendMessage(Message.error(Msg.keys.ISOWORLD_NOT_FOUND));
            return CommandResult.success();
        }

        // Confirmation
        if (!(confirm.containsKey(pPlayer.getUniqueId().toString()))) {
            confirm.put(pPlayer.getUniqueId().toString(), timestamp);
            pPlayer.sendMessage(Text.of(Text.builder("[IsoWorlds]: ").color(TextColors.GOLD)
                    .append(Text.of(Text.builder(Msg.keys.CONFIRMATION).color(TextColors.AQUA))).build()));
            return CommandResult.success();
        } else {
            long millis = timestamp.getTime() - (confirm.get(pPlayer.getUniqueId().toString()).getTime());
            long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
            if (minutes >= 1) {
                confirm.remove(pPlayer.getUniqueId().toString());
                pPlayer.sendMessage(Text.of(Text.builder("[IsoWorlds]: ").color(TextColors.GOLD)
                        .append(Text.of(Text.builder(Msg.keys.CONFIRMATION).color(TextColors.AQUA))).build()));
                return CommandResult.success();
            }
        }
        confirm.remove(pPlayer.getUniqueId().toString());
        fullpath = (ManageFiles.getPath() + StatAction.PlayerToUUID(pPlayer) + "-IsoWorld");
        worldname = (StatAction.PlayerToUUID(pPlayer) + "-IsoWorld");
        File sourceDir = new File(ManageFiles.getPath() + worldname);
        File destDir = new File(ManageFiles.getPath() + "/IsoWorlds-REFONTE/" + worldname);
        destDir.mkdir();
        if (!Sponge.getServer().getWorld(worldname).isPresent()) {
            pPlayer.sendMessage(Message.error(Msg.keys.ISOWORLD_NOT_FOUND));
            return CommandResult.success();
        }
        if (Sponge.getServer().getWorld(worldname).get().isLoaded()) {
            Collection<Player> colPlayers = Sponge.getServer().getWorld(worldname).get().getPlayers();
            Location<World> spawn = Sponge.getServer().getWorld("Isolonice").get().getSpawnLocation();
            for (Player player : colPlayers) {
                player.setLocation(spawn);
                player.sendMessage(Text.of(Text.builder("[IsoWorlds]: ").color(TextColors.GOLD)
                        .append(Text.of(Text.builder(Msg.keys.REFONTE_KICK).color(TextColors.AQUA))).build()));
            }
            World world = Sponge.getServer().getWorld(worldname).get();
            Sponge.getServer().unloadWorld(world);
        }

        //iWorldsUtils.deleteDir(sourceDir);
        Optional<WorldProperties> optionalWorld = Sponge.getServer().getWorldProperties(worldname);
        WorldProperties world = optionalWorld.get();
        try {
            if (Sponge.getServer().deleteWorld(world).get()) {
                Logger.info("Le monde: " + worldname + " a bien été supprimé");
            }
        } catch (InterruptedException | ExecutionException ie) {
            ie.printStackTrace();
        }


        // DELETE WORLD
        if (!IsoWorldsAction.deleteIsoWorld(pPlayer.getUniqueId().toString())) {
            pPlayer.sendMessage(Text.of(Text.builder("[IsoWorlds]: ").color(TextColors.GOLD)
                    .append(Text.of(Text.builder(Msg.keys.EXISTE_IWORLD).color(TextColors.AQUA))).build()));
            return CommandResult.success();
        }

        pPlayer.sendMessage(Text.of(Text.builder("[IsoWorlds]: ").color(TextColors.GOLD)
                .append(Text.of(Text.builder(Msg.keys.SUCCES_REFONTE).color(TextColors.AQUA))).build()));

        plugin.cooldown.addPlayerCooldown(pPlayer, Cooldown.REFONTE, Cooldown.REFONTE_DELAY);

        Sponge.getCommandManager().process(pPlayer, "iw");

        return CommandResult.success();
    }


    // Constructeurs
    public static CommandSpec getCommand() {
        return CommandSpec.builder()
                .description(Text.of("Commandes de refonte des iWorlds"))
                    .permission("iworlds.refonte")
                .executor(new Reforge())
                .build();
    }
}