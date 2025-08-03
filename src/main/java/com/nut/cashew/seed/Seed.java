package com.nut.cashew.seed;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

public class Seed {
	public int index;
	public String name;

	private final Map<String, Long> powerMap = new HashMap<>();
	public final Stack<Alliance> remainAlliances = new Stack<>();
	public final Stack<Alliance> rankedAlliances = new Stack<>(); // the worst the earlier
	public final List<String> winners = new LinkedList<>();

	public boolean isOpen = false;

	public Seed(int index, String name) {
		this.index = index;
		this.name = name;
	}

	private Map<Alliance, Long> getAlliancePower(List<Player> players) {
		Map<Alliance, Long> alliancePowerMap = new HashMap<>();
		players.forEach(p -> alliancePowerMap.merge(p.alliance, p.power, Long::sum));
		return alliancePowerMap;
	}

	private List<Alliance> sortAlliancesByPower(Map<Alliance, Long> alliancePowerMap) {
		return alliancePowerMap.entrySet()
				.stream()
				.sorted(Map.Entry.<Alliance, Long>comparingByValue().reversed())
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());
	}

	public void registerPlayers(List<Player> players) {
		powerMap.clear();
		remainAlliances.clear();
		rankedAlliances.clear();
		players.forEach(p -> powerMap.put(p.name, p.power));

		Map<Alliance, Long> alliancePowerMap = getAlliancePower(players);
		List<Alliance> sortedAlliances = sortAlliancesByPower(alliancePowerMap);
		sortedAlliances.forEach(remainAlliances::push);
	}

	public long getPower(String player) {
		checkState(powerMap.containsKey(player));
		return powerMap.get(player);
	}
}
