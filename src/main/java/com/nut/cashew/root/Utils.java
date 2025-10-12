package com.nut.cashew.root;

import org.javatuples.Pair;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

	public static Pair<Integer, String> parsePromptPair(String input) {
		Matcher m = Pattern.compile("^(\\d+)(.*)$").matcher(input);
		if (m.matches()) {
			return new Pair<>(Integer.parseInt(m.group(1)), m.group(2));
		} else {
			return new Pair<>(1, input);
		}
	}

	public static String generateRandomAnsiColor() {
		Random rand = new Random();
		int r = rand.nextInt(256);
		int g = rand.nextInt(256);
		int b = rand.nextInt(256);
		return String.format("\u001B[38;2;%d;%d;%dm", r, g, b); // ANSI 24-bit color code
	}

	public static double distance(int x1, int y1, int x2, int y2) {
		return Math.abs(x2 - x1) + Math.abs(y2 - y1);
	}
}
