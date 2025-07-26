package com.nut.cashew;

public class Room {
	private final Environment environment;
	private Altar altar;

	public Room(Environment environment) {
		this.environment = environment;
	}

	public Environment getEnvironment() {
		return environment;
	}

	public void setAltar(Altar altar) {
		this.altar = altar;
	}

	public Altar getAltar() {
		return altar;
	}

	public String render() {
		if (altar != null) {
			return switch (altar.level) {
				case 5 -> "\u001B[35mA\u001B[0m"; // Magenta
				case 4 -> "\u001B[31mA\u001B[0m"; // Red
				case 3 -> "\u001B[33mA\u001B[0m"; // Yellow
				case 2 -> "\u001B[36mA\u001B[0m"; // Cyan
				case 1 -> "\u001B[37mA\u001B[0m"; // White
				default -> "A";
			};
		}
		return environment.coloredSymbol();
	}
}
