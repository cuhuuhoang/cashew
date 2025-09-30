package com.nut.cashew.twig.ai;

public class NoDefFullRally extends AiController {


	@Override
	public void makeAction() {
		action_clearDefend();
		action_rallyFullTroop();
	}
}
