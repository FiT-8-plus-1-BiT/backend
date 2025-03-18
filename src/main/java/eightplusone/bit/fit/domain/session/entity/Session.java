package eightplusone.bit.fit.domain.session.entity;

import java.time.LocalDateTime;

import eightplusone.bit.fit.domain.speaker.entity.Speaker;
import eightplusone.bit.fit.domain.tag.entity.Tag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
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
	private Integer audioChannel;

	@OneToOne(fetch = FetchType.LAZY)
	private Tag tag;

	@OneToOne(fetch = FetchType.LAZY)
	private Speaker speaker;
}
