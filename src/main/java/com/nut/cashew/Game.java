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

	public static void main(String[] args) throws IOException {
		Terminal terminal = TerminalBuilder.terminal();
		LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
		terminal.enterRawMode();

		MapData map = new MapData();
		PlayerSet playerSet = new PlayerSet(map);

		while (true) {
			Player player = playerSet.getCurPlayer();
			List<String> screenBox = player.box();
			terminal.puts(Capability.clear_screen);
			screenBox.forEach(s -> terminal.writer().println(s));
			terminal.flush();

			if (player.notHasAction()) {
				String line = reader.readLine("> ");
				if (line == null || line.trim().isEmpty()) continue;
				if (line.trim().equalsIgnoreCase("quit")) break;

				String command = line.trim();
				List<Pair<Integer, String>> pairs = parsePairs(command);
				if (pairs.size() == 1) {
					int num = pairs.get(0).getValue0();
					String action = pairs.get(0).getValue1();
					if ("p".equals(action)) {
						playerSet.setCurPlayer(num);
						continue;
					}
				}
				pairs.forEach(p -> {
					for (int i = 0; i < p.getValue0(); i++) {
						player.addAction(p.getValue1());
					}
				});
			}
			// do action for all players (AI included)
			playerSet.doAction();
		}

		terminal.writer().println("Goodbye!");
		terminal.flush();
		terminal.close();
	}

	public static List<Pair<Integer, String>> parsePairs(String input) {
		List<Pair<Integer, String>> result = new ArrayList<>();

		// Case: input starts with letter — treat as 1 + full string
		if (!Character.isDigit(input.charAt(0))) {
			result.add(new Pair<>(1, input));
			return result;
		}

		Matcher m = Pattern.compile("(\\d+)([^\\d]+)").matcher(input);
		while (m.find()) {
			int number = Integer.parseInt(m.group(1));
			String text = m.group(2);
			result.add(new Pair<>(number, text));
		}

		return result;
	}
}
