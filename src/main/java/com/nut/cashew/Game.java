package com.nut.cashew;

import org.javatuples.Pair;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

import org.jline.utils.InfoCmp.Capability;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Game {

	public static void main(String[] args) throws IOException, InterruptedException {
		Terminal terminal = TerminalBuilder.terminal();
		LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
		terminal.enterRawMode();

		MapData map = new MapData();
		PlayerSet playerSet = new PlayerSet(map);
		int autoTurnCount = 0;

		while (true) {

			// print current user
			Player player = playerSet.getCurPlayer();
			List<String> screenBox = player.box(playerSet);
			terminal.puts(Capability.clear_screen);
			screenBox.forEach(s -> terminal.writer().println(s));
			if (autoTurnCount > 0) {
				terminal.writer().println("Auto: " + autoTurnCount);
			}
			terminal.flush();

			if (player.notHasAction() && autoTurnCount-- <= 0) {
				String line = reader.readLine("> ");
				if (line == null || line.trim().isEmpty()) continue;
				if (line.trim().equalsIgnoreCase("quit")) break;

				String command = line.trim();
				Pair<Integer, String> pair = parseSinglePair(command);
				int num = pair.getValue0();
				String action = pair.getValue1();
				if ("p".equals(action)) {
					playerSet.setCurPlayer(num);
					continue;
				}
				if ("a".equals(action)) {
					autoTurnCount=num;
					continue;
				}
				for (int i = 0; i < pair.getValue0(); i++) {
					player.addAction(pair.getValue1());
				}
			}
			// do action for all players (AI included)
			playerSet.doAction();
			//
			Thread.sleep(300);
			// start turn
			playerSet.startTurn();
		}

		terminal.writer().println("Goodbye!");
		terminal.flush();
		terminal.close();
	}

	public static Pair<Integer, String> parseSinglePair(String input) {
		Matcher m = Pattern.compile("^(\\d+)(.*)$").matcher(input);
		if (m.matches()) {
			return new Pair<>(Integer.parseInt(m.group(1)), m.group(2));
		} else {
			return new Pair<>(1, input);
		}
	}
}
