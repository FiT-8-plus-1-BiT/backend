package eightplusone.bit.fit.domain.user.entity.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum YearLevel {
	NEWBIE("신입"),
	JUNIOR("주니어"),
	MIDDLE("미들"),
	SENIOR("시니어");

	private final String yearLevel;

	YearLevel(String yearLevel) {
		this.yearLevel = yearLevel;
	}

	@JsonValue
	public String getYearLevel() {
		return yearLevel;
	}
}
