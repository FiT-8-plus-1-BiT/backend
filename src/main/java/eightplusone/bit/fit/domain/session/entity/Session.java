package eightplusone.bit.fit.domain.session.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name = "sessions")
public class Session {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long sessionId;

	@Column(nullable = false, length = 50)
	private String title;

	@Column(nullable = false, length = 200)
	private String sessionImage;

	@Column(nullable = false, length = 200)
	private String summary;

	@Column(nullable = false)
	private LocalDateTime startTime;

	@Column(nullable = false)
	private LocalDateTime endTime;

	@Column(nullable = false)
	private Integer standardCount;

	@Column(nullable = false)
	private Integer number;
}
