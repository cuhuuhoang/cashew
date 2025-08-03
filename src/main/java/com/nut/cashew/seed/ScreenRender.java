package com.nut.cashew.seed;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import static com.nut.cashew.seed.Const.*;

public class ScreenRender {

	public static final int MAP_VIEW_WIDTH = 60;
	public static final int MAP_VIEW_HEIGHT = 29;

	//
	public static final int BOX_MESSAGES_HEIGHT = 6;
	public static final int BOX_MESSAGES_WIDTH = 30;
	//
	public static final int BOX_INFO_HEIGHT = 2;
	public static final int BOX_INFO_WIDTH = 40;
	//
	public static final int BOX_LOOK_HEIGHT = 6;
	public static final int BOX_LOOK_WIDTH = 28;
	//
	public static final int BOX_STATS_HEIGHT = 6;
	public static final int BOX_STATS_WIDTH = 40;
	//
	public static final int BOX_ARENA_HEIGHT = 3;
	public static final int BOX_ARENA_WIDTH = 40;
	//
	public static final int BOX_GLOBAL_HEIGHT = 20;
	public static final int BOX_GLOBAL_WIDTH = 40;
	//
	public static final int BOX_RANK_HEIGHT = 20;
	public static final int BOX_RANK_WIDTH = 55;
	//
	public static final int BOX_ALLIANCE_RANK_HEIGHT = 15;
	public static final int BOX_ALLIANCE_RANK_WIDTH = 55;

	public final MessageBox rankBox = new MessageBox("Rank", BOX_RANK_WIDTH, BOX_RANK_HEIGHT);
	public final MessageBox allianceBox = new MessageBox("Alliance", BOX_ALLIANCE_RANK_WIDTH, BOX_ALLIANCE_RANK_HEIGHT);
	public final MessageBox globalBox = new MessageBox("Global", BOX_GLOBAL_WIDTH, BOX_GLOBAL_HEIGHT);
	private final MessageBox mapBox = new MessageBox("Map", MAP_VIEW_WIDTH, MAP_VIEW_HEIGHT);

	public final MessageBox lookBox = new MessageBox("Look", BOX_LOOK_WIDTH, BOX_LOOK_HEIGHT);
	public final MessageBox statsBox = new MessageBox("Stats", BOX_STATS_WIDTH, BOX_STATS_HEIGHT);
	public final MessageBox infoBox = new MessageBox("Info", BOX_INFO_WIDTH, BOX_INFO_HEIGHT);
	public final MessageBox arenaBox = new MessageBox("Arena", BOX_ARENA_WIDTH, BOX_ARENA_HEIGHT);
	private final MessageBox dummyMessageBox = new MessageBox("Message", BOX_MESSAGES_WIDTH, BOX_MESSAGES_HEIGHT);

	private final MapData map;
	private Player povPlayer;
	private Room povRoom;
	private int povRoomNum = 1;


	public ScreenRender(MapData map) {
		this.map = map;
	}

	@SafeVarargs
	private static List<String> combineColumns(List<String>... lists) {
		List<String> result = new LinkedList<>();
		int maxHeight = Arrays.stream(lists).mapToInt(List::size).max().orElse(0);
		List<Integer> widthList = Arrays.stream(lists).map(strings -> strings.get(0).length())
				.collect(Collectors.toList());
		for (int i = 0; i < maxHeight; i++) {
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j < lists.length; j++) {
				List<String> list = lists[j];
				if (i < list.size()) {
					sb.append(list.get(i));
				} else {
					sb.append(" ".repeat(widthList.get(j)));
				}
			}
			result.add(sb.toString());
		}
		return result;
	}

	@SafeVarargs
	private static List<String> combineRows(List<String>... lists) {
		List<String> result = new LinkedList<>();
		int maxWidth = Arrays.stream(lists).mapToInt(strings -> strings.get(0).length()).max().orElse(0);
		for(List<String> list : lists) {
			for (String s : list) {
				StringBuilder sb = new StringBuilder();
				sb.append(s);
				int length = MessageBox.stripAnsi(s).length();
				if (length < maxWidth) {
					sb.append(" ".repeat(maxWidth - length));
				}
				result.add(sb.toString());
			}
		}
		return result;
	}



	private void setRank(PlayerSet playerSet) {
		rankBox.clear();
		var sortedPlayers = playerSet.getPlayers().stream()
				.sorted((p1, p2) -> Long.compare(p2.power, p1.power))
				.collect(Collectors.toList());
		int singlePowerLength = String.valueOf(MAX_POWER).length();
		for (int i = 0; i < Math.min(BOX_RANK_HEIGHT, sortedPlayers.size()); i++) {
			var player = sortedPlayers.get(i);
			rankBox.addMessage(String.format("%s: %" +singlePowerLength + "d (%.2f, %.2f)", player.getFullName(), player.power, player.crit, player.grow));
		}

		allianceBox.clear();
		var alliancePower = playerSet.getAlliances().stream()
				.map(alliance -> {
					var alliancePlayers = playerSet.getPlayers().stream()
							.filter(p -> p.alliance.equals(alliance))
							.collect(Collectors.toList());
					var totalPower = alliancePlayers.stream().mapToLong(p -> p.power).sum();
					var avgCrit = calculateAverage(alliancePlayers, Player::getCrit);
					var avgGrow = calculateAverage(alliancePlayers, Player::getGrow);
					var avgAgg = calculateAverage(alliancePlayers, p -> p.getCharacteristic().getAggressive());
					var avgCz  = calculateAverage(alliancePlayers, p -> p.getCharacteristic().getCrazy());

					return Map.entry(alliance, new Object[]{totalPower, avgCrit, avgGrow, avgAgg, avgCz});
				})
				.sorted((e1, e2) -> {
					int compare = Integer.compare(e1.getKey().seed.index, e2.getKey().seed.index);
					if (compare != 0) {
						return compare;
					}
					return Long.compare((Long) e2.getValue()[0], (Long) e1.getValue()[0]);
				})
				.collect(Collectors.toList());

		int alliancePowerLength = String.valueOf(MAX_POWER * TOTAL_PLAYER / TOTAL_ALLIANCE).length();
		alliancePower.stream()
				.limit(BOX_ALLIANCE_RANK_HEIGHT)
				.forEach(entry -> allianceBox.addMessage(String.format("%s %s: %" + alliancePowerLength + "d Ct:%.2f Gr:%.2f Ag:%.2f, Cr:%.2f",
						entry.getKey().seed.name +
								(entry.getKey().seed.index < entry.getKey().prevSeed.index ? "↑" :
										entry.getKey().seed.index > entry.getKey().prevSeed.index ? "↓" : " "),
						entry.getKey().name,
						(Long) entry.getValue()[0],
						(Double) entry.getValue()[1],
						(Double) entry.getValue()[2],
						(Double) entry.getValue()[3],
						(Double) entry.getValue()[4])));

		arenaBox.clear();
		for (Seed seed : playerSet.getSeeds()) {
			int printCount = 9;
			String lastWinners = seed.winners.size() > printCount
					? String.join(" ", seed.winners.subList(seed.winners.size() - printCount, seed.winners.size()))
					: String.join(" ", seed.winners);
			arenaBox.addMessage(seed.name + (lastWinners.isEmpty() ? "" : ": " + lastWinners));
		}
	}

	private double calculateAverage(List<Player> players, ToDoubleFunction<Player> valueExtractor){
//		return players.stream()
//				.mapToDouble(p -> valueExtractor.applyAsDouble(p) * p.power)
//				.sum() / players.stream()
//				.mapToInt(p -> p.power)
//				.sum();
		return players.stream()
				.mapToDouble(valueExtractor)
				.average()
				.orElse(0);
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
					sb.append(rooms[x][y].render(povPlayer));
				}
			}
			mapBox.addMessage(sb.toString());
		}
	}


	public void look() {
		lookBox.clear();
		if (povRoom.boss != null) {
			lookBox.addMessage("Boss Pow: " + povRoom.boss.power + "; Reward: " + povRoom.boss.reward);
		}
		if (povRoom.altar != null && povRoom.altar.level > 0) {
			lookBox.addMessage("Altar lv." + povRoom.altar.level);
		}
		if (povRoom.lobby != null) {
			lookBox.addMessage("Lobby");
		}

		StringBuilder sb = new StringBuilder();
		for (Player player : povRoom.getPlayers()) {
			if (player != povPlayer) {
				sb.append(player.getFullName()).append(" ");
			}
		}
		if (!sb.isEmpty()) {
			lookBox.addMessage("Players: " + sb);
		}

		if (lookBox.isEmpty()) {
			lookBox.addMessage("Nothing");
		}
		statsBox.clear();
		if (povPlayer != null) {
			statsBox.addMessage("Name: " + povPlayer.getFullName());
			for (String s : povPlayer.characteristic.print() ) {
				statsBox.addMessage(s);
			}
			statsBox.addMessage(String.format("Power: %d/%d", povPlayer.power, povPlayer.maxPower));
			statsBox.addMessage(String.format("Crit: %.3f; Grow: %.3f", povPlayer.crit, povPlayer.grow));
		}
	}

	public void setPOV(Player player) {
		if (player.room == null) throw new IllegalArgumentException("Player is null");
		setPOV(player, player.room);
	}

	public void setVRoom(int num) {
		povRoomNum = num;
	}

	private void setPOV(Room room) {
		if (room == null) throw new IllegalArgumentException("Room is null");
		setPOV(null, room);
	}

	private void setPOV(@Nullable Player player, Room room) {
		this.povPlayer = player;
		this.povRoom = room;
	}

	private void updateEventBox(EventController eventController) {
		infoBox.clear();
		infoBox.addMessage("Turn: " + eventController.turnCount);
		if (map.arena.isOpen) {
			infoBox.addMessage("In Arena Event");
		}
	}

	public List<String> box(PlayerSet playerSet, EventController eventController) {
		if (povRoomNum == 1) {
			if (eventController.getPovRoom() != null) {
				setPOV(eventController.getPovRoom());
			} else {
				setPOV(playerSet.getPlayers().stream()
						.min((p1, p2) -> Long.compare(p2.power, p1.power))
						.orElseThrow(IllegalStateException::new));
			}
		}
		setRank(playerSet);
		updateMapView();
		look();
		updateEventBox(eventController);
		//
		List<String> messagePanel = povPlayer != null ? povPlayer.messageBox.box() : dummyMessageBox.box();
		List<String> infoPanel = combineColumns(messagePanel, lookBox.box());
		List<String> firstCol = combineRows(infoPanel, mapBox.box());
		//
		List<String> secondCol = combineRows(
				globalBox.box(),
				arenaBox.box(),
				infoBox.box(),
				statsBox.box());
		//
		List<String> thirdCol = combineRows(
				rankBox.box(),
				allianceBox.box());
		// Print combined panels
		return combineColumns(firstCol, secondCol, thirdCol);
	}
}
