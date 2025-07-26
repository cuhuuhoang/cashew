package com.nut.cashew;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

import org.jline.utils.InfoCmp.Capability;

import java.util.*;

import static com.nut.cashew.BoxHelper.*;

public class Game {
	static final int FULL_WIDTH = 49;
	static final int FULL_HEIGHT = 14;

	static final int LEFT_WIDTH = 21;
	static final int RIGHT_WIDTH = FULL_WIDTH - LEFT_WIDTH - 4;
	static final int MAP_HEIGHT = FULL_HEIGHT - 3;

	public static void main(String[] args) throws IOException {
		Terminal terminal = TerminalBuilder.terminal();
		LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
		terminal.enterRawMode();

		MessageBox messageBox = new MessageBox("Messages", RIGHT_WIDTH, 5);
		MessageBox coordsBox = new MessageBox("Coordinates", RIGHT_WIDTH, 1);
		MessageBox lookBox = new MessageBox("Look", RIGHT_WIDTH, 2);

		MapData map = new MapData();
		Player player = new Player(map, coordsBox, lookBox);

//		Deque<String> messages = new LinkedList<>();

		while (true) {
			terminal.puts(Capability.clear_screen);
			terminal.flush();
			
			List<String> leftPanel = new ArrayList<>();

			// Top panel - Map
			leftPanel.add(topBorder("Map", LEFT_WIDTH));
			int startX = player.getX() - LEFT_WIDTH / 2;
			int startY = player.getY() - MAP_HEIGHT / 2;

			for (int y = 0; y < MAP_HEIGHT; y++) {
				StringBuilder sb = new StringBuilder();
				sb.append("│");
				for (int x = 0; x < LEFT_WIDTH; x++) {
					int mapX = startX + x;
					int mapY = startY + y;
					if (mapX == player.getX() && mapY == player.getY()) {
						sb.append("@");
					} else if (map.getRoom(mapX, mapY) == null) {
						sb.append(" ");
					} else {
						sb.append(map.getRoom(mapX, mapY).render());
					}
				}
				sb.append("│");
				leftPanel.add(sb.toString());
			}
			leftPanel.add(bottomBorder(LEFT_WIDTH));

			List<String> rightPanel = new LinkedList<>();
			rightPanel.addAll(messageBox.box());
			rightPanel.addAll(coordsBox.box());
			// Middle panel - Messages

			// Print combined panels
			for (int i = 0; i < leftPanel.size(); i++) {
				terminal.writer().print(leftPanel.get(i));
				if (i < rightPanel.size()) {
					terminal.writer().print(rightPanel.get(i));
				}
				terminal.writer().println();
			}
			terminal.flush();

			// full width 49
			// Bottom panel - Input
			String line = reader.readLine("> ");
			if (line == null) continue;
			if (line.trim().equalsIgnoreCase("quit")) break;

			String command = line.trim();
			if (command.matches("\\d+[hjkl]")) {
				int steps = Integer.parseInt(command.substring(0, command.length() - 1));
				char direction = command.charAt(command.length() - 1);
				switch (direction) {
					case 'h' -> player.move(-steps, 0, map);
					case 'j' -> player.move(0, steps, map);
					case 'k' -> player.move(0, -steps, map);
					case 'l' -> player.move(steps, 0, map);
				}
			} else {
				switch (command) {
					case "h" -> player.move(-1, 0, map);
					case "j" -> player.move(0, 1, map);
					case "k" -> player.move(0, -1, map);
					case "l" -> player.move(1, 0, map);
					default -> messageBox.addMessage("Unknown command: " + line);
				}
			}

			Room room = map.getRoom(player.x, player.y);
			lookBox.clear();
			if (room.getAltar() != null && room.getAltar().level > 0) {
				lookBox.addMessage("Altar lv." + room.getAltar().level);
			} else {
				lookBox.addMessage("Nothing");
			}
			coordsBox.clear();
			coordsBox.addMessage(player.x + "," + player.y);

			// Keep message queue size small
//			while (messages.size() > 50) messages.removeFirst();
		}

		terminal.writer().println("Goodbye!");
		terminal.flush();
		terminal.close();
	}
}
