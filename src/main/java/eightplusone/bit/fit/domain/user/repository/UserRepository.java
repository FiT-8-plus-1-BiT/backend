package eightplusone.bit.fit.domain.user.repository;

import eightplusone.bit.fit.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByProvider(String provider);

	boolean existsByEmail(String email);

	User findLoginUserByEmail(String email);

	Optional<User> findByEmail(String email);
}
