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

	@Getter
	public final List<Seed> seeds;

	public PlayerSet(MapData map, ScreenRender screenRender) {
		List<Alliance> tmpAlliances = new ArrayList<>();
		for (int i = 0; i < TOTAL_ALLIANCE; i++) {
			String name = String.format("A%0" + String.valueOf(TOTAL_ALLIANCE - 1).length() + "d", i);
			tmpAlliances.add(new Alliance(name));
		}
		alliances = List.copyOf(tmpAlliances);
		int playersPerAlliance = TOTAL_PLAYER / TOTAL_ALLIANCE;
		Map<Alliance, Integer> allianceCount = new HashMap<>();
		alliances.forEach(a -> allianceCount.put(a, 0));
		Random r = new Random();
		List<Player> tmpPlayers = new ArrayList<>();
		for (int i = 0; i < Const.TOTAL_PLAYER; i++) {
			Characteristic characteristic =
					new Characteristic(r.nextDouble(), r.nextDouble(), r.nextDouble(), r.nextDouble(), r.nextDouble());
			List<Alliance> availableAlliances = alliances.stream()
					.filter(a -> allianceCount.get(a) < playersPerAlliance)
					.collect(Collectors.toList());
			Alliance alliance = availableAlliances.get(r.nextInt(availableAlliances.size()));
			allianceCount.put(alliance, allianceCount.get(alliance) + 1);
			String name = String.format("P%0" + String.valueOf(Const.TOTAL_PLAYER - 1).length() + "d", i);
			Player player = new Player(map, name, screenRender.globalBox, characteristic, alliance);
			alliance.players.add(player);
			player.respawn();
			tmpPlayers.add(player);
		}
		alliances.forEach(a -> {
			Player leader = a.players.stream()
					.max(Comparator.comparingDouble(p -> p.characteristic.leadership)).orElseThrow();
			a.promoteLeader(leader);
		});
		players = List.copyOf(tmpPlayers);
		List<Seed> tmpSeeds = new ArrayList<>();
		for (int i = 0; i < TOTAL_SEED; i++) {
			tmpSeeds.add(new Seed(i, "S" + (i + 1), map.arenas.get(i), map.lobbies.get(i)));
		}
		seeds = List.copyOf(tmpSeeds);
		alliances.forEach(a -> a.seed = seeds.get(0));
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
