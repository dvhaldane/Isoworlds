package sponge.Utils;

import sponge.IworldsSponge;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.property.block.SolidCubeProperty;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.title.Title;
import org.spongepowered.api.world.Location;

import java.text.DecimalFormat;
import java.util.UUID;

/**
 * Created by Edwin on 08/10/2017.
 */
public class IworldsUtils {

    private final IworldsSponge plugin = IworldsSponge.instance;

    public static boolean isSolid(Location blockLoc) {
        if (blockLoc.getProperty(SolidCubeProperty.class).isPresent()) {
            SolidCubeProperty property = (SolidCubeProperty) blockLoc.getProperty(SolidCubeProperty.class).get();
            return property.getValue();
        }
        return false;
    }

    // Méthode pour convertir type player à uuid
    public static UUID PlayerToUUID(Player player) {
        UUID uuid;
        uuid = player.getUniqueId();
        return (uuid);
    }

    // Envoie un message au serveur
    public static void sm(String msg) {
        Sponge.getServer().getBroadcastChannel().send(Text.of("[iWorlds]: " + msg));
    }

    // Envoie un message à la console
    public static void cm(String msg) {
        Sponge.getServer().getConsole().sendMessage(Text.of("[iWorlds]: " + msg));
    }

    // Executer une commande sur le serveur
    public void cmds(String cmd) {
        Sponge.getCommandManager().process(Sponge.getServer().getConsole(), cmd);
    }

    // Titles
    public static Title title(String message) {
        Text text = Text.of(Text.builder(message).color(TextColors.AQUA).build());
        Title title = Title.builder().title(Text.of(text)).build();
        return title;
    }

    public static Title subtitle(String message) {
        Text text = Text.of(Text.builder(message).color(TextColors.GOLD).build());
        Title subtitle = Title.builder().subtitle(Text.of(message)).build();
        return subtitle;
    }

    public static Title titleSubtitle(String title, String subtitle) {
        Text Titre = Text.of(Text.builder(title).color(TextColors.GOLD).build());
        Text SousTitre = Text.of(Text.builder(subtitle).color(TextColors.AQUA).build());
        Title ready =(Title) Title.of(Titre, SousTitre);
        return ready;
    }

    // TPS
    private static final DecimalFormat tpsFormat = new DecimalFormat("#0.00");
    // TPS
    public static Text getTPS(double currentTps){
        TextColor colour;

        if (currentTps > 18) {
            colour = TextColors.GREEN;
        } else if (currentTps > 15) {
            colour = TextColors.YELLOW;
        } else {
            colour = TextColors.RED;
        }
        return Text.of(colour, tpsFormat.format(currentTps));
    }
}