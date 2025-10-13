package com.nut.cashew.bud;

import com.nut.cashew.root.MessageBox;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.nut.cashew.root.MessageBox.combineColumns;
import static com.nut.cashew.root.MessageBox.combineRows;

public class ScreenRender {
	private final MapData map;

	public static final int MAP_VIEW_WIDTH = 60;
	public static final int MAP_VIEW_HEIGHT = 34;
	//
	public final MessageBox globalBox = new MessageBox("Global", 40, 18);
	private final MessageBox mapBox = new MessageBox("Map", MAP_VIEW_WIDTH, MAP_VIEW_HEIGHT);
	public final MessageBox lookBox = new MessageBox("Look", 40, 5);
	public final MessageBox infoBox = new MessageBox("Info", 40, 7);
	public final MessageBox rankBox = new MessageBox("Rank", 40, 15);
	public final MessageBox winnerBox = new MessageBox("Winner", 40, 15);

	public Room povRoom = null;
	public boolean addedWinner = false;
	public final AtomicReference<String> screenText = new AtomicReference<>("Game starting...");
	public final StringBuilder prompt = new StringBuilder();

	public ScreenRender(MapData map) {
		this.map = map;
	}
	
	public void reset() {
		povRoom = map.getRoom(0, 0);
		addedWinner = false;
	}

	public void moveUp() {
		if (map.inMap(povRoom.x, povRoom.y - MAP_VIEW_HEIGHT / 2)) {
			povRoom = map.getRoom(povRoom.x, povRoom.y - MAP_VIEW_HEIGHT / 2);
		}
	}

	public void moveDown() {
		if (map.inMap(povRoom.x, povRoom.y + MAP_VIEW_HEIGHT / 2)) {
			povRoom = map.getRoom(povRoom.x, povRoom.y + MAP_VIEW_HEIGHT / 2);
		}
	}

	public void moveLeft() {
		if (map.inMap(povRoom.x - MAP_VIEW_WIDTH / 2, povRoom.y)) {
			povRoom = map.getRoom(povRoom.x - MAP_VIEW_WIDTH / 2, povRoom.y);
		}
	}

	public void moveRight() {
		if (map.inMap(povRoom.x + MAP_VIEW_WIDTH / 2, povRoom.y)) {
			povRoom = map.getRoom(povRoom.x + MAP_VIEW_WIDTH / 2, povRoom.y);
		}
	}

	private void updateLookBox() {
		lookBox.clear();
		lookBox.addMessage("Pos: " + povRoom.x + "," + povRoom.y);
	}

	private void updateMapView(Player player) {
		Set<String> viewable = null;
		if (player != null) {
//			viewable = new HashSet<>();
//			List<Room> rooms = player.viewableRooms(player.sight, player.direction);
//			for (Room room : rooms) {
//				viewable.add(room.x + "-" + room.y);
//			}
		}

		Room[][] rooms = new Room[MAP_VIEW_WIDTH][MAP_VIEW_HEIGHT];
		int startX = povRoom.x - MAP_VIEW_WIDTH / 2;
		int startY = povRoom.y - MAP_VIEW_HEIGHT / 2;

		for (int y = 0; y < MAP_VIEW_HEIGHT; y++) {
			for (int x = 0; x < MAP_VIEW_WIDTH; x++) {
				int mapX = startX + x;
				int mapY = startY + y;
				rooms[x][y] = map.getRoom(mapX, mapY);
			}
		}
		mapBox.clear();
		for (int y = 0; y < MAP_VIEW_HEIGHT; y++) {
			StringBuilder sb = new StringBuilder();
			for (int x = 0; x < MAP_VIEW_WIDTH; x++) {
				if (rooms[x][y] == null) {
					sb.append(" ");
				} else {
					String text = rooms[x][y].render();
//					if (!text.contains("@")) {
//						int mapX = startX + x;
//						int mapY = startY + y;
//						if (viewable != null && viewable.contains(mapX + "-" + mapY) ) {
//							sb.append("+");
//							continue;
//						}
//					}
					sb.append(text);
				}
			}
			mapBox.addMessage(sb.toString());
		}
	}

	private void updateEventBox(EventController eventController) {
		infoBox.clear();
		infoBox.addMessage("Turn: " + eventController.turnCount);
	}
	
	private void updateRankBox(List<Player> players, EventController eventController) {
		rankBox.clear();
		Map<Player, Integer> playerKills = new HashMap<>();
		for (Player player : players) {
			playerKills.put(player, player.killCount);
		}
		playerKills.entrySet().stream()
				.sorted(Map.Entry.<Player, Integer>comparingByValue().reversed())
				.limit(rankBox.getHeight())
				.forEach(entry -> {
					Player player = entry.getKey();
					rankBox.addMessage(player.coloredText(player.name) + " H:" + player.health + " K: " + player.killCount);
				});
		winnerBox.clear();
		int maxValue = eventController.winners.values().stream().max(Integer::compareTo).orElse(0);
		int width = String.valueOf(maxValue).length();
		eventController.winners.entrySet().stream()
				.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
				.limit(winnerBox.getHeight())
				.forEach(entry -> winnerBox.addMessage(String.format("%0" + width + "d", entry.getValue())
						+ ": " + entry.getKey()));
	}

	public void updateScreen(EventController eventController) {
		// update box
 		updateMapView(map.players.get(0));
		updateEventBox(eventController);
		updateLookBox();
		updateRankBox(map.players, eventController);
		// merge box
		List<String> result = combineColumns(
			mapBox.box(),
			combineRows(
					globalBox.box(),
					infoBox.box(),
					lookBox.box()
			),
			combineRows(
				rankBox.box(),
				winnerBox.box()
			)
		);

		screenText.set(String.join("\n", result));
	}
}
