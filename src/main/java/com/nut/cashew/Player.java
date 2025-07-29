package com.nut.cashew;


import lombok.Getter;
import org.javatuples.Pair;

import java.util.*;
import java.util.stream.Collectors;

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
	@Getter
	private final MapData map;
	public final AiController aiController;
	public final Queue<String> nextAction;
	public final Characteristic characteristic;
	public Room room;

	public double crit = 1.0;
	public double grow = 1.0;
	public int power = 100;
	public final Alliance alliance;

	public Player(MapData map, String name, MessageBox globalBox, Characteristic characteristic, Alliance alliance) {
		this.name = name;
		this.map = map;
		this.alliance = alliance;
		this.aiController = new AiController(this);
		this.messageBox = new MessageBox("Messages", BOX_MESSAGES_WIDTH, BOX_MESSAGES_HEIGHT);
		this.coordsBox = new MessageBox("Coordinates", COL_2_WIDTH, BOX_COORDS_HEIGHT);
		this.lookBox = new MessageBox("Look", BOX_LOOK_WIDTH, BOX_LOOK_HEIGHT);
		this.statsBox = new MessageBox("Stats", COL_2_WIDTH, BOX_STATS_HEIGHT);
		this.nextAction = new ArrayDeque<>();
		this.globalBox = globalBox;
		this.characteristic = characteristic;
		this.room = null;
		//
	}

	public void respawn() {
		message("Respawn");
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
			Pair<Boolean, String> result = tryMove(px, py, true);
			if (result.getValue0()) {
				this.x = px;
				this.y = py;
				map.getRoom(px, py).addPlayer(this);
				room = map.getRoom(px, py);
				break;
			}
		}
	}

	public Pair<Boolean, String> tryMove(int x, int y, boolean respawn) {
		if (!map.inMap(x, y)) {
			return new Pair<>(false, "Out of map");
		}
		if (map.getRoom(x, y).getAltar() != null && power < map.getRoom(x, y).getAltar().entryPower()) {
			return new Pair<>(false, "Not enough power");
		}
		if (map.getRoom(x, y).getAltar() != null && !map.getRoom(x, y).getAltar().isOpen) {
			return new Pair<>(false, "Altar is closed");
		}
		if (map.getRoom(x, y).getArena() != null) {
			return new Pair<>(false, "Can not move to Arena");
		}
		if (!respawn) {
			if (map.getRoom(x, y).getArena() != null && map.getArena().isOpen) {
				return new Pair<>(false, "Can not move out of Arena");
			}
		}
		return new Pair<>(true, "Moved " + x + "," + y);
	}

	public void moveToArena() {
		Room room = map.getArena().room;
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
		Pair<Boolean, String> result = tryMove(newX, newY, false);
		if (result.getValue0()) {
			moveNoCheck(newX, newY);
		}
		message(result.getValue1());
	}

	public void attack(String targetName) {
		if (map.getRoom(x, y).getAltar() == null && map.getRoom(x, y).getArena() == null) {
			message("Can not attack open field");
			return;
		}
		if (map.getRoom(x, y).getAltar() != null && map.getRoom(x,y).getAltar().level <= MAX_ALTAR_SAFE_LEVEL) {
			message("Can not attack at safe altar");
			return;
		}
		for (Player target : map.getRoom(x, y).getPlayers()) {
			if (target.name.equalsIgnoreCase(targetName)) {
				if (target.alliance.name.equalsIgnoreCase(alliance.name)) {
					message("Can not attack same alliance");
					return;
				}
				Random rand = new Random();
				double damage = rand.nextDouble() * Math.min(target.power / target.crit, power / crit);
				this.power -= Math.min((int) (damage * (1 + target.crit)), power);
				target.power -= Math.min ((int) (damage * (1 + crit)), target.power);
				if (getCurrentRoom().getAltar() != null && getCurrentRoom().getAltar().level > 0) {
					int altarLevel = getCurrentRoom().getAltar().level;
					double critIncrease = (double) altarLevel * altarLevel / 100;
					crit += critIncrease;
					target.crit += critIncrease;
				}
				String message = String.format("%s attacked %s, damage %d", name, targetName, (int) damage);
				message(message);
				return;
			}
		}
		message("Target disappeared");
	}

	public Room getCurrentRoom() {
		return map.getRoom(x, y);
	}

	public void takeTreasure() {
		Room room = map.getRoom(x, y);
		if (room.getTreasure() != null) {
			Treasure treasure = room.getTreasure();
			double reward = treasure.reward;
			map.getTreasures().remove(treasure);
			room.setTreasure(null);
			message("Take treasure " + String.format("%.2f", reward));
//			globalBox.addMessage(getFullName() + " takes treasure " + String.format("%.2f", reward));
			grow += reward;
		} else {
			message("No treasure");
		}
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
			int inc = (int) ((double) room.getAltar().powerGain() / Math.max(room.getPlayers().size(), 1));
			inc = Math.max(inc, 1);
			power += (int) (inc * grow);
			if (power > (double) MAX_POWER * 2d) {
				power = BASE_POWER;
				globalBox.addMessage(getFullName() + " Power invalid");
				respawn();
			} else if (power > MAX_POWER) {
				power = MAX_POWER;
			}

			message("Sit at altar, power increase " + inc);
		}
	}

	public String getFullName() {
		return "["+alliance.name + "]" +name;
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
				sb.append(player.getFullName()).append(" ");
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
		statsBox.addMessage("Name: " + getFullName());
		statsBox.addMessage("Char: " + characteristic);
		statsBox.addMessage(String.format("Power: %d; Cr: %.2f; Gr: %.2f", power, crit, grow));
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
			case "respawn" -> respawn();
			case "treasure" -> takeTreasure();
			default -> message("Unknown command: " + command);
		}
	}

	public List<String> combineColumns(List<List<String>> lists) {
		List<String> result = new LinkedList<>();
		int maxHeight = lists.stream().mapToInt(List::size).max().orElse(0);
		List<Integer> widthList = lists.stream().map(strings -> strings.get(0).length())
				.collect(Collectors.toList());
		for (int i = 0; i < maxHeight; i++) {
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j < lists.size(); j++) {
				List<String> list = lists.get(j);
				if (i < list.size()) {
					sb.append(list.get(i));
				} else {
					sb.append(" ".repeat(widthList.get(j)));
				}
			}
			result.add(sb.toString());
		}
		return result;
	}

	public List<String> combineRows(List<List<String>> lists) {
		List<String> result = new LinkedList<>();
		int maxWidth = lists.stream().mapToInt(strings -> strings.get(0).length()).max().orElse(0);
		for(List<String> list : lists) {
			for (String s : list) {
				StringBuilder sb = new StringBuilder();
				sb.append(s);
				int length = MessageBox.stripAnsi(s).length();
				if (length < maxWidth) {
					sb.append(" ".repeat(maxWidth - length));
				}
				result.add(sb.toString());
			}
		}
		return result;
	}

	public List<String> box(PlayerSet playerSet) {
		List<String> infoPanel = combineColumns(List.of(messageBox.box(), lookBox.box()));
		List<String> firstCol = combineRows(List.of(infoPanel, getViewMap().box()));
		//
		playerSet.setRank();
		//
		List<String> secondCol = new LinkedList<>();
		secondCol.addAll(globalBox.box());
		secondCol.addAll(playerSet.infoBox.box());
		secondCol.addAll(statsBox.box());
		//
		List<String> thirdCol = new LinkedList<>();
		thirdCol.addAll(playerSet.rankBox.box());
		thirdCol.addAll(playerSet.allianceBox.box());

		// Print combined panels
		List<String> result = combineColumns(List.of(firstCol, secondCol, thirdCol));
		return result;
	}
}
