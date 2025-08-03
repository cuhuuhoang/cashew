package com.nut.cashew.seed.room;

import com.nut.cashew.seed.Room;

public class Boss {
	public final Room room;
	public long power;
	public double reward;
	public double finalReward;

	public Boss(Room room, long power, double reward, double finalReward) {
		this.room = room;
		this.power = power;
		this.reward = reward;
		this.finalReward = finalReward;
	}
}
