/*
    This file is part of the HeavenMS MapleStory Server
*/
package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import server.maps.FieldLimit;
import server.maps.MapleMap;
import server.maps.MiniDungeonInfo;

/**
 * @author Custom - Warp to map by ID command
 */
public class MapCommand extends Command {
    {
        setDescription("Warp to a map by ID. Usage: @map <mapid>");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        if (params.length < 1) {
            player.yellowMessage("Usage: @map <mapid>");
            return;
        }

        try {
            int mapId = Integer.parseInt(params[0]);
            MapleMap target = c.getChannelServer().getMapFactory().getMap(mapId);

            if (target == null) {
                player.yellowMessage("Map ID " + mapId + " does not exist.");
                return;
            }

            if (!player.isAlive()) {
                player.dropMessage(1, "This command cannot be used when you're dead.");
                return;
            }

            if (!player.isGM()) {
                if (player.getEventInstance() != null || MiniDungeonInfo.isDungeonMap(player.getMapId()) || FieldLimit.CANNOTMIGRATE.check(player.getMap().getFieldLimit())) {
                    player.dropMessage(1, "This command cannot be used in this map.");
                    return;
                }
            }

            player.saveLocationOnWarp();
            player.changeMap(target, target.getRandomPlayerSpawnpoint());
            player.dropMessage("Warped to map: " + mapId);
        } catch (NumberFormatException e) {
            player.yellowMessage("Invalid map ID. Please enter a valid number.");
        }
    }
}
