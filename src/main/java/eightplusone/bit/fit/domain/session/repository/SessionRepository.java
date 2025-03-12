package eightplusone.bit.fit.domain.session.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import eightplusone.bit.fit.domain.session.entity.Session;

public interface SessionRepository extends JpaRepository<Session, Long> {
	Optional<Session> findByAudioChannel(Integer audioChannel);
}
