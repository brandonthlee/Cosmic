/*
    This file is part of the HeavenMS MapleStory Server, commands OdinMS-based
    Copyleft (L) 2016 - 2019 RonanLana

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/*
   @Author: Arthur L - Refactored command content into modules
*/
package client.command.commands.gm2;

import client.Character;
import client.Client;
import client.command.Command;
import config.YamlConfig;

public class SpCommand extends Command {
    {
        setDescription("Set available SP.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        
        // Check if player is GM and has parameters with player name
        if (c.getPlayer().isGM() && params.length >= 2) {
            Character victim = c.getWorldServer().getPlayerStorage().getCharacterByName(params[0]);
            if (victim != null) {
                int newSp = Integer.parseInt(params[1]);
                if (newSp < 0) {
                    newSp = 0;
                } else if (newSp > YamlConfig.config.server.MAX_AP) {
                    newSp = YamlConfig.config.server.MAX_AP;
                }

                victim.updateRemainingSp(newSp);
                player.dropMessage(5, "SP given to " + victim.getName());
            } else {
                player.message("Player '" + params[0] + "' could not be found.");
            }
            return;
        }

        // Regular player usage: @sp <amount>
        if (params.length < 1) {
            player.yellowMessage("Usage: @sp <amount> (to set your own SP)");
            player.yellowMessage("Usage: !sp <playername> <amount> (for GMs to set other's SP)");
            return;
        }

        int newSp = Integer.parseInt(params[0]);
        if (newSp < 0) {
            newSp = 0;
        } else if (newSp > YamlConfig.config.server.MAX_AP) {
            newSp = YamlConfig.config.server.MAX_AP;
        }

        player.updateRemainingSp(newSp);
        player.dropMessage("SP set to: " + newSp);
    }
}
