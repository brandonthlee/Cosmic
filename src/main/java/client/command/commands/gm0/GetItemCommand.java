/*
    This file is part of the HeavenMS MapleStory Server
*/
package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import client.inventory.manipulator.InventoryManipulator;
import config.YamlConfig;
import constants.inventory.ItemConstants;
import server.ItemInformationProvider;

/**
 * @author Custom - Get item by ID command
 */
public class GetItemCommand extends Command {
    {
        setDescription("Get an item by its ID. Usage: @getitem <itemid> [quantity]");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        if (params.length < 1) {
            player.yellowMessage("Usage: @getitem <itemid> [quantity]");
            return;
        }

        try {
            int itemId = Integer.parseInt(params[0]);
            ItemInformationProvider ii = ItemInformationProvider.getInstance();

            if (ii.getName(itemId) == null) {
                player.yellowMessage("Item id '" + params[0] + "' does not exist.");
                return;
            }

            short quantity = 1;
            if (params.length >= 2) {
                quantity = Short.parseShort(params[1]);
            }

            if (YamlConfig.config.server.BLOCK_GENERATE_CASH_ITEM && ii.isCash(itemId)) {
                player.yellowMessage("You cannot create a cash item with this command.");
                return;
            }

            // Check for pets
            if (ItemConstants.isPet(itemId)) {
                player.yellowMessage("Pets cannot be obtained with this command. Use @help for other commands.");
                return;
            }

            short flag = 0;
            if (player.gmLevel() < 3) {
                flag |= ItemConstants.ACCOUNT_SHARING;
                flag |= ItemConstants.UNTRADEABLE;
            }

            InventoryManipulator.addById(c, itemId, quantity, "", -1, flag, -1);
            player.dropMessage("Item obtained: " + ii.getName(itemId) + " x" + quantity);
        } catch (NumberFormatException e) {
            player.yellowMessage("Invalid item ID. Please enter a valid number.");
        }
    }
}
