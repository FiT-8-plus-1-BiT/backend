package eightplusone.bit.fit.domain.user.entity;

import java.util.ArrayList;
import java.util.List;

import eightplusone.bit.fit.domain.auth.enums.Role;
import eightplusone.bit.fit.domain.interest.entity.MyInterest;
import eightplusone.bit.fit.domain.mysession.entity.MySession;
import eightplusone.bit.fit.domain.user.entity.enums.YearLevel;
import eightplusone.bit.fit.global.base.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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

	@Enumerated(EnumType.STRING)
	@Column(name = "years")
	private YearLevel years;

	@Column(name = "image_name")
	private String imageName;

	@Column(name = "image_url")
	private String imageUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", length = 20, nullable = false)
	private Role role;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<MySession> mySessions = new ArrayList<>();

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<MyInterest> myInterests = new ArrayList<>();

	@Builder
	private User(String email, String name, String provider, String job, YearLevel years, Role role) {
		this.email = email;
		this.name = name;
		this.provider = provider;
		this.job = job;
		this.years = years;
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

	public void updateProfileInfo(String name, String job, YearLevel years) {
		this.name = name;
		this.job = job;
		this.years = years;
	}

	public void updateProfileImage(String imageName, String imageUrl) {
		this.imageName = imageName;
		this.imageUrl = imageUrl;
	}

	public void deleteProfileImage() {
		this.imageName = null;
		this.imageUrl = null;
	}
}
