package com.nut.cashew.room;

import com.nut.cashew.Room;

public class Arena {
	public boolean isOpen;
	public final Room room;

	public Arena(Room room) {
		this.room = room;
		this.isOpen = false;
	}
}
