package com.nut.cashew;

import java.util.*;

public class AiController {
	private double distance(int x1, int y1, int x2, int y2) {
		return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
	}

	private final Player player;

	public AiController(Player player) {
		this.player = player;
	}

	public String nextMove() {
		ViewMap viewMap = player.getViewMap();
		Room[][] rooms = viewMap.getRooms();
		// list all altars
		List<Altar> altars = new LinkedList<>();
		for (Room[] value : rooms) {
			for (Room room : value) {
				if (room != null && room.getAltar() != null) {
					altars.add(room.getAltar());
				}
			}
		}
		// filter available altars
		altars.removeIf(a -> a.entryPower() > player.power);
		int bestLevel = altars.stream().mapToInt(Altar::getLevel).max().orElse(0);
		altars.removeIf(a -> a.level < bestLevel);
		// if no altar
		if (altars.isEmpty()) {
			player.message("AI: No Altar, move 0,0");
			return moveTo(0, 0);
		}
		// find nearest altar
		List<Room> destinationRooms = new LinkedList<>();
		for (Altar altar : altars) {
			destinationRooms.add(altar.room);
		}
		Room nearestRoom = nearestRoom(destinationRooms);
		if (nearestRoom != null) {
			player.message("AI: Altar" + nearestRoom.getAltar().level);
			if (nearestRoom.x == player.getX() && nearestRoom.y == player.getY()) {
				player.message("AI: No need to move");
				Random random = new Random();
				if (player.getCurrentRoom().getPlayers().size() > 1) {
					if (random.nextDouble() <= player.characteristic.aggressive) {
						Player target;
						if (random.nextDouble() <= player.characteristic.crazy) {
							target = player.getCurrentRoom().getPlayers()
									.stream()
									.filter(p -> p != player)
									.filter(p -> p.power > player.power)
									.findFirst().orElse(null);
						} else {
							target = player.getCurrentRoom().getPlayers()
									.stream()
									.filter(p -> p != player)
									.filter(p -> p.power <= player.power)
									.findFirst().orElse(null);
						}
						if (target != null) {
							player.message("AI: attack " + target.name);
							return "attack " + target.name;
						}
					}
				}
			}
			return moveTo(nearestRoom);
		}
		player.message("AI: No idea, move 0,0");
		return moveTo(0, 0);
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
			var r = player.tryMove(player.getX() + 1, player.getY());
			if (!r.getValue0()) {
				return "j";
			}
			return "l";
		}
		if (x < player.getX()) {
			var r = player.tryMove(player.getX() - 1, player.getY());
			if (!r.getValue0()) {
				return "k";
			}
			return "h";
		}
		if (y > player.getY()) {
			var r = player.tryMove(player.getX(), player.getY() + 1);
			if (!r.getValue0()) {
				return "l";
			}
			return "j";
		}
		if (y < player.getY()) {
			var r = player.tryMove(player.getX(), player.getY() - 1);
			if (!r.getValue0()) {
				return "h";
			}
			return "k";
		}
		return "";
	}
}
