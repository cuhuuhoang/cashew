package com.nut.cashew.twig.ai.strategies;

import com.nut.cashew.twig.ai.AiController;
import com.nut.cashew.twig.ai.Strategy;

public class NoDef extends Strategy {

	@Override
	public void accept(AiController aiController) {
		aiController.action_clearDefend();
	}
}
