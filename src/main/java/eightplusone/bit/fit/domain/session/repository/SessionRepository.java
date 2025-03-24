package eightplusone.bit.fit.domain.session.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import eightplusone.bit.fit.domain.session.entity.Session;
import io.lettuce.core.dynamic.annotation.Param;

public interface SessionRepository extends JpaRepository<Session, Long>, SessionRepositoryCustom {
	Optional<Session> findByAudioChannel(Integer audioChannel);

	Page<Session> findAll(Pageable pageable);

	List<Session> findByIsLiveTrue();

	@Modifying
	@Query("""
		update Session s set s.isLive = :isLive where s.audioChannel = :audioChannel
		""")
	void updateIsLiveByAudioChannel(@Param("audioChannel") Integer audioChannel, @Param("isLive") boolean isLive);
}
