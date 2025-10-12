package com.nut.cashew.bud;

import com.nut.cashew.bud.ml.QLAiController;

import java.util.concurrent.atomic.AtomicBoolean;

public class EventController {

	private final MapData map;
	private final ScreenRender screenRender;
	public int turnCount;
	public boolean gameOver = false;
	public AtomicBoolean needReset = new AtomicBoolean(false);
	public AtomicBoolean autoReset = new AtomicBoolean(false);

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
		map.players.forEach(player -> {
//			player.setAiController(AiController.create(player, map));
			try {
				player.setAiController(new QLAiController(player, map, "qtable.csv"));

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			player.setGlobalBox(screenRender.globalBox);
			player.respawn();
		});
	}

	public void loop() {
		if (gameOver) {
			return;
		}
		turnCount++;

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
			screenRender.globalBox.addMessage("🏆 Team " + winningTeam + " wins by holding the throne!");
		}


		// update screen
		screenRender.updateScreen(this);
	}

	private void doAction(Player player) {
		player.doAction();
	}
}
