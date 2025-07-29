package com.nut.cashew;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Characteristic {
	double aggressive;
	double crazy;
	double careful;

	public String toString() {
		return String.format("Ag: %.2f, Cz: %.2f, Cf: %.2f",
				aggressive, crazy, careful);
	}
}
