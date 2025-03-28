package eightplusone.bit.fit.domain.interest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import eightplusone.bit.fit.domain.interest.entity.Interest;

public interface InterestRepository extends JpaRepository<Interest, Long> {

	Interest findByName(String name);
}
