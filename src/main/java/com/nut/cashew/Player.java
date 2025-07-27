package com.nut.cashew;


import lombok.Getter;
import org.javatuples.Pair;

import java.util.*;

import static com.nut.cashew.Const.*;
import static com.nut.cashew.Const.BOX_LOOK_HEIGHT;
import static com.nut.cashew.Const.COL_2_WIDTH;

public class Player {
	@Getter
	public int x;
	@Getter
	public int y;

	private final MessageBox messageBox;
	private final MessageBox coordsBox;
	private final MessageBox lookBox;
	private final MessageBox statsBox;
	private final MessageBox globalBox;
	public final String name;
	private final MapData map;
	public final AiController aiController;
	public final Queue<String> nextAction;
	public final Characteristic characteristic;
	public Room room;

	public int power = 100;

	public Player(MapData map, String name, MessageBox globalBox, Characteristic characteristic) {
		this.name = name;
		this.map = map;
		this.aiController = new AiController(this);
		this.messageBox = new MessageBox("Messages", COL_2_WIDTH, BOX_MESSAGES_HEIGHT);
		this.coordsBox = new MessageBox("Coordinates", COL_2_WIDTH, BOX_COORDS_HEIGHT);
		this.lookBox = new MessageBox("Look", COL_2_WIDTH, BOX_LOOK_HEIGHT);
		this.statsBox = new MessageBox("Stats", COL_2_WIDTH, BOX_STATS_HEIGHT);
		this.nextAction = new ArrayDeque<>();
		this.globalBox = globalBox;
		this.characteristic = characteristic;
		this.room = null;
		//
	}

	public void respawn() {
		message("Respawn");
		globalBox.addMessage(String.format("%s respawned", name));
		nextAction.clear();
		Random rand = new Random();
		if (this.room != null) {
			this.room.removePlayer(this);
		}
		while (true) {
			double angle = rand.nextDouble() * 2 * Math.PI;
			int radius = 40 + rand.nextInt(11); // 40–50
			int px = (int) Math.round(Math.cos(angle) * radius);
			int py = (int) Math.round(Math.sin(angle) * radius);
			Pair<Boolean, String> result = map.tryMove(this, px, py);
			if (result.getValue0()) {
				this.x = px;
				this.y = py;
				map.getRoom(px, py).addPlayer(this);
				room = map.getRoom(px, py);
				break;
			}
		}
	}

	public Pair<Boolean, String> tryMove(int newX, int newY) {
		return map.tryMove(this, newX, newY);
	}

	public void move(int dx, int dy) {
		int newX = x + dx;
		int newY = y + dy;
		Pair<Boolean, String> result = tryMove(newX, newY);
		if (result.getValue0()) {
			map.getRoom(x, y).removePlayer(this);
			x = newX;
			y = newY;
			map.getRoom(x, y).addPlayer(this);
			this.room = map.getRoom(x, y);
			if (map.getRoom(x, y).getAltar() != null) {
				globalBox.addMessage(String.format("%s arrived altar lv%d %d,%d",
						name, map.getRoom(x, y).getAltar().level, x, y));
			}
		}
		message(result.getValue1());
	}

	public void attack(String targetName) {
		for (Player target : map.getRoom(x, y).getPlayers()) {
			if (target.name.equalsIgnoreCase(targetName)) {
				Random rand = new Random();
				int damage = (int) (rand.nextDouble() * Math.min(target.power, power));
				this.power -= damage;
				target.power -= damage;
				String message = String.format("%s attacked %s, damage %d", name, targetName, damage);
				message(message);
				globalBox.addMessage(message);
				return;
			}
		}
		message("Target disappeared");
	}

	public Room getCurrentRoom() {
		return map.getRoom(x, y);
	}

	public void startTurn() {
		Room room = map.getRoom(x, y);
		if (power < 100) {
			message("Low power " + power);
			power = 100;
			respawn();
		}
		if (room.getAltar() != null && room.getAltar().level > 0) {
			if (power < room.getAltar().entryPower()) {
				respawn();
			}
			int inc = (int) ((double) room.getAltar().powerGain() / room.getPlayers().size());
			power += inc;
			message("Sit at altar, power increase " + inc);
		}
	}

	public void look() {
		Room room = map.getRoom(x, y);
		lookBox.clear();
		if (room.getAltar() != null && room.getAltar().level > 0) {
			lookBox.addMessage("Altar lv." + room.getAltar().level);
		}

		StringBuilder sb = new StringBuilder();
		for (Player player : room.getPlayers()) {
			if (player != this) {
				sb.append(player.name).append(" ");
			}
		}
		if (!sb.isEmpty()) {
			lookBox.addMessage("Players: " + sb);
		}

		if (lookBox.isEmpty()) {
			lookBox.addMessage("Nothing");
		}
		coordsBox.clear();
		coordsBox.addMessage(x + "," + y);
		statsBox.clear();
		statsBox.addMessage("Name: " + name);
		statsBox.addMessage("Char: " + characteristic);
		statsBox.addMessage("Power: " + power);
	}

	public ViewMap getViewMap() {
		look();
		return new ViewMap(this, map);
	}

	public void message(String message) {
		messageBox.addMessage(message);
	}

	public void addAction(String command) {
		nextAction.add(command);
	}

	public String getAction() {
		return nextAction.poll();
	}

	public boolean notHasAction() {
		return nextAction.isEmpty();
	}

	public void doAction(String command) {
		if (command == null || command.trim().isEmpty()) {
			message("No action");
			return;
		}
		if (command.startsWith("attack ") || command.startsWith("att ") ) {
			String[] items = command.split(" ");
			if (items.length < 2) {
				message("No target");
			}
			attack(items[1]);
			return;
		}
		switch (command) {
			case "h" -> move(-1, 0);
			case "j" -> move(0, 1);
			case "k" -> move(0, -1);
			case "l" -> move(1, 0);
			default -> message("Unknown command: " + command);
		}
	}

	public List<String> box(PlayerSet playerSet) {
		List<String> firstPanel = new ArrayList<>(getViewMap().box());
		//
		List<String> secondPanel = new LinkedList<>();
		secondPanel.addAll(messageBox.box());
		secondPanel.addAll(coordsBox.box());
		secondPanel.addAll(lookBox.box());
		secondPanel.addAll(statsBox.box());
		//
		List<String> thirdPanel = new LinkedList<>();
		playerSet.setRank();
		thirdPanel.addAll(playerSet.rankBox.box());
		thirdPanel.addAll(globalBox.box());

		// Print combined panels
		List<String> result = new LinkedList<>();
		for (int i = 0; i < SCREEN_HEIGHT; i++) {
			StringBuilder sb = new StringBuilder();
			if (i < firstPanel.size()) {
				sb.append(firstPanel.get(i));
			} else {
				sb.append(" ".repeat(MAP_VIEW_WIDTH + 2));
			}
			if (i < secondPanel.size()) {
				sb.append(secondPanel.get(i));
			} else {
				sb.append(" ".repeat(COL_2_WIDTH + 2));
			}
			if (i < thirdPanel.size()) {
				sb.append(thirdPanel.get(i));
			} else {
				sb.append(" ".repeat(COL_3_WIDTH + 2));
			}
			result.add(sb.toString());
		}
		return result;
	}
}
