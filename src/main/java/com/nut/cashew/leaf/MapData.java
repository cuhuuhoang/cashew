package com.nut.cashew.leaf;


import com.nut.cashew.root.Utils;

import java.util.*;

public class MapData {

	public static final int MAX_MINE_LEVEL = 20;
	public static final int MINE_PER_LEVEL = 10;
	public static final int MAP_FULL_WIDTH = 101;
	public static final int MAP_FULL_HEIGHT = 101;
	//
	public static final int OFFSET = MAP_FULL_WIDTH / 2;

	private Room[][] rooms;
	public List<Bot> bots;

	public MapData() {
	}

	public void init() {
		rooms = new Room[MAP_FULL_WIDTH][MAP_FULL_HEIGHT];
		for (int x = 0; x < MAP_FULL_WIDTH; x++) {
			for (int y = 0; y < MAP_FULL_HEIGHT; y++) {
				rooms[x][y] = new Room(x - OFFSET, y - OFFSET);
			}
		}

		Stack<Room> emptyRooms = collectAndShuffleEmptyRooms();
		for (int i = 0; i < MAX_MINE_LEVEL; i++) {
			for (int j = 0; j < MINE_PER_LEVEL; j++) {
				Objects.requireNonNull(emptyRooms.pop()).setMine(i + 1);
			}
		}

		bots = new LinkedList<>();

		Bot master = new Bot(this, Utils.generateRandomAnsiColor());
		master.room = getRoom(0, 0);
		getRoom(0, 0).constructFactory(master, this);
	}

	public Room getRoom(int x, int y) {
		int ix = x + OFFSET;
		int iy = y + OFFSET;
		if (ix < 0 || ix >= MAP_FULL_WIDTH || iy < 0 || iy >= MAP_FULL_HEIGHT) {
			return null;
		}
		return rooms[ix][iy];
	}

	public boolean inMap(int x, int y) {
		int ix = x + OFFSET;
		int iy = y + OFFSET;
		return ix >= 0 && ix < MAP_FULL_WIDTH && iy >= 0 && iy < MAP_FULL_HEIGHT;
	}

	private Stack<Room> collectAndShuffleEmptyRooms() {
		Stack<Room> emptyRooms = new Stack<>();
		for (int x = 0; x < MAP_FULL_WIDTH; x++) {
			for (int y = 0; y < MAP_FULL_HEIGHT; y++) {
				if (x != OFFSET && y != OFFSET) {
					emptyRooms.push(rooms[x][y]);
				}
			}
		}
		Collections.shuffle(emptyRooms);
		emptyRooms.push(getRoom(0, 0));
		return emptyRooms;
	}
}
