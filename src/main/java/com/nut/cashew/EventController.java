package com.nut.cashew;

import com.nut.cashew.room.Altar;
import lombok.Getter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.nut.cashew.Const.*;
import static com.nut.cashew.Const.MAX_ALTAR_LEVEL;
import static com.nut.cashew.Const.MAX_ALTAR_SAFE_LEVEL;
import static com.nut.cashew.ScreenRender.*;

public class EventController {
	private final MapData map;
	private final List<Player> players;
	private final ScreenRender screenRender;
	public final MessageBox infoBox = new MessageBox("Info", BOX_INFO_WIDTH, BOX_INFO_HEIGHT);

	@Getter
	private Room povRoom;
	private final AtomicInteger autoSpeed;
	public boolean slowNextEvent;

	public EventController(MapData map, List<Player> players, ScreenRender screenRender, AtomicInteger autoSpeed) {
		this.map = map;
		this.players = players;
		this.screenRender = screenRender;
		this.autoSpeed = autoSpeed;
	}

	public int turnCount;

	public void eventCheck() {

		turnCount++;
		for (Altar altar : map.altars) {
			int sharePlayers = 0;
			for (Player player : altar.room.getPlayers()) {
				if (altar.players.contains(player.name)) {
					sharePlayers++;
				}
			}
			for (Player player : altar.room.getPlayers()) {
				if (altar.players.contains(player.name)) {
					int inc = (int) ((double) altar.powerGain() / Math.max(sharePlayers, 1));
					inc = Math.max(inc, 1);
					player.power += (int) (inc * player.grow);
					if (player.power > (double) player.maxPower * 2d) {
						player.power = BASE_POWER;
						screenRender.globalBox.addMessage(player.getFullName() + " Power invalid");
						player.respawn();
					} else if (player.power > player.maxPower) {
						player.power = player.maxPower;
						player.reachedMax = true;
					}
					player.message("Sit at altar, power increase " + inc);
				}
			}
		}
		if (turnCount > 0 && turnCount % TREASURE_SPAWN_TURN == 0) {
			map.placeTreasure();
		}

		if (turnCount > 0 && slowNextEvent && turnCount % ARENA_INTERVAL == ARENA_INTERVAL - 50) {
			autoSpeed.set(BASE_SPEED);
			slowNextEvent = false;
		}

		if (turnCount > 0 && turnCount % ARENA_INTERVAL == 0) {
			povRoom = map.arena.room;
			map.arena.isOpen = true;
			players.forEach(p -> {
				if (p.power >= MIN_ARENA_POWER) {
					p.moveToArena();
				}
			});
		}

		if (map.arena.isOpen) {
			Set<String> alliances = new HashSet<>();
			List<Player> roomPlayers = List.copyOf(map.arena.room.getPlayers());
			for (Player p : roomPlayers) {
				if (p.power < MIN_ARENA_POWER) {
					p.respawn();
				} else {
					alliances.add(p.alliance.name);
				}
			}
			if (alliances.size() == 1) {
				screenRender.globalBox.addTimeMessage("Arena Winner: " + alliances.iterator().next());
				map.arena.isOpen = false;
				players.forEach(p -> {
					if (alliances.contains(p.alliance.name)) {
						p.grow += 0.1;
						if (p.reachedMax) {
							p.reachedMax = false;
							p.maxPower = (int) (p.maxPower * RESET_POWER_LIMIT);
							p.power = BASE_POWER;
							p.message("Power reset");
						}
					}
				});
				List<Player> remainPlayers = List.copyOf(map.arena.room.getPlayers());
				for (Player p : remainPlayers) {
					p.grow += 0.1;
					p.respawn();
				}
			} else if (alliances.isEmpty()) {
				map.arena.isOpen = false;
				if (!map.arena.room.getPlayers().isEmpty()) {
					throw new RuntimeException("Arena room is not empty");
				}
				screenRender.globalBox.addMessage("Arena No Alliance Win");
			}
		}

		int maxDiffAltarLevel = MAX_ALTAR_LEVEL - MAX_ALTAR_SAFE_LEVEL; // 4
		int sessionLength = 500;
		int altarSkipTurn = 400;
		int bossSkipTurn = 100;
		int altarInterval = (sessionLength - altarSkipTurn) / (maxDiffAltarLevel + 1); // 200
		if (turnCount % ARENA_INTERVAL < ARENA_INTERVAL - ARENA_SAFE_INTERVAL) {
			for (int i = 1; i <= maxDiffAltarLevel; i++) {
				if (turnCount % sessionLength == altarSkipTurn + i * altarInterval) {
					// time to open altar level, MAX_ALTAR_SAFE_LEVEL + 1 + i
					int finalI = i;
					map.altars.forEach(altar -> {
						if (altar.level == MAX_ALTAR_SAFE_LEVEL + finalI) {
							if (altar.isOpen) throw new RuntimeException("Altar is already open");
							altar.isOpen = true;
							povRoom = altar.room;
						}
					});
				}
			}
			// spawn boss
			if (turnCount > sessionLength && turnCount % sessionLength == bossSkipTurn) {
				long bossPower = players.stream().mapToLong(p -> (long) (p.power * p.crit)).sum();
				double reward = 0.2;
				map.placeBoss(bossPower, reward);
			}
		}
		if (turnCount > 0 && turnCount % sessionLength == 0) {
			for (int i = MAX_ALTAR_SAFE_LEVEL + 1  ; i <= MAX_ALTAR_LEVEL ; i++) {
				int finalI = i;
				map.altars.forEach(altar -> {
					if (altar.level == finalI) {
						List<Player> players = List.copyOf(altar.room.getPlayers());
						altar.isOpen = false;
						povRoom = null;
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
		if (map.arena.isOpen) {
			infoBox.addMessage("In Arena Event");
		}
	}
}
