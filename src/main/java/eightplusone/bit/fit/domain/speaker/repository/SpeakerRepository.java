package eightplusone.bit.fit.domain.speaker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import eightplusone.bit.fit.domain.speaker.entity.Speaker;

public interface SpeakerRepository extends JpaRepository<Speaker, Long> {
	@Query("select sp from Speaker sp join fetch sp.session")
	List<Speaker> findAllWithSession();
}
