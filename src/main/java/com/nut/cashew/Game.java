package com.nut.cashew;

import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Game {

	public static void main(String[] args) throws IOException, InterruptedException {
		Terminal terminal = TerminalBuilder.terminal();
		LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
		terminal.enterRawMode();

		MapData map = new MapData();
		ScreenRender screenRender = new ScreenRender();
		PlayerSet playerSet = new PlayerSet(map, screenRender);
		EventController eventController = new EventController(map, playerSet.getPlayers(), screenRender);
		AtomicInteger autoSpeed = new AtomicInteger(100);

		Thread inputThread = getInputThread(reader, autoSpeed, playerSet);
		inputThread.start();

		while (true) {
			Player player = playerSet.getCurPlayer();
			List<String> screenBox = screenRender.box(player, map, playerSet, eventController);
			terminal.puts(Capability.clear_screen);
			screenBox.forEach(s -> terminal.writer().println(s));
			terminal.flush();

			playerSet.doAction();
			Thread.sleep(autoSpeed.get());
			eventController.eventCheck();
			playerSet.startTurn();
		}
	}

	private static @NotNull Thread getInputThread(LineReader reader, AtomicInteger autoSpeed, PlayerSet playerSet) {
		Thread inputThread = new Thread(() -> {
			while (true) {
				try {
					String line = reader.readLine("> ");
					if (line == null) continue;

					String command = line.trim();
					if (command.isEmpty()) {
						autoSpeed.set(100);
						continue;
					}
					if (command.equalsIgnoreCase("q")) {
						System.exit(0);
					}
					Pair<Integer, String> pair = parseSinglePair(command);
					int num = pair.getValue0();
					String action = pair.getValue1();

					if ("p".equals(action) || action.isEmpty()) {
						playerSet.setCurPlayer(num);
					} else if ("s".equals(action)) {
						autoSpeed.set(0);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		inputThread.setDaemon(true);
		return inputThread;
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
