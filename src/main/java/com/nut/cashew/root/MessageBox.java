package com.nut.cashew.root;

import lombok.Getter;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MessageBox {
	private final String name;
	private final int width;
	@Getter
	private final int height;
	private final List<String> messages;

	public MessageBox(String name, int width, int height) {
		this.name = name;
		this.width = width;
		this.height = height;
		this.messages = new LinkedList<>();
	}

	private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");

	public void addTimeMessage(String message) {
		String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
		addMessage(String.format("%s: %s", time, message));
	}
	
	private void addSubMessage(String message) {
		String stripped = stripAnsi(message);
		if (stripped.length() > width) {
			int cutIndex = findCutIndex(message, width);
			messages.add(padRight(message.substring(0, cutIndex), width));
			addMessage("-" + message.substring(cutIndex));
		} else {
			messages.add(padRight(message, width));
		}
	}

	private static final int MAX_MESSAGES = 100;

	public void addMessage(String message) {
		addSubMessage(message);
		while (messages.size() > MAX_MESSAGES) {
			messages.remove(0);
		}
	}

	public static String stripAnsi(String input) {
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

	public static String topBorder(String name, int width) {
		return "╭─ " + name + " " + "─".repeat(width - name.length() - 3) + "╮";
	}

	public static String bottomBorder(int width) {
		return "╰" + "─".repeat(width) + "╯";
	}


	public boolean isEmpty() {
		return messages.isEmpty();
	}

	@SafeVarargs
	public static List<String> combineColumns(List<String>... lists) {
		List<String> result = new LinkedList<>();
		int maxHeight = Arrays.stream(lists).mapToInt(List::size).max().orElse(0);
		List<Integer> widthList = Arrays.stream(lists).map(strings -> strings.get(0).length())
				.collect(Collectors.toList());
		for (int i = 0; i < maxHeight; i++) {
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j < lists.length; j++) {
				List<String> list = lists[j];
				if (i < list.size()) {
					sb.append(list.get(i));
				} else {
					sb.append(" ".repeat(widthList.get(j)));
				}
			}
			result.add(sb.toString());
		}
		return result;
	}

	@SafeVarargs
	public static List<String> combineRows(List<String>... lists) {
		List<String> result = new LinkedList<>();
		int maxWidth = Arrays.stream(lists).mapToInt(strings -> strings.get(0).length()).max().orElse(0);
		for(List<String> list : lists) {
			for (String s : list) {
				StringBuilder sb = new StringBuilder();
				sb.append(s);
				int length = MessageBox.stripAnsi(s).length();
				if (length < maxWidth) {
					sb.append(" ".repeat(maxWidth - length));
				}
				result.add(sb.toString());
			}
		}
		return result;
	}
}
