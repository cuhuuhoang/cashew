package com.nut.cashew;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

import java.util.Random;


import org.jline.utils.InfoCmp.Capability;

import java.util.*;

public class Game {
	static final int MAP_WIDTH = 101;
	static final int MAP_HEIGHT = 101;
	static final int VIEW_WIDTH = 21;
	static final int VIEW_HEIGHT = 11;

	enum Environment {
		GRASS('.', "\u001B[32m"),
		WATER('~', "\u001B[34m");

		final char symbol;
		final String color;

		Environment(char symbol, String color) {
			this.symbol = symbol;
			this.color = color;
		}
	}

	static class Room {
		final Environment env;
		final int altarLevel;

		Room(Environment env, int altarLevel) {
			this.env = env;
			this.altarLevel = altarLevel;
		}

		String render() {
			return env.color + (altarLevel > 0 ? Integer.toString(altarLevel) : String.valueOf(env.symbol)) + "\u001B[0m";
		}
	}

	static class MapData {
		final Room[][] rooms = new Room[MAP_WIDTH][MAP_HEIGHT];

		MapData() {
			Random rand = new Random();
			for (int y = 0; y < MAP_HEIGHT; y++) {
				for (int x = 0; x < MAP_WIDTH; x++) {
					Environment env = rand.nextBoolean() ? Environment.GRASS : Environment.WATER;
					rooms[x][y] = new Room(env, 0);
				}
			}

			placeAltars();
		}

		void placeAltars() {
			int[] dx = { -1, 0, 1 };
			for (int level = 5; level >= 1; level--) {
				int radius = (6 - level) * 10;
				int count = level * 4;
				double angleStep = 2 * Math.PI / count;
				for (int i = 0; i < count; i++) {
					int x = MAP_WIDTH / 2 + (int) (radius * Math.cos(i * angleStep));
					int y = MAP_HEIGHT / 2 + (int) (radius * Math.sin(i * angleStep));
					if (inBounds(x, y)) {
						rooms[x][y] = new Room(Environment.GRASS, level);
					}
				}
			}
			rooms[MAP_WIDTH / 2][MAP_HEIGHT / 2] = new Room(Environment.GRASS, 5); // center altar
		}

		boolean inBounds(int x, int y) {
			return x >= 0 && y >= 0 && x < MAP_WIDTH && y < MAP_HEIGHT;
		}

		Room getRoom(int x, int y) {
			return inBounds(x, y) ? rooms[x][y] : new Room(Environment.GRASS, 0);
		}
	}

	static class Player {
		int x, y;

		Player(MapData map) {
			Random rand = new Random();
			int border = MAP_WIDTH / 2;
			while (true) {
				x = rand.nextInt(MAP_WIDTH);
				y = rand.nextInt(MAP_HEIGHT);
				int dist = Math.max(Math.abs(x - border), Math.abs(y - border));
				if (dist >= 40) break;
			}
		}
	}

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
			int startX = player.x - VIEW_WIDTH / 2;
			int startY = player.y - VIEW_HEIGHT / 2;

			for (int y = 0; y < VIEW_HEIGHT; y++) {
				for (int x = 0; x < VIEW_WIDTH; x++) {
					int mapX = startX + x;
					int mapY = startY + y;
					if (mapX == player.x && mapY == player.y) {
						terminal.writer().print("@");
					} else {
						terminal.writer().print(map.getRoom(mapX, mapY).render());
					}
				}
				terminal.writer().println();
			}

			// Middle panel - Messages
			terminal.writer().println("╭─ Messages ────────────────────────╮");
			List<String> recent = new ArrayList<>(messages);
			int msgDisplay = Math.min(5, recent.size());
			for (int i = recent.size() - msgDisplay; i < recent.size(); i++) {
				terminal.writer().println("│ " + recent.get(i));
			}
			for (int i = msgDisplay; i < 5; i++) {
				terminal.writer().println("│");
			}
			terminal.writer().println("╰───────────────────────────────────╯");

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
			if (room.altarLevel > 0) {
				messages.add("You see an altar of level " + room.altarLevel + " at (" + player.x + "," + player.y + ")");
			}

			// Keep message queue size small
			while (messages.size() > 50) messages.removeFirst();
		}

		terminal.writer().println("Goodbye!");
		terminal.flush();
		terminal.close();
	}
}
