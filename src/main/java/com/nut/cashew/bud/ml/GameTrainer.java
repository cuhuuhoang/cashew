package com.nut.cashew.bud.ml;

import com.nut.cashew.bud.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Direction-aware Q-learning for reaching the throne.
 * - State: (x, y, facingDirection)
 * - Action: (movement in {h,j,k,l,s}, new facingDirection in {n,s,e,w})
 * - Save/load Q-table as CSV for reuse during gameplay.
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
	// Action spec (movement + direction)
	// ==================================================================
	private static final char[] MOVES = {'h', 'j', 'k', 'l', 's'}; // left, down, up, right, stay
	private static final char[] DIRS  = {'n', 's', 'w', 'e'};

	/** Action specification used by the agent/Q-table (immutable). */
	public static final class ActionSpec {
		public final char move;     // h j k l s
		public final char faceDir;  // n s w e
		public ActionSpec(char move, char faceDir) { this.move = move; this.faceDir = faceDir; }
		public Action toAction() { Action a = new Action(); a.movement = move; a.direction = faceDir; return a; }
		@Override public String toString() { return "" + move + ":" + faceDir; }
		@Override public boolean equals(Object o){
			if (this == o) return true; if (!(o instanceof ActionSpec)) return false;
			ActionSpec other = (ActionSpec) o; return move == other.move && faceDir == other.faceDir;
		}
		@Override public int hashCode(){ return Objects.hash(move, faceDir); }
	}

	/** All 20 action combinations (5 moves × 4 facing directions). */
	private static final List<ActionSpec> ALL_ACTIONS = buildAllActions();
	private static List<ActionSpec> buildAllActions() {
		List<ActionSpec> list = new ArrayList<>(MOVES.length * DIRS.length);
		for (char m : MOVES) for (char d : DIRS) list.add(new ActionSpec(m, d));
		return Collections.unmodifiableList(list);
	}

	// ==================================================================
	// Q-table keyed by state (x,y,dir) and ActionSpec
	// ==================================================================
	public static final class StateKey {
		final int x, y; final char dir;
		StateKey(int x, int y, char dir) { this.x = x; this.y = y; this.dir = dir; }
		@Override public boolean equals(Object o){
			if (this == o) return true; if (!(o instanceof StateKey)) return false;
			StateKey s = (StateKey) o; return x == s.x && y == s.y && dir == s.dir;
		}
		@Override public int hashCode(){ return Objects.hash(x, y, dir); }
	}

	private static final class QKey {
		final StateKey s; final ActionSpec a;
		QKey(StateKey s, ActionSpec a){ this.s = s; this.a = a; }
		@Override public boolean equals(Object o){
			if (this == o) return true; if (!(o instanceof QKey)) return false;
			QKey k = (QKey) o; return Objects.equals(s, k.s) && Objects.equals(a, k.a);
		}
		@Override public int hashCode(){ return Objects.hash(s, a); }
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

		/** ε-greedy over ALL_ACTIONS; returns full ActionSpec (movement + direction). */
		public ActionSpec chooseAction(StateKey s, double epsilon) {
			if (RNG.nextDouble() < epsilon) {
				return ALL_ACTIONS.get(RNG.nextInt(ALL_ACTIONS.size()));
			}
			double best = Double.NEGATIVE_INFINITY;
			ActionSpec bestA = ALL_ACTIONS.get(0);
			for (ActionSpec a : ALL_ACTIONS) {
				double v = getQ(s, a);
				if (v > best) { best = v; bestA = a; }
			}
			return bestA;
		}

		/** Greedy action (no exploration) for deployment. */
		public ActionSpec bestAction(StateKey s) { return chooseAction(s, 0.0); }

		// ---------- Persistence (CSV: x,y,dir,move,faceDir,q) ----------
		public void saveCsv(File file) throws IOException {
			try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
				w.write("x,y,dir,move,face,q\n");
				for (Map.Entry<QKey, Double> e : q.entrySet()) {
					QKey k = e.getKey(); double v = e.getValue();
					w.write(k.s.x + "," + k.s.y + "," + k.s.dir + "," + k.a.move + "," + k.a.faceDir + "," + v + "\n");
				}
			}
		}
		public static RlAgent loadCsv(File file) throws IOException {
			RlAgent agent = new RlAgent();
			if (!file.exists()) return agent; // empty table ok
			try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
				String line = r.readLine(); // header
				while ((line = r.readLine()) != null) {
					if (line.isBlank()) continue;
					String[] t = line.split(",", -1);
					int x = Integer.parseInt(t[0]);
					int y = Integer.parseInt(t[1]);
					char dir = t[2].charAt(0);
					char move = t[3].charAt(0);
					char face = t[4].charAt(0);
					double qv = Double.parseDouble(t[5]);
					StateKey s = new StateKey(x, y, dir);
					ActionSpec a = new ActionSpec(move, face);
					agent.setQ(s, a, qv);
				}
			}
			return agent;
		}
	}

	// ==================================================================
	// Distance shaping helper
	// ==================================================================
	private static double distToThrone(MapData map, Room room) {
		if (map.throne == null || room == null) return 0.0;
		double dx = map.throne.x - room.x;
		double dy = map.throne.y - room.y;
		return Math.sqrt(dx*dx + dy*dy);
	}

	// ==================================================================
	// Training entry point
	// ==================================================================
	public static void main(String[] args) throws Exception {
		String modelPath = (args.length > 0 ? args[0] : "qtable.csv");

		MapData map = new MapData();
		map.init();
		Player player = map.players.get(0);

		RlAgent agent = RlAgent.loadCsv(new File(modelPath)); // warm-start if exists

		double epsilon = EPSILON_START;

		for (int episode = 1; episode <= EPISODES; episode++) {
			// reset world
			player.respawn();
			boolean done = false;
			int steps = 0;
			double episodeReward = 0;

			while (!done && steps < MAX_STEPS) {
				steps++;
				StateKey s = new StateKey(player.getX(), player.getY(), Character.toLowerCase(player.direction));

				// choose and enqueue full Action
				ActionSpec a = agent.chooseAction(s, epsilon);
				player.addAction(a.toAction());

				// before/after for reward
				Room before = player.getCurrentRoom();
				int bx = player.getX(), by = player.getY();
				player.doAction();
				Room after = player.getCurrentRoom();
				int ax = (after != null ? after.x : bx);
				int ay = (after != null ? after.y : by);
				char newDir = Character.toLowerCase(a.faceDir);

				// update throne control
				map.updateThroneControl();

				// reward shaping
				double reward;
				boolean moved = (bx != ax) || (by != ay);
				if (after == null) {
					reward = -8;         // fell out of map / invalid
				} else if (after.throne) {
					// Check who currently owns the throne
					String winningTeam = map.getWinner();

					if (winningTeam != null) {
						// Someone has already won (could be us or the opponent)
						if (winningTeam.equals(player.team)) {
							// our team won
							reward = 100;
						} else {
							// enemy team won
							reward = -50; // optional penalty for losing
						}
						done = true; // episode ends when any team wins

					} else if (map.throne.getPlayers().stream().anyMatch(p -> p.team.equals(player.team))) {
						// our team is currently on throne but not yet won
						reward = 10.0; // per-turn reward for staying on throne
					} else {
						// we're on the throne but not counted yet (transition turn)
						reward = 5.0;
					}
				} else {
					double d0 = distToThrone(map, before);
					double d1 = distToThrone(map, after);
					double progress = (d0 - d1);         // positive when closer
					reward = -1.0 /* step cost */ + 0.6 * progress + (moved ? 0.0 : -2.0);
				}
				episodeReward += reward;

				// Q-update: max_a' Q(s', a')
				StateKey s2 = new StateKey(ax, ay, newDir);
				double bestNext = Double.NEGATIVE_INFINITY;
				for (ActionSpec ap : ALL_ACTIONS) {
					bestNext = Math.max(bestNext, agent.getQ(s2, ap));
				}
				double oldQ = agent.getQ(s, a);
				double newQ = oldQ + ALPHA * (reward + GAMMA * (done ? 0.0 : bestNext) - oldQ);
				agent.setQ(s, a, newQ);
			}

			epsilon = Math.max(EPSILON_MIN, epsilon * EPSILON_DECAY);

			System.out.printf("Episode %4d | steps=%3d | R=%.2f | eps=%.3f%n", episode, Math.min(steps, MAX_STEPS), episodeReward, epsilon);

			// Save periodically (and at end).
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
