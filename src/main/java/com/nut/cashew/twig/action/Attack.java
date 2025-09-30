package com.nut.cashew.twig.action;

import com.nut.cashew.twig.room.Castle;
import com.nut.cashew.twig.room.Troop;

import java.util.LinkedList;
import java.util.List;

public class Attack {
	public Castle attacker;
	public Castle target;
	public List<Troop> troops;
	public boolean rallied;

	public Attack(Castle attacker, Castle target) {
		this.attacker = attacker;
		this.target = target;
		this.troops = new LinkedList<>();
		this.rallied = false;
	}
}
