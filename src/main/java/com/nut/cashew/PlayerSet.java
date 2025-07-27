package com.nut.cashew;

import java.util.ArrayList;
import java.util.List;

public class PlayerSet {
	private final List<Player> players;
	private int currentPlayer = 1;

	public PlayerSet(MapData map) {
		this.players = new ArrayList<>();
		for (int i = 0; i < Const.TOTAL_PLAYER; i++) {
			players.add(new Player(map, "P"+i));
		}
	}

	public Player getCurPlayer() {
		return players.get(currentPlayer);
	}

	public void setCurPlayer(int index) {
		currentPlayer = index;
	}

	public void doAction() {
		players.forEach(player -> {
			if (player.notHasAction()) {
				player.addAction(player.aiController.nextMove());
			}
			player.doAction();
		});
	}
}
