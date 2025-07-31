package com.nut.cashew;

import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

import static com.nut.cashew.Const.*;

public class PlayerSet {

	@Getter
	private final List<Player> players;

	@Getter
	public final List<Alliance> alliances;

	public PlayerSet(MapData map, ScreenRender screenRender) {
		List<Alliance> tmpAlliances = new ArrayList<>();
		for (int i = 0; i < TOTAL_ALLIANCE; i++) {
			tmpAlliances.add(new Alliance("A" + i));
		}
		alliances = List.copyOf(tmpAlliances);
		int playersPerAlliance = TOTAL_PLAYER / TOTAL_ALLIANCE;
		Map<Alliance, Integer> allianceCount = new HashMap<>();
		alliances.forEach(a -> allianceCount.put(a, 0));
		Random random = new Random();
		List<Player> tmpPlayers = new ArrayList<>();
		for (int i = 0; i < Const.TOTAL_PLAYER; i++) {
			Characteristic characteristic =
					new Characteristic(random.nextDouble(), random.nextDouble(), random.nextDouble());
			List<Alliance> availableAlliances = alliances.stream()
					.filter(a -> allianceCount.get(a) < playersPerAlliance)
					.collect(Collectors.toList());
			Alliance alliance = availableAlliances.get(random.nextInt(availableAlliances.size()));
			allianceCount.put(alliance, allianceCount.get(alliance) + 1);
			String name = String.format("P%0" + String.valueOf(Const.TOTAL_PLAYER - 1).length() + "d", i);
			Player player = new Player(map, name, screenRender.globalBox, characteristic, alliance);
			player.respawn();
			tmpPlayers.add(player);
		}
		players = List.copyOf(tmpPlayers);
	}

	public void doAction() {
		players.forEach(player -> {
			String action = player.getAction();
			if (action == null || action.trim().isEmpty()) {
				action = player.aiController.nextMove();
			}
			player.addAction(action);
			//
			action = player.getAction();
			player.doAction(action);
		});
	}
}
