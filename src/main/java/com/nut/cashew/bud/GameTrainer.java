package com.nut.cashew.bud;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Q-learning trainer supporting two teams (A/B) and attack actions.
 * - State: (x, y, direction, enemyCount, distBucket)
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
	private static final double EPSILON_MIN = 0.10;    // keep exploring longer
	private static final double EPSILON_DECAY = 0.999; // slower decay for exploration

	private static final Random RNG = new Random(1337);

	// ==================================================================
	// Action spec (movement + direction)
	// ==================================================================
	private static final char[] MOVES = {'h', 'j', 'k', 'l', 's'}; // left, down, up, right, stay
	private static final char[] DIRS = {'n', 's', 'w', 'e'};

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
				// only attack if opponent is visible
				boolean canSee = self.viewableRooms().stream()
						.flatMap(r -> r.getPlayers().stream())
						.anyMatch(p -> p.name.equals(opponent.name));
				if (canSee) {
					a.target = opponent.name;
				}
			}
			return a;
		}
	}

	private static final List<ActionSpec> ALL_ACTIONS = buildAllActions();

	private static List<ActionSpec> buildAllActions() {
		List<ActionSpec> list = new ArrayList<>(MOVES.length * DIRS.length);
		for (char m : MOVES)
			for (char d : DIRS)
				list.add(new ActionSpec(m, d));
		return Collections.unmodifiableList(list);
	}

	// ==================================================================
	// StateKey includes throne distance bucket
	// ==================================================================
	@AllArgsConstructor
	@EqualsAndHashCode(of = {"x", "y", "dir", "enemyCount", "distBucket", "turnBucket"})
	public static final class StateKey {
		final int x, y;
		final char dir;
		final int enemyCount;
		final int distBucket;
		final int turnBucket;
	}


	@AllArgsConstructor
	@EqualsAndHashCode(of = {"s", "a"})
	private static final class QKey {
		final StateKey s;
		final ActionSpec a;
	}

	// ==================================================================
	// Q-learning table with save/load
	// ==================================================================
	public static final class RlAgent {
		private final Map<QKey, Double> q = new HashMap<>();

		public double getQ(StateKey s, ActionSpec a) {
			return q.getOrDefault(new QKey(s, a), 0.0);
		}

		public void setQ(StateKey s, ActionSpec a, double v) {
			q.put(new QKey(s, a), v);
		}

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
			File tmpFile = new File(file.getParent(), file.getName() + ".tmp");
			Files.deleteIfExists(tmpFile.toPath());
			try (BufferedWriter w = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(tmpFile), StandardCharsets.UTF_8))) {

				w.write("x,y,dir,enemyCount,distBucket,turnBucket,move,face,q\n");

				for (Map.Entry<QKey, Double> e : q.entrySet()) {
					QKey k = e.getKey();
					double v = e.getValue();

					w.write(k.s.x + "," +
							k.s.y + "," +
							k.s.dir + "," +
							k.s.enemyCount + "," +
							k.s.distBucket + "," +
							k.s.turnBucket + "," +
							k.a.move + "," +
							k.a.faceDir + "," +
							v + "\n");
				}

				w.flush();
			}
			Files.deleteIfExists(file.toPath());
			if (!tmpFile.renameTo(file)) {
				throw new IOException("Failed to rename temp file");
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
					int distBucket = Integer.parseInt(t[4]);
					int turnBucket = Integer.parseInt(t[5]);
					char move = t[6].charAt(0);
					char face = t[7].charAt(0);
					double qv = Double.parseDouble(t[8]);
					StateKey s = new StateKey(x, y, dir, enemyCount, distBucket, turnBucket);
					ActionSpec a = new ActionSpec(move, face);
					agent.setQ(s, a, qv);
				}
			}
			return agent;
		}
	}

	// ==================================================================
	// QLAiController (for using trained model)
	// ==================================================================
	public static final class QLAiController extends AiController {
		private final Player player;
		private final MapData map;
		private final RlAgent agent;
		private int turnCounter = 0;

		public QLAiController(Player player, MapData map) throws Exception {
			this.player = player;
			this.map = map;
			this.agent = RlAgent.loadCsv(new File("qtable.csv"));
		}

		@Override
		public void makeAction() {
			turnCounter++;
			List<Player> opponents = player.findOpponents();
			double dist = map.distToThrone(player.getCurrentRoom());
			int distBucket = (int) (dist / 3);
			int turnBucket = turnCounter / 10;

			GameTrainer.StateKey s = new GameTrainer.StateKey(
					player.getX(), player.getY(), Character.toLowerCase(player.direction),
					opponents.size(), distBucket, turnBucket);

			GameTrainer.ActionSpec a = agent.chooseAction(s, 0.0);
			player.addAction(a.toAction(player, !opponents.isEmpty() ? opponents.get(0) : null));
		}
	}

	// ==================================================================
	// Training entry point
	// ==================================================================
	public static void main(String[] args) throws Exception {
		String modelPath = "qtable.csv";

		MapData map = new MapData();
		map.init();

		RlAgent agent = RlAgent.loadCsv(new File(modelPath));

		double epsilon = EPSILON_START;

		for (int episode = 1; episode <= EPISODES; episode++) {
			String trainTeam = MapData.TEAMS.get(episode % MapData.TEAMS.size());
			Player learner = map.players.stream().filter(p -> p.team.equals(trainTeam)).findFirst().orElseThrow();
			List<Player> opponents = map.players.stream().filter(p -> !p.team.equals(trainTeam)).collect(Collectors.toList());

			map.init();
			map.players.forEach(player -> {
				player.respawn();
				player.health = Player.MAX_HEALTH;
				player.dead = false;
			});

			boolean done = false;
			int steps = 0;

			while (!done && steps < MAX_STEPS) {
				steps++;
				List<Player> targets = learner.findOpponents();

				double dist = map.distToThrone(learner.getCurrentRoom());
				int distBucket = (int) (dist / 3);
				int turnBucket = (int) (steps / 10);

				StateKey s = new StateKey(
						learner.getX(), learner.getY(), Character.toLowerCase(learner.direction),
						targets.size(), distBucket, turnBucket);

				// --- Opponent random move ---
				for (Player opponent : opponents) {
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
				}

				// --- Learner action (ε-greedy + persistent noise) ---
				double effectiveEpsilon = epsilon + RNG.nextDouble() * 0.05; // small extra noise
				ActionSpec a;

				if (RNG.nextDouble() < 0.02) {
					// Force random exploration every ~2% of steps
					a = ALL_ACTIONS.get(RNG.nextInt(ALL_ACTIONS.size()));
				} else {
					a = agent.chooseAction(s, Math.min(1.0, effectiveEpsilon));
				}

				learner.addAction(a.toAction(learner, targets.isEmpty() ? null : targets.get(0)));

				// Perform actions
				Room before = learner.getCurrentRoom();
				int bx = learner.getX(), by = learner.getY();

				double actionScore = learner.doAction();
				opponents.forEach(Player::doAction);

				Room after = learner.getCurrentRoom();
				int ax = (after != null ? after.x : bx);
				int ay = (after != null ? after.y : by);
				char newDir = Character.toLowerCase(a.faceDir);
				List<Player> newOpponents = learner.findOpponents();

				map.updateThroneControl();

				// --- Reward with stronger gradient & absolute distance bonus ---
				double reward;
				boolean moved = (bx != ax) || (by != ay);
				String winningTeam = map.getWinner();

				if (learner.dead) {
					reward = -100;
					done = true;
				} else if (winningTeam != null) {
					reward = winningTeam.equals(learner.team) ? 100 : -100;
					done = true;
				} else if (after == null) {
					reward = -5;
				} else if (after.throne) {
					reward = 20;
				} else if (after.lava) {
					reward = -50;
				} else {
					double d0 = map.distToThrone(before);
					double d1 = map.distToThrone(after);
					double progress = (d0 - d1);
					reward = -0.1 + 5.0 * progress + (moved ? 0.0 : -0.5);
					reward += 20.0 / (1.0 + d1); // proximity bonus
				}
				reward += actionScore;

				double newDist = map.distToThrone(learner.getCurrentRoom());
				int newDistBucket = (int) (newDist / 3);
				int newTurnBucket = (int) (steps / 10);
				StateKey s2 = new StateKey(ax, ay, newDir, newOpponents.size(), newDistBucket, newTurnBucket);
				double bestNext = Double.NEGATIVE_INFINITY;
				for (ActionSpec ap : ALL_ACTIONS) {
					bestNext = Math.max(bestNext, agent.getQ(s2, ap));
				}
				double oldQ = agent.getQ(s, a);
				double newQ = oldQ + ALPHA * (reward + GAMMA * (done ? 0.0 : bestNext) - oldQ);
				agent.setQ(s, a, newQ);
			}

			epsilon = Math.max(EPSILON_MIN, epsilon * EPSILON_DECAY);

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
