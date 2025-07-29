package com.nut.cashew;

public class Simulator {
	public static void main(String[] args) {
		MapData map = new MapData();
		PlayerSet playerSet = new PlayerSet(map);
		int turn = 10000;
		for (int i = 0; i <= turn; i++) {
			if (i % 1000 == 0) {
				System.out.println(String.format("%02d:%02d:%02d", i / 3600, (i % 3600) / 60, i % 60) + " Turn: " + i);
			}
			playerSet.addAction();
			playerSet.doAction();
			playerSet.startTurn();

		}
		playerSet.setRank();
		playerSet.rankBox.box().forEach(System.out::println);
		playerSet.globalBox.box().forEach(System.out::println);
	}

}
