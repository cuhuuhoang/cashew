package com.nut.cashew;

import com.nut.cashew.room.Altar;

public class Const {
	//
	public static final int MAP_FULL_WIDTH = 101;
	public static final int MAP_FULL_HEIGHT = 101;
	//
	public static final int BASE_SPEED = 100;


	//
	public static final int TOTAL_PLAYER=100;
	//
	public static final int MAX_ALTAR_LEVEL=5;
	public static final int MAX_ALTAR_SAFE_LEVEL =3;
	public static final int ALTAR_MULTI = 4;
	public static final long MAX_POWER= Altar.entryPower(MAX_ALTAR_LEVEL) * 2;
	public static final long BASE_POWER=100;
	//
	public static final int TOTAL_ALLIANCE = 10;
	public static final int ARENA_INTERVAL = 5000;
	public static final int ARENA_SAFE_INTERVAL = ARENA_INTERVAL / 5;
	public static final int MIN_ARENA_POWER = 1000;
	public static final int TREASURE_SPAWN_TURN = 10;
	public static final double TREASURE_MAX_REWARD = 0.02;
	public static final double TREASURE_MIN_REWARD = 0.001;


	public static final double RESET_POWER_LIMIT = 1.1d;

}
