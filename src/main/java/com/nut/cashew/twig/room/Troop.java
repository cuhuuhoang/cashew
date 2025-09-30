package com.nut.cashew.twig.room;

import com.nut.cashew.twig.MapData;
import com.nut.cashew.twig.Room;
import org.javatuples.Pair;

import static com.google.common.base.Preconditions.checkNotNull;

public class Troop {
	private final Castle master;
	public Room room;

	public Troop(Room room, Castle master) {
		this.room = checkNotNull(room);
		this.master = checkNotNull(master);
	}

	public String render() {
		return master.colorer("♟");
	}

	public Castle getMaster() {
		return master.getMaster();
	}

	private Pair<Boolean, String> tryMove(MapData map, Room dest) {
		int x = dest.x;
		int y = dest.y;
		if (!map.inMap(x, y)) {
			return new Pair<>(false, "Out of map");
		}
		return new Pair<>(true, "Moved " + x + "," + y);
	}

	public void moveToDirection(MapData map, Castle castle) {
		moveToDirection(map, castle.room);
	}

	public void moveToDirection(MapData map, Room room) {
		moveToDirection(map, room.x, room.y);
	}

	private void moveToDirection(MapData map, int x, int y) {
		if (x > room.x) {
			moveTo(map, map.getRoom(room.x + 1, room.y));
		} else if (x < room.x) {
			moveTo(map, map.getRoom(room.x - 1, room.y));
		} else if (y > room.y) {
			moveTo(map, map.getRoom(room.x, room.y + 1));
		} else if (y < room.y) {
			moveTo(map, map.getRoom(room.x, room.y - 1));
		}
	}

	private void moveTo(MapData map, Room room) {
		var r = tryMove(map, room);
		if (!r.getValue0()) {
			throw new IllegalStateException("Can not move to " + room.x + "," + room.y +
					" from " + room.x + "," + room.y + " because " + r.getValue1());
		}
		this.room.troops.remove(this);
		this.room = room;
		this.room.troops.add(this);
	}

}
