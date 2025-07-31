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

		AtomicInteger autoSpeed = new AtomicInteger(100);
		MapData map = new MapData();
		ScreenRender screenRender = new ScreenRender(map);
		PlayerSet playerSet = new PlayerSet(map, screenRender);
		screenRender.setPOV(playerSet.getPlayers().get(0));
		EventController eventController = new EventController(map, playerSet.getPlayers(), screenRender, autoSpeed);

		Thread inputThread = getInputThread(reader, autoSpeed, playerSet, screenRender, eventController);
		inputThread.start();

		while (true) {
			List<String> screenBox = screenRender.box(playerSet, eventController);
			terminal.puts(Capability.clear_screen);
			screenBox.forEach(s -> terminal.writer().println(s));
			terminal.flush();

			playerSet.doAction();
			Thread.sleep(autoSpeed.get());
			eventController.eventCheck();
		}
	}

	private static @NotNull Thread getInputThread(LineReader reader, AtomicInteger autoSpeed, PlayerSet playerSet, ScreenRender screenRender, EventController eventController) {
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
						screenRender.setVRoom(0);
						screenRender.setPOV(playerSet.getPlayers().get(num));
					} else if ("v".equals(action)) {
						screenRender.setVRoom(num);
					} else if ("s".equals(action)) {
						autoSpeed.set(0);
					} else if ("n".equals(action)) {
						autoSpeed.set(0);
						eventController.slowNextEvent = true;
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
