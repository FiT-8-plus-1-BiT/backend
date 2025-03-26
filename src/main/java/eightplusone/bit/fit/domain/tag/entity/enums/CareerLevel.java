package eightplusone.bit.fit.domain.tag.entity.enums;

import java.util.List;

import lombok.Getter;

public enum CareerLevel {
	NEWBIE("신입", "초급"),
	JUNIOR("주니어", "초급"),
	MIDDLE("미들", "중급"),
	SENIOR("시니어", "고급");

	private final String careerLevel;
	@Getter
	private final String tagLevel;

	CareerLevel(String careerLevel, String tagLevel) {
		this.careerLevel = careerLevel;
		this.tagLevel = tagLevel;
	}

	public static String mapToTagLevel(String input) {
		for (CareerLevel level : values()) {
			if (level.careerLevel.equalsIgnoreCase(input)) {
				return level.getTagLevel();
			}
		}
		return "초급"; // default value
	}

	public static int calculateScore(String userLevel, String tagLevel) {
		if (userLevel.equals(tagLevel))
			return 2;

		List<String> levels = List.of("초급", "중급", "고급");
		int diff = Math.abs(levels.indexOf(userLevel) - levels.indexOf(tagLevel));
		return diff == 1 ? 1 : 0;
	}
}
