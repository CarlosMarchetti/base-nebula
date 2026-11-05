package dev.nebula.client.managers;

import dev.nebula.client.Main;
import dev.nebula.client.gui.settings.impl.ListSetting;
import dev.nebula.client.utils.PlayerUtils;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class ClanManager {
    private final ListSetting alliedClans;
    private final ListSetting enemyClans;
    private final ListSetting alliedPlayers;

    public ClanManager(dev.nebula.client.gui.settings.SettingsManager settings) {
        alliedClans = new ListSetting("Allied Clans", null, new ArrayList<>());
        enemyClans = new ListSetting("Enemy Clans", null, new ArrayList<>());
        alliedPlayers = new ListSetting("Allied Players", null, new ArrayList<>());

        settings.addSetting(alliedClans);
        settings.addSetting(enemyClans);
        settings.addSetting(alliedPlayers);
    }
    public void replaceAll(List<String> allies,List<String> enemies,List<String> players){
        alliedClans.setValue(allies);enemyClans.setValue(enemies);alliedPlayers.setValue(players);
    }


	public boolean isAlly(Object player) {
        if (player == null) return false;
        String playerName = PlayerUtils.getDisplayName(player);
        String clan = PlayerUtils.getClan(player);
        return ((ArrayList<String>) alliedPlayers.getValue()).contains(playerName) ||
               (clan != null && !clan.isEmpty() && ((ArrayList<String>) alliedClans.getValue()).contains(clan));
    }

	public boolean isEnemy(Object player) {
        if (player == null) return false;
        String clan = PlayerUtils.getClan(player);
        return clan != null && !clan.isEmpty() && ((ArrayList<String>) enemyClans.getValue()).contains(clan);
    }

    public ListSetting getAlliedClansSetting() {
        return alliedClans;
    }

    public ListSetting getEnemyClansSetting() {
        return enemyClans;
    }

    public ListSetting getAlliedPlayersSetting() {
        return alliedPlayers;
    }

	public List<String> getAlliedClans() {
        return (List<String>) alliedClans.getValue();
    }

    public List<String> getEnemyClans() {
        return (List<String>) enemyClans.getValue();
    }

    public List<String> getAlliedPlayers() {
        return (List<String>) alliedPlayers.getValue();
    }

    public void addAlliedClan(String clan) {
        alliedClans.addValue(clan);
    }

    public void removeAlliedClan(String clan) {
        alliedClans.removeValue(clan);
    }

    public void addEnemyClan(String clan) {
        enemyClans.addValue(clan);
    }

    public void removeEnemyClan(String clan) {
        enemyClans.removeValue(clan);
    }

    public void addAlliedPlayer(String player) {
        alliedPlayers.addValue(player);
    }

    public void removeAlliedPlayer(String player) {
        alliedPlayers.removeValue(player);
    }
}
