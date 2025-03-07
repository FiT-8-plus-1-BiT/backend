package eightplusone.bit.fit.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import eightplusone.bit.fit.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByProvider(String provider);

	boolean existsByEmail(String email);

	User findLoginUserByEmail(String email);
}
