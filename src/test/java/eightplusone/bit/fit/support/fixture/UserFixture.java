package eightplusone.bit.fit.support.fixture;

import eightplusone.bit.fit.domain.auth.enums.Role;
import eightplusone.bit.fit.domain.user.entity.User;
import lombok.Getter;

@Getter
public enum UserFixture {
	USER_FIXTURE_1("google_test1", "test@gmail.com", "홍길동", "백엔드", 1, "데이터베이스", Role.USER),
	USER_FIXTURE_2("google_test2", "test2@gmail.com", "존도", "프론트엔드", 2, "협업", Role.USER),
	USER_FIXTURE_3("google_test3", "test3@gmail.com", "제인도", "데브옵스", 3, "클라우드", Role.USER);

	private final String provider;
	private final String email;
	private final String name;
	private final String job;
	private final Integer years;
	private final String interests;
	private final Role role;

	UserFixture(String provider, String email, String name, String job, Integer years, String interests, Role role) {
		this.provider = provider;
		this.email = email;
		this.name = name;
		this.job = job;
		this.years = years;
		this.interests = interests;
		this.role = role;
	}

	public User createUser() {
		return User.builder()
			.provider(provider)
			.email(email)
			.name(name)
			.job(job)
			.years(years)
			.interests(interests)
			.role(role)
			.build();
	}
}
