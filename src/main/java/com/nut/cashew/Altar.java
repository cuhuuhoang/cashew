package com.nut.cashew;


import lombok.Getter;

public class Altar {
	@Getter
	public final int level;
	public final Room room;
	public boolean isOpen;

	public Altar(int level, Room room) {
		this.level = level;
		this.room = room;
		this.isOpen = level <= Const.MAX_ALTAR_SAFE_LEVEL;
	}
	
	public int powerGain() {
		if (level <= Const.MAX_ALTAR_SAFE_LEVEL) {
			return (int) Math.pow(level, 3) * 100;
		}
		return (int) Math.pow(Const.MAX_ALTAR_SAFE_LEVEL,
				level - Const.MAX_ALTAR_SAFE_LEVEL + 3) * 100;
	}

	public int entryPower() {
		return entryPower(level);
	}

	public static int entryPower(int level) {
		return (int) Math.pow(level, 5) * 100;
	}

}
