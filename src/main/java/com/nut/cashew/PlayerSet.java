package com.nut.cashew;

import java.util.*;
import java.util.stream.Collectors;

import static com.nut.cashew.Const.*;

public class PlayerSet {
	private final List<Player> players;
	private int currentPlayer = 1;

	public final MessageBox rankBox = new MessageBox("Rank", COL_3_WIDTH, BOX_RANK_HEIGHT);
	public final MessageBox allianceBox = new MessageBox("Alliance", COL_3_WIDTH, BOX_ALLIANCE_RANK_HEIGHT);
	public final MessageBox globalBox = new MessageBox("Global", COL_3_WIDTH, BOX_GLOBAL_HEIGHT);
	public final MessageBox infoBox = new MessageBox("Info", COL_2_WIDTH, BOX_INFO_HEIGHT);

	public int turnCount;
	public final List<Alliance> alliances;
	private final MapData map;

	public PlayerSet(MapData map) {
		this.map = map;
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
			Player player = new Player(map, "P"+i, globalBox, characteristic, alliance);
			player.respawn();
			tmpPlayers.add(player);
		}
		players = List.copyOf(tmpPlayers);
	}
	
	public void setRank() {
		rankBox.clear();
		var sortedPlayers = players.stream()
				.sorted((p1, p2) -> Integer.compare(p2.power, p1.power))
				.collect(Collectors.toList());
		for (int i = 0; i < Math.min(BOX_RANK_HEIGHT, sortedPlayers.size()); i++) {
			var player = sortedPlayers.get(i);
			rankBox.addMessage(String.format("[%s]%s: %d (%.2f, %.2f)", player.alliance.name, player.name, player.power, player.crit, player.grow));
		}
		
		allianceBox.clear();
		var alliancePower = alliances.stream()
				.map(alliance -> Map.entry(alliance,
						players.stream()
								.filter(p -> p.alliance.equals(alliance))
								.mapToInt(p -> p.power)
								.sum()))
				.sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
				.collect(Collectors.toList());

		alliancePower.stream()
				.limit(BOX_ALLIANCE_RANK_HEIGHT)
				.forEach(entry -> allianceBox.addMessage(String.format("%s: %d", entry.getKey().name, entry.getValue())));

		infoBox.clear();
		infoBox.addMessage("Turn: " + turnCount);
		if (map.getArena().isOpen) {
			infoBox.addMessage("In Arena Event");
		}

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
		turnCount++;

		if (turnCount > 0 && turnCount % TREASURE_SPAWN_TURN == 0) {
			map.placeTreasure();
		}

		if (turnCount > 0 && turnCount % 5000 == 0) {
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
				globalBox.addTimeMessage("Arena Winner: " + alliances.iterator().next());
			} else if (alliances.isEmpty()) {
				map.getArena().isOpen = false;
				if (!map.getArena().room.getPlayers().isEmpty()) {
					throw new RuntimeException("Arena room is not empty");
				}
				globalBox.addMessage("Arena No Alliance Win");
			}
		}

		int maxDiffAltarLevel = MAX_ALTAR_LEVEL - MAX_ALTAR_SAFE_LEVEL; // 4
		int sessionLength = 500;
		int skipTurn = 400;
		int altarInterval = (sessionLength - skipTurn) / (maxDiffAltarLevel + 1); // 200
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
		if (turnCount > 0 && turnCount % sessionLength == 0) {
			for (int i = MAX_ALTAR_SAFE_LEVEL + 1  ; i <= MAX_ALTAR_LEVEL ; i++) {
				int finalI = i;
				map.getAltars().forEach(altar -> {
					if (altar.level == finalI) {
						if (!altar.isOpen) throw new RuntimeException("Altar is not open");
						List<Player> players = List.copyOf(altar.room.getPlayers());
						altar.isOpen = false;
						players.forEach(Player::respawn);
					}
				});
			}
		}


		players.forEach(Player::startTurn);
	}

	public void addAction() {
//		Batch batch = new Batch(8);
//		players.forEach(player -> batch.add(() -> {
//
//		}));
//		batceh.execute();
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
