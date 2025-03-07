package eightplusone.bit.fit.support.fixture;

import eightplusone.bit.fit.domain.auth.enums.Role;
import eightplusone.bit.fit.domain.user.entity.User;
import lombok.Getter;

@Getter
public enum UserFixture {
	USER_FIXTURE_1("google_test1", "test@gmail.com", "홍길동", Role.USER),
	USER_FIXTURE_2("google_test2", "test2@gmail.com", "존도", Role.USER),
	USER_FIXTURE_3("google_test3", "test3@gmail.com", "제인도", Role.USER);

	private final String provider;
	private final String email;
	private final String name;
	private final Role role;

	UserFixture(String provider, String email, String name, Role role) {
		this.provider = provider;
		this.email = email;
		this.name = name;
		this.role = role;
	}

	public User createUser() {
		return User.builder()
			.provider(provider)
			.email(email)
			.name(name)
			.role(role)
			.build();
	}
}
