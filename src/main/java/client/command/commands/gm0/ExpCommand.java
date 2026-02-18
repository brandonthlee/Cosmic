/*
    This file is part of the HeavenMS MapleStory Server
*/
package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;

/**
 * @author Custom - Personal exp multiplier command
 */
public class ExpCommand extends Command {
    {
        setDescription("Set your personal exp multiplier. Usage: @exp <multiplier> (1 = normal, 2 = 2x, etc)");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        if (params.length < 1) {
            player.yellowMessage("Usage: @exp <multiplier>");
            player.yellowMessage("Current personal exp multiplier: " + player.getExpRate() + "x");
            return;
        }

        try {
            int multiplier = Integer.parseInt(params[0]);
            
            if (multiplier < 1) {
                player.yellowMessage("Multiplier must be at least 1.");
                return;
            }
            
            if (multiplier > 10000) {
                player.yellowMessage("Multiplier cannot exceed 10000.");
                return;
            }

            c.getWorldServer().setExpRate(multiplier);
            player.dropMessage("Personal exp multiplier set to: " + multiplier + "x");
        } catch (NumberFormatException e) {
            player.yellowMessage("Invalid multiplier. Please enter a valid number.");
        }
    }
}
