package com.nut.cashew;

public class AiController {
	private final Player player;

	public AiController(Player player) {
		this.player = player;
	}

	public String nextMove() {
		return "h";
	}
}
