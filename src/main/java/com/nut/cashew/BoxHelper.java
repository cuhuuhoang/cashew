package com.nut.cashew;

public class BoxHelper {

	public static String topBorder(String name, int width) {
		return "╭─ " + name + " " + "─".repeat(width - name.length() - 3) + "╮";
	}

	public static String bottomBorder(int width) {
		return "╰" + "─".repeat(width) + "╯";
	}

}
