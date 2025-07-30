package com.nut.cashew;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import static com.nut.cashew.Const.*;

public class ScreenRender {

	public static final int MAP_VIEW_WIDTH = 60;
	public static final int MAP_VIEW_HEIGHT = 30;

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
	public static final int BOX_GLOBAL_HEIGHT = 20;
	public static final int BOX_GLOBAL_WIDTH = 40;
	//
	public static final int BOX_RANK_HEIGHT = 20;
	public static final int BOX_RANK_WIDTH = 50;
	//
	public static final int BOX_ALLIANCE_RANK_HEIGHT = 10;
	public static final int BOX_ALLIANCE_RANK_WIDTH = 50;

	public final MessageBox rankBox = new MessageBox("Rank", BOX_RANK_WIDTH, BOX_RANK_HEIGHT);
	public final MessageBox allianceBox = new MessageBox("Alliance", BOX_ALLIANCE_RANK_WIDTH, BOX_ALLIANCE_RANK_HEIGHT);
	public final MessageBox globalBox = new MessageBox("Global", BOX_GLOBAL_WIDTH, BOX_GLOBAL_HEIGHT);
	private final MessageBox mapBox = new MessageBox("Map", MAP_VIEW_WIDTH, MAP_VIEW_HEIGHT);

	private static List<String> combineColumns(List<List<String>> lists) {
		List<String> result = new LinkedList<>();
		int maxHeight = lists.stream().mapToInt(List::size).max().orElse(0);
		List<Integer> widthList = lists.stream().map(strings -> strings.get(0).length())
				.collect(Collectors.toList());
		for (int i = 0; i < maxHeight; i++) {
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j < lists.size(); j++) {
				List<String> list = lists.get(j);
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

	private static List<String> combineRows(List<List<String>> lists) {
		List<String> result = new LinkedList<>();
		int maxWidth = lists.stream().mapToInt(strings -> strings.get(0).length()).max().orElse(0);
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
				.sorted((p1, p2) -> Integer.compare(p2.power, p1.power))
				.collect(Collectors.toList());
		int singlePowerLength = String.valueOf(MAX_POWER).length();
		for (int i = 0; i < Math.min(BOX_RANK_HEIGHT, sortedPlayers.size()); i++) {
			var player = sortedPlayers.get(i);
			rankBox.addMessage(String.format("[%s]%s: %" +singlePowerLength + "d (%.2f, %.2f)", player.alliance.name, player.name, player.power, player.crit, player.grow));
		}

		allianceBox.clear();
		var alliancePower = playerSet.getAlliances().stream()
				.map(alliance -> {
					var alliancePlayers = playerSet.getPlayers().stream()
							.filter(p -> p.alliance.equals(alliance))
							.collect(Collectors.toList());
					var totalPower = alliancePlayers.stream().mapToInt(p -> p.power).sum();
					var avgCrit = calculateWeightedAverage(alliancePlayers, Player::getCrit);
					var avgGrow = calculateWeightedAverage(alliancePlayers, Player::getGrow);
					var avgAgg = calculateWeightedAverage(alliancePlayers, p -> p.getCharacteristic().getAggressive());
					var avgCz  = calculateWeightedAverage(alliancePlayers, p -> p.getCharacteristic().getCrazy());

					return Map.entry(alliance, new Object[]{totalPower, avgCrit, avgGrow, avgAgg, avgCz});
				})
				.sorted((e1, e2) -> Integer.compare((Integer) e2.getValue()[0], (Integer) e1.getValue()[0]))
				.collect(Collectors.toList());

		int alliancePowerLength = String.valueOf(MAX_POWER * TOTAL_PLAYER / TOTAL_ALLIANCE).length();
		alliancePower.stream()
				.limit(BOX_ALLIANCE_RANK_HEIGHT)
				.forEach(entry -> allianceBox.addMessage(String.format("%s: %" + alliancePowerLength + "d Ct:%.2f Gr:%.2f Ag:%.2f, Cr:%.2f)",
						entry.getKey().name,
						(Integer) entry.getValue()[0],
						(Double) entry.getValue()[1],
						(Double) entry.getValue()[2],
						(Double) entry.getValue()[3],
						(Double) entry.getValue()[4])));

	}

	private double calculateWeightedAverage(List<Player> players, ToDoubleFunction<Player> valueExtractor){
		return players.stream()
				.mapToDouble(p -> valueExtractor.applyAsDouble(p) * p.power)
				.sum() / players.stream()
				.mapToInt(p -> p.power)
				.sum();
	}

	public void updateMapView(Player player, MapData map) {
		mapBox.clear();
		Room[][] rooms = new Room[MAP_VIEW_WIDTH][MAP_VIEW_HEIGHT];
		int startX = player.getX() - MAP_VIEW_WIDTH / 2;
		int startY = player.getY() - MAP_VIEW_HEIGHT / 2;

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
					sb.append(rooms[x][y].render(player));
				}
			}
			mapBox.addMessage(sb.toString());
		}
	}

	public List<String> box(Player player, MapData map, PlayerSet playerSet, EventController eventController) {
		setRank(playerSet);
		updateMapView(player, map);
		//
		player.look();
		//
		List<String> infoPanel = combineColumns(List.of(player.messageBox.box(), player.lookBox.box()));
		List<String> firstCol = combineRows(List.of(infoPanel, mapBox.box()));
		//
		List<String> secondCol = new LinkedList<>();
		secondCol.addAll(globalBox.box());
		secondCol.addAll(eventController.infoBox.box());
		secondCol.addAll(player.statsBox.box());
		//
		List<String> thirdCol = new LinkedList<>();
		thirdCol.addAll(rankBox.box());
		thirdCol.addAll(allianceBox.box());

		// Print combined panels
		return combineColumns(List.of(firstCol, secondCol, thirdCol));
	}
}
