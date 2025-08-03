package com.nut.cashew.seed.room;

import com.nut.cashew.seed.Alliance;
import com.nut.cashew.seed.Room;

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
