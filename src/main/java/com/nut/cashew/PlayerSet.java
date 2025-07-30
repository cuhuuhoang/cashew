package com.nut.cashew;

import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

import static com.nut.cashew.Const.*;

public class PlayerSet {
	@Getter
	private final List<Player> players;
	private int currentPlayer = 1;

	private final ScreenRender screenRender;

	@Getter
	public final List<Alliance> alliances;

	public PlayerSet(MapData map, ScreenRender screenRender) {
		this.screenRender = screenRender;
		List<Alliance> tmpAlliances = new ArrayList<>();
		for (int i = 0; i < TOTAL_ALLIANCE; i++) {
			tmpAlliances.add(new Alliance("A" + i));
		}
		alliances = List.copyOf(tmpAlliances);
		List<Player> tmpPlayers = new ArrayList<>();
		for (int i = 0; i < Const.TOTAL_PLAYER; i++) {
			Random random = new Random();
			Characteristic characteristic =
					new Characteristic(random.nextDouble(), random.nextDouble(), random.nextDouble());
			Alliance alliance = alliances.get(random.nextInt(TOTAL_ALLIANCE));
			String name = String.format("P%0" + String.valueOf(Const.TOTAL_PLAYER - 1).length() + "d", i);
			Player player = new Player(map, name, screenRender.globalBox, characteristic, alliance);
			player.respawn();
			tmpPlayers.add(player);
		}
		players = List.copyOf(tmpPlayers);
	}

	public Player getCurPlayer() {
		return players.get(currentPlayer);
	}

	public void setCurPlayer(int index) {
		if (index < 0 || index > players.size()) {
			return;
		}
		currentPlayer = index;
	}

	public void startTurn() {
		players.forEach(Player::startTurn);
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
