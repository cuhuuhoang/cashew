package com.nut.cashew.bud;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * Multi-agent Q-learning trainer for Cashew Bud 5v5 game.
 * Each player learns its own Q-table independently.
 */
public class Ter {

	// ========================= Hyperparameters =========================
	private static final int EPISODES = 1500;
	private static final int MAX_STEPS = 400;
	private static final double ALPHA = 0.10;          // learning rate
	private static final double GAMMA = 0.92;          // discount factor
	private static final double EPSILON_START = 1.0;   // exploration
	private static final double EPSILON_MIN = 0.10;
	private static final double EPSILON_DECAY = 0.999; // per episode

	private static final Random RNG = new Random(42);

	// ==================================================================
	// Action spec (movement + direction)
	// ==================================================================
	private static final char[] MOVES = {'h', 'j', 'k', 'l', 's'};
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
				// attack if opponent visible
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
		List<ActionSpec> list = new ArrayList<>();
		for (char m : MOVES)
			for (char d : DIRS)
				list.add(new ActionSpec(m, d));
		return Collections.unmodifiableList(list);
	}

	// ==================================================================
	// State
	// ==================================================================
	@AllArgsConstructor
	@EqualsAndHashCode(of = {"x", "y", "dir", "enemyCount", "distBucket"})
	public static final class StateKey {
		final int x, y;
		final char dir;
		final int enemyCount;
		final int distBucket;
	}

	@AllArgsConstructor
	@EqualsAndHashCode(of = {"s", "a"})
	private static final class QKey {
		final StateKey s;
		final ActionSpec a;
	}

	// ==================================================================
	// Q-learning agent (independent per player)
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
			File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
			try (BufferedWriter w = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))) {
				w.write("x,y,dir,enemyCount,distBucket,move,face,q\n");
				for (Map.Entry<QKey, Double> e : q.entrySet()) {
					QKey k = e.getKey();
					double v = e.getValue();
					w.write(k.s.x + "," + k.s.y + "," + k.s.dir + "," + k.s.enemyCount + "," + k.s.distBucket + ","
							+ k.a.move + "," + k.a.faceDir + "," + v + "\n");
				}
			}
			if (!tempFile.renameTo(file)) {
				if (!file.delete() || !tempFile.renameTo(file)) {
					throw new IOException("Failed to rename temp file to " + file);
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
					int distBucket = Integer.parseInt(t[4]);
					char move = t[5].charAt(0);
					char face = t[6].charAt(0);
					double qv = Double.parseDouble(t[7]);
					StateKey s = new StateKey(x, y, dir, enemyCount, distBucket);
					ActionSpec a = new ActionSpec(move, face);
					agent.setQ(s, a, qv);
				}
			}
			return agent;
		}
	}

	// ==================================================================
	// Reward computation (shared helper)
	// ==================================================================
	private static double computeReward(Player p, MapData map, Room before, Room after, double actionScore) {
		if (p.dead) return -100;
		String winningTeam = map.getWinner();
		if (winningTeam != null) {
			return winningTeam.equals(p.team) ? 100 : -100;
		}
		if (after == null) return -5;

		double d0 = map.distToThrone(before);
		double d1 = map.distToThrone(after);
		double progress = (d0 - d1);
		double reward = -0.1 + 5.0 * progress + 20.0 / (1.0 + d1);
		reward += actionScore;
		return reward;
	}

	// ==================================================================
	// Main training entry
	// ==================================================================
	public static void main(String[] args) throws Exception {
		String modelDir = (args.length > 0 ? args[0] : "agents");
		new File(modelDir).mkdirs();

		MapData baseMap = new MapData();
		baseMap.init();

		List<Player> allPlayers = new ArrayList<>(baseMap.players);

		// Use a thread pool (8 threads)
		ExecutorService executor = Executors.newFixedThreadPool(10);

		List<Future<?>> futures = new ArrayList<>();

		for (Player p : allPlayers) {
			Future<?> f = executor.submit(() -> {
				try {
					trainAgentForPlayer(p, modelDir);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			futures.add(f);
		}

		// Wait for all threads to finish
		for (Future<?> f : futures) f.get();

		executor.shutdown();
		System.out.println("✅ All agents finished training!");
	}

	private static void trainAgentForPlayer(Player prototype, String modelDir) throws Exception {
		String name = prototype.name;
		File modelFile = new File(modelDir, name + "_q.csv");

		// Create isolated environment per agent
		MapData map = new MapData();
		map.init();

		Player player = map.players.stream().filter(p -> p.name.equals(name)).findFirst().orElseThrow();
		RlAgent agent = RlAgent.loadCsv(modelFile);

		double epsilon = EPSILON_START;

		for (int episode = 1; episode <= EPISODES; episode++) {
			map.init();
			map.players.forEach(Player::respawn);
			map.players.forEach(p -> { p.health = Player.MAX_HEALTH; p.dead = false; });

			boolean done = false;
			int steps = 0;
			double episodeReward = 0;

			while (!done && steps < MAX_STEPS) {
				steps++;
				List<Player> enemies = player.findOpponents();
				double dist = map.distToThrone(player.getCurrentRoom());
				int distBucket = (int) (dist / 3);

				StateKey s = new StateKey(player.getX(), player.getY(),
						Character.toLowerCase(player.direction), enemies.size(), distBucket);

				Player target = enemies.isEmpty() ? null : enemies.get(RNG.nextInt(enemies.size()));
				ActionSpec a = agent.chooseAction(s, epsilon);
				player.addAction(a.toAction(player, target));
				Room before = player.getCurrentRoom();

				double actionScore = player.doAction();
				map.updateThroneControl();
				Room after = player.getCurrentRoom();

				double reward = computeReward(player, map, before, after, actionScore);
				episodeReward += reward;

				List<Player> newEnemies = player.findOpponents();
				double newDist = map.distToThrone(player.getCurrentRoom());
				int newDistBucket = (int) (newDist / 3);
				StateKey s2 = new StateKey(player.getX(), player.getY(),
						Character.toLowerCase(player.direction), newEnemies.size(), newDistBucket);

				double bestNext = Double.NEGATIVE_INFINITY;
				for (ActionSpec ap : ALL_ACTIONS)
					bestNext = Math.max(bestNext, agent.getQ(s2, ap));

				double oldQ = agent.getQ(s, a);
				double newQ = oldQ + ALPHA * (reward + GAMMA * bestNext - oldQ);
				agent.setQ(s, a, newQ);

				if (map.getWinner() != null)
					done = true;
			}

			epsilon = Math.max(EPSILON_MIN, epsilon * EPSILON_DECAY);
			if (episode % 200 == 0 || episode == EPISODES)
				agent.saveCsv(modelFile);
		}
	}

}
