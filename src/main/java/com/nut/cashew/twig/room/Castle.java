package com.nut.cashew.twig.room;

import com.nut.cashew.twig.ai.AiController;
import com.nut.cashew.twig.Room;
import com.nut.cashew.twig.action.Attack;
import com.nut.cashew.twig.action.Defend;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static com.google.common.base.Preconditions.checkState;
import static com.nut.cashew.root.Utils.generateRandomAnsiColor;

public class Castle {

	public static final int MAX_CONCURRENT_ATTACK = 1;

	public final String name;
	public final Room room;
	public final String colorCode;
	public final boolean isMine;

	private Castle master;
	public List<Castle> slaves;
	public List<Troop> troops;

	public List<Attack> attacks;
	public List<Attack> attackers;
	public Defend defend;

	private int troopCount;

	public final AiController aiController;

	public int computeTroopCount() {
		troopCount = troops.size();
		slaves.forEach(castle -> {
			troopCount += castle.computeTroopCount();
		});
		return troopCount;
	}

	public int maxRally() {
		if (isMine && master == null) {
			return 0;
		}
		if (master == null) {
			return troopCount;
		} else {
			return master.troopCount;
		}
	}

	public Castle(Room room, String name, boolean isMine) {
		this.name = name;
		this.room = room;
		this.isMine = isMine;
		this.colorCode = isMine ? "\u001B[38;2;255;255;255m" : generateRandomAnsiColor();
		this.master = null;
		this.slaves = new ArrayList<>();
		this.troops = new ArrayList<>();
		this.attacks = new ArrayList<>();
		this.attackers = new ArrayList<>();
		this.defend = new Defend();
		//
		this.troops.add(new Troop(room, this));
		this.aiController = AiController.create();
		this.troopCount = troops.size();
	}

	public void cancelAttack(Attack attack) {
		checkState(attacks.contains(attack));
		attacks.remove(attack);
		checkState(attack.target.attackers.contains(attack));
		attack.target.attackers.remove(attack);
	}

	public void attack(Castle target, List<Troop> troops) {
		if (isMine) {
			return;
		}
		if (attacks.size() > MAX_CONCURRENT_ATTACK) {
			return;
		}
		if (troops.size() > maxRally()) {
			return;
		}
		Attack attack = new Attack(this, target);
		attack.troops.addAll(troops);
		attacks.add(attack);
		target.attackers.add(attack);
	}

	public String render() {
		if (isMine) {
			return colorer("⛏");
		} else {
			return colorer("♛");
		}
	}

	public String colorer(String text) {
		if (master != null) {
			return master.colorer(text);
		}
		return colorCode + text + "\u001B[0m";
	}

	public Castle getMaster() {
		if (master == null) {
			return this;
		}
		return master.getMaster();
	}

	public List<Troop> getFreeTroops() {
		List<Troop> troops = new LinkedList<>(getAllTroops());
		List<Castle> castles = getAllCastles();
		for (Castle castle : castles) {
			for (Attack attack : castle.attacks) {
				for (Troop troop : attack.troops) {
					troops.remove(troop);
				}
			}
			for (Troop troop : castle.defend.troops) {
				troops.remove(troop);
			}
		}
		return troops;
	}

	public List<Troop> getAllTroops() {
		List<Troop> troops = new LinkedList<>(this.troops);
		for (Castle castle : slaves) {
			troops.addAll(castle.getAllTroops());
		}
		return troops;
	}

	public List<Castle> getAllCastles() {
		List<Castle> castles = new LinkedList<>();
		castles.add(this);
		for (Castle castle : slaves) {
			castles.addAll(castle.getAllCastles());
		}
		return castles;
	}

	private void removeTroop(List<Troop> toRemove) {
		for (Troop troop : toRemove) {
			defend.troops.remove(troop);
			for (Attack attack : attacks) {
				attack.troops.remove(troop);
			}
		}
		slaves.forEach(castle -> castle.removeTroop(toRemove));
	}

	public void changeMaster(Castle master) {
		// cancel all attacks
		List<Castle> allCastles = new LinkedList<>(getAllCastles());
		for (Castle castle : allCastles) {
			List<Attack> attackList = new LinkedList<>(castle.attacks);
			for (Attack attack : attackList) {
				castle.cancelAttack(attack);
			}
		}
		// remove all troops
		List<Troop> allTroops = new LinkedList<>(getAllTroops());
		if (this.master != null) {
			this.master.slaves.remove(this);
			getMaster().removeTroop(allTroops);
		}

		for (Troop troop : allTroops) {
			defend.troops.remove(troop);
		}
		// change
		this.master = master;
		this.master.slaves.add(this);
	}

}

