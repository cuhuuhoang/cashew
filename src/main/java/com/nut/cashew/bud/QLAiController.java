package com.nut.cashew.bud;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class QLAiController extends AiController {

	private static final Random RNG = new Random(42);

	private final Player player;
	private final MapData map;
	private final Ter.RlAgent agent;

	public QLAiController(Player player, MapData map, String modelDir) throws Exception {
		this.player = player;
		this.map = map;

		File modelFile = new File(modelDir, player.name + "_q.csv");
		if (modelFile.exists()) {
			this.agent = Ter.RlAgent.loadCsv(modelFile);
			System.out.println("✅ Loaded Q-table for " + player.name);
		} else {
			System.err.println("⚠️ No Q-table found for " + player.name + ", starting empty.");
			this.agent = new Ter.RlAgent();
		}
	}

	@Override
	public void makeAction() {
		if (player.dead) return;

		List<Player> opponents = player.viewableRooms().stream()
				.flatMap(r -> r.getPlayers().stream())
				.filter(p -> !p.team.equals(player.team) && !p.dead)
				.collect(Collectors.toList());

		double dist = map.distToThrone(player.getCurrentRoom());
		int distBucket = (int) (dist / 3);

		Ter.StateKey s = new Ter.StateKey(
				player.getX(),
				player.getY(),
				Character.toLowerCase(player.direction),
				opponents.size(),
				distBucket
		);

		Ter.ActionSpec act = agent.chooseAction(s, 0.0);
		Player target = opponents.isEmpty() ? null : opponents.get(RNG.nextInt(opponents.size()));
		player.addAction(act.toAction(player, target));
	}
}
