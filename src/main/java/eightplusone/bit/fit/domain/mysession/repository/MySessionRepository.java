package eightplusone.bit.fit.domain.mysession.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import eightplusone.bit.fit.domain.mysession.entity.MySession;
import eightplusone.bit.fit.domain.mysession.enums.MySessionType;

public interface MySessionRepository extends JpaRepository<MySession, Long> {
	@Query(""" 
		select ms from MySession ms left join fetch ms.session where ms.user.id = :userId and ms.type = :type 
		""")
	List<MySession> findSessionsByUserIdAndType(Long userId, MySessionType type);

	@Modifying
	@Query(""" 
		delete from MySession ms where ms.user.id = :userId and ms.session.sessionId = :sessionId and ms.type = :type
		""")
	int deleteByUserIdAndSessionIdAndType(Long userId, Long sessionId, MySessionType type);

	List<MySession> findByUserId(Long userId);
}
