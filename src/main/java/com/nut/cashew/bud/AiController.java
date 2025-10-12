package com.nut.cashew.bud;

import java.util.*;
import java.util.Random;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static com.nut.cashew.root.Utils.distance;

public class AiController {

	protected Player player;
	protected MapData map;

	private void init(Player player, MapData map) {
		this.player = player;
		this.map = map;
	}

	public void makeAction() {
		if (player.dead) {
			return;
		}
		Action action = new Action();
		Player target = null;
		List<Room> viewableRooms = player.viewableRooms()
				.stream()
				.filter(room -> room.getPlayers().stream().anyMatch(p -> !p.team.equals(player.team)))
				.collect(Collectors.toList());
		if (!viewableRooms.isEmpty()) {
			// find nearest room
			Room nearestRoom = nearestRoom(viewableRooms);
			target = nearestRoom.getPlayers().stream()
					.filter(p -> !p.name.equals(player.team))
					.findAny().orElse(null);
		}
		if (target != null) {
//			action.target = target.name;
			action.movement = moveTo(target.room);
			// direction?
		} else {
			// random move
			List<Character> movements = Arrays.asList('h', 'j', 'k', 'l');
			Random random = new Random();
			action.movement = movements.get(random.nextInt(movements.size()));
			// random direction
			List<Character> directions = Arrays.asList('n', 's', 'w', 'e');
			action.direction = directions.get(random.nextInt(directions.size()));
		}
		player.addAction(action);
	}

	public Room nearestRoom(List<Room> rooms) {
		if (rooms == null || rooms.isEmpty()) return null;
		rooms.sort((r1, r2) -> Double.compare(
				distance(r1.x, r1.y, player.getX(), player.getY()),
				distance(r2.x, r2.y, player.getX(), player.getY())
		));
		return rooms.get(0);
	}

	public static AiController create(Player player, MapData map) {
		AiController controller = new AiController();
		controller.init(player, map);
		return controller;
	}

	public char moveTo(Room room) {
		return moveTo(room.x, room.y);
	}

	public char moveTo(int x, int y) {
		if (x > player.getX()) {
			var r = player.tryMove(player.getX() + 1, player.getY());
			if (!r.getValue0()) {
				return 'j';
			}
			return 'l';
		}
		if (x < player.getX()) {
			var r = player.tryMove(player.getX() - 1, player.getY());
			if (!r.getValue0()) {
				return 'k';
			}
			return 'h';
		}
		if (y > player.getY()) {
			var r = player.tryMove(player.getX(), player.getY() + 1);
			if (!r.getValue0()) {
				return 'l';
			}
			return 'j';
		}
		if (y < player.getY()) {
			var r = player.tryMove(player.getX(), player.getY() - 1);
			if (!r.getValue0()) {
				return 'h';
			}
			return 'k';
		}
		return 's';
	}

}
