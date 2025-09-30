package com.nut.cashew.twig;


import com.nut.cashew.twig.room.Castle;

import java.util.*;

import static com.google.common.base.Preconditions.checkState;


public class MapData {

	public static final int TOTAL_CASTLE = 100;
	public static final int TOTAL_MINE = 200;
	public static final int MAP_FULL_WIDTH = 101;
	public static final int MAP_FULL_HEIGHT = 101;
	//
	public static final int OFFSET = MAP_FULL_WIDTH / 2;


	private Room[][] rooms;
	public List<Castle> castles;

	public MapData() {
		castles = new ArrayList<>();
	}

	public void init() {

		rooms = new Room[MAP_FULL_WIDTH][MAP_FULL_HEIGHT];
		for (int x = 0; x < MAP_FULL_WIDTH; x++) {
			for (int y = 0; y < MAP_FULL_HEIGHT; y++) {
				rooms[x][y] = new Room(x - OFFSET, y - OFFSET);
			}
		}
		Queue<Room> emptyRooms = emptyRooms(rooms);
		// castles
		castles.clear();
		for (int i = 0; i < TOTAL_CASTLE; i++) {
			Castle castle = null;
			while (castle == null) {
				castle = placeCastle(i, false, emptyRooms);
			}
			castles.add(castle);
		}
		// mines
		for (int i = 0; i < TOTAL_MINE; i++) {
			Castle castle = null;
			while (castle == null) {
				castle = placeCastle(i, true, emptyRooms);
			}
			castles.add(castle);
		}
		//
	}

	public Room getRoom(int x, int y) {
		int ix = x + OFFSET;
		int iy = y + OFFSET;
		if (ix < 0 || ix >= MAP_FULL_WIDTH || iy < 0 || iy >= MAP_FULL_HEIGHT) {
			return null;
		}
		return rooms[ix][iy];
	}

	private static Queue<Room> emptyRooms(Room[][] rooms) {
		List<Room> emptyRooms = new LinkedList<>();
		for (int x = 0; x < MAP_FULL_WIDTH; x++) {
			for (int y = 0; y < MAP_FULL_HEIGHT; y++) {
				Room room = rooms[x][y];
				if (room.isEmpty()) {
					emptyRooms.add(room);
				}
			}
		}
		if (emptyRooms.isEmpty()) {
			throw new IllegalStateException("no empty rooms");
		}
		Collections.shuffle(emptyRooms);
		return new LinkedList<>(emptyRooms);
	}

	public boolean inMap(int x, int y) {
		int ix = x + OFFSET;
		int iy = y + OFFSET;
		return ix >= 0 && ix < MAP_FULL_WIDTH && iy >= 0 && iy < MAP_FULL_HEIGHT;
	}

	public Castle placeCastle(int index, boolean isMine, Queue<Room> emptyRooms) {
		checkState(emptyRooms != null && !emptyRooms.isEmpty());
		Room room = emptyRooms.poll();
		String name = (isMine ? "M" : "C" ) + String.format("%0" +
				String.valueOf(TOTAL_CASTLE + TOTAL_MINE).length() + "d", index);
		Castle castle = new Castle(room, name, isMine);
		room.castle = castle;
		return castle;
	}
}
