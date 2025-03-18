package eightplusone.bit.fit.domain.session.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import eightplusone.bit.fit.domain.session.entity.Session;

public interface SessionRepository extends JpaRepository<Session, Long> {
	Optional<Session> findByAudioChannel(Integer audioChannel);

	@Query("SELECT s FROM Session s JOIN FETCH s.speaker JOIN FETCH s.tag")
	List<Session> findAllWithSpeakerAndTag();
}
