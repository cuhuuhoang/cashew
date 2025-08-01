package com.nut.cashew;

import com.nut.cashew.room.Altar;
import com.nut.cashew.room.Arena;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.nut.cashew.Const.*;
import static com.nut.cashew.Const.MAX_ALTAR_LEVEL;
import static com.nut.cashew.Const.MAX_ALTAR_SAFE_LEVEL;
import static com.nut.cashew.ScreenRender.*;
import static com.nut.cashew.room.Arena.SEED_PROMOTE_COUNT;

public class EventController {
	private final MapData map;
	private final List<Player> players;
	private final List<Alliance> alliances;
	private final List<Seed> seeds;
	private final ScreenRender screenRender;
	public final MessageBox globalBox;
	@Getter
	private Room povRoom;
	private final AtomicInteger autoSpeed;
	public boolean slowNextEvent;
	public boolean isGlobalArenaOpen = false;

	public EventController(MapData map, PlayerSet playerSet, ScreenRender screenRender, AtomicInteger autoSpeed) {
		this.map = map;
		this.players = playerSet.getPlayers();
		this.alliances = playerSet.getAlliances();
		this.seeds = playerSet.getSeeds();
		this.screenRender = screenRender;
		this.autoSpeed = autoSpeed;
		this.globalBox = screenRender.globalBox;
	}

	public int turnCount;

	private void rewardArena(Alliance winner, Seed seed) {
		// add to ranked
		seed.arena.rankedAlliances.push(winner);
		// final winner
		globalBox.addMessage("Arena " + seed.name + " Winner: " + winner.name);
		seed.arena.isOpen = false;
		players.forEach(p -> {
			if (winner == p.alliance) {
				p.checkReset();
			}
		});
		// MVP single player
		List<Player> mvpPlayers = players.stream()
				.filter(p -> p.alliance.seed == seed)
				.filter(p -> winner != p.alliance)
				.sorted((p1, p2) -> Double.compare(p2.power * p2.crit, p1.power * p1.crit))
				.limit(10)
				.collect(Collectors.toList());
		mvpPlayers.forEach(Player::checkReset);
		globalBox.addMessage("MVP " + seed.name + ": " + mvpPlayers.stream()
				.map(Player::getFullName).collect(Collectors.joining(", ")));
	}

	private void handleArena() {

		// check open arena
		if (turnCount > 0 && turnCount % ARENA_INTERVAL == 0) {
			players.forEach(Player::moveToLobby);
			isGlobalArenaOpen = true;
			for (Seed seed : seeds) {
				seed.arena.isOpen = true;
				List<Player> seedPlayers = players.stream()
						.filter(p -> p.alliance.seed == seed)
						.collect(Collectors.toList());
				seed.arena.registerPlayers(seedPlayers);
			}
		}

		if (!isGlobalArenaOpen) return;

		for (Seed seed : seeds) {
			if (!seed.arena.isOpen) continue;
			Arena arena = seed.arena;
			povRoom = arena.room;

			// fight time
			if (!arena.alliancesInBattle.isEmpty()) {
				Set<Alliance> alliancesInArena = arena.room.getPlayers().stream()
						.map(p -> p.alliance).collect(Collectors.toSet());
				Alliance winner;
				if (alliancesInArena.isEmpty()) {
					winner = arena.alliancesInBattle.get(0);
				} else if (alliancesInArena.size() == 1) {
					winner = alliancesInArena.iterator().next();
				} else {
					winner = null;
				}
				if (winner != null) {
					// loser
					Alliance loser = arena.alliancesInBattle.stream()
							.filter(alliance -> alliance != winner)
							.findFirst().orElseThrow();
					// check ranked alliances
					arena.rankedAlliances.push(loser);
					arena.alliancesInBattle.clear();
					// clear arena room
					List<Player> arenaPlayers = List.copyOf(arena.room.getPlayers());
					for (Player p : arenaPlayers) {
						p.power = arena.getPower(p.name);
						p.moveToLobby();
					}
					// reward winner
					if (arena.remainAlliances.isEmpty()) {
						rewardArena(winner, seed);
					} else {
						// round winner
						arena.remainAlliances.push(winner);
					}
				}
			}

			// add alliance to arena battle
			if (arena.isOpen && arena.alliancesInBattle.isEmpty()) {
				if (arena.remainAlliances.isEmpty()) {
					arena.isOpen = false;
					globalBox.addMessage("Arena " + seed.name + " is empty");
				} else if (arena.remainAlliances.size() == 1) {
					// single player
					Alliance winner = arena.remainAlliances.pop();
					rewardArena(winner, seed);
				} else {
					for (int i = 0; i < 2; i++) {
						arena.alliancesInBattle.add(arena.remainAlliances.pop());
					}
					for (Alliance alliance : arena.alliancesInBattle) {
						List<Player> lobbyPlayers = List.copyOf(seed.lobby.room.getPlayers());
						for (Player p : lobbyPlayers) {
							if (alliance == p.alliance) {
								p.moveToArena();
							}
						}
					}
				}
			}


			if (arena.isOpen) {
				break; // one seed at a time
			}
		}

		for (Seed seed : seeds) {
			if (seed.arena.isOpen) {
				return; // still in progress
			}
		}
		// no arena open
		povRoom = null;
		// do promote, change seeds
		final int promoteWinners = 2;
		List<List<Alliance>> allianceEachSeeds = new ArrayList<>();
		for (int i = 0; i < seeds.size(); i++) {
			allianceEachSeeds.add(new ArrayList<>());
		}
		// compute count of alliances each seed
		List<Integer> allianceCountEachSeed = new ArrayList<>();
		for (int i = 0; i < seeds.size() - 1; i++) {
			allianceCountEachSeed.add(alliances.size() / seeds.size());
		}
		allianceCountEachSeed.add(alliances.size() - allianceCountEachSeed.stream().mapToInt(Integer::intValue).sum());

		// promote winners
		for (int i = 0; i < seeds.size(); i++) {
			int dest = i == 0 ? 0 : i - 1;
			for (int j = 0; j < promoteWinners; j++) {
				if (!seeds.get(i).arena.rankedAlliances.isEmpty()) {
					allianceEachSeeds.get(dest).add(seeds.get(i).arena.rankedAlliances.pop());
				}
			}
		}

		// fill the rest
		for (int i = 0; i < seeds.size(); i++) {
			while (allianceEachSeeds.get(i).size() < allianceCountEachSeed.get(i)) {
				for (Seed seed : seeds) {
					if (!seed.arena.rankedAlliances.isEmpty()) {
						allianceEachSeeds.get(i).add(seed.arena.rankedAlliances.pop());
						break;
					}
				}
			}
		}

		// do the seed change
		for (int i = 0; i < allianceEachSeeds.size(); i++) {
			Seed seed = seeds.get(i);
			for (Alliance alliance : allianceEachSeeds.get(i)) {
				alliance.seed = seed;
			}
		}

		// do clean lobby
		seeds.forEach(seed -> {
			List<Player> lobbyPlayers = List.copyOf(seed.lobby.room.getPlayers());
			lobbyPlayers.forEach(Player::respawn);
		});

		povRoom = null;
		isGlobalArenaOpen = false;
	}

	public void eventCheck() {

		turnCount++;
		// check altar reward
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

		// treasure
		if (turnCount > 0 && turnCount % TREASURE_SPAWN_TURN == 0) {
			map.placeTreasure();
		}

		// slow pov
		if (turnCount > 0 && slowNextEvent && turnCount % ARENA_INTERVAL == ARENA_INTERVAL - 50) {
			autoSpeed.set(BASE_SPEED);
			slowNextEvent = false;
		}

		// arena
		handleArena();

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
			long maxBossPower = players.stream().mapToLong(p -> (long) (p.power * p.crit)).sum();
			if (turnCount > sessionLength && turnCount % sessionLength == bossSkipTurn) {
				Random r = new Random();
				double bossRate = 0.1 + r.nextDouble() * 0.9;
				long bossPower = (long) (bossRate * maxBossPower * 2d);
				double reward = 0.05 * bossRate;
				for (int i = 0; i < 10; i++) {
					map.placeBoss(bossPower, reward / 2 + r.nextDouble() * reward / 2);
				}

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
	}
}
