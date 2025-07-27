package com.nut.cashew;

import java.util.ArrayList;
import java.util.List;

public class Room {
	private Altar altar;
	private final List<Player> players;

	public final int x;
	public final int y;

	public Room(int x, int y) {
		this.x = x;
		this.y = y;
		this.players = new ArrayList<>();
	}

	public void setAltar(Altar altar) {
		this.altar = altar;
	}

	public Altar getAltar() {
		return altar;
	}

	public String render(Player player) {
		if (altar != null) {
			return switch (altar.level) {
				case 5 -> "\u001B[35mA\u001B[0m"; // Magenta
				case 4 -> "\u001B[31mA\u001B[0m"; // Red
				case 3 -> "\u001B[33mA\u001B[0m"; // Yellow
				case 2 -> "\u001B[36mA\u001B[0m"; // Cyan
				case 1 -> "\u001B[37mA\u001B[0m"; // White
				default -> "A";
			};
		}
		if (players.contains(player)) {
			return "@";
		}
		for (Player p : players) {
			if (p != player) {
				return "\u001B[31m@\u001B[0m";
			}
		}
		return "\u001B[32m.\u001B[0m";
	}

	public void addPlayer(Player player) {
		players.add(player);
	}

	public void removePlayer(Player player) {
		players.remove(player);
	}

	public List<Player> getPlayers() {
		return players;
	}
}
