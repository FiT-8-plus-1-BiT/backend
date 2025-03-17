package eightplusone.bit.fit.domain.user.entity;

import eightplusone.bit.fit.domain.auth.enums.Role;
import eightplusone.bit.fit.global.base.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long id;

	@Column(name = "email", length = 30, unique = true)
	private String email;

	@Column(name = "name", length = 20, nullable = false)
	private String name;

	@Column(name = "provider", length = 100, unique = true)
	private String provider;

	@Column(name = "job", length = 20)
	private String job;

	@Column(name = "years")
	private Integer years;

	@Column(name = "interests", length = 30)
	private String interests;

	@Column(name = "image", length = 200)
	private String image;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", length = 20, nullable = false)
	private Role role;

	@Builder
	private User(String email, String name, String provider, String job, Integer years, String interests, Role role) {
		this.email = email;
		this.name = name;
		this.provider = provider;
		this.job = job;
		this.years = years;
		this.interests = interests;
		this.role = role;
	}

	public static User of(String email, String name, String provider, Role role) {
		return User.builder()
			.email(email)
			.name(name)
			.provider(provider)
			.role(role)
			.build();
	}

	public void updateProfileInfo(String job, Integer years, String interests) {
		this.job = job;
		this.years = years;
		this.interests = interests;
	}
}
