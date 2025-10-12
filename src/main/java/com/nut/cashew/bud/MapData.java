package com.nut.cashew.bud;


import java.util.*;

import static com.nut.cashew.root.Utils.generateRandomAnsiColor;

public class MapData {

//	public static final String MAP_STRING =
//			"............................................................" +
//					"............................................................" +
//					"............................................................" +
//					"......xxxxxx......xxxxxx.......xxxxxxx........xxxxxxx......." +
//					"......xxxxxx......xxxxxx.......xxxxxxx........xxxxxxx......." +
//					"......xxxxxx......xxxxxx.......xxxxxxx........xxxxxxx......." +
//					"......xxxxxx......xxxxxx.......xxxxxxx........xxxxxxx......." +
//					"............................................................" +
//					"............................................................" +
//					"............................................................" +
//					"............................................................" +
//					"............xxxxxxxxx................xxxxxxxxxx............." +
//					"............xxxxxxxxx................xxxxxxxxxx............." +
//					"............xxxxxxxxx................xxxxxxxxxx............." +
//					"............xxxxxxxxx................xxxxxxxxxx............." +
//					"............xxxxxxxxx................xxxxxxxxxx............." +
//					"............................................................" +
//					"............................................................" +
//					"............................................................" +
//					"...............xxxxxxxxxxxxxxxxxxxxxxxxxxx.................." +
//					"...............xxxxxxxxxxxxxxxxxxxxxxxxxxx.................." +
//					"............................................................" +
//					"............................................................" +
//					"............................................................" +
//					"............................................................" +
//					"............xxxxxxxxx................xxxxxxxxxx............." +
//					"............xxxxxxxxx................xxxxxxxxxx............." +
//					"............xxxxxxxxx................xxxxxxxxxx............." +
//					"............xxxxxxxxx................xxxxxxxxxx............." +
//					"............................................................" +
//					"............................................................" +
//					"............................................................" +
//					"............................................................" +
//					"......xxxxxx......xxxxxx.......xxxxxxx........xxxxxxx......." +
//					"......xxxxxx......xxxxxx.......xxxxxxx........xxxxxxx......." +
//					"......xxxxxx......xxxxxx.......xxxxxxx........xxxxxxx......." +
//					"............................................................" +
//					"............................................................";
	public static final String MAP_STRING =
					".....................................\n" +
					".....................................\n" +
					".....................................\n" +
					".....xxx.........xxx.........xxx.....\n" +
					".....xxx.........xxx.........xxx.....\n" +
					".....xxx.........xxx.........xxx.....\n" +
					"...........xxx.........xxx...........\n" +
					"...........xxx.........xxx...........\n" +
					"...........xxx.........xxx...........\n" +
					".....xxx.....................xxx.....\n" +
					"..A..xxx..........0..........xxx..B..\n" +
					".....xxx.....................xxx.....\n" +
					"...........xxx.........xxx...........\n" +
					"...........xxx.........xxx...........\n" +
					"...........xxx.........xxx...........\n" +
					".....xxx.........xxx.........xxx.....\n" +
					".....xxx.........xxx.........xxx.....\n" +
					".....xxx.........xxx.........xxx.....\n" +
					".....................................\n" +
					".....................................\n" +
					".....................................\n" ;
	public static final List<String> TEAMS = List.of("A");
	public static final int TOTAL_PLAYER_EACH_TEAM = 1;
	public static final int TOTAL_PLAYER = TEAMS.size() * TOTAL_PLAYER_EACH_TEAM;
	public static final int MAP_FULL_WIDTH = MAP_STRING.split("\n")[0].length();
	public static final int MAP_FULL_HEIGHT = MAP_STRING.split("\n").length;
	public static final int OFFSET_WIDTH = MAP_FULL_WIDTH / 2;
	public static final int OFFSET_HEIGHT = MAP_FULL_HEIGHT / 2;

	private Room[][] rooms;
	public List<Player> players;
	public Room throne;
	public Map<String, Room> respawnRooms = new HashMap<>();

	public MapData() {
		players = new ArrayList<>();
	}

	public void init() {

		rooms = new Room[MAP_FULL_WIDTH][MAP_FULL_HEIGHT];
		String mapString = MAP_STRING.replaceAll("\n", "");
		for (int x = 0; x < MAP_FULL_WIDTH; x++) {
			for (int y = 0; y < MAP_FULL_HEIGHT; y++) {
				rooms[x][y] = new Room(x - OFFSET_WIDTH, y - OFFSET_HEIGHT);
				int index = x + MAP_FULL_WIDTH * y;
				char c = mapString.charAt(index);
				if (c == 'x') {
					rooms[x][y].blocked = true;
				} else if (c == '0') {
					rooms[x][y].throne = true;
					throne = rooms[x][y];
				} else if (TEAMS.contains(String.valueOf(c))) {
					respawnRooms.put(String.valueOf(c), rooms[x][y]);
				}
			}
		}

		Queue<String> teams = new ArrayDeque<>();
		for (int i = 0; i < TOTAL_PLAYER_EACH_TEAM; i++) {
			teams.addAll(TEAMS);
		}
		Map<String, String> teamColors = new HashMap<>();
		for (String team : TEAMS) {
			teamColors.put(team, generateRandomAnsiColor());
		}

		players = new ArrayList<>();
		for (int i = 0; i < TOTAL_PLAYER; i++) {
			String team = teams.poll();
			Player player = new Player(this, "P" + i, team, teamColors.get(team));
			players.add(player);
		}

	}

	public Room getRoom(int x, int y) {
		int ix = x + OFFSET_WIDTH;
		int iy = y + OFFSET_HEIGHT;
		if (ix < 0 || ix >= MAP_FULL_WIDTH || iy < 0 || iy >= MAP_FULL_HEIGHT) {
			return null;
		}
		return rooms[ix][iy];
	}

	public boolean inMap(int x, int y) {
		int ix = x + OFFSET_WIDTH;
		int iy = y + OFFSET_HEIGHT;
		return ix >= 0 && ix < MAP_FULL_WIDTH && iy >= 0 && iy < MAP_FULL_HEIGHT;
	}

}
