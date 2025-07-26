package com.nut.cashew;

import java.util.Random;

public class Player {
	public int x;
	public int y;

	public Player(MapData map) {
		Random rand = new Random();
		while (true) {
			double angle = rand.nextDouble() * 2 * Math.PI;
			int radius = 40 + rand.nextInt(11); // 40–50
			int px = (int) Math.round(Math.cos(angle) * radius);
			int py = (int) Math.round(Math.sin(angle) * radius);

			if (map.inBounds(px, py)) {
				this.x = px;
				this.y = py;
				break;
			}
		}
	}

	public int getX() { return x; }
	public int getY() { return y; }

	public void move(int dx, int dy, MapData map) {
		int newX = x + dx;
		int newY = y + dy;
		if (map.inBounds(newX, newY)) {
			x = newX;
			y = newY;
		}
	}
}
