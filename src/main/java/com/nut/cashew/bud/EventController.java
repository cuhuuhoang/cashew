package com.nut.cashew.bud;


import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventController {

	private final MapData map;
	private final ScreenRender screenRender;
	public int turnCount;
	public boolean gameOver = false;
	public AtomicBoolean needReset = new AtomicBoolean(false);
	public AtomicBoolean autoReset = new AtomicBoolean(false);
	public Map<String, Integer> winners = new java.util.HashMap<>();

	public EventController(MapData map, ScreenRender screenRender) {
		this.map = map;
		this.screenRender = screenRender;
	}

	public void checkReset() {
		if (needReset.get() || (autoReset.get() && gameOver)) {
			init();
		}
	}

	public void init() {
		// self
		gameOver = false;
		turnCount = 0;
		needReset.set(false);
		// other
		map.init();
		screenRender.reset();
		try {
			GameTrainer.RlAgent agent = GameTrainer.RlAgent.loadCsv(new File("qtable.csv"));
			map.players.forEach(player -> {
				player.setAiController(new GameTrainer.QLAiController(player, map, agent));
				player.setGlobalBox(screenRender.globalBox);
				player.respawn();
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void loop() {
		if (gameOver) {
			return;
		}
		turnCount++;

		if (turnCount > 5 && turnCount % 5 == 0) {
			map.placeLava();
		}

		List<Room> lavaRooms = new LinkedList<>(map.getLavaRooms());
		lavaRooms.forEach(room -> {
			List<Player> players = new LinkedList<>(room.getPlayers());
			players.forEach(player -> {
				screenRender.globalBox.addMessage(player.name + " is hit by lava!");
				player.damageSelf(10);
			});
		});


		// make actions
		map.players.forEach(player -> {
			player.aiController.makeAction();
			doAction(player);
		});

		// update throne control each turn
		map.updateThroneControl();

		// check for victory
		String winningTeam = map.getWinner();
		if (winningTeam != null) {
			gameOver = true;
			if ("tie".equals(winningTeam)) {
				screenRender.globalBox.addMessage("🎉 It's a tie!");
			} else {
				screenRender.globalBox.addMessage("🏆 Team " + winningTeam + " wins by holding the throne!");
				winners.compute(winningTeam, (k, v) -> v == null ? 1 : v + 1);
			}
		}


		// update screen
		screenRender.updateScreen(this);
	}

	private void doAction(Player player) {
		player.doAction();
	}
}
