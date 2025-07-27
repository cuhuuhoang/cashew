package com.nut.cashew;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Characteristic {
	double aggressive;
	double crazy;

	public String toString() {
		return String.format("agg: %.2f, crazy: %.2f", aggressive, crazy);
	}
}
