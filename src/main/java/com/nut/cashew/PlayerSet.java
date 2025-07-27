package com.nut.cashew;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.nut.cashew.Const.*;

public class PlayerSet {
	private final List<Player> players;
	private int currentPlayer = 1;

	public final MessageBox rankBox = new MessageBox("Rank", COL_3_WIDTH, BOX_RANK_HEIGHT);
	public final MessageBox globalBox = new MessageBox("Global", COL_3_WIDTH, BOX_GLOBAL_HEIGHT);

	public PlayerSet(MapData map) {
		List<Player> tmpPlayers = new ArrayList<>();
		for (int i = 0; i < Const.TOTAL_PLAYER; i++) {
			Random random = new Random();
			Characteristic characteristic = new Characteristic(random.nextDouble(), random.nextDouble());
			Player player = new Player(map, "P"+i, globalBox, characteristic);
			player.respawn();
			tmpPlayers.add(player);
		}
		players = List.copyOf(tmpPlayers);
	}
	
	public void setRank() {
		rankBox.clear();
		var pairs = players.stream()
				.map(p -> new org.javatuples.Pair<>(p.power, p.name))
				.sorted((p1, p2) -> Integer.compare(p2.getValue0(), p1.getValue0()))
				.collect(Collectors.toList());
		for (int i = 0; i < Math.min(BOX_RANK_HEIGHT, pairs.size()); i++) {
			var pair = pairs.get(i);
			rankBox.addMessage(String.format("%s: %d", pair.getValue1(), pair.getValue0()));
		}
	}

	public Player getCurPlayer() {
		return players.get(currentPlayer);
	}

	public void setCurPlayer(int index) {
		currentPlayer = index;
	}

	public void startTurn() {
		players.forEach(Player::startTurn);
	}

	public void doAction() {
		players.forEach(player -> {
			String action = player.getAction();
			if (action == null || action.trim().isEmpty() || "a".equals(action)) {
				action = player.aiController.nextMove();
			}
			player.doAction(action);
		});
	}
}
