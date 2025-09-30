package com.nut.cashew.twig.ai;

import com.nut.cashew.twig.MapData;
import com.nut.cashew.twig.Room;
import com.nut.cashew.twig.room.Castle;
import com.nut.cashew.twig.room.Troop;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static com.nut.cashew.twig.room.Castle.MAX_CONCURRENT_ATTACK;

public abstract class AiController {

	protected Castle castle;
	protected MapData map;

	public void init(Castle castle, MapData map) {
		this.castle = castle;
		this.map = map;
	}

	private static final Map<Class<? extends AiController>, Integer> controllers = Map.of(
			NoDefFullRally.class, 1,
			NoDefRallyOne.class, 1,
			FullDefRallyOne.class, 1,
			FullDefFullRally.class, 1,
			OneDefFullRally.class, 1,
			OneDefRallyOne.class, 1
	);

	public static AiController create() {
		try {
			int totalWeight = controllers.values().stream().mapToInt(Integer::intValue).sum();
			int r = new Random().nextInt(totalWeight);
			for (var entry : controllers.entrySet()) {
				r -= entry.getValue();
				if (r < 0) {
					return entry.getKey().getDeclaredConstructor().newInstance();
				}
			}
			throw new IllegalStateException("Failed to select controller");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String name() {
		return this.getClass().getSimpleName();
	}

	public abstract void makeAction();

	protected Room nearestRoom(List<Room> rooms, Room room) {
		if (rooms == null || rooms.isEmpty()) return null;
		rooms.sort((r1, r2) -> Double.compare(
				distance(r1.x, r1.y, room.x, room.y),
				distance(r2.x, r2.y, room.x, room.y)
		));
		return rooms.get(0);
	}

	protected double distance(int x1, int y1, int x2, int y2) {
		return Math.abs(x2 - x1) + Math.abs(y2 - y1);
	}

	// ACTION PARTS
	public void action_fullDefend() {
		List<Troop> troops = castle.getFreeTroops();
		List<Castle> castles = castle.getAllCastles();
		for (Castle castle1 : castles) {
			int maxAttack = castle1.attackers.stream()
					.map(attack -> attack.troops.size())
					.max(Comparator.comparing(attack -> attack))
					.orElse(0);
			fillDefend(castle1, troops, maxAttack);
		}
	}

	protected void action_oneDefend() {
		List<Troop> troops = castle.getFreeTroops();
		List<Castle> castles = castle.getAllCastles();
		for (Castle castle1 : castles) {
			int needDefend = castle1.attackers.isEmpty() ? 0 : 1;
			fillDefend(castle1, troops, needDefend);
		}
	}

	private void fillDefend(Castle castle, List<Troop> troops, int needDefend) {
		if (castle.defend.troops.size() > needDefend) {
			for (int i = 0; i < castle.defend.troops.size() - needDefend; i++) {
				castle.defend.troops.remove(0);
			}
		} else {
			int diff = needDefend - castle.defend.troops.size();
			for (int i = 0; i < Math.min(troops.size(), diff); i++) {
				castle.defend.troops.add(troops.get(i));
			}
		}
	}

	public void action_clearDefend() {
		List<Castle> castles = castle.getAllCastles();
		for (Castle castle1 : castles) {
			castle1.defend.troops.clear();
		}
	}

	private Room getCenterRoom(List<Troop> troops) {
		int centerX = 0;
		int centerY = 0;
		for (Troop troop : troops) {
			centerX += troop.room.x;
			centerY += troop.room.y;
		}
		centerX /= troops.size();
		centerY /= troops.size();
		//
		return map.getRoom(centerX, centerY);
	}

	public void action_rallyOneTroop() {
		if (isDominated()) {
			action_rallyFullTroop();
			return;
		}
		while (true) {
			List<Troop> troops = castle.getFreeTroops();
			if (troops.isEmpty()) {
				return;
			}
			List<Room> attackable = getAttackable(troops, false);
			if (attackable.isEmpty()) {
				return;
			}
			Room centerRoom = getCenterRoom(troops);
			Room nearestAttackable = nearestRoom(attackable, centerRoom);
			//
			List<Room> rallyPoints = getRallyPoints(nearestAttackable.castle, false);
			if (rallyPoints.isEmpty()) {
				return;
			}
			Room nearestRallyPoint = nearestRoom(rallyPoints, nearestAttackable);
			doAttack(troops, nearestRallyPoint.castle, nearestAttackable.castle, 1);
		}
	}

	private boolean isDominated() {
		return castle.maxRally() > (double) map.castles.size() * 0.95;
	}

	protected void action_rallyNTroop(int n) {
		if (isDominated()) {
			action_rallyFullTroop();
			return;
		}
		while (true) {
			List<Troop> troops = castle.getFreeTroops();
			if (troops.size() < n) {
				return;
			}
			List<Room> attackable = getAttackable(troops, false);
			if (attackable.isEmpty()) {
				return;
			}
			Room centerRoom = getCenterRoom(troops);
			Room nearestAttackable = nearestRoom(attackable, centerRoom);
			//
			List<Room> rallyPoints = getRallyPoints(nearestAttackable.castle, true);
			if (rallyPoints.isEmpty()) {
				return;
			}
			Room nearestRallyPoint = nearestRoom(rallyPoints, nearestAttackable);
			doAttack(troops, nearestRallyPoint.castle, nearestAttackable.castle, n);
		}
	}

	public void action_rallyFullTroop() {
		while (true) {
			List<Troop> troops = castle.getFreeTroops();
			if (troops.isEmpty()) {
				return;
			}
			List<Room> attackable = getAttackable(troops, true);
			if (attackable.isEmpty()) {
				return;
			}
			Room centerRoom = getCenterRoom(troops);
			Room nearestAttackable = nearestRoom(attackable, centerRoom);
			//
			List<Room> rallyPoints = getRallyPoints(nearestAttackable.castle, true);
			if (rallyPoints.isEmpty()) {
				return;
			}
			Room nearestRallyPoint = nearestRoom(rallyPoints, nearestAttackable);
			doAttack(troops, nearestRallyPoint.castle, nearestAttackable.castle, nearestAttackable.castle.maxRally() + 1);
		}
	}

	private List<Room> getAttackable(List<Troop> freeTroops, boolean checkTargetMaxRally) {
		Set<Castle> attacked = castle.getAllCastles().stream()
				.map(c -> c.attacks)
				.flatMap(Collection::stream)
				.map(a -> a.target)
				.collect(Collectors.toSet());
		var s = map.castles.stream()
				.filter(c -> !attacked.contains(c))
				.filter(c -> c.getMaster() != castle)
				.filter(c -> c.attackers.stream().noneMatch(t -> t.attacker == castle));
		if (checkTargetMaxRally) {
			s = s.filter(c -> freeTroops.size() > c.maxRally());
		}
		return s.map(c -> c.room).collect(Collectors.toList());
	}

	private List<Room> getRallyPoints(Castle target, boolean checkTargetMaxRally) {
		var s = castle.getAllCastles().stream()
				.filter(c -> !c.isMine)
				.filter(c -> c.attacks.size() < MAX_CONCURRENT_ATTACK);
		if (checkTargetMaxRally) {
			s = s.filter(c -> c.maxRally() > target.maxRally());
		}
		return s
				.map(c -> c.room)
				.collect(Collectors.toList());
	}

	private void doAttack(List<Troop> freeTroops, Castle from, Castle target, int count) {
		List<Troop> attackTroops = get_nearestTroop(freeTroops, from.room, count);
		checkState(from.getMaster() == castle);
		from.attack(target, attackTroops);
	}

	private List<Troop> get_nearestTroop(List<Troop> troops, Room room, int count) {
		if (troops == null || troops.isEmpty()) return null;
		troops.sort((t1, t2) -> Double.compare(
				distance(t1.room.x, t1.room.y, room.x, room.y),
				distance(t2.room.x, t2.room.y, room.x, room.y)
		));
		return troops.subList(0, Math.min(troops.size(), count));
	}
}
