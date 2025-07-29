package com.nut.cashew;

public class Arena {
	public boolean isOpen;
	public final Room room;

	public Arena(Room room) {
		this.room = room;
		this.isOpen = false;
	}
}
