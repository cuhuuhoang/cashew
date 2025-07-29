package com.nut.cashew;

public class Const {
	//
	public static final int MAP_FULL_WIDTH = 101;
	public static final int MAP_FULL_HEIGHT = 101;
	//

	public static final int SCREEN_WIDTH = 140;
	public static final int SCREEN_HEIGHT = 35;

	public static final int MAP_VIEW_WIDTH = 60;
	public static final int MAP_VIEW_HEIGHT = 30;
	public static final int COL_2_WIDTH = 40;
	public static final int COL_3_WIDTH = SCREEN_WIDTH - MAP_VIEW_WIDTH - COL_2_WIDTH - 4;

	public static final int BOX_MESSAGES_HEIGHT = 6;
	public static final int BOX_COORDS_HEIGHT = 1;
	public static final int BOX_INFO_HEIGHT = 2;
	public static final int BOX_LOOK_HEIGHT = 6;
	public static final int BOX_STATS_HEIGHT = 5;
	public static final int BOX_GLOBAL_HEIGHT = 7;
	public static final int BOX_RANK_HEIGHT = 15;
	public static final int BOX_ALLIANCE_RANK_HEIGHT = 5;
	//
	public static final int TOTAL_PLAYER=100;
	//
	public static final int MAX_ALTAR_LEVEL=5;
	public static final int MAX_ALTAR_SAFE_LEVEL =3;
	public static final int ALTAR_MULTI = 4;
	public static final int MAX_POWER=Altar.entryPower(MAX_ALTAR_LEVEL) * 2;
	public static final int BASE_POWER=100;
	//
	public static final int TOTAL_ALLIANCE = 10;
	public static final int MIN_ARENA_POWER = 10000;
	public static final int TREASURE_SPAWN_TURN = 200;
	public static final double TREASURE_MAX_REWARD = 0.2;
	public static final double TREASURE_MIN_REWARD = 0.01;

}
