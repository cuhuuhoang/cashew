package com.nut.cashew.leaf;


import com.nut.cashew.root.MessageBox;
import lombok.Getter;
import lombok.Setter;
import org.javatuples.Pair;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import static com.google.common.base.Preconditions.checkState;

public class Bot {
	@Getter
	public int x;
	@Getter
	public int y;

	public int level = 1;
	public final MapData map;
	public final String colorCode;
	public final Queue<String> nextAction;

	public final Map<Integer, Integer> resources;
	@Setter
	public  MessageBox globalBox;
	@Getter
	public Room room;

	public Bot(MapData map, String colorCode) {
		this.map = map;
		this.nextAction = new ArrayDeque<>();
		this.room = null;
		this.colorCode = colorCode;
		this.resources = new HashMap<>();
	}

	public void dump(int resource, int amount) {
		int actual = resources.getOrDefault(resource, 0);
		checkState(actual >= amount, "Not enough resources");
		resources.put(resource, actual - amount);
		this.room.resources.put(resource, this.room.resources.getOrDefault(resource, 0) + amount);
	}

	public void take(int resource, int amount) {
		int actual = this.room.resources.getOrDefault(resource, 0);
		checkState(actual >= amount, "Not enough resources");
		this.room.resources.put(resource, actual - amount);
		resources.put(resource, resources.getOrDefault(resource, 0) + amount);
		checkState(resources.values().stream().mapToInt(Integer::intValue).sum() <= level, "Too much resources");
	}

	public void respawn(Room room) {
		nextAction.clear();
		if (this.room != null) {
			this.room.removeBot(this);
		}
		this.room = room;
		this.room.addBot(this);
		this.x = this.room.x;
		this.y = this.room.y;
	}

	public String render() {
		return coloredText("@");
	}

	public String coloredText(String text) {
		return colorCode + text + "\u001B[0m";
	}


	public Pair<Boolean, String> tryMove(int x, int y) {
		if (!map.inMap(x, y)) {
			return new Pair<>(false, "Out of map");
		}
		return new Pair<>(true, "Moved " + x + "," + y);
	}


	private void moveNoCheck(int newX, int newY) {
		map.getRoom(x, y).removeBot(this);
		x = newX;
		y = newY;
		map.getRoom(x, y).addBot(this);
		this.room = map.getRoom(x, y);
	}

	public void move(int dx, int dy) {
		int newX = x + dx;
		int newY = y + dy;
		Pair<Boolean, String> result = tryMove(newX, newY);
		if (result.getValue0()) {
			moveNoCheck(newX, newY);
		}
	}


	public Room getCurrentRoom() {
		return map.getRoom(x, y);
	}

	public void message(String message) {
		if (globalBox == null) return;
		globalBox.addMessage("[" + coloredText("x") + "] " + message);
	}

	public void addAction(String action) {
		nextAction.add(action);
	}

	private String getAction() {
		return nextAction.poll();
	}

	public void doAction() {
		String action = getAction();
		if (action == null) {
			return;
		}
		switch (action) {
			case "h" -> move(-1, 0);
			case "j" -> move(0, 1);
			case "k" -> move(0, -1);
			case "l" -> move(1, 0);
			default -> message("Unknown action: " + action);
		}
	}

}
