package com.nut.cashew.room;

import com.nut.cashew.Room;

public class Boss {
	public final Room room;
	public long power;
	public double reward;

	public Boss(Room room, long power, double reward) {
		this.room = room;
		this.power = power;
		this.reward = reward;
	}
}
