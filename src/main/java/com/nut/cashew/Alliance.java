package com.nut.cashew;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;

public class Alliance {
	public String name;
	public double taxRate;
	public Player leader;
	public Seed prevSeed;
	public Seed seed;

	public List<Player> players;

	public Alliance(String name) {
		this.name = name;
		this.players = new LinkedList<>();
	}

	public void promoteLeader(Player player) {
		if (player.alliance != this) {
			throw new IllegalArgumentException("Player is not in this alliance");
		}
		if (leader != null) {
			leader.name = leader.name.replace("L", "P");
		}
		leader = player;
		leader.name = leader.name.replace("P", "L");
		taxRate = leader.characteristic.greedy / 10;
	}
}
