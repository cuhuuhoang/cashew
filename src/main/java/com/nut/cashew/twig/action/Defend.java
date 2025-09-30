package com.nut.cashew.twig.action;

import com.nut.cashew.twig.room.Castle;
import com.nut.cashew.twig.room.Troop;

import java.util.LinkedList;
import java.util.List;

public class Defend {
	public final List<Troop> troops;

	public Defend() {
		this.troops = new LinkedList<>();
	}
}
