package com.nut.cashew.bud;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Q-learning trainer supporting two teams (A/B) and attack actions.
 * - State: (x, y, direction)
 * - Action: movement + direction + optional attack target
 * - Shared Q-table between both teams (alternate training)
 */
public class GameTrainer {

	// ========================= Hyperparameters =========================
	private static final int EPISODES = 2000;
	private static final int MAX_STEPS = 400;
	private static final double ALPHA = 0.10;          // learning rate
	private static final double GAMMA = 0.92;          // discount factor
	private static final double EPSILON_START = 1.00;  // exploration
	private static final double EPSILON_MIN = 0.05;
	private static final double EPSILON_DECAY = 0.996; // per episode

	private static final Random RNG = new Random(1337);

	// ==================================================================
	// Action spec (movement + direction + target)
	// ==================================================================
	private static final char[] MOVES = {'h', 'j', 'k', 'l', 's'}; // left, down, up, right, stay
	private static final char[] DIRS = {'n', 's', 'w', 'e'};

	/** Action specification used by the agent/Q-table (immutable). */
	@AllArgsConstructor
	@EqualsAndHashCode(of = {"move", "faceDir"})
	@ToString
	public static final class ActionSpec {
		public final char move;
		public final char faceDir;

		public Action toAction(Player self, Player opponent) {
			Action a = new Action();
			a.movement = move;
			a.direction = faceDir;
			if (self != null && opponent != null) {
				// always try to attack as possible
				a.target = opponent.name;
			}
			return a;
		}
	}

	/** All 40 actions (5 moves × 4 dirs × 2 attack options) */
	private static final List<ActionSpec> ALL_ACTIONS = buildAllActions();

	private static List<ActionSpec> buildAllActions() {
		List<ActionSpec> list = new ArrayList<>(MOVES.length * DIRS.length * 2);
		for (char m : MOVES)
			for (char d : DIRS)
				list.add(new ActionSpec(m, d));
		return Collections.unmodifiableList(list);
	}

	@AllArgsConstructor
	@EqualsAndHashCode(of = {"x", "y", "dir", "enemyCount"})
	public static final class StateKey {
		final int x, y;
		final char dir;
		final int enemyCount;
	}

	@AllArgsConstructor
	@EqualsAndHashCode(of = {"s", "a"})
	private static final class QKey {
		final StateKey s;
		final ActionSpec a;
	}

	/** Q-learning table with save/load. */
	public static final class RlAgent {
		private final Map<QKey, Double> q = new HashMap<>();

		public double getQ(StateKey s, ActionSpec a) {
			return q.getOrDefault(new QKey(s, a), 0.0);
		}

		public void setQ(StateKey s, ActionSpec a, double v) {
			q.put(new QKey(s, a), v);
		}

		/** ε-greedy over ALL_ACTIONS */
		public ActionSpec chooseAction(StateKey s, double epsilon) {
			if (RNG.nextDouble() < epsilon)
				return ALL_ACTIONS.get(RNG.nextInt(ALL_ACTIONS.size()));

			double best = Double.NEGATIVE_INFINITY;
			ActionSpec bestA = ALL_ACTIONS.get(0);
			for (ActionSpec a : ALL_ACTIONS) {
				double v = getQ(s, a);
				if (v > best) {
					best = v;
					bestA = a;
				}
			}
			return bestA;
		}

		public void saveCsv(File file) throws IOException {
			try (BufferedWriter w = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
				w.write("x,y,dir,enemyCount,move,face,q\n");
				for (Map.Entry<QKey, Double> e : q.entrySet()) {
					QKey k = e.getKey();
					double v = e.getValue();
					w.write(k.s.x + "," + k.s.y + "," + k.s.dir + "," + k.s.enemyCount + ","
							+ k.a.move + "," + k.a.faceDir + "," + v + "\n");
				}
			}
		}

		public static RlAgent loadCsv(File file) throws IOException {
			RlAgent agent = new RlAgent();
			if (!file.exists()) return agent;
			try (BufferedReader r = new BufferedReader(
					new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
				String line = r.readLine(); // header
				while ((line = r.readLine()) != null) {
					if (line.isBlank()) continue;
					String[] t = line.split(",", -1);
					int x = Integer.parseInt(t[0]);
					int y = Integer.parseInt(t[1]);
					char dir = t[2].charAt(0);
					int enemyCount = Integer.parseInt(t[3]);
					char move = t[4].charAt(0);
					char face = t[5].charAt(0);
					double qv = Double.parseDouble(t[6]);
					StateKey s = new StateKey(x, y, dir, enemyCount);
					ActionSpec a = new ActionSpec(move, face);
					agent.setQ(s, a, qv);
				}
			}
			return agent;
		}
	}

	public static final class QLAiController extends AiController {
		private final Player player;
		private final MapData map;
		private final RlAgent agent;

		public QLAiController(Player player, MapData map, String modelCsvPath) throws Exception {
			this.player = player;
			this.map = map;
			this.agent = RlAgent.loadCsv(new File(modelCsvPath));
		}

		@Override
		public void makeAction() {
			List<Player> opponents = player.findOpponents();

			GameTrainer.StateKey s = new GameTrainer.StateKey(
					player.getX(), player.getY(), Character.toLowerCase(player.direction), opponents.size());

			GameTrainer.ActionSpec a = agent.chooseAction(s, 0.0); // greedy policy

			player.addAction(a.toAction(player, !opponents.isEmpty() ? opponents.get(0) : null));
		}

		private List<Player> findOpponents(Player self) {
			return self.viewableRooms().stream()
					.flatMap(r -> r.getPlayers().stream())
					.filter(p -> !p.team.equals(self.team) && !p.dead)
					.collect(Collectors.toList());
		}
	}

	// ==================================================================
	// Training entry point
	// ==================================================================
	public static void main(String[] args) throws Exception {
		String modelPath = (args.length > 0 ? args[0] : "qtable.csv");

		MapData map = new MapData();
		map.init();

		Player playerA = map.players.stream().filter(p -> p.team.equals("A")).findFirst().orElseThrow();
		Player playerB = map.players.stream().filter(p -> p.team.equals("B")).findFirst().orElseThrow();

		RlAgent agent = RlAgent.loadCsv(new File(modelPath));

		double epsilon = EPSILON_START;

		for (int episode = 1; episode <= EPISODES; episode++) {
			String trainTeam = (episode % 2 == 0) ? "A" : "B";
			Player learner = trainTeam.equals("A") ? playerA : playerB;
			Player opponent = trainTeam.equals("A") ? playerB : playerA;

			map.init();
			learner.respawn();
			opponent.respawn();
			learner.health = 100;
			opponent.health = 100;
			learner.dead = false;
			opponent.dead = false;

			boolean done = false;
			int steps = 0;
			double episodeReward = 0;

			while (!done && steps < MAX_STEPS) {
				steps++;
				List<Player> opponents = learner.findOpponents();
				StateKey s = new StateKey(learner.getX(), learner.getY(), Character.toLowerCase(learner.direction), opponents.size());

				// --- Opponent random move or attack ---
				Action oppAct = new Action();
				oppAct.target = learner.name;
				char[] oppMoves = {'h', 'j', 'k', 'l', 's'};
				oppAct.movement = oppMoves[RNG.nextInt(oppMoves.length)];
				oppAct.direction = switch (oppAct.movement) {
					case 'h' -> 'w';
					case 'j' -> 's';
					case 'k' -> 'n';
					case 'l' -> 'e';
					default -> opponent.direction;
				};
				opponent.addAction(oppAct);

				// --- Learner action ---
				ActionSpec a = agent.chooseAction(s, epsilon);
				learner.addAction(a.toAction(learner, opponent));

				// Perform actions
				Room before = learner.getCurrentRoom();
				int bx = learner.getX(), by = learner.getY();

				learner.doAction();
				opponent.doAction();

				Room after = learner.getCurrentRoom();
				int ax = (after != null ? after.x : bx);
				int ay = (after != null ? after.y : by);
				char newDir = Character.toLowerCase(a.faceDir);
				List<Player> newOpponents = learner.findOpponents();

				map.updateThroneControl();

				// --- Reward ---
				double reward;
				boolean moved = (bx != ax) || (by != ay);
				String winningTeam = map.getWinner();

				if (learner.dead) {
					reward = -100; // death
					done = true;
				} else if (opponent.dead) {
					reward = 100; // kill opponent
					done = true;
				} else if (winningTeam != null) {
					reward = winningTeam.equals(learner.team) ? 100 : -100;
					done = true;
				} else if (a.toAction(learner, opponent).target != null) {
					reward = 15; // attack action
				} else if (after == null) {
					reward = -5;
				} else if (map.throne.getPlayers().stream().anyMatch(p -> p.team.equals(learner.team))) {
					reward = 10;
				} else if (map.throne.getPlayers().stream().anyMatch(p -> p.team.equals(opponent.team))) {
					reward = -5;
				} else {
					double d0 = map.distToThrone(before);
					double d1 = map.distToThrone(after);
					double progress = (d0 - d1);
					reward = -1.0 + 0.6 * progress + (moved ? 0.0 : -2.0);
				}

				episodeReward += reward;

				// --- Q-update ---
				StateKey s2 = new StateKey(ax, ay, newDir, newOpponents.size());
				double bestNext = Double.NEGATIVE_INFINITY;
				for (ActionSpec ap : ALL_ACTIONS) {
					bestNext = Math.max(bestNext, agent.getQ(s2, ap));
				}
				double oldQ = agent.getQ(s, a);
				double newQ = oldQ + ALPHA * (reward + GAMMA * (done ? 0.0 : bestNext) - oldQ);
				agent.setQ(s, a, newQ);
			}

			epsilon = Math.max(EPSILON_MIN, epsilon * EPSILON_DECAY);
			System.out.printf("Episode %4d | Team=%s | Steps=%3d | R=%.2f | eps=%.3f%n",
					episode, trainTeam, Math.min(steps, MAX_STEPS), episodeReward, epsilon);

			if (episode % 100 == 0 || episode == EPISODES) {
				try {
					agent.saveCsv(new File(modelPath));
				} catch (IOException ioe) {
					System.err.println("WARN: failed to save Q-table: " + ioe.getMessage());
				}
			}
		}

		System.out.println("✅ Training finished. Q-table saved.");
	}
}
