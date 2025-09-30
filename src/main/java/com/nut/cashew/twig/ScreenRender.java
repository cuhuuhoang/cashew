package com.nut.cashew.twig;

import com.nut.cashew.root.MessageBox;
import com.nut.cashew.twig.room.Castle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.nut.cashew.root.MessageBox.combineColumns;
import static com.nut.cashew.root.MessageBox.combineRows;

public class ScreenRender {
	private final MapData map;

	public static final int MAP_VIEW_WIDTH = 60;
	public static final int MAP_VIEW_HEIGHT = 38;
	//
	public final MessageBox globalBox = new MessageBox("Global", 40, 20);
	private final MessageBox mapBox = new MessageBox("Map", MAP_VIEW_WIDTH, MAP_VIEW_HEIGHT);
	public final MessageBox lookBox = new MessageBox("Look", 40, 7);
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

	private void updateMapView() {
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
					sb.append(rooms[x][y].render());
				}
			}
			mapBox.addMessage(sb.toString());
		}
	}

	private void updateEventBox(List<Castle> castles, EventController eventController) {
		infoBox.clear();
		infoBox.addMessage("Turn: " + eventController.turnCount);
		int masterCount = 0;
		for (Castle castle : castles) {
			if (castle.getMaster() == castle && !castle.isMine) {
				masterCount++;
			}
		}
		infoBox.addMessage("Master: " + masterCount);
		if (eventController.autoReset.get()) {
			infoBox.addMessage("Auto Reset: ON");
		}
	}
	
	private void updateRankBox(List<Castle> castles, EventController eventController) {
		rankBox.clear();
		Map<Castle, Integer> castlePower = new HashMap<>();
		for (Castle castle : castles) {
			if (castle.getMaster() == castle && !castle.isMine) {
				castlePower.put(castle, castle.maxRally());
			}
		}
		castlePower.entrySet().stream()
				.sorted(Map.Entry.<Castle, Integer>comparingByValue().reversed())
				.limit(rankBox.getHeight())
				.forEach(entry -> rankBox.addMessage(entry.getKey().render() + " " + entry.getKey().name +
						": " + entry.getValue() + " " + entry.getKey().getFreeTroops().size() + " " + entry.getKey().aiController.name()));

//		if (!addedWinner && eventController.gameOver) {
//			addedWinner = true;
//			Castle winner = eventController.winner;
//			winnerBox.addMessage("Winner: " + eventController.turnCount + " " + winner.aiController.name());
//		}
		winnerBox.clear();
		int maxValue = eventController.aiWinners.values().stream().max(Integer::compareTo).orElse(0);
		int width = String.valueOf(maxValue).length();
		eventController.aiWinners.entrySet().stream()
				.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
				.limit(winnerBox.getHeight())
				.forEach(entry -> winnerBox.addMessage(String.format("%0" + width + "d", entry.getValue())
						+ ": " + entry.getKey()));
	}

	public void updateScreen(EventController eventController) {
		// update box
 		updateMapView();
		updateEventBox(map.castles, eventController);
		updateLookBox();
		updateRankBox(map.castles, eventController);
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

		result.add("> " + prompt);
		screenText.set(String.join("\n", result));
	}
}
