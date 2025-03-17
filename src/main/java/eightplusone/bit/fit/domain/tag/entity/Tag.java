package eightplusone.bit.fit.domain.tag.entity;

import eightplusone.bit.fit.domain.session.entity.Session;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long tagId;

	@Column(nullable = false, length = 20)
	private String field;

	@Column(nullable = false, length = 20)
	private String topic;

	@Column(nullable = false, length = 20)
	private String type;

	@Column(nullable = false, length = 5)
	private String level;

	@OneToOne
	@JoinColumn(name = "session_id", nullable = false)
	private Session session;
}
