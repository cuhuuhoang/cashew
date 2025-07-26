package com.nut.cashew;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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

	public void addMessage(String message) {
		if (message.length() > width) {
			messages.add(message.substring(0, width));
			addMessage("-" + message.substring(width));
		} else {
			messages.add(message + " ".repeat(width - message.length()));
		}
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
}
