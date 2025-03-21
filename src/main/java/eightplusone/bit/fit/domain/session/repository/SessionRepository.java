package eightplusone.bit.fit.domain.session.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import eightplusone.bit.fit.domain.session.entity.Session;

public interface SessionRepository extends JpaRepository<Session, Long>, SessionRepositoryCustom {
	Optional<Session> findByAudioChannel(Integer audioChannel);

	Page<Session> findAll(Pageable pageable);
}
