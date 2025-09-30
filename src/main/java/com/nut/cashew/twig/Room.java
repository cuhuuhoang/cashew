package com.nut.cashew.twig;

import com.nut.cashew.twig.room.Castle;
import com.nut.cashew.twig.room.Troop;

import java.util.*;

public class Room {
	public final int x;
	public final int y;

	public Castle castle;
	public List<Troop> troops;

	public Room(int x, int y) {
		this.x = x;
		this.y = y;
		this.troops = new LinkedList<>();
		this.castle = null;
	}

	public boolean isEmpty() {
		return castle == null;
	}

	public String render() {
		if (castle != null) {
			return castle.render();
		}
		if (!troops.isEmpty()) {
			try {
				return troops.get(0).render();
			} catch (Exception ignored) {
			}
		}
		return "\u001B[32m.\u001B[0m";
	}
}
