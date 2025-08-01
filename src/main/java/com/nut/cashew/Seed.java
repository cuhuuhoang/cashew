package com.nut.cashew;

import com.nut.cashew.room.Arena;
import com.nut.cashew.room.Lobby;

public class Seed {
	public int index;
	public String name;

	public Arena arena;
	public Lobby lobby;


	public Seed(int index, String name, Arena arena, Lobby lobby) {
		this.index = index;
		this.name = name;
		this.arena = arena;
		this.lobby = lobby;
	}
}
