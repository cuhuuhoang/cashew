package com.nut.cashew.room;

import com.nut.cashew.Alliance;
import com.nut.cashew.Room;

import java.util.LinkedList;
import java.util.List;

public class Arena {
	public boolean isOpen;
	public final Room room;


	public final List<Alliance> alliancesInBattle = new LinkedList<>();

	public Arena(Room room) {
		this.room = room;
		this.isOpen = false;
	}

}
