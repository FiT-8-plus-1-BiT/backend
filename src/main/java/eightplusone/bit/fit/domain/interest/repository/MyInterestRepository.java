package eightplusone.bit.fit.domain.interest.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import eightplusone.bit.fit.domain.interest.entity.MyInterest;

public interface MyInterestRepository extends JpaRepository<MyInterest, Long> {
	@Query("""
		select mi from MyInterest mi left join fetch mi.interest where mi.user.id = :userId
		""")
	List<MyInterest> findByUserIdWithInterests(Long userId);
}
