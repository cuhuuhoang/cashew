package com.nut.cashew.seed;


import com.nut.cashew.root.MessageBox;
import com.nut.cashew.seed.room.Boss;
import com.nut.cashew.seed.room.Treasure;
import lombok.Getter;
import org.javatuples.Pair;

import java.util.*;

import static com.nut.cashew.seed.Const.*;
import static com.nut.cashew.seed.ScreenRender.*;

public class Player {
	@Getter
	public int x;
	@Getter
	public int y;

	public final MessageBox messageBox;
	public final MessageBox globalBox;
	public String name;
	public final MapData map;
	public final AiController aiController;
	public final Queue<String> nextAction;
	@Getter
	public final Characteristic characteristic;
	public Room room;

	@Getter
	public double crit = 1.0;
	@Getter
	public double grow = 1.0;
	@Getter
	public long power = 100;
	public long maxPower = MAX_POWER;
	public boolean reachedMax = false;
	public final Alliance alliance;

	public Player(MapData map, String name, MessageBox globalBox, Characteristic characteristic, Alliance alliance) {
		this.name = name;
		this.map = map;
		this.alliance = alliance;
		this.aiController = new AiController(this);
		this.messageBox = new MessageBox("Messages", BOX_MESSAGES_WIDTH, BOX_MESSAGES_HEIGHT);
		this.nextAction = new ArrayDeque<>();
		this.globalBox = globalBox;
		this.characteristic = characteristic;
		this.room = null;
		//
	}

	public void respawn() {
		message("Respawn");
		nextAction.clear();
		Random rand = new Random();
		if (this.room != null) {
			this.room.removePlayer(this);
		}
		while (true) {
			double angle = rand.nextDouble() * 2 * Math.PI;
			int radius = 40 + rand.nextInt(11); // 40–50
			int px = (int) Math.round(Math.cos(angle) * radius);
			int py = (int) Math.round(Math.sin(angle) * radius);
			Pair<Boolean, String> result = tryMove(px, py, true);
			if (result.getValue0()) {
				this.x = px;
				this.y = py;
				map.getRoom(px, py).addPlayer(this);
				room = map.getRoom(px, py);
				break;
			}
		}
	}

	public Pair<Boolean, String> tryMove(int x, int y, boolean respawn) {
		if (!map.inMap(x, y)) {
			return new Pair<>(false, "Out of map");
		}
		if (map.getRoom(x, y).altar != null && power < map.getRoom(x, y).altar.entryPower()) {
			return new Pair<>(false, "Not enough power");
		}
		if (map.getRoom(x, y).altar != null && !map.getRoom(x, y).altar.isOpen) {
			return new Pair<>(false, "Altar is closed");
		}
		if (map.getRoom(x, y).arena != null) {
			return new Pair<>(false, "Can not move to Arena");
		}
		if (map.getRoom(x, y).lobby != null) {
			return new Pair<>(false, "Can not move to Wait Room");
		}
		if (!respawn) {
			if (map.getRoom(x, y).arena != null) {
				return new Pair<>(false, "Can not move out of Arena");
			}
			if (map.getRoom(x, y).lobby != null) {
				return new Pair<>(false, "Can not move out of Wait Room");
			}
		}
		return new Pair<>(true, "Moved " + x + "," + y);
	}

	public void moveToArena() {
		moveNoCheck(map.arena.room);
	}

	public void moveToLobby() {
		moveNoCheck(map.lobby.room);
	}

	private void moveNoCheck(Room room) {
		moveNoCheck(room.x, room.y);
	}

	private void moveNoCheck(int newX, int newY) {
		map.getRoom(x, y).removePlayer(this);
		x = newX;
		y = newY;
		map.getRoom(x, y).addPlayer(this);
		this.room = map.getRoom(x, y);
	}

	public void move(int dx, int dy) {
		int newX = x + dx;
		int newY = y + dy;
		Pair<Boolean, String> result = tryMove(newX, newY, false);
		if (result.getValue0()) {
			moveNoCheck(newX, newY);
		}
		message(result.getValue1());
	}

	public void increaseCrit(double increase) {
		if (alliance.leader == this) {
			crit += increase;
		} else {
			alliance.leader.crit += increase * alliance.taxRate;
			crit += increase * (1 - alliance.taxRate);
		}
	}

	public void increaseGrow(double increase) {
		if (alliance.leader == this) {
			grow += increase;
		} else {
			alliance.leader.grow += increase * alliance.taxRate;
			grow += increase * (1 - alliance.taxRate);
		}
	}

	public void checkReset() {
		if (reachedMax) {
			reachedMax = false;
			double ratio = switch (alliance.seed.index) {
				case 0 -> 1.0;
				case 1 -> 0.7;
				case 2 -> 0.5;
				default -> 0;
			};
			long resetPower = (long) (RESET_POWER_GAIN * ratio);
			increaseMaxPower(resetPower);
			power = BASE_POWER;
			message("Power reset");
		}
	}

	public void increaseMaxPower(long increase) {
		if (alliance.leader == this) {
			maxPower += increase;
		} else {
			alliance.leader.maxPower += (long) (increase * alliance.taxRate);
			maxPower += (long) (increase * (1 - alliance.taxRate));
		}
	}

	public void attack(String targetName) {
		if (map.getRoom(x, y).boss != null && "boss".equalsIgnoreCase(targetName) ) {
			double dame = power * crit;
			Boss boss = map.getRoom(x, y).boss;
			if (dame >= boss.power) {
				double reward = boss.reward + boss.finalReward;
				increaseGrow(reward);
				map.bosses.remove(boss);
				boss.room.boss = null;
				message("killed boss, gain " + String.format("%.5f", reward));
			} else {
				double reward = boss.reward * dame / boss.power;
				increaseGrow(reward);
				boss.power -= (long) dame;
				boss.reward -= reward;
				message("attacked boss, gain " + String.format("%.5f", reward));
			}
		}
		if (map.getRoom(x, y).altar == null && map.getRoom(x, y).arena == null) {
			message("Can not attack open field");
			return;
		}
		if (map.getRoom(x, y).altar != null && map.getRoom(x,y).altar.level <= MAX_ALTAR_SAFE_LEVEL) {
			message("Can not attack at safe altar");
			return;
		}
		for (Player target : map.getRoom(x, y).getPlayers()) {
			if (target.name.equalsIgnoreCase(targetName)) {
				if (target.alliance.name.equalsIgnoreCase(alliance.name)) {
					message("Can not attack same alliance");
					return;
				}
				double damage = Math.min(target.power / target.crit, power / crit);
				this.power -= Math.min((int) (damage * target.crit), power);
				target.power -= Math.min((int) (damage * crit), target.power);
				if (getCurrentRoom().altar != null && getCurrentRoom().altar.level > MAX_ALTAR_SAFE_LEVEL) {
					int altarLevel = getCurrentRoom().altar.level;
					double critIncrease = (double) altarLevel * altarLevel / 1000d;
					increaseCrit(critIncrease);
					target.increaseCrit(critIncrease);
				}
				checkRespawn();
				target.checkRespawn();
				String message = String.format("%s attacked %s, damage %d", name, targetName, (int) damage);
				message(message);
				return;
			}
		}
		message("Target disappeared");
	}

	public void sit() {
		message("Sit");
		if (getCurrentRoom().altar != null) {
			getCurrentRoom().altar.players.add(name);
		}
	}

	public Room getCurrentRoom() {
		return map.getRoom(x, y);
	}

	public void takeTreasure() {
		Room room = map.getRoom(x, y);
		if (room.treasure != null) {
			Treasure treasure = room.treasure;
			double reward = treasure.reward;
			map.treasures.remove(treasure);
			room.treasure = null;
			message("Take treasure " + String.format("%.2f", reward));
			increaseGrow(reward);
		} else {
			message("No treasure");
		}
	}

	public void checkRespawn() {
		Room room = map.getRoom(x, y);
		if (room.arena != null && power < BASE_POWER) {
			power = alliance.seed.getPower(name);
			moveToLobby();
		}
		if (power < BASE_POWER) {
			message("Low power " + power);
			power = BASE_POWER;
			respawn();
		}
		if (room.altar != null && room.altar.level > 0) {
			if (power < room.altar.entryPower()) {
				respawn();
			}
		}
	}

	public String getFullName() {
		return "[" + alliance.name + "]" + name;
	}


	public void message(String message) {
		messageBox.addMessage(message);
	}

	public void addAction(String command) {
		nextAction.add(command);
	}

	public String getAction() {
		return nextAction.poll();
	}

	public void doAction(String command) {
		if (command == null || command.trim().isEmpty()) {
			message("No action");
			return;
		}
		if (command.startsWith("attack ") || command.startsWith("att ") ) {
			String[] items = command.split(" ");
			if (items.length < 2) {
				message("No target");
			}
			attack(items[1]);
			return;
		}
		switch (command) {
			case "h" -> move(-1, 0);
			case "j" -> move(0, 1);
			case "k" -> move(0, -1);
			case "l" -> move(1, 0);
			case "respawn" -> respawn();
			case "treasure" -> takeTreasure();
			case "sit" -> sit();
			default -> message("Unknown command: " + command);
		}
	}

}
