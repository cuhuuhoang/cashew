package com.nut.cashew.leaf;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventController {

	private final MapData map;
	private final ScreenRender screenRender;
	private final AiController aiController;
	public int turnCount;
	public boolean gameOver = false;
	public AtomicBoolean needReset = new AtomicBoolean(false);
	public AtomicBoolean autoReset = new AtomicBoolean(false);
	public Map<String, Integer> winners = new HashMap<>();

	public EventController(MapData map, ScreenRender screenRender) {
		this.map = map;
		this.screenRender = screenRender;
		this.aiController = new AiController();
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
		aiController.init(map, map.bots);
	}

	public void loop() {
		if (gameOver) {
			return;
		}
		turnCount++;

		aiController.makeAction();
		map.bots.forEach(Bot::doAction);

		// update screen
		screenRender.updateScreen(this);
	}
}
