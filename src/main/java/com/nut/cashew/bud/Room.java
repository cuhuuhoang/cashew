package com.nut.cashew.bud;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class Room {
	@Getter
	private final List<Player> players;

	public final int x;
	public final int y;

	public char block;
	public boolean blocked;
	public boolean throne;
	public boolean lava;

	public Room(int x, int y) {
		this.x = x;
		this.y = y;
		this.players = new ArrayList<>();
		this.blocked = false;
		this.throne = false;
	}

	public String render() {
		if (blocked) {
			return "\u001B[32m" + block +"\u001B[0m";
		}
		if (throne) {
			return "\u001B[31m♕\u001B[0m";
		}
		if (!players.isEmpty()) {
			try {
				return players.get(0).render();
			} catch (Exception ignored) {
			}
		}
		if (lava) {
			return "\u001B[31m~\u001B[0m";
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
