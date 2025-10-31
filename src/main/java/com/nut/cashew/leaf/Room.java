package com.nut.cashew.leaf;

import com.nut.cashew.root.Utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

public class Room {
	public final List<Bot> bots;
	public Factory factory;

	public final int x;
	public final int y;

	private int mineLevel;
	public Map<Integer, Integer> resources;

	public void constructFactory(Bot bot, MapData map) {
		checkState(factory == null, "Factory already constructed");
		checkState(mineLevel > 0, "No mine here");
		checkState(bot.level >= mineLevel, "Bot level too low");
		checkState(bot.room == this, "Bot not in this room");
		consumeForFactory();
		Bot newBot = new Bot(map, Utils.generateRandomAnsiColor());
		factory = new Factory(newBot);
		newBot.respawn(this);
		map.bots.add(newBot);
	}

	private void consumeForFactory() {
		int level = factory == null ? 1 : factory.level + 1;
		int needed = (int) Math.pow(2, level);
		for (int i = 1; i <= level; i++) {
			int actual = resources.get(i);
			if (actual < needed) {
				throw new RuntimeException("Not enough resources");
			}
			resources.put(i, actual - needed);
		}
	}

	public void upgradeFactory() {
		checkState(factory != null, "No factory here");
		consumeForFactory();
		factory.level++;
		factory.bot.level++;
	}

	public Room(int x, int y) {
		this.x = x;
		this.y = y;
		this.bots = new LinkedList<>();
		this.factory = null;
		this.mineLevel = 0;
		this.resources = new HashMap<>();
	}

	public int scanMineLevel(Bot bot) {
		int distance = Utils.distance(x, y, bot.x, bot.y);
		checkState(distance <= bot.level, "Too far");
		if (mineLevel > bot.level + 1) {
			return 0;
		}
		return mineLevel;
	}

	public Map<Integer, Integer> scanResources(Bot bot) {
		int distance = Utils.distance(x, y, bot.x, bot.y);
		checkState(distance <= bot.level, "Too far");
		Map<Integer, Integer> result = new HashMap<>();
		for (int i = 1; i <= bot.level + 1; i++) {
			int actual = resources.getOrDefault(i, 0);
			result.put(i, actual);
		}
		return result;
	}

	public void setMine(int mineLevel) {
		this.mineLevel = mineLevel;
		this.resources.put(mineLevel, Integer.MAX_VALUE / 2);
	}

	public String render() {
		if (factory != null) {
			try {
				return factory.render();
			} catch (Exception ignored) {
			}
		}
		if (!bots.isEmpty()) {
			try {
				return bots.get(0).render();
			} catch (Exception ignored) {
			}
		}
		if (mineLevel > 0) {
			return "\u001B[31m" + Math.min(mineLevel, 9) + "\u001B[0m";
		}
		return "\u001B[32m.\u001B[0m";
	}

	public void addBot(Bot bot) {
		bots.add(bot);
	}

	public void removeBot(Bot bot) {
		bots.remove(bot);
	}
}
