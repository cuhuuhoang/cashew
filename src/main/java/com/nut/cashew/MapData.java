package com.nut.cashew;


import org.javatuples.Pair;

import static com.nut.cashew.Const.*;

public class MapData {
	public static final int OFFSET = MAP_FULL_WIDTH / 2;

	private final Room[][] rooms = new Room[MAP_FULL_WIDTH][MAP_FULL_HEIGHT];


	public MapData() {
		for (int x = 0; x < MAP_FULL_WIDTH; x++) {
			for (int y = 0; y < MAP_FULL_HEIGHT; y++) {
				rooms[x][y] = new Room(x - OFFSET, y - OFFSET);
			}
		}
		placeAltars();
	}

	public Room getRoom(int x, int y) {
		int ix = x + OFFSET;
		int iy = y + OFFSET;
		if (ix < 0 || ix >= MAP_FULL_WIDTH || iy < 0 || iy >= MAP_FULL_HEIGHT) {
			return null;
		}
		return rooms[ix][iy];
	}

	private void placeAltars() {
		// Level 5: center
		placeAltar(0, 0, 5);

		// Level 4: 4 points at radius 10
		int r4 = 10;
		for (int[] d : new int[][]{{r4, 0}, {-r4, 0}, {0, r4}, {0, -r4}})
			placeAltar(d[0], d[1], 4);

		// Level 3: 8 diagonal at radius 20
		int r3 = 20;
		for (int[] d : new int[][]{{r3, r3}, {r3, -r3}, {-r3, r3}, {-r3, -r3},
				{r3, 0}, {0, r3}, {-r3, 0}, {0, -r3}})
			placeAltar(d[0], d[1], 3);

		// Level 2: 12 around radius 30 (evenly spaced)
		int r2 = 30;
		for (int i = 0; i < 12; i++) {
			double angle = 2 * Math.PI * i / 12;
			int x = (int)Math.round(Math.cos(angle) * r2);
			int y = (int)Math.round(Math.sin(angle) * r2);
			placeAltar(x, y, 2);
		}

		// Level 1: 16 around radius 40
		int r1 = 40;
		for (int i = 0; i < 16; i++) {
			double angle = 2 * Math.PI * i / 16;
			int x = (int)Math.round(Math.cos(angle) * r1);
			int y = (int)Math.round(Math.sin(angle) * r1);
			placeAltar(x, y, 1);
		}
	}

	private void placeAltar(int x, int y, int level) {
		Room room = getRoom(x, y);
		room.setAltar(new Altar(level, room));
	}

	public Pair<Boolean, String> tryMove(Player player, int x, int y) {
		if (!inMap(x, y)) {
			return new Pair<>(false, "Out of map");
		}
		if (getRoom(x, y).getAltar() != null && player.power < getRoom(x, y).getAltar().entryPower()) {
			return new Pair<>(false, "Not enough power");
		}
		return new Pair<>(true, "Moved " + x + "," + y);
	}

	private boolean inMap(int x, int y) {
		int ix = x + OFFSET;
		int iy = y + OFFSET;
		return ix >= 0 && ix < MAP_FULL_WIDTH && iy >= 0 && iy < MAP_FULL_HEIGHT;
	}

}
