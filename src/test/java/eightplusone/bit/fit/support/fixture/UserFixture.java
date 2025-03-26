package eightplusone.bit.fit.support.fixture;

import eightplusone.bit.fit.domain.auth.enums.Role;
import eightplusone.bit.fit.domain.user.entity.User;
import eightplusone.bit.fit.domain.user.entity.enums.YearLevel;
import lombok.Getter;

@Getter
public enum UserFixture {
	USER_FIXTURE_1("google_test1", "test@gmail.com", "홍길동", "백엔드", YearLevel.NEWBIE, Role.USER),
	USER_FIXTURE_2("google_test2", "test2@gmail.com", "존도", "프론트엔드", YearLevel.JUNIOR, Role.USER),
	USER_FIXTURE_3("google_test3", "test3@gmail.com", "제인도", "데브옵스", YearLevel.SENIOR, Role.USER);

	private final String provider;
	private final String email;
	private final String name;
	private final String job;
	private final YearLevel years;
	private final Role role;

	UserFixture(String provider, String email, String name, String job, YearLevel years, Role role) {
		this.provider = provider;
		this.email = email;
		this.name = name;
		this.job = job;
		this.years = years;
		this.role = role;
	}

	public User createUser() {
		return User.builder()
			.provider(provider)
			.email(email)
			.name(name)
			.job(job)
			.years(years)
			.role(role)
			.build();
	}
}
