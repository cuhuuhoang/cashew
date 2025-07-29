package com.nut.cashew;

import org.javatuples.Pair;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Game {

	public static void main(String[] args) throws IOException, InterruptedException {
		Terminal terminal = TerminalBuilder.terminal();
		LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
		terminal.enterRawMode();

		MapData map = new MapData();
		PlayerSet playerSet = new PlayerSet(map);

		AtomicBoolean autoRunning = new AtomicBoolean(false);
		AtomicInteger autoTurnCount = new AtomicInteger(0);
		AtomicInteger autoSpeed = new AtomicInteger(100);
		Object lock = new Object();

		Thread inputThread = new Thread(() -> {
			autoTurnCount.set(9999999);
			autoRunning.set(true);
			synchronized (lock) {
				lock.notifyAll(); // wake main thread
			}
			while (true) {
				try {
					String line = reader.readLine("> ");
					if (line == null) continue;

					String command = line.trim();
					if (command.isEmpty()) {
						autoSpeed.set(100);
						continue;
					}
					if (command.equalsIgnoreCase("quit")) {
						System.exit(0);
					}
					if (command.equalsIgnoreCase("stop")) {
						autoRunning.set(false);
						continue;
					}
					Pair<Integer, String> pair = parseSinglePair(command);
					int num = pair.getValue0();
					String action = pair.getValue1();

					if ("p".equals(action) || action.isEmpty()) {
						playerSet.setCurPlayer(num);
					} else if ("a".equals(action)) {
						autoTurnCount.set(num);
						autoRunning.set(true);
						synchronized (lock) {
							lock.notifyAll(); // wake main thread
						}
					} else if ("s".equals(action)) {
						autoSpeed.set(0);
					} else {
//						Player player = playerSet.getCurPlayer();
//						for (int i = 0; i < num; i++) {
//							player.addAction(action);
//						}
						synchronized (lock) {
							lock.notifyAll(); // in case we need immediate step
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		inputThread.setDaemon(true);
		inputThread.start();

		while (true) {
			Player player = playerSet.getCurPlayer();
			List<String> screenBox = player.box(playerSet);
			terminal.puts(Capability.clear_screen);
			screenBox.forEach(s -> terminal.writer().println(s));
			if (autoRunning.get()) {
				terminal.writer().println("Auto: " + autoTurnCount.get());
			}
			terminal.flush();

			if (!autoRunning.get() && player.notHasAction()) {
				synchronized (lock) {
					lock.wait(); // wait for input
				}
			} else {
				playerSet.addAction();
				playerSet.doAction();
				Thread.sleep(autoSpeed.get());
				playerSet.startTurn();

				if (autoRunning.get()) {
					if (autoTurnCount.decrementAndGet() <= 0) {
						autoRunning.set(false);
					}
				}
			}
		}
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
