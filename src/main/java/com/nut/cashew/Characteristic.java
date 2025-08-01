package com.nut.cashew;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
@AllArgsConstructor
public class Characteristic {
	double aggressive;
	double crazy;
	double careful;
	double leadership;
	double greedy;

	public List<String> print() {
		List<String> result = new LinkedList<>();
		result.add(String.format("Ag: %.2f, Cz: %.2f, Cf: %.2f",
				aggressive, crazy, careful));
		result.add(String.format("Ld: %.2f, Gd: %.2f",
				leadership, greedy));
		return result;
	}
}
