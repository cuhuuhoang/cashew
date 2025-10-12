package com.nut.cashew.bud;


import com.nut.cashew.root.MessageBox;
import com.nut.cashew.root.Utils;
import lombok.Getter;
import lombok.Setter;
import org.javatuples.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static com.nut.cashew.root.Utils.generateRandomAnsiColor;

public class Player {
	@Getter
	public int x;
	@Getter
	public int y;
	public static final double MAX_HEALTH = 20;
	public static final int MAX_SIGHT = 10;

	public double health = MAX_HEALTH;
	public int sight = MAX_SIGHT;
	public boolean dead = false;
	public char direction = 'n';

	public final String name;
	public final String team;
	public final MapData map;
	public final String colorCode;
	@Setter
	public AiController aiController;
	public final Queue<Action> nextAction;
	@Setter
	public  MessageBox globalBox;
	@Getter
	public Room room;

	public Player(MapData map, String name, String team, String colorCode) {
		this.name = name;
		this.team = team;
		this.map = map;
		this.nextAction = new ArrayDeque<>();
		this.room = null;
		this.colorCode = colorCode;
		//
	}

	public void respawn() {
		nextAction.clear();
		if (this.room != null) {
			this.room.removePlayer(this);
		}
		this.room = map.respawnRooms.get(team);
		this.room.addPlayer(this);
		this.x = this.room.x;
		this.y = this.room.y;
//		List<Pair<Integer, Integer>> positions = new ArrayList<>();
//		for (int i = 1; i < MapData.MAP_FULL_HEIGHT - 1; i++) {
//			positions.add(new Pair<>(-MapData.MAP_FULL_WIDTH / 2 + 1, i-MapData.MAP_FULL_HEIGHT / 2));
//			positions.add(new Pair<>(MapData.MAP_FULL_WIDTH / 2 - 1, i-MapData.MAP_FULL_HEIGHT /2));
//		}
//		for (int i = 1; i < MapData.MAP_FULL_WIDTH - 1; i++) {
//			positions.add(new Pair<>(i-MapData.MAP_FULL_WIDTH / 2, -MapData.MAP_FULL_HEIGHT / 2+1));
//			positions.add(new Pair<>(i-MapData.MAP_FULL_WIDTH / 2, MapData.MAP_FULL_HEIGHT / 2-1));
//		}
//		message("s");
//		while (true) {
//			Pair<Integer, Integer> position = positions.get(rand.nextInt(positions.size()));
//			int px = position.getValue0();
//			int py = position.getValue1();
//			Pair<Boolean, String> result = tryMove(px, py);
//			if (result.getValue0()) {
//				this.x = px;
//				this.y = py;
//				map.getRoom(px, py).addPlayer(this);
//				room = map.getRoom(px, py);
//				break;
//			}
//		}
	}

	public String render() {
		char icon;
		switch (direction) {
			case 'n': icon = '^'; break; // U+25B2
			case 's': icon = 'v'; break; // U+25BC
			case 'w': icon = '<'; break; // U+25C4
			case 'e': icon = '>'; break; // U+25BA
			default:  icon = '@'; break; // fallback
		}
		return colorCode + icon + "\u001B[0m";
	}


	public Pair<Boolean, String> tryMove(int x, int y) {
		if (!map.inMap(x, y)) {
			return new Pair<>(false, "Out of map");
		}
		if (map.getRoom(x, y).throne) {
			if (!map.getRoom(x, y).getPlayers().isEmpty()) {
				return new Pair<>(false, "Throne is occupied");
			}
		}
		if (map.getRoom(x, y).blocked) {
			return new Pair<>(false, "Map is blocked");
		}
		return new Pair<>(true, "Moved " + x + "," + y);
	}

	private void moveNoCheck(Room room) {
		moveNoCheck(room.x, room.y);
	}

	private void moveNoCheck(int newX, int newY) {
		map.getRoom(x, y).removePlayer(this);
		x = newX;
		y = newY;
		map.getRoom(x, y).addPlayer(this);
		this.room = map.getRoom(x, y);
	}

	public void move(int dx, int dy) {
		int newX = x + dx;
		int newY = y + dy;
		Pair<Boolean, String> result = tryMove(newX, newY);
		if (result.getValue0()) {
			moveNoCheck(newX, newY);
		}
//		message(result.getValue1());
	}

	public double attack(String targetName) {
		Player player = viewableRooms(sight, direction).stream()
				.flatMap(r -> r.getPlayers().stream())
				.filter(p -> p.name.equalsIgnoreCase(targetName))
				.findFirst().orElse(null);
		if (player == null || player.team.equalsIgnoreCase(team)) {
			return 0;
		}
		double damage = Math.max(2 * sight - Utils.distance(x, y, player.x, player.y), 1);
		player.health -= damage;
		player.checkDeath();
		message("attack " + targetName);
		return damage;
	}

	public void checkDeath() {
		if (health <= 0) {
			message("is dead");
			dead = true;
			nextAction.clear();
			room.removePlayer(this);
			room = null;
		}
	}

	public Room getCurrentRoom() {
		return map.getRoom(x, y);
	}

	public void message(String message) {
		if (globalBox == null) return;
		globalBox.addMessage("[" + name + "] " + message);
	}

	public void addAction(Action action) {
		nextAction.add(action);
	}

	private Action getAction() {
		return nextAction.poll();
	}

	public double doAction() {
		if (dead) {
			return 0;
		}
		Action action = getAction();
		if (action == null) {
//			message("No action");
			return 0;
		}
		switch (action.movement) {
			case 'h' -> move(-1, 0);
			case 'j' -> move(0, 1);
			case 'k' -> move(0, -1);
			case 'l' -> move(1, 0);
			default -> {//stay
				}
		}
		direction = action.direction;

		if (action.target != null && !action.target.isEmpty()) {
			return attack(action.target);
		}
		return 0;
	}

	public List<Room> viewableRooms() {
		return viewableRooms(sight, direction);
	}

	public List<Room> viewableRooms(int sightRange, char direction) {
		List<Room> visible = new ArrayList<>();

		// Always see your current tile
		if (room != null) {
			visible.add(room);
		} else {
			return visible;
		}

		for (int tx = x - sightRange; tx <= x + sightRange; tx++) {
			for (int ty = y - sightRange; ty <= y + sightRange; ty++) {
				Room target = map.getRoom(tx, ty);
				if (target == null) continue;

				int dx = tx - x;
				int dy = ty - y;

				// Optional circular vision
				if (dx * dx + dy * dy > sightRange * sightRange) continue;

				// Filter by direction
				if (!inViewCone(direction, dx, dy)) continue;

				if (canSee(map, room, target)) {
					visible.add(target);
				}
			}
		}

		return visible;
	}

	private boolean inViewCone(char direction, int dx, int dy) {
		switch (Character.toLowerCase(direction)) {
			case 'n': return dy <= 0 && Math.abs(dx) <= Math.abs(dy);
			case 's': return dy >= 0 && Math.abs(dx) <= Math.abs(dy);
			case 'w': return dx <= 0 && Math.abs(dy) <= Math.abs(dx);
			case 'e': return dx >= 0 && Math.abs(dy) <= Math.abs(dx);
			default:  return true; // fallback if invalid direction
		}
	}

	private boolean canSee(MapData map, Room from, Room to) {
		int x0 = from.x;
		int y0 = from.y;
		int x1 = to.x;
		int y1 = to.y;

		int dx = Math.abs(x1 - x0);
		int dy = Math.abs(y1 - y0);
		int sx = Integer.compare(x1, x0);
		int sy = Integer.compare(y1, y0);
		int err = dx - dy;

		while (true) {
			Room current = map.getRoom(x0, y0);
			if (current == null) return false;

			if (x0 == x1 && y0 == y1) return true;
			if (current.blocked && current != from) return false;

			int e2 = 2 * err;
			if (e2 > -dy) { err -= dy; x0 += sx; }
			if (e2 <  dx) { err += dx; y0 += sy; }

			if (!map.inMap(x0, y0)) return false;
		}
	}


	public List<Player> findOpponents() {
		return this.viewableRooms().stream()
				.flatMap(r -> r.getPlayers().stream())
				.filter(p -> !p.team.equals(team) && !p.dead)
				.collect(Collectors.toList());
	}

}
