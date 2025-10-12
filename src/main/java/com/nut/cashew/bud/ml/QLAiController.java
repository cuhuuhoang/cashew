package com.nut.cashew.bud.ml;

import com.nut.cashew.bud.*;

import java.io.File;

import static com.nut.cashew.bud.ml.GameTrainer.*;
public class QLAiController extends AiController {

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
		StateKey s = new StateKey(player.getX(), player.getY(), Character.toLowerCase(player.direction));
		ActionSpec a = agent.bestAction(s);   // greedy
		player.addAction(a.toAction());
	}

	public static AiController create(Player p, MapData m, String modelCsvPath) throws Exception {
		return new QLAiController(p, m, modelCsvPath);
	}
}
