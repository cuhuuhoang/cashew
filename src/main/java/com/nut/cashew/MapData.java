package com.nut.cashew;


import com.nut.cashew.room.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static com.nut.cashew.Const.*;

public class MapData {
	public static final int OFFSET = MAP_FULL_WIDTH / 2;

	@Getter
	private final List<Altar> altars;
	@Getter
	private final List<Treasure> treasures;
	@Getter
	private final List<Boss> bosses;
	@Getter
	private final Arena arena;
	@Getter
	private final WaitRoom waitRoom;

	@Getter
	private final Room[][] rooms = new Room[MAP_FULL_WIDTH][MAP_FULL_HEIGHT];


	public MapData() {
		for (int x = 0; x < MAP_FULL_WIDTH; x++) {
			for (int y = 0; y < MAP_FULL_HEIGHT; y++) {
				rooms[x][y] = new Room(x - OFFSET, y - OFFSET);
			}
		}
		altars = List.copyOf(placeAltars());
		arena = placeArena();
		waitRoom = placeWaitRoom();
		treasures = new LinkedList<>();
		bosses = new LinkedList<>();
	}

	public Room getRoom(int x, int y) {
		int ix = x + OFFSET;
		int iy = y + OFFSET;
		if (ix < 0 || ix >= MAP_FULL_WIDTH || iy < 0 || iy >= MAP_FULL_HEIGHT) {
			return null;
		}
		return rooms[ix][iy];
	}

	private Room emptyRoom() {
		List<Room> emptyRooms = new ArrayList<>();
		for (int x = 0; x < MAP_FULL_WIDTH; x++) {
			for (int y = 0; y < MAP_FULL_HEIGHT; y++) {
				Room room = rooms[x][y];
				if (room.isEmpty()) {
					emptyRooms.add(room);
				}
			}
		}
		if (emptyRooms.isEmpty()) {
			return null;
		}
		return emptyRooms.get(new Random().nextInt(emptyRooms.size()));
	}
	
	public void placeTreasure() {
		Room room = emptyRoom();
		if (room == null) {
			return;
		}
		Treasure treasure = new Treasure(room, TREASURE_MIN_REWARD + new Random().nextDouble() * (TREASURE_MAX_REWARD - TREASURE_MIN_REWARD));
		room.treasure = treasure;
		treasures.add(treasure);
	}

	public void placeBoss(long power, double reward) {
		Room room = emptyRoom();
		if (room == null) {
			return;
		}
		Boss boss = new Boss(room, power, reward);
		room.boss = boss;
		bosses.add(boss);
	}
	
	private Arena placeArena() {
		Room room = getRoom(0, -OFFSET + 2);
		Arena arena = new Arena(room);
		room.arena = arena;
		return arena;
	}

	private WaitRoom placeWaitRoom() {
		Room room = getRoom(0, -OFFSET + 1);
		WaitRoom waitRoom = new WaitRoom(room);
		room.waitRoom = waitRoom;
		return waitRoom;
	}

	private List<Altar> placeAltars() {
		List<Altar> tmpAltars = new LinkedList<>();
		// center
		tmpAltars.add(placeAltar(0, 0, MAX_ALTAR_LEVEL));
		// around
		for (int level = 1; level <= MAX_ALTAR_LEVEL; level++) {
			int r1 = (int) (((double) Math.min(MAP_FULL_WIDTH, MAP_FULL_HEIGHT) / 2 - 5) / MAX_ALTAR_LEVEL * (MAX_ALTAR_LEVEL - level));
			int amount = (MAX_ALTAR_LEVEL - level) * ALTAR_MULTI;
			for (int i = 0; i < amount; i++) {
				double angle = 2 * Math.PI * i / amount;
				int x = (int) Math.round(Math.cos(angle) * r1);
				int y = (int) Math.round(Math.sin(angle) * r1);
				tmpAltars.add(placeAltar(x, y, level));
			}
		}
		return tmpAltars;
	}

	private Altar placeAltar(int x, int y, int level) {
		Room room = getRoom(x, y);
		Altar altar = new Altar(level, room);
		room.altar = altar;
		return altar;
	}

	public boolean inMap(int x, int y) {
		int ix = x + OFFSET;
		int iy = y + OFFSET;
		return ix >= 0 && ix < MAP_FULL_WIDTH && iy >= 0 && iy < MAP_FULL_HEIGHT;
	}

}
