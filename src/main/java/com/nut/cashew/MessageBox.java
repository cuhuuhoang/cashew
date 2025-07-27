package com.nut.cashew;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import static com.nut.cashew.BoxHelper.bottomBorder;
import static com.nut.cashew.BoxHelper.topBorder;

public class MessageBox {
	private final String name;
	private final int width;
	private final int height;
	private final List<String> messages;

	public MessageBox(String name, int width, int height) {
		this.name = name;
		this.width = width;
		this.height = height;
		this.messages = new LinkedList<>();
	}

	private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");

	public void addMessage(String message) {
		String stripped = stripAnsi(message);
		if (stripped.length() > width) {
			int cutIndex = findCutIndex(message, width);
			messages.add(padRight(message.substring(0, cutIndex), width));
			addMessage("-" + message.substring(cutIndex));
		} else {
			messages.add(padRight(message, width));
		}
	}

	private String stripAnsi(String input) {
		return ANSI_PATTERN.matcher(input).replaceAll("");
	}

	private int findCutIndex(String input, int visibleWidth) {
		int visibleCount = 0;
		int index = 0;
		boolean inAnsi = false;

		while (index < input.length() && visibleCount < visibleWidth) {
			char c = input.charAt(index);
			if (c == '\u001B') {
				inAnsi = true;
			}
			if (!inAnsi) {
				visibleCount++;
			}
			if (inAnsi && c == 'm') {
				inAnsi = false;
			}
			index++;
		}
		return index;
	}

	private String padRight(String input, int targetVisibleLength) {
		String stripped = stripAnsi(input);
		int padLength = targetVisibleLength - stripped.length();
		return input + " ".repeat(Math.max(0, padLength));
	}


	public void clear() {
		messages.clear();
	}

	public List<String> box() {
		List<String> result = new LinkedList<>();
		result.add(topBorder(name, width));
		int messageDisplay = height;
		int msgDisplay = Math.min(messageDisplay, messages.size());
		for (int i = messages.size() - msgDisplay; i < messages.size(); i++) {
			StringBuilder sb = new StringBuilder();
			sb.append("│").append(messages.get(i)).append("│");
			result.add(sb.toString());
		}

		for (int i = msgDisplay; i < messageDisplay; i++) {
			StringBuilder sb = new StringBuilder();
			sb.append("│").append(" ".repeat(width)).append("│");
			result.add(sb.toString());
		}
		result.add(bottomBorder(width));
		return result;
	}

	public boolean isEmpty() {
		return messages.isEmpty();
	}
}
