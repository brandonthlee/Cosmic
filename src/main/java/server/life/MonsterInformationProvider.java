/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server.life;

import config.YamlConfig;
import constants.inventory.ItemConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import provider.Data;
import provider.DataProvider;
import provider.DataProviderFactory;
import provider.DataTool;
import provider.wz.WZFiles;
import server.ItemInformationProvider;
import tools.DatabaseConnection;
import tools.Pair;
import tools.Randomizer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MonsterInformationProvider {
    private static final Logger log = LoggerFactory.getLogger(MonsterInformationProvider.class);
    // Author : LightPepsi

    private static final MonsterInformationProvider instance = new MonsterInformationProvider();

    // list of maple items
    private static final List<Integer> MAPLE_ITEM_LIST = List.of(1302020, 1302030, 1302033, 1302035, 1302036, 1302058, 1302064, 1302066, 1302067, 1302080, 1312032, 1312033, 1322054, 1322055, 1332025, 1332055, 1332056, 1332057, 1372034, 1382009, 1382012, 1382039, 1382040, 1402039, 1402040, 1412011, 1412027, 1412028, 1422014, 1422029, 1422032, 1432012, 1432040, 1432041, 1432046, 1442024, 1442030, 1442051, 1442052, 1452016, 1452022, 1452045, 1452046, 1462014, 1462019, 1462040, 1462041, 1472030, 1472032, 1472055, 1472056, 1482020, 1482021, 1482022, 1492020, 1492021, 1492022, 1092030);

    // list of production simulators
    private static final List<Integer> PRODUCTION_SIMULATOR_LIST = List.of(4130000, 4130001, 4130002, 4130003, 4130004, 4130005, 4130006, 4130007, 4130008, 4130009, 4130010, 4130011, 4130012, 4130013, 4130014, 4130015, 4130016, 4130017);

    // list of dark scrolls
    private static final List<Integer> DARK_SCROLL_LIST = List.of(2040008, 2040009, 2040011, 2040012, 2040013, 2040014, 2040015, 2040103, 2040104, 2040108, 2040109, 2040203, 2040204, 2040208, 2040209, 2040304, 2040305, 2040306, 2040307, 2040308, 2040309, 2040404, 2040405, 2040406, 2040407, 2040408, 2040409, 2040410, 2040411, 2040508, 2040509, 2040510, 2040511, 2040518, 2040519, 2040520, 2040521, 2040604, 2040605, 2040606, 2040607, 2040608, 2040609, 2040610, 2040611, 2040712, 2040713, 2040714, 2040715, 2040716, 2040717, 2040808, 2040809, 2040810, 2040811, 2040812, 2040813, 2040814, 2040815, 2040904, 2040905, 2040906, 2040907, 2040908, 2040909, 2040916, 2040917, 2040921, 2040922, 2041026, 2041027, 2041028, 2041029, 2041030, 2041031, 2041032, 2041033, 2041034, 2041035, 2041036, 2041037, 2041038, 2041039, 2041040, 2041041, 2043004, 2043005, 2043006, 2043007, 2043104, 2043105, 2043204, 2043205, 2043304, 2043305, 2043704, 2043705, 2043804, 2043805, 2044004, 2044005, 2044104, 2044105, 2044204, 2044205, 2044304, 2044305, 2044404, 2044405, 2044504, 2044505, 2044604, 2044605, 2044704, 2044705);

    // list of anniversary scrolls
    private static final List<Integer> ANNIVERSARY_SCROLL_LIST = List.of(2040315, 2040912, 2041059, 2041060, 2041061, 2041062, 2043013, 2043108, 2043208, 2043308, 2043708, 2043808, 2044008, 2044108, 2044208, 2044308, 2044408, 2044508, 2044608, 2044708, 2044810, 2044905);

    public static MonsterInformationProvider getInstance() {
        return instance;
    }

    private final Map<Integer, List<MonsterDropEntry>> drops = new HashMap<>();
    private final List<MonsterGlobalDropEntry> globaldrops = new ArrayList<>();
    private final Map<Integer, List<MonsterGlobalDropEntry>> continentdrops = new HashMap<>();

    private final Map<Integer, List<Integer>> dropsChancePool = new HashMap<>();    // thanks to ronan
    private final Set<Integer> hasNoMultiEquipDrops = new HashSet<>();
    private final Map<Integer, List<MonsterDropEntry>> extraMultiEquipDrops = new HashMap<>();

    private final Map<Pair<Integer, Integer>, Integer> mobAttackAnimationTime = new HashMap<>();
    private final Map<MobSkill, Integer> mobSkillAnimationTime = new HashMap<>();

    private final Map<Integer, Pair<Integer, Integer>> mobAttackInfo = new HashMap<>();

    private final Map<Integer, Boolean> mobBossCache = new HashMap<>();
    private final Map<Integer, String> mobNameCache = new HashMap<>();

    protected MonsterInformationProvider() {
        retrieveGlobal();
    }

    public final List<MonsterGlobalDropEntry> getRelevantGlobalDrops(int mapid) {
        int continentid = mapid / 100000000;

        List<MonsterGlobalDropEntry> contiItems = continentdrops.get(continentid);
        if (contiItems == null) {   // continent separated global drops found thanks to marcuswoon
            contiItems = new ArrayList<>();

            for (MonsterGlobalDropEntry e : globaldrops) {
                if (e.continentid < 0 || e.continentid == continentid) {
                    contiItems.add(e);
                }
            }

            continentdrops.put(continentid, contiItems);
        }

        return contiItems;
    }

    private void retrieveGlobal() {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM drop_data_global WHERE chance > 0");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                globaldrops.add(new MonsterGlobalDropEntry(
                        rs.getInt("itemid"),
                        rs.getInt("chance"),
                        rs.getByte("continent"),
                        rs.getInt("minimum_quantity"),
                        rs.getInt("maximum_quantity"),
                        rs.getShort("questid")));
            }
        } catch (SQLException e) {
            log.error("Error retrieving global drops", e);
        }
    }

    public List<MonsterDropEntry> retrieveEffectiveDrop(final int monsterId) {
        // this reads the drop entries searching for multi-equip, properly processing them
        List<MonsterDropEntry> list = retrieveDrop(monsterId);
        if (hasNoMultiEquipDrops.contains(monsterId) || !YamlConfig.config.server.USE_MULTIPLE_SAME_EQUIP_DROP) {
            return list;
        }

        List<MonsterDropEntry> multiDrops = extraMultiEquipDrops.get(monsterId), extra = new LinkedList<>();
        if (multiDrops == null) {
            multiDrops = new LinkedList<>();

            for (MonsterDropEntry mde : list) {
                if (ItemConstants.isEquipment(mde.itemId) && mde.Maximum > 1) {
                    multiDrops.add(mde);

                    int rnd = Randomizer.rand(mde.Minimum, mde.Maximum);
                    for (int i = 0; i < rnd - 1; i++) {
                        extra.add(mde);   // this passes copies of the equips' MDE with min/max quantity > 1, but idc on equips they are unused anyways
                    }
                }
            }

            if (!multiDrops.isEmpty()) {
                extraMultiEquipDrops.put(monsterId, multiDrops);
            } else {
                hasNoMultiEquipDrops.add(monsterId);
            }
        } else {
            for (MonsterDropEntry mde : multiDrops) {
                int rnd = Randomizer.rand(mde.Minimum, mde.Maximum);
                for (int i = 0; i < rnd - 1; i++) {
                    extra.add(mde);
                }
            }
        }

        List<MonsterDropEntry> ret = new LinkedList<>(list);
        ret.addAll(extra);

        return ret;
    }

    public final List<MonsterDropEntry> retrieveDrop(final int monsterId) {
        if (drops.containsKey(monsterId)) {
            drops.clear();
        }
        final List<MonsterDropEntry> ret = new LinkedList<>();

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT itemid, chance, minimum_quantity, maximum_quantity, questid FROM drop_data WHERE dropperid = ?")) {
            ps.setInt(1, monsterId);

            // brandon: modify drops
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int itemId = rs.getInt("itemId");
                    if (!MAPLE_ITEM_LIST.contains(itemId) && !PRODUCTION_SIMULATOR_LIST.contains(itemId) && !DARK_SCROLL_LIST.contains(itemId) && !ANNIVERSARY_SCROLL_LIST.contains(itemId)) {
                        MonsterDropEntry mde = new MonsterDropEntry(rs.getInt("itemid"), rs.getInt("chance"), rs.getInt("minimum_quantity"), rs.getInt("maximum_quantity"), rs.getShort("questid"));
                        int mdeItemId = mde.itemId;
                        if (ItemConstants.isChaosScroll(mdeItemId) || ItemConstants.isCleanSlate(mdeItemId) || ItemConstants.isBullet(mdeItemId) || ItemConstants.isMagicPowder(mdeItemId) || ItemConstants.isMonsterCard(mdeItemId)) {
                            mde.chance = 0;
                        }
                        else if (ItemConstants.isAccessory(mdeItemId) || ItemConstants.isEquipment(mdeItemId) || ItemConstants.isWeapon(mdeItemId) || ItemConstants.isScroll(mdeItemId)) {
                            mde.chance = Math.round((float) mde.chance / 7);
                        }
                        else if (ItemConstants.isRechargeable(mdeItemId) || ItemConstants.isThrowingStar(mdeItemId) || ItemConstants.isArrow(mdeItemId)) {
                            mde.chance = Math.round((float) mde.chance / 10 * 4);
                        }
                        ret.add(mde);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return ret;
        }

        drops.put(monsterId, ret);
        return ret;
    }

    public final List<Integer> retrieveDropPool(final int monsterId) {  // ignores Quest and Party Quest items
        if (dropsChancePool.containsKey(monsterId)) {
            return dropsChancePool.get(monsterId);
        }

        ItemInformationProvider ii = ItemInformationProvider.getInstance();

        List<MonsterDropEntry> dropList = retrieveDrop(monsterId);
        List<Integer> ret = new ArrayList<>();

        int accProp = 0;
        for (MonsterDropEntry mde : dropList) {
            if (!ii.isQuestItem(mde.itemId) && !ii.isPartyQuestItem(mde.itemId)) {
                accProp += mde.chance;
            }

            ret.add(accProp);
        }

        if (accProp == 0) {
            ret.clear();    // don't accept mobs dropping no relevant items
        }
        dropsChancePool.put(monsterId, ret);
        return ret;
    }

    public final void setMobAttackAnimationTime(int monsterId, int attackPos, int animationTime) {
        mobAttackAnimationTime.put(new Pair<>(monsterId, attackPos), animationTime);
    }

    public final Integer getMobAttackAnimationTime(int monsterId, int attackPos) {
        Integer time = mobAttackAnimationTime.get(new Pair<>(monsterId, attackPos));
        return time == null ? 0 : time;
    }

    public final void setMobSkillAnimationTime(MobSkill skill, int animationTime) {
        mobSkillAnimationTime.put(skill, animationTime);
    }

    public final Integer getMobSkillAnimationTime(MobSkill skill) {
        Integer time = mobSkillAnimationTime.get(skill);
        return time == null ? 0 : time;
    }

    public final void setMobAttackInfo(int monsterId, int attackPos, int mpCon, int coolTime) {
        mobAttackInfo.put((monsterId << 3) + attackPos, new Pair<>(mpCon, coolTime));
    }

    public final Pair<Integer, Integer> getMobAttackInfo(int monsterId, int attackPos) {
        if (attackPos < 0 || attackPos > 7) {
            return null;
        }
        return mobAttackInfo.get((monsterId << 3) + attackPos);
    }

    public static ArrayList<Pair<Integer, String>> getMobsIDsFromName(String search) {
        DataProvider dataProvider = DataProviderFactory.getDataProvider(WZFiles.STRING);
        ArrayList<Pair<Integer, String>> retMobs = new ArrayList<>();
        Data data = dataProvider.getData("Mob.img");
        List<Pair<Integer, String>> mobPairList = new LinkedList<>();
        for (Data mobIdData : data.getChildren()) {
            int mobIdFromData = Integer.parseInt(mobIdData.getName());
            String mobNameFromData = DataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME");
            mobPairList.add(new Pair<>(mobIdFromData, mobNameFromData));
        }
        for (Pair<Integer, String> mobPair : mobPairList) {
            if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                retMobs.add(mobPair);
            }
        }
        return retMobs;
    }

    public boolean isBoss(int id) {
        Boolean boss = mobBossCache.get(id);
        if (boss == null) {
            try {
                boss = LifeFactory.getMonster(id).isBoss();
            } catch (NullPointerException npe) {
                boss = false;
            } catch (Exception e) {   //nonexistant mob
                boss = false;

                log.warn("Non-existent mob id {}", id, e);
            }

            mobBossCache.put(id, boss);
        }

        return boss;
    }

    public String getMobNameFromId(int id) {
        String mobName = mobNameCache.get(id);
        if (mobName == null) {
            DataProvider dataProvider = DataProviderFactory.getDataProvider(WZFiles.STRING);
            Data mobData = dataProvider.getData("Mob.img");

            mobName = DataTool.getString(mobData.getChildByPath(id + "/name"), "");
            mobNameCache.put(id, mobName);
        }

        return mobName;
    }

    public final void clearDrops() {
        drops.clear();
        hasNoMultiEquipDrops.clear();
        extraMultiEquipDrops.clear();
        dropsChancePool.clear();
        globaldrops.clear();
        continentdrops.clear();
        retrieveGlobal();
    }
}
