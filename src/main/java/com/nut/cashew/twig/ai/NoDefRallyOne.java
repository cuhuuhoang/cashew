package com.nut.cashew.twig.ai;

public class NoDefRallyOne extends AiController {

	@Override
	public void makeAction() {
		action_clearDefend();
		action_rallyOneTroop();
	}
}

