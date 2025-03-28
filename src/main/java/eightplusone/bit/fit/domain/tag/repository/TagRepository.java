package eightplusone.bit.fit.domain.tag.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import eightplusone.bit.fit.domain.tag.entity.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {
	Tag findBySession_SessionId(Long sessionId);
}
