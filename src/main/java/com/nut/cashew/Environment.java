package com.nut.cashew;

import java.util.Random;

public enum Environment {
	PLANT('.', "\u001B[32m"),    // green .
	WATER('~', "\u001B[34m"),    // blue ~
	SAND(':', "\u001B[33m"),     // yellow :
	ROCK('#', "\u001B[37m"),     // white #
	EMPTY(' ', "\u001B[0m");     // default

	private final char symbol;
	private final String color;

	Environment(char symbol, String color) {
		this.symbol = symbol;
		this.color = color;
	}

	public String coloredSymbol() {
		return color + symbol + "\u001B[0m";
	}

	public static Environment random(Random rand) {
		return switch (rand.nextInt(10)) {
			case 0, 1 -> WATER;
			case 2 -> ROCK;
			case 3 -> SAND;
			default -> PLANT;
		};
	}
}
