package eightplusone.bit.fit.domain.session.entity;

import eightplusone.bit.fit.domain.mysession.entity.MySession;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

	@Column(nullable = false)
	private Long lectureDuration; // 강연 시간 (초 단위)

	@OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<MySession> mySessions = new ArrayList<>();

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
		this.lectureDuration = Duration.between(startTime, endTime).getSeconds(); // 강연 시간 저장
	}

	// 강연 시간이 변경될 경우 업데이트
	public void updateSessionTime(LocalDateTime startTime, LocalDateTime endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.lectureDuration = Duration.between(startTime, endTime).getSeconds();
	}
}
