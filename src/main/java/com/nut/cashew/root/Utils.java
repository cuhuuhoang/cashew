package com.nut.cashew.root;

import org.javatuples.Pair;

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
}
