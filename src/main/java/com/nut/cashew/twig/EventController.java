package com.nut.cashew.twig;

import com.nut.cashew.twig.action.Attack;
import com.nut.cashew.twig.room.Castle;
import com.nut.cashew.twig.room.Troop;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

public class EventController {

	private final MapData map;
	private final ScreenRender screenRender;
	public int turnCount;
	public boolean gameOver = false;
	public Castle winner = null;
	public AtomicBoolean needReset = new AtomicBoolean(false);
	public AtomicBoolean autoReset = new AtomicBoolean(false);
	
	public Map<String, Integer> aiWinners = new HashMap<>();

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
		winner = null;
		turnCount = 0;
		needReset.set(false);
		// other
		map.init();
		screenRender.reset();
		map.castles.forEach(castle -> castle.aiController.init(castle, map));
	}

	public void loop() {
		turnCount++;

		// make actions
		map.castles.forEach(castle -> {
			if (castle.getMaster() == castle && !castle.isMine) {
				// make action
				castle.aiController.makeAction();
			}
			List<Castle> castles = castle.getAllCastles();
			for (Castle castle1 : castles) {
				doAction(castle1);
			}
		});
		
		// check winner
		if (!gameOver) {
			if (turnCount > 5000 && autoReset.get()) {
				gameOver = true;
				aiWinners.compute("Draw", (k, v) -> v == null ? 1 : v + 1);
			} else {
				List<Castle> remain = map.castles.stream()
						.filter(castle -> castle.getMaster() == castle && !castle.isMine)
						.collect(Collectors.toList());
				if (remain.size() == 1) {
					winner = remain.get(0);
					aiWinners.compute(winner.aiController.name(), (k, v) -> v == null ? 1 : v + 1);
					gameOver = true;
				}
			}
		}

		// update screen
		screenRender.updateScreen(this);
	}

	private void doAction(Castle castle) {
		// defense
		for (Troop troop : castle.defend.troops) {
			troop.moveToDirection(map, castle);
		}
		// attack
		List<Attack> attacks = List.copyOf(castle.attacks);
		for (Attack attack : attacks) {
			for (Troop troop : attack.troops) {
				checkState(troop.getMaster() == castle.getMaster());
			}
			if (attack.troops.isEmpty()) {
				castle.cancelAttack(attack);
			} else if (attack.rallied) {
				boolean arrived = attack.troops.stream().noneMatch(troop -> troop.room != attack.target.room);
				if (arrived) {
					int defendCount = Math.toIntExact(attack.target.defend.troops.stream()
							.filter(troop -> troop.room == attack.target.room).count());
					castle.cancelAttack(attack);
					if (attack.troops.size() > defendCount) {
						if (!attack.target.isMine) {
							screenRender.globalBox.addMessage(turnCount + ": "
									+ castle.getMaster().render() + " " + castle.getMaster().name
									+ " " + castle.name + " takes " + attack.target.name
									+ " (" + attack.target.getAllTroops().size() + ")");
						}
						attack.target.changeMaster(castle);
						updateMaxRally();
					}
				} else {
					for (Troop troop : attack.troops) {
						troop.moveToDirection(map, attack.target);
					}
				}
			} else {
				boolean arrived = attack.troops.stream().noneMatch(troop -> troop.room != castle.room);
				if (arrived) {
					attack.rallied = true;
					for (Troop troop : attack.troops) {
						troop.moveToDirection(map, attack.target);
					}
				} else {
					for (Troop troop : attack.troops) {
						troop.moveToDirection(map, castle);
					}
				}
			}
		}
	}

	public void updateMaxRally() {
		map.castles.forEach(castle -> {
			if (castle.getMaster() == castle) {
				castle.computeTroopCount();
			}
		});
	}

}
