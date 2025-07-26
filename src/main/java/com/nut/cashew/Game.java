package com.nut.cashew;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

import org.jline.utils.InfoCmp.Capability;

import java.util.*;

public class Game {
	static final int VIEW_WIDTH = 21;
	static final int VIEW_HEIGHT = 6;
	public static void main(String[] args) throws IOException {
		Terminal terminal = TerminalBuilder.terminal();
		LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
		terminal.enterRawMode();

		MapData map = new MapData();
		Player player = new Player(map);
		Deque<String> messages = new LinkedList<>();

		while (true) {
			terminal.puts(Capability.clear_screen);
			terminal.flush();

			// Top panel - Map
			terminal.writer().println("╭─ Map ─────────────────────────────╮");
			int startX = player.getX() - VIEW_WIDTH / 2;
			int startY = player.getY() - VIEW_HEIGHT / 2;

			for (int y = 0; y < VIEW_HEIGHT; y++) {
				terminal.writer().print("│");
				for (int x = 0; x < VIEW_WIDTH; x++) {
					int mapX = startX + x;
					int mapY = startY + y;
					if (mapX == player.getX() && mapY == player.getY()) {
						terminal.writer().print("@");
					} else if (map.getRoom(mapX, mapY) == null) {
						terminal.writer().print(" ");
					} else {
						terminal.writer().print(map.getRoom(mapX, mapY).render());
					}
				}
				terminal.writer().print("│");
				terminal.writer().println();
			}
			terminal.writer().println("╰────────────────────────────────────────╯");

			// Middle panel - Messages
			terminal.writer().println("╭─ Messages ─────────────────────────────╮");
			List<String> recent = new ArrayList<>(messages);
			int messageDisplay = 4;
			int msgDisplay = Math.min(messageDisplay, recent.size());
			for (int i = recent.size() - msgDisplay; i < recent.size(); i++) {
				terminal.writer().println("│ " + recent.get(i));
			}
			for (int i = msgDisplay; i < messageDisplay; i++) {
				terminal.writer().println("│");
			}
			terminal.writer().println("╰────────────────────────────────────────╯");

			// Bottom panel - Input
			String line = reader.readLine("> ");
			if (line == null) continue;
			if (line.trim().equalsIgnoreCase("quit")) break;

			switch (line.trim()) {
				case "h" -> player.x--;
				case "j" -> player.y++;
				case "k" -> player.y--;
				case "l" -> player.x++;
				default -> messages.add("Unknown command: " + line);
			}

			Room room = map.getRoom(player.x, player.y);
			if (room.getAltar() != null && room.getAltar().level > 0) {
				messages.add("You see an altar of level " + room.getAltar().level + " at (" + player.x + "," + player.y + ")");
			}

			// Keep message queue size small
			while (messages.size() > 50) messages.removeFirst();
		}

		terminal.writer().println("Goodbye!");
		terminal.flush();
		terminal.close();
	}
}
