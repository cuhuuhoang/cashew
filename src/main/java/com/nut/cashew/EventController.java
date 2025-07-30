package com.nut.cashew;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.nut.cashew.Const.*;
import static com.nut.cashew.Const.MAX_ALTAR_LEVEL;
import static com.nut.cashew.Const.MAX_ALTAR_SAFE_LEVEL;
import static com.nut.cashew.ScreenRender.*;

public class EventController {
	private final MapData map;
	private final List<Player> players;
	private final ScreenRender screenRender;
	public final MessageBox infoBox = new MessageBox("Info", BOX_INFO_WIDTH, BOX_INFO_HEIGHT);

	public EventController(MapData map, List<Player> players, ScreenRender screenRender) {
		this.map = map;
		this.players = players;
		this.screenRender = screenRender;
	}

	public int turnCount;

	public void eventCheck() {

		turnCount++;

		if (turnCount > 0 && turnCount % TREASURE_SPAWN_TURN == 0) {
			map.placeTreasure();
		}

		if (turnCount > 0 && turnCount % ARENA_INTERVAL == 0) {
			map.getArena().isOpen = true;
			players.forEach(p -> {
				if (p.power >= MIN_ARENA_POWER) {
					p.moveToArena();
				}
			});
		}

		if (map.getArena().isOpen) {
			Set<String> alliances = new HashSet<>();
			List<Player> roomPlayers = List.copyOf(map.getArena().room.getPlayers());
			for (Player p : roomPlayers) {
				if (p.power < MIN_ARENA_POWER) {
					p.respawn();
				} else {
					alliances.add(p.alliance.name);
				}
			}
			if (alliances.size() == 1) {
				map.getArena().isOpen = false;
				players.forEach(p -> {
					if (alliances.contains(p.alliance.name)) {
						p.grow += 0.05;
					}
				});
				List<Player> remainPlayers = List.copyOf(map.getArena().room.getPlayers());
				for (Player p : remainPlayers) {
					p.grow += 0.05;
					p.respawn();
				}
				screenRender.globalBox.addTimeMessage("Arena Winner: " + alliances.iterator().next());
			} else if (alliances.isEmpty()) {
				map.getArena().isOpen = false;
				if (!map.getArena().room.getPlayers().isEmpty()) {
					throw new RuntimeException("Arena room is not empty");
				}
				screenRender.globalBox.addMessage("Arena No Alliance Win");
			}
		}

		int maxDiffAltarLevel = MAX_ALTAR_LEVEL - MAX_ALTAR_SAFE_LEVEL; // 4
		int sessionLength = 500;
		int skipTurn = 400;
		int altarInterval = (sessionLength - skipTurn) / (maxDiffAltarLevel + 1); // 200
		if (turnCount % ARENA_INTERVAL < ARENA_INTERVAL - ARENA_SAFE_INTERVAL) {
			for (int i = 1; i <= maxDiffAltarLevel; i++) {
				if (turnCount % sessionLength == skipTurn + i * altarInterval) {
					// time to open altar level, MAX_ALTAR_SAFE_LEVEL + 1 + i
					int finalI = i;
					map.getAltars().forEach(altar -> {
						if (altar.level == MAX_ALTAR_SAFE_LEVEL + finalI) {
							if (altar.isOpen) throw new RuntimeException("Altar is already open");
							altar.isOpen = true;
						}
					});
				}
			}
		}
		if (turnCount > 0 && turnCount % sessionLength == 0) {
			for (int i = MAX_ALTAR_SAFE_LEVEL + 1  ; i <= MAX_ALTAR_LEVEL ; i++) {
				int finalI = i;
				map.getAltars().forEach(altar -> {
					if (altar.level == finalI) {
						List<Player> players = List.copyOf(altar.room.getPlayers());
						altar.isOpen = false;
						players.forEach(Player::respawn);
					}
				});
			}
		}

		updateBox();
	}

	private void updateBox() {

		infoBox.clear();
		infoBox.addMessage("Turn: " + turnCount);
		if (map.getArena().isOpen) {
			infoBox.addMessage("In Arena Event");
		}
	}
}
