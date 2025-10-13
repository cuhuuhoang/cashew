package com.nut.cashew.bud;


import com.nut.cashew.root.Utils;
import org.javatuples.Pair;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

public class Game {
	public static final int DEFAULT_SPEED = 500;
	private static void handleInput(String line, AtomicInteger autoSpeed, ScreenRender screenRender, EventController eventController) {
		if (line == null) return;

		String command = line.trim();
		if (command.isEmpty()) {
			autoSpeed.set(DEFAULT_SPEED);
			return;
		}
		if (command.equalsIgnoreCase("q")) {
			System.exit(0);
		}
		Pair<Integer, String> pair = Utils.parsePromptPair(command);
		int num = pair.getValue0();
		String action = pair.getValue1();

		if ("s".equals(action)) {
			autoSpeed.set(0);
		} if ("h".equals(action)) {
			screenRender.moveLeft();
		} else if ("j".equals(action)) {
			screenRender.moveDown();
		} else if ("k".equals(action)) {
			screenRender.moveUp();
		} else if ("l".equals(action)) {
			screenRender.moveRight();
		} else if ("r".equals(action)) {
			eventController.needReset.set(true);
		} else if ("auto".equals(action)) {
			if (eventController.autoReset.get()) {
				eventController.autoReset.set(false);
			} else {
				eventController.autoReset.set(true);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		// Set terminal to raw mode
		Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "stty -echo -icanon < /dev/tty"}).waitFor();

		// Restore terminal settings on exit
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "stty sane < /dev/tty"}).waitFor();
			} catch (Exception ignored) {}
		}));

		InputStream in = System.in;

		// Game objects
		AtomicInteger autoSpeed = new AtomicInteger(DEFAULT_SPEED);
		MapData map = new MapData();
		ScreenRender screenRender = new ScreenRender(map);
		StringBuilder typed = screenRender.prompt;
		EventController eventController = new EventController(map, screenRender);
		eventController.init();

		if (args.length > 0 && "auto".equals(args[0])) {
			eventController.autoReset.set(true);
		}


		Thread gameThread = new Thread(() -> {
			while (true) {
				eventController.checkReset();
				// next turn
				try {
					eventController.loop();
				}catch (Exception ex){
					ex.printStackTrace();
					System.exit(1);
				}
				try {
					Thread.sleep(autoSpeed.get());
				} catch (InterruptedException ignored) {}
			}
		});
		gameThread.setDaemon(true);
		gameThread.start();

		// Background thread to update display
		Thread displayThread = new Thread(() -> {
			while (true) {
				System.out.print("\033[H\033[2J");
				System.out.flush();
				System.out.println(screenRender.screenText.get());
				try {
					Thread.sleep(100);
				} catch (InterruptedException ignored) {}
			}
		});
		displayThread.setDaemon(true);
		displayThread.start();

		// Read character-by-character
		while (true) {
			int ch = in.read();
			if (ch == 3) break; // Ctrl+C to exit
			if (ch == '\n' || ch == '\r') {// Enter clears typed input
				handleInput(typed.toString(), autoSpeed, screenRender, eventController);
				typed.setLength(0);
			} else {
				if (ch == 's' || ch == 'h' || ch == 'j' || ch == 'k' || ch == 'l') {
					handleInput(String.valueOf((char) ch), autoSpeed, screenRender, eventController);
				} else {
					typed.append((char) ch);
				}
			}
		}
	}
}
