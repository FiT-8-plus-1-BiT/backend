package eightplusone.bit.fit.domain.session.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import eightplusone.bit.fit.domain.mysession.entity.MySession;
import eightplusone.bit.fit.domain.speaker.entity.Speaker;
import eightplusone.bit.fit.domain.tag.entity.Tag;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "sessions")
@NoArgsConstructor
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

	@OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<MySession> mySessions = new ArrayList<>();

	@OneToOne(fetch = FetchType.LAZY)
	private Tag tag;

	@OneToOne(fetch = FetchType.LAZY)
	private Speaker speaker;

	@Builder
	public Session(String title, String sessionImage, String summary, LocalDateTime startTime, LocalDateTime endTime,
		Integer standardCount, Integer audioChannel) {
		this.title = title;
		this.sessionImage = sessionImage;
		this.summary = summary;
		this.startTime = startTime;
		this.endTime = endTime;
		this.standardCount = standardCount;
		this.audioChannel = audioChannel;
	}
}
