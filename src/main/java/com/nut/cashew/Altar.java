package com.nut.cashew;


import lombok.Getter;

public class Altar {
	@Getter
	public final int level;
	public final Room room;

	public Altar(int level, Room room) {
		this.level = level;
		this.room = room;
	}
	
	public int powerGain() {
		return (int) Math.pow(level, 2) * 100;
	}

	public int entryPower() {
		return entryPower(level);
	}

	public static int entryPower(int level) {
		return (int) Math.pow(level, 4) * 100;
	}

}
