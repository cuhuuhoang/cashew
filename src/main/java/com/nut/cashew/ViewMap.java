package com.nut.cashew;

import lombok.Getter;

import java.util.List;

import static com.nut.cashew.Const.*;

public class ViewMap {
	@Getter
	private final Room[][] rooms;

	private final Player player;
	private final MessageBox mapBox;

	public ViewMap(Player player, MapData map) {
		mapBox = new MessageBox("Map", MAP_VIEW_WIDTH, MAP_VIEW_HEIGHT);
		rooms = new Room[MAP_VIEW_WIDTH][MAP_VIEW_HEIGHT];
		this.player = player;
		int startX = player.getX() - MAP_VIEW_WIDTH / 2;
		int startY = player.getY() - MAP_VIEW_HEIGHT / 2;

		for (int y = 0; y < MAP_VIEW_HEIGHT; y++) {
			for (int x = 0; x < MAP_VIEW_WIDTH; x++) {
				int mapX = startX + x;
				int mapY = startY + y;
				rooms[x][y] = map.getRoom(mapX, mapY);
			}
		}
	}

	public List<String> box() {
		mapBox.clear();
		for (int y = 0; y < MAP_VIEW_HEIGHT; y++) {
			StringBuilder sb = new StringBuilder();
			for (int x = 0; x < MAP_VIEW_WIDTH; x++) {
				if (rooms[x][y] == null) {
					sb.append(" ");
				} else {
					sb.append(rooms[x][y].render(this.player));
				}
			}
			mapBox.addMessage(sb.toString());
		}
		return mapBox.box();
	}

}
