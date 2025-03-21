package eightplusone.bit.fit.domain.speaker.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import eightplusone.bit.fit.domain.speaker.entity.Speaker;

public interface SpeakerRepository extends JpaRepository<Speaker, Long> {
}
