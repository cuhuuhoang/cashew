package com.nut.cashew.seed;

import com.nut.cashew.seed.room.*;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.nut.cashew.seed.Const.MAX_ALTAR_SAFE_LEVEL;

public class Room {
	@Getter
	private final List<Player> players;

	public Altar altar;
	public Arena arena;
	public Lobby lobby;
	public Treasure treasure;
	public Boss boss;

	public final int x;
	public final int y;

	public boolean isEmpty() {
		return altar == null && arena == null && treasure == null && boss == null && lobby == null;
	}

	public Room(int x, int y) {
		this.x = x;
		this.y = y;
		this.players = new ArrayList<>();
	}

	public String render(@Nullable Player player) {
		if (altar != null) {
			if (altar.level <= MAX_ALTAR_SAFE_LEVEL) {
				return "\u001B[36m" + altar.level + "\u001B[0m";
			}
			return "\u001B[35m" + altar.level + "\u001B[0m";
		}
//		if (treasure != null) {
//			return "\u001B[33m.\u001B[0m";
//		}
		if (boss != null) {
			return "\u001B[31mB\u001B[0m";
		}
		if (lobby != null) {
			return "L";
		}
		if (player == null) {
			if (!players.isEmpty()) {
				return "\u001B[31m@\u001B[0m";
			}
		} else {
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
