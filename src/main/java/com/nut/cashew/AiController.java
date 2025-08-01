package com.nut.cashew;

import java.util.*;
import java.util.stream.Collectors;

import com.nut.cashew.room.Altar;
import org.javatuples.Pair;

import static com.nut.cashew.Const.MAX_ALTAR_SAFE_LEVEL;

// threadsafe
public class AiController {

	int g;
	private double distance(int x1, int y1, int x2, int y2) {
		return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
	}

	private final Player player;
	public AiController(Player player) {
		this.player = player;
		 g = Integer.parseInt(player.alliance.name.substring(1));
	}

	public String nextMove() {
		
		MapData map = player.map;
		Room curRoom = player.getCurrentRoom();

		// lobby
		if (curRoom.lobby != null) {
			player.message("AI: Lobby");
			return "";
		}

		// arena
		if (curRoom.arena != null) {
			player.message("AI: Arena Event");
			return doCombat();
		}

		// treasure check
		if (curRoom.treasure != null) {
			player.message("AI: Treasure");
			return "treasure";
		}

		// boss check
		if (curRoom.boss != null) {
			player.message("AI: Boss");
			return "attack boss";
		}

		List<Room> treasureRooms = map.treasures.stream()
				.map(t -> t.room).collect(Collectors.toList());
		if (!treasureRooms.isEmpty()) {
			Room bestRoom = nearestRoom(treasureRooms);
			double distance = distance(bestRoom.x, bestRoom.y, player.getX(), player.getY());
			if (distance <= 1 && curRoom.altar == null) {
				player.message("AI: Treasure (" + bestRoom.x + "," + bestRoom.y + ")");
				return moveTo(bestRoom);
			}
		}

		List<Room> bossRooms = map.bosses.stream()
				.map(b -> b.room).collect(Collectors.toList());
		if (!bossRooms.isEmpty()) {
			Room bestRoom = nearestRoom(bossRooms);
			double distance = distance(bestRoom.x, bestRoom.y, player.getX(), player.getY());
			if (distance < 10) {
				player.message("AI: Boss (" + bestRoom.x + "," + bestRoom.y + ")");
				return moveTo(bestRoom);
			}
		}

		Room[][] rooms = map.getRooms();

		// list all altars
		List<Altar> altars = new LinkedList<>();
		for (Room[] value : rooms) {
			for (Room room : value) {
				if (room != null && room.altar != null) {
					altars.add(room.altar);
				}
			}
		}
		// filter available altars
		altars.removeIf(a -> {
			if (!a.isOpen) return true;
			if (a.level == 1) return false;
			if (a.level <= MAX_ALTAR_SAFE_LEVEL) {
				return a.entryPower() > player.power;
			} else {
				return a.entryPower() >
						player.power / (1 + player.characteristic.careful);
			}
		});
		if (curRoom.altar != null) {
			altars.add(curRoom.altar);
		}

		// if no altar
		if (altars.isEmpty()) {
			player.message("AI: No Altar, respawn");
			return "respawn";
		}

		// find best room
		Room bestRoom = altars.stream()
				.map(a -> {
					boolean hasMe = a.room.getPlayers().stream().anyMatch(p -> p.name.equals(player.name));
					int roomSize = a.room.getPlayers().size() + (hasMe ? 0 : 1);
					double distance = Math.max(Math.sqrt(distance(a.room.x, a.room.y, player.getX(), player.getY())), 1);
					double gain = Math.max((double) a.powerGain() / Math.max(roomSize, 1), 1);
					double score = gain / distance;
					return new Pair<>(a.room, score);
				})
				.max(Comparator.comparingDouble(Pair::getValue1))
				.map(Pair::getValue0)
				.orElseThrow();


		player.message("AI: Altar" + bestRoom.altar.level + "(" + bestRoom.x + "," + bestRoom.y + ")");
		if (bestRoom.x == player.getX() && bestRoom.y == player.getY()) {
			player.message("AI: No need to move");
			String combat = doCombat();
			if (combat.isEmpty()) {
				return "sit";
			}
			return combat;
		}
		return moveTo(bestRoom);
	}

	public String doCombat() {
		Random random = new Random();
		Room curRoom = player.getCurrentRoom();
		if (curRoom.getPlayers().size() == 1) {
			return "";
		}
		if (curRoom.arena != null ||
				(curRoom.altar != null &&
				curRoom.altar.level > MAX_ALTAR_SAFE_LEVEL)) {
			if (random.nextDouble() <= player.characteristic.aggressive || curRoom.arena != null) {
				Player target = null;
				if (random.nextDouble() <= player.characteristic.crazy) {
					target = curRoom.getPlayers().stream()
							.filter(p -> p != player)
							.filter(p -> !p.alliance.name.equals(player.alliance.name))
							.collect(Collectors.groupingBy(p -> p.alliance.name))
							.entrySet().stream()
							.map(entry -> Map.entry(entry.getKey(),
									entry.getValue().stream()
											.mapToDouble(p -> p.power * p.crit).sum()))
							.max(Map.Entry.comparingByValue())
							.map(entry -> curRoom.getPlayers().stream()
									.filter(p -> p.alliance.name.equals(entry.getKey()))
									.max(Comparator.comparingDouble(p -> p.power * p.crit)))
							.flatMap(p -> p)
							.orElse(null);
				}
				if (target == null){
					target = curRoom.getPlayers().stream()
							.filter(p -> p != player)
							.filter(p -> !p.alliance.name.equals(player.alliance.name))
							.collect(Collectors.groupingBy(p -> p.alliance.name))
							.entrySet().stream()
							.map(entry -> Map.entry(entry.getKey(),
									entry.getValue().stream()
											.mapToDouble(p -> p.power * p.crit).sum()))
							.min(Map.Entry.comparingByValue())
							.map(entry -> curRoom.getPlayers().stream()
									.filter(p -> p.alliance.name.equals(entry.getKey()))
									.min(Comparator.comparingDouble(p -> p.power * p.crit)))
							.flatMap(p -> p)
							.orElse(null);
				}
				if (target != null) {
					player.message("AI: attack " + target.name);
					return "attack " + target.name;
				}
			}
		}
		return "";
	}

	public Room nearestRoom(List<Room> rooms) {
		if (rooms == null || rooms.isEmpty()) return null;
		rooms.sort((r1, r2) -> Double.compare(
				distance(r1.x, r1.y, player.getX(), player.getY()),
				distance(r2.x, r2.y, player.getX(), player.getY())
		));
		return rooms.get(0);
	}

	public String moveTo(Room room) {
		return moveTo(room.x, room.y);
	}

	public String moveTo(int x, int y) {
		if (x > player.getX()) {
			var r = player.tryMove(player.getX() + 1, player.getY(), false);
			if (!r.getValue0()) {
				return "j";
			}
			return "l";
		}
		if (x < player.getX()) {
			var r = player.tryMove(player.getX() - 1, player.getY(), false);
			if (!r.getValue0()) {
				return "k";
			}
			return "h";
		}
		if (y > player.getY()) {
			var r = player.tryMove(player.getX(), player.getY() + 1, false);
			if (!r.getValue0()) {
				return "l";
			}
			return "j";
		}
		if (y < player.getY()) {
			var r = player.tryMove(player.getX(), player.getY() - 1, false);
			if (!r.getValue0()) {
				return "h";
			}
			return "k";
		}
		return "";
	}
}
