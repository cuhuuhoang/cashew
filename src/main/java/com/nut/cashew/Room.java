package com.nut.cashew;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static com.nut.cashew.Const.MAX_ALTAR_SAFE_LEVEL;

public class Room {
	@Getter
	@Setter
	private Altar altar;
	@Getter
	private final List<Player> players;
	@Getter
	@Setter
	private Arena arena;
	@Getter
	@Setter
	private Treasure treasure;

	public final int x;
	public final int y;

	public Room(int x, int y) {
		this.x = x;
		this.y = y;
		this.players = new ArrayList<>();
	}

	public String render(Player player) {
		if (altar != null) {
//			return switch (altar.level) {
//				case 5 -> "\u001B[35mA\u001B[0m"; // Magenta
//				case 4 -> "\u001B[31mA\u001B[0m"; // Red
//				case 3 -> "\u001B[33mA\u001B[0m"; // Yellow
//				case 2 -> "\u001B[36mA\u001B[0m"; // Cyan
//				case 1 -> "\u001B[37mA\u001B[0m"; // White
//				default -> "A";
//			};
			if (altar.level <= MAX_ALTAR_SAFE_LEVEL) {
				return "\u001B[36m" + altar.level + "\u001B[0m";
			}
			return "\u001B[35m" + altar.level + "\u001B[0m";
		}
		if (treasure != null) {
			return "\u001B[33mT\u001B[0m";
		}
		if (players.contains(player)) {
			return "@";
		}
		for (Player p : players) {
			if (p != player) {
				if (p.alliance.name.equals(player.alliance.name)) {
					return "\u001B[36m@\u001B[0m";
				}
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

}
