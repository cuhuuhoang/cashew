package com.nut.cashew;


import java.util.*;

import static com.nut.cashew.Const.*;
import static com.nut.cashew.Const.BOX_LOOK_HEIGHT;
import static com.nut.cashew.Const.COL_2_WIDTH;

public class Player {
	public int x;
	public int y;

	private final MessageBox messageBox;
	private final MessageBox coordsBox;
	private final MessageBox lookBox;
	public final String name;
	public final MapData map;
	public final AiController aiController;

	public final Queue<String> nextAction;

	public Player(MapData map, String name) {
		this.name = name;
		this.map = map;
		this.aiController = new AiController(this);
		this.messageBox = new MessageBox("Messages", COL_2_WIDTH, BOX_MESSAGES_HEIGHT);
		this.coordsBox = new MessageBox("Coordinates", COL_2_WIDTH, BOX_COORDS_HEIGHT);
		this.lookBox = new MessageBox("Look", COL_2_WIDTH, BOX_LOOK_HEIGHT);
		this.nextAction = new ArrayDeque<>();
		//
		Random rand = new Random();
		while (true) {
			double angle = rand.nextDouble() * 2 * Math.PI;
			int radius = 40 + rand.nextInt(11); // 40–50
			int px = (int) Math.round(Math.cos(angle) * radius);
			int py = (int) Math.round(Math.sin(angle) * radius);

			if (map.inBounds(px, py)) {
				this.x = px;
				this.y = py;
				map.getRoom(px, py).addPlayer(this);
				break;
			}
		}
	}

	public int getX() { return x; }
	public int getY() { return y; }

	public void move(int dx, int dy, MapData map) {
		int newX = x + dx;
		int newY = y + dy;
		if (map.inBounds(newX, newY)) {
			map.getRoom(x, y).removePlayer(this);
			x = newX;
			y = newY;
			message("Moved " + dx + "," + dy);
			map.getRoom(x, y).addPlayer(this);
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

	public void doAction() {
		String command = getAction();
		if (command == null || command.trim().isEmpty()) {
			message("No action");
			return;
		}
		switch (command) {
			case "h" -> move(-1, 0, map);
			case "j" -> move(0, 1, map);
			case "k" -> move(0, -1, map);
			case "l" -> move(1, 0, map);
			default -> message("Unknown command: " + command);
		}
	}

	public List<String> box() {
		List<String> leftPanel = new ArrayList<>(getViewMap().box());
		//
		List<String> rightPanel = new LinkedList<>();
		rightPanel.addAll(messageBox.box());
		rightPanel.addAll(coordsBox.box());
		rightPanel.addAll(lookBox.box());

		// Print combined panels
		List<String> result = new LinkedList<>();
		for (int i = 0; i < leftPanel.size(); i++) {
			StringBuilder sb = new StringBuilder();
			sb.append(leftPanel.get(i));
			if (i < rightPanel.size()) {
				sb.append(rightPanel.get(i));
			}
			result.add(sb.toString());
		}
		return result;
	}
}
