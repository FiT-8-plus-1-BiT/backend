package eightplusone.bit.fit.domain.session.dto;

import eightplusone.bit.fit.domain.session.entity.Session;
import eightplusone.bit.fit.domain.speaker.entity.Speaker;
import eightplusone.bit.fit.domain.tag.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScoredSession implements Comparable<ScoredSession> {
	private final int score;
	private final Session session;
	private final Tag tag;
	private final Speaker speaker;

	@Override
	public int compareTo(ScoredSession other) {
		return Integer.compare(other.score, this.score);
	}
}