package com.nut.cashew.leaf;

public class Factory {

	public final Bot bot;
	public int level = 1;

	public Factory(Bot bot) {
		this.bot = bot;
	}

	public String render() {
		return "\u001B[32m⌂\u001B[0m";
	}
}
