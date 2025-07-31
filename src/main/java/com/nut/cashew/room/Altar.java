package com.nut.cashew.room;


import com.nut.cashew.Const;
import com.nut.cashew.Room;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

public class Altar {
	@Getter
	public final int level;
	public final Room room;
	public boolean isOpen;
	public Set<String> players = new HashSet<>();

	public Altar(int level, Room room) {
		this.level = level;
		this.room = room;
		this.isOpen = level <= Const.MAX_ALTAR_SAFE_LEVEL;
	}
	
	public long powerGain() {
		if (level <= Const.MAX_ALTAR_SAFE_LEVEL) {
			return (long) Math.pow(level, 3) * 100;
		}
		return (long) Math.pow(Const.MAX_ALTAR_SAFE_LEVEL,
				level - Const.MAX_ALTAR_SAFE_LEVEL + 3) * 100;
	}

	public long entryPower() {
		return entryPower(level);
	}

	public static long entryPower(int level) {
		return (long) Math.pow(level, 5) * 100;
	}

}
